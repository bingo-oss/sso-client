package net.bingosoft.oss.ssoclient.internal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Base64 {
    public static String urlEncode(String src){
        try {
            return urlEncode(src.getBytes(JWT.UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    public static String urlEncode(byte[] src){
        return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(src);
    }
    
    public static byte[] urlDecode(String str) {
        if (str == null) {
            return null;
        }
        try {
            return org.apache.commons.codec.binary.Base64.decodeBase64(str.getBytes(JWT.UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    public static byte[] mimeDecode(String str) {
        try {
            return org.apache.commons.codec.binary.Base64.decodeBase64(str.getBytes(JWT.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}