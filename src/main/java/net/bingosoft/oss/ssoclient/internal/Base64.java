package net.bingosoft.oss.ssoclient.internal;

import bingoee.sso.client.CharsetName;
import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Base64 {
    public static byte[] urlDecode(String str) {
        if (str == null) {
            return null;
        }
        try {
            return org.apache.commons.codec.binary.Base64.decodeBase64(str.getBytes(CharsetName.UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    public static byte[] mimeDecode(String str) {
        try {
            org.apache.commons.codec.binary.Base64.decodeBase64(str.getBytes(CharsetName.UTF8));
            return new BASE64Decoder().decodeBuffer(str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}