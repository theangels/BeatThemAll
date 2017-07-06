package org.ubibots.motiondectction;

import android.Manifest;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

@SuppressWarnings("deprecation")
public class Bluetooth extends TabActivity {
    private final static String TAG = "TAG";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Opencv not loaded");

        } else {
            Log.d(TAG, "Opencv loaded");

        }
    }

    /**
     * Called when the activity is first created.
     */

    enum ServerOrCilent {
        NONE,
        SERVICE,
        CILENT
    }

    ;

    private Context mContext;
    static AnimationTabHost mTabHost;
    static String BlueToothAddress = "null";
    static ServerOrCilent serviceOrCilent = ServerOrCilent.NONE;
    static boolean isOpen = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mContext = this;
        setContentView(R.layout.main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }

        //实例化
        mTabHost = (AnimationTabHost) getTabHost();
        mTabHost.addTab(mTabHost.newTabSpec("Tab1")
                .setIndicator("设备列表", getResources().getDrawable(android.R.drawable.ic_menu_add))
                .setContent(new Intent(mContext, DeviceActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("Tab2").
                setIndicator("对话列表", getResources().getDrawable(android.R.drawable.ic_menu_add))
                .setContent(new Intent(mContext, ChatActivity.class)));

        mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
            public void onTabChanged(String tabId) {
                // TODO Auto-generated method stub
                if (tabId.equals("Tab1")) {
                }
            }
        });
        mTabHost.setCurrentTab(0);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Toast.makeText(mContext, "address:", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onDestroy() {
        /* unbind from the service */
        super.onDestroy();
    }

    public class SiriListItem {
        String message;
        boolean isSiri;

        public SiriListItem(String msg, boolean siri) {
            message = msg;
            isSiri = siri;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // Permission Denied
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}