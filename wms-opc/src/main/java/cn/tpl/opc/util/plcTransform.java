package cn.tpl.opc.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class plcTransform {


    public void receive_plc_data() throws IOException {

        Socket socket = new Socket("192.0.2.19",6000);
        InputStream is;
        while(true){
            is = socket.getInputStream();
            //plc发送22位数据，plc一位对应byte数组中四位
            byte[] b = new byte[88];
            int readLength = -1;
            if((readLength = is.read(b,0,88)) != -1){
                //转16进制
                String result = bytesToHexString_b(b);
                //转换成10进制
                List<Integer> state_list = dataProcess(result);
                //业务处理。。。
            }

        }

    }


    public static List<Integer> dataProcess(String result) {
        List<Integer> list = new ArrayList<>();
        //字符串长度
        int string_len = result.length();
        for (int i =0;i< string_len;){
            String string_poi = "";
            if (4+i > string_len){
                string_poi =result.substring(i);
            }else {
                string_poi = result.substring(i,4+i);
            }
            String data_index_one = string_poi.substring(0,2);
            String data_index_two = string_poi.substring(2,4);
            Integer number = Integer.parseInt(data_index_two+data_index_one,16);
            list.add(number);
            i+=4;
        }
        return list;
    }


    private static String bytesToHexString_b(byte[] bytes){

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }


    //byte[] 转 Byte[]
    public static Byte[] toObjects(byte[] bytesPrim) {
        Byte[] bytes = new Byte[bytesPrim.length];
        int i = 0;
        for (byte b : bytesPrim) bytes[i++] = b; // Autoboxing
        return bytes;
    }


    //Byte[] 转 byte[]
    public static byte[] toPrimitives(Byte[] oBytes) {
        byte[] bytes = new byte[oBytes.length];
        for (int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }
        return bytes;
    }

    //byte转十六进制字符
    public static String byteToHex(byte b) {
        String hex = Integer.toHexString(b & 0xFF);
        if (hex.length() == 1) {
            hex = '0' + hex;
        }
        return hex.toUpperCase(Locale.getDefault());
    }

    //byte[] 转 字符串的bit
    public static String byteToBit(byte[] bs) {
        String result = "";
        for (byte b : bs) {
            result = result
                    + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1)
                    + (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1)
                    + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)
                    + (byte) ((b >> 1) & 0x1) + (byte) ((b >> 0) & 0x1);
        }
        return result;
    }

    //String 转 byte[]
    public static byte[] stringToByteArr(String value, String decode) {
        try {
            return value.getBytes(decode);
        } catch (UnsupportedEncodingException e) {

        }
        return null;
    }

    //byte[] 转 String
    public static String byteArrToString(byte[] arr, String decode) {
        try {
            if (arr.length == 0) {
                return null;
            }
            return new String(arr, decode);
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

    //byte[] 转 数值
    public static Number byteArrToNumber(byte[] arr, boolean isBig, int trim) {
        if (arr == null || arr.length == 0) {
            return null;
        }
        // 舍弃掉数组长度超过8的部分
        if (arr.length > trim) {
            for (int i = trim; i < arr.length; i++) {
                arr[i] = 0;
            }
        }
        long total = 0;
        for (int i = 0; i < arr.length; i++) {
            long arrVal = arr[i] & 0xFF;
            arrVal = isBig ? arrVal << ((arr.length - i - 1) * 8) : arrVal << ((i) * 8);
            total |= arrVal;
        }
        return total;
    }

    //数值 转 byte[]
    public static byte[] numberToByteArr(Number number, boolean isBig, int trim) {
        if (number == null) {
            return null;
        }
        if (trim <= 0) {
            return null;
        }
        long value = number.longValue();
        byte[] arr = new byte[trim];
        for (int i = 0; i < trim; i++) {
            long val = value >> (i * 8);
            val &= 0xff;
            if (isBig) {
                arr[trim - i - 1] = (byte) val;
            } else {
                arr[i] = (byte) val;
            }
        }
        return arr;
    }
    //十六进制字符串转byte数组
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] ba = new byte[len / 2];

        for(int i = 0; i < ba.length; ++i) {
            int j = i * 2;
            int t = Integer.parseInt(s.substring(j, j + 2), 16);
            byte b = (byte)(t & 255);
            ba[i] = b;
        }

        return ba;
    }
    //16进制字符串转ASCII码
    public static String convertHexToASC(String hex){
        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        for( int i=0; i<hex.length()-1; i+=2 ){
            String output = hex.substring(i, (i + 2));
            int decimal = Integer.parseInt(output, 16);
            sb.append((char)decimal);
            temp.append(decimal);
        }
        return sb.toString();
    }

    //byte数组转十六进制字符串以小写字母形式显示
    public static String ByteArrayToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
    //ASCII码转十六进制字符串
    public static String ASCToHex(String str){
        char[] chars = str.toCharArray();
        StringBuffer hex = new StringBuffer();
        for(int i = 0; i < chars.length; i++){
            hex.append(Integer.toHexString((int)chars[i]));
        }
        return hex.toString();
    }


    }




