package org.eclipse.leshan.server.demo.mt.utils;

import java.lang.reflect.Array;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteUtil {
    
    private static final Logger LOG = LoggerFactory.getLogger(ByteUtil.class);

    public static final int CFG_HEADER_BYTES = 8;
    public static final int VALUE_BYTES = 4;

    //offset fills in LITTLE_ENDIAN last @offset bytes till 4byte array
    public static byte[] getEmptyByteArray(int offset) {
        byte[] rawValue = new byte[CFG_HEADER_BYTES];//empty array for init
        if(offset > 0) { //if int is 2 byte add 2 last byte for parsing
            for( int i = 0; i < offset; i++) {
                rawValue[(CFG_HEADER_BYTES - offset) + i] = 0;
            }
        }
        return rawValue;
    }

    //String with more than 1 byte works only with BIG_ENDIAN  
    public static int bitStringToInt(String b, boolean isSign) {
        int value = 0;
        for (int i = 0; i < b.length(); i++) {
            if(b.charAt(i) == '1') { 
                int add = (int) Math.pow(2, (b.length() - 1 - i));
                if(isSign && i == 0) { //first sing minus
                    add *=-1;
                }
                value += add;
            }
        }
        return value;
    }

    //for unsigned LITTLE_ENDIAN convert to BIG_ENDIAN //todo: improves performance
    public static int byteToInt(byte[] b, boolean isSign) {
        int value = 0;
        StringBuilder sb = new StringBuilder();
        for (byte by : b) {
            sb.insert(0, byteToString(by)); 
        }
        value = bitStringToInt(sb.toString(), isSign);
        return value;
    }

    //with sign value
    public static int byteToInt(byte[] b) {
        int value = 0;
        for (int i = 0; i < b.length; i++) {
            //value = (value << 8) + (b[i] & 0xff); //BIG_ENDIAN
            value += ((int) b[i] & 0xffL) << (8 * i); //LITTLE_ENDIAN
        }
        return value;
    }

    public static float byteArrayToFloat(byte[] bytes) {
        return Float.intBitsToFloat(byteToInt(bytes));  
    }

    public static byte[] floatToByteArray(float f) {
        int intBits =  Float.floatToIntBits(f);
        return new byte[] {
            (byte) ((intBits) & 0xFF), //LITTLE_ENDIAN
            (byte) ((intBits >> 8) & 0xFF),
            (byte) ((intBits >> 16) & 0xFF),
            (byte) ((intBits >> 24) & 0xFF)
        };
    }

    public static String byteToString(byte b) {
        return  String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }

    public static byte[] concatenate(byte[]... bArray) {
        int len = 0;
        for(byte[] arr : bArray) {
            len += arr.length;
        } 
        byte[] res = (byte[]) Array.newInstance(bArray[0].getClass().getComponentType(), len);
        int pos = 0;
        for(byte[] arr : bArray) {
            System.arraycopy(arr, 0, res, pos, arr.length);
            pos += arr.length;
        }
        return res;
    }
    
    public static byte[][] split(byte[] bArray, int len) {
        if(bArray.length > len && bArray.length % len == 0) {
            byte[][] result = new byte[bArray.length / len][];   
            int pos = 0;
            for(int i = 0; i < bArray.length / len; i++) {
                byte[] tmpArray = new byte[len];
                System.arraycopy(bArray, pos, tmpArray, 0, len);   
                result[i] = tmpArray;
                pos += len; 
            }
            return result; 
        } else {
            return new byte[][] {bArray}; 
        }
    }

    // public static byte[] concatenate(byte[] a, byte[] b) {
    //     int aLen = a.length;
    //     int bLen = b.length;
    
    //     byte[] c = (byte[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
    //     System.arraycopy(a, 0, c, 0, aLen);
    //     System.arraycopy(b, 0, c, aLen, bLen);

    //     return c;
    // }
}