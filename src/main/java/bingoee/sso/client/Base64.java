package bingoee.sso.client;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.UnsupportedEncodingException;

/**
 * Created by kael on 2017/4/14.
 */
public class Base64 {
    public static String encode(String s) {
        if (s == null) return null;
        try {
            return java.util.Base64.getEncoder().encodeToString(s.getBytes(CharsetName.UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        /*
        try {
            return (new BASE64Encoder()).encode(s.getBytes(CharsetName.UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        */
    }
    public static byte[] decode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return java.util.Base64.getUrlDecoder().decode(bytes);
        /*
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            byte[] b = decoder.decodeBuffer(new String(bytes,CharsetName.UTF8));
            return b;
        } catch (Exception e) {
            return null;
        }
        */
    }
    public static byte[] decode(String str) {
        return java.util.Base64.getMimeDecoder().decode(str);
        /*
        try {
            return decode(str.getBytes(CharsetName.UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        */
    }
}
