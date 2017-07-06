package org.ubibots.motiondectction.util;

/**
 * Created by Michael on 2016/11/7 0007.
 */

public class Tools {
    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if(hexString.equals("0")){
                hexString="00";
            }
            if(hexString.length()==1){
                hexString = "0" + hexString;
            }
            result += hexString.toUpperCase()+"  ";
        }
        return result;
    }
    public static String saveData(byte[] command){
        String splitChart = "#";
        String data = new String(command);
        return data;
    }

    public static byte[] loadData(String data){
        byte[] temp =data.getBytes();
        byte[] load = new byte[16];
        load[0]=(byte)0xFE;
        load[1]=(byte)0xF9;

        for(int i = 2;i<16;i++){
            load[i] = temp[i+4];
        }
        return load;
    }

}
