package org.ubibots.motiondectction;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.ubibots.motiondectction.Bluetooth.ServerOrCilent;
import org.ubibots.motiondectction.model.Arm7Bot;
import org.ubibots.motiondectction.util.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


import android.view.MotionEvent;
import android.view.SurfaceView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8U;

public class ChatActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    //7-bots
    private static boolean canRecognition = false;
    private static int type = 0;//0还没开始   1移动到hit点  2移动到wait点
    private static int wait = 6;

    private Button positionAButton;
    private Button positionBButton;
    private Button positionCButton;
    private Button positionDButton;

    private clientThread clientConnectThread = null;
    private BluetoothSocket socket = null;
    private BluetoothDevice device = null;
    private readThread mReadThread = null;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    SharedPreferences saveData;
    SharedPreferences loadData;

    Arm7Bot arm7Bot = new Arm7Bot();

    Context mContext;
    //	private RockerView myRock;
    private Handler updateUI;

    byte[] motorPosition = {(byte) 0xfe, (byte) 0xf9, 0x01, 0x77, 0x03, 0x77, 0x02, 0x11, 0x01, 0x77, 0x02, 0x11, 0x02, 0x57, 0x02, 0x74};
    byte[] positionA = {(byte) 0xfe, (byte) 0xf9, 0x03, 0x74, 0x03, 0x74, 0x02, 0x69, 0x03, 0x74, 0x03, 0x74, 0x03, 0x74, 0x01, 0x48};
    byte[] positionB = {(byte) 0xfe, (byte) 0xf9, 0x03, 0x74, 0x03, 0x74, 0x02, 0x69, 0x03, 0x74, 0x03, 0x74, 0x03, 0x74, 0x01, 0x48};
    byte[] positionC = {(byte) 0xfe, (byte) 0xf9, 0x03, 0x74, 0x03, 0x74, 0x02, 0x69, 0x03, 0x74, 0x03, 0x74, 0x03, 0x74, 0x01, 0x48};
    byte[] positionD = {(byte) 0xfe, (byte) 0xf9, 0x03, 0x74, 0x03, 0x74, 0x02, 0x69, 0x03, 0x74, 0x03, 0x74, 0x03, 0x74, 0x01, 0x48};
    byte[] positionRest = {(byte) 0xfe, (byte) 0xf9, 0x03, 0x74, 0x03, 0x74, 0x02, 0x69, 0x03, 0x74, 0x03, 0x74, 0x03, 0x74, 0x01, 0x48};

    //OpenCV
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private JavaCameraView openCvCameraView;
    private Point[] borderPoint;
    private boolean isBorder = false;
    private BackgroundSubtractorMOG2 pMOG4;
    private Mat mogImg;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        mContext = this;

        initOpenCV();
        init7Bot();
    }

    private void initOpenCV() {
        borderPoint = new Point[3];
        checkCamera();
    }

    private void init7Bot() {
        final TextView info = (TextView) findViewById(R.id.txt_info);

        final Button goBackButton = (Button) findViewById(R.id.goBackButton);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageHandle(positionRest);
                wait = 0;
            }
        });
        goBackButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                System.arraycopy(arm7Bot.getCalibrationMotorPosition(), 0, positionRest, 0, arm7Bot.getCalibrationMotorPosition().length);
                positionRest[14] = 0x03;
                positionRest[15] = 0x20;
                SharedPreferences.Editor editor = saveData.edit();
                editor.putString("rest", Tools.saveData(positionRest));
                editor.apply();
                sendMessageHandle(positionRest);
                return true;
            }
        });

        final Button disconnectButton = (Button) findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) {
                    shutdownClient();
                }
                Bluetooth.isOpen = false;
                Bluetooth.serviceOrCilent = ServerOrCilent.NONE;
                Toast.makeText(mContext, "已断开连接！", Toast.LENGTH_SHORT).show();
            }
        });

        final Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canRecognition = true;
                byte[] speed = {(byte) 0xFE, (byte) 0xF7, 0x49, 0x49, 0x49, 0x49, 0x49, 0x54, 0x47};
                sendMessageHandle(speed);
            }
        });

        final Button freeModeButton = (Button) findViewById(R.id.freeModeButton);
        freeModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageHandle(arm7Bot.getForcelessMode());
            }
        });

        positionAButton = (Button) findViewById(R.id.positionAButton);
        positionAButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageHandle(positionA);
                arm7Bot.setMove(true);
                wait = 0;
                type = 1;
            }
        });
        positionAButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                System.arraycopy(arm7Bot.getCalibrationMotorPosition(), 0, positionA, 0, arm7Bot.getCalibrationMotorPosition().length);
                positionA[14] = 0x03;
                positionA[15] = 0x20;
                SharedPreferences.Editor editor = saveData.edit();
                editor.putString("positionA", Tools.saveData(positionA));
                editor.apply();
                return true;
            }
        });

        positionBButton = (Button) findViewById(R.id.positionBButton);
        positionBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageHandle(positionB);
                type = 1;
                arm7Bot.setMove(true);
                wait = 0;
            }
        });
        positionBButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                System.arraycopy(arm7Bot.getCalibrationMotorPosition(), 0, positionB, 0, arm7Bot.getCalibrationMotorPosition().length);
                positionB[14] = 0x03;
                positionB[15] = 0x20;
                SharedPreferences.Editor editor = saveData.edit();
                editor.putString("positionB", Tools.saveData(positionB));
                editor.apply();
                return true;
            }
        });

        positionCButton = (Button) findViewById(R.id.positionCButton);
        positionCButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageHandle(positionC);
                type = 1;
                arm7Bot.setMove(true);
                wait = 0;
            }
        });
        positionCButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                System.arraycopy(arm7Bot.getCalibrationMotorPosition(), 0, positionC, 0, arm7Bot.getCalibrationMotorPosition().length);
                positionC[14] = 0x03;
                positionC[15] = 0x20;
                SharedPreferences.Editor editor = saveData.edit();
                editor.putString("positionC", Tools.saveData(positionC));
                editor.apply();
                return true;
            }
        });

        positionDButton = (Button) findViewById(R.id.positionDButton);
        positionDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageHandle(positionD);
                type = 1;
                arm7Bot.setMove(true);
                wait = 0;
            }
        });
        positionDButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                System.arraycopy(arm7Bot.getCalibrationMotorPosition(), 0, positionD, 0, arm7Bot.getCalibrationMotorPosition().length);
                positionD[14] = 0x03;
                positionD[15] = 0x20;
                SharedPreferences.Editor editor = saveData.edit();
                editor.putString("positionD", Tools.saveData(positionD));
                editor.apply();
                return true;
            }
        });

        saveData = getSharedPreferences("positionInfo", Context.MODE_PRIVATE);
        loadData = getSharedPreferences("positionInfo", Context.MODE_PRIVATE);

        positionA = Tools.loadData(loadData.getString("positionA", Tools.saveData(motorPosition)));
        positionB = Tools.loadData(loadData.getString("positionB", Tools.saveData(motorPosition)));
        positionC = Tools.loadData(loadData.getString("positionC", Tools.saveData(motorPosition)));
        positionD = Tools.loadData(loadData.getString("positionD", Tools.saveData(motorPosition)));
        positionRest = Tools.loadData(loadData.getString("rest", Tools.saveData(arm7Bot.getMotorPosition())));

        //定义hangdle用来接收线程传来的传感器数据，并对数据进行处理，将其转换成16进制模式。
        updateUI = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1 && (wait++) % 100 > 5) {
                    Bundle temp = (Bundle) msg.obj;
                    int[] receiver = temp.getIntArray("receiver");
                    byte[] ttemp = new byte[receiver != null ? receiver.length : 0];

                    if ((receiver != null ? receiver.length : 0) == 17) {
                        for (int i = 0; i < receiver.length; i++) {
                            ttemp[i] = (byte) receiver[i];
                        }
                        Arm7Bot.analysisReceived(receiver);
                        info.setText(bytesToHexString(ttemp) + " 1 " + String.valueOf(arm7Bot.isMove()) + " 2 " + String.valueOf(canRecognition));

                        if (type == 1 && !canRecognition) {
                            //移动到hit点
                            if (!arm7Bot.isMove()) {
                                type = 2;
                                goBackButton.callOnClick();
                            }
                        } else if (type == 2 && !canRecognition) {
                            if (!arm7Bot.isMove()) {
                                type = 0;
                                canRecognition = true;
                            }
                        }
                    }
                }
            }
        };
    }


    @Override
    public synchronized void onPause() {
        super.onPause();
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
    }


    // 设置横屏并且取消蓝牙
    @Override
    public synchronized void onResume() {
        /**
         * 设置为横屏
         */
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onResume();

        if (Bluetooth.isOpen) {
            Toast.makeText(mContext, "连接已经打开，可以通信。如果要再建立连接，请先断开！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) {
            String address = Bluetooth.BlueToothAddress;
            if (!address.equals("null")) {
                device = mBluetoothAdapter.getRemoteDevice(address);
                clientConnectThread = new clientThread();
                clientConnectThread.start();
                Bluetooth.isOpen = true;
            } else {
                Toast.makeText(mContext, "address is null !", Toast.LENGTH_SHORT).show();
            }
        } else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE) {

            Bluetooth.isOpen = true;
        }
    }


    //初始化内容
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
        if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) {
            shutdownClient();
        }
        Bluetooth.isOpen = false;
        Bluetooth.serviceOrCilent = ServerOrCilent.NONE;
    }


    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (byte aByte : bytes) {
            String hexString = Integer.toHexString(aByte & 0xFF);
            if (hexString.equals("0")) {
                hexString = "00";
            }
            if (hexString.length() == 1) {
                hexString = "0" + hexString;
            }
            result += hexString.toUpperCase() + "  ";
        }
        return result;
    }


    public void hitPosition(int position) {
        if (position == 0) {
            positionAButton.callOnClick();
        } else if (position == 1) {
            positionBButton.callOnClick();
        } else if (position == 2) {
            positionCButton.callOnClick();
        } else {
            positionDButton.callOnClick();
        }
    }


    /**
     * -------------------------------------------权限相关--------------------------------------------------
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera();
            } else {
                // Permission Denied
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    /**
     * -------------------------------------------OpenCV相关--------------------------------------------------
     */
    private void checkCamera() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            initCamera();
        }
    }


    private void initCamera() {
        openCvCameraView = (JavaCameraView) findViewById(R.id.camera);
        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);
        openCvCameraView.setMaxFrameSize(1280, 720);
        openCvCameraView.enableFpsMeter();
        openCvCameraView.enableView();

        openCvCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        Point point = new Point(event.getX(), event.getY());
                        if (borderPoint[2] == null) {
                            borderPoint[0] = point;
                            borderPoint[1] = null;
                            borderPoint[2] = new Point();
                            isBorder = false;
                        } else {
                            borderPoint[1] = point;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        borderPoint[2] = null;
                        isBorder = true;
                        break;
                }
                return true;
            }
        });
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        pMOG4 = new BackgroundSubtractorMOG2();
        mogImg = new Mat();
        tmpMat = new Mat();
        mask = new Mat();
        zeroMat = new Mat();
        zeroMask = new Mat(new Size(width, height), CV_8U, new Scalar(0));
        kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));

    }


    @Override
    public void onCameraViewStopped() {

    }

    private int findMaxIndex(int[] position) {
        int ind = 0;
        for (int i = 1; i < position.length; i++) {
            if (position[ind] < position[i]) {
                ind = i;
            }
        }
        return ind;
    }

    private Mat mask;
    private Mat zeroMat;
    private Mat zeroMask;
    private Mat tmpMat;
    private Mat kernelErode;

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat grayImg = inputFrame.gray();

        if (borderPoint[0] != null && borderPoint[1] != null) {
            zeroMask.copyTo(mask);
            Core.rectangle(mask, borderPoint[0], borderPoint[1], new Scalar(255), -1);

            zeroMat.copyTo(tmpMat);
            grayImg.copyTo(tmpMat, mask);

            tmpMat.copyTo(grayImg);

            if (isBorder) {
                pMOG4.apply(grayImg, mogImg, 0.9);
                Imgproc.erode(mogImg, mogImg, kernelErode);

                List<MatOfPoint> contours = new ArrayList<>();

                Mat hierarchy = tmpMat;
                zeroMat.copyTo(hierarchy);

                Imgproc.findContours(mogImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                if (canRecognition) {
                    int[] positionCnt = {0, 0, 0, 0};
                    if (contours.size() != 0) {
                        MatOfPoint2f contoursPoint2fMat;
                        List<Point> pointList = new ArrayList<>();
                        for (MatOfPoint contour : contours) {
                            contoursPoint2fMat = new MatOfPoint2f();
                            pointList.addAll(contour.toList());
                            contoursPoint2fMat.fromList(pointList);
                            RotatedRect rotatedRect = Imgproc.minAreaRect(contoursPoint2fMat);
                            int position = (int) ((rotatedRect.center.x - borderPoint[0].x) / (borderPoint[1].x - borderPoint[0].x) * 4);
                            if (position >= 0 && position <= 3) {
                                positionCnt[position]++;
                            }
                            contoursPoint2fMat.release();
                            Core.line(grayImg, rotatedRect.center, rotatedRect.center, new Scalar(255), 10);
                            pointList.clear();
                        }
                        for (MatOfPoint contour : contours) {
                            contour.release();
                        }
                    }
                    int hitIndex = findMaxIndex(positionCnt);
                    if (positionCnt[hitIndex] != 0) {
                        hitPosition(hitIndex);
                        canRecognition = false;
                    }
                }
            }
        }
        return grayImg;
    }


    /**
     * -------------------------------------------蓝牙相关--------------------------------------------------
     */
    //发送数据
    private void sendMessageHandle(byte[] msg) {
        if (socket == null) {
            Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            OutputStream os = socket.getOutputStream();
            os.write(msg);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    //读取数据
    private class readThread extends Thread {
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream mmInStream = null;

            try {
                mmInStream = socket.getInputStream();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            while (true) {
                try {
                    // Read from the InputStream
                    if ((bytes = mmInStream != null ? mmInStream.read(buffer) : 0) > 0) {
                        int[] buf_data = new int[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];
                        }
                        Bundle data = new Bundle();
                        data.putIntArray("receiver", buf_data);
                        Message msg = new Message();
                        msg.obj = data;
                        msg.what = 1;
                        updateUI.sendMessage(msg);
                    }
                } catch (IOException e) {
                    try {
                        mmInStream.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        }
    }


    //开启客户端
    private class clientThread extends Thread {
        public void run() {
            try {
                //创建一个Socket连接：只需要服务器在注册时的UUID号
                // socket = device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                //连接
                Message msg2 = new Message();
                msg2.obj = "请稍候，正在连接服务器:" + Bluetooth.BlueToothAddress;
                msg2.what = 0;
                //LinkDetectedHandler.sendMessage(msg2);

                socket.connect();

                Message msg = new Message();
                msg.obj = "已经连接上服务端！可以发送信息。";
                msg.what = 0;
                //LinkDetectedHandler.sendMessage(msg);
                //启动接受数据
                mReadThread = new readThread();
                mReadThread.start();
            } catch (IOException e) {
                Log.e("connect", "", e);
                Message msg = new Message();
                msg.obj = "连接服务端异常！断开连接重新试一试。";
                msg.what = 0;
                //LinkDetectedHandler.sendMessage(msg);
            }
        }
    }


    /* 停止客户端连接 */
    private void shutdownClient() {
        new Thread() {
            public void run() {
                if (clientConnectThread != null) {
                    clientConnectThread.interrupt();
                    clientConnectThread = null;
                }
                if (mReadThread != null) {
                    mReadThread.interrupt();
                    mReadThread = null;
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    socket = null;
                }
            }
        }.start();
    }


    public class deviceListItem {
        String message;
        boolean isSiri;

        public deviceListItem(String msg, boolean siri) {
            message = msg;
            isSiri = siri;
        }
    }
}