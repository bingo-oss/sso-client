/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.bingosoft.oss.ssoclient.internal;

import net.bingosoft.oss.ssoclient.exception.InvalidTokenException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

public class JWT {

    public static final String UTF_8 = "UTF-8";
    public static final String ALG_SHA256WITHRSA = "SHA256withRSA";
    public static final String ALG_HMACSHA256 = "HMACSHA256";

    /**
     * 使用RS256算法验证jwt
     * 如果token验证不正确,返回<code>null</code>
     *
     * @throws InvalidTokenException 如果token的格式不正确
     */
    public static Map<String, Object> verify(String token, final RSAPublicKey pk) throws InvalidTokenException {
        // RS256签名验证器
        Verifier verifier = new Verifier() {
            @Override
            public boolean verifySignature(String content, String payload, String signature) {
                return JWT.verifySignature(content,signature,pk);
            }
        };
        return verify(token, verifier);
    }

    /**
     * 使用HS256算法验证jwt
     * 如果token验证不正确，返回<code>null</code>
     *
     * @throws InvalidTokenException 如果token格式不正确
     */
    public static Map<String, Object> verify(String token, final String secret) throws InvalidTokenException{
        // HS256签名验证器
        Verifier verifier = new Verifier() {
            @Override
            public boolean verifySignature(String content, String payload, String signature) {
                return JWT.verifySignature(content,secret,signature);
            }
        };
        return verify(token, verifier);
    }
    
    /**
     * 基于RS256算法验证
     * @throws InvalidTokenException 如果签名的格式不正确
     */
    private static boolean verifySignature(String content, String signed, RSAPublicKey pk) throws InvalidTokenException {
        try {
            byte[] signedData = Base64.urlDecode(signed);
            byte[] contentData = content.getBytes(JWT.UTF_8);

            Signature signature = Signature.getInstance(ALG_SHA256WITHRSA);
            signature.initVerify(pk);
            signature.update(contentData);

            return signature.verify(signedData);
        }catch (SignatureException e) {
            throw new InvalidTokenException("Invalid signature", e);
        }catch (Exception e) {
            throw new RuntimeException("Verify signature error", e);
        }
    }
    
    /**
     * 基于HS256算法验证
     * @throws InvalidTokenException 如果签名的格式不正确
     */
    private static boolean verifySignature(String content, String secret, String signature) throws InvalidTokenException{
        try {
            SecretKey secretKey = new SecretKeySpec(secret.getBytes(), ALG_HMACSHA256);
            Mac mac = Mac.getInstance(ALG_HMACSHA256);
            mac.init(secretKey);
            byte[] bytes = mac.doFinal(content.getBytes());
            String base64 = base64UrlEncode(bytes);
            return  Strings.equals(base64,signature);
        } catch (Exception e) {
            throw new RuntimeException("Verify signature error", e);
        }
    }
    
    private static String base64UrlEncode(byte[] bytes){
        String encoded = Base64.urlEncode(bytes);
        StringBuilder sb = new StringBuilder(encoded);
        while (sb.charAt(sb.length() - 1) == '=') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
    
    private static Map<String, Object> verify(String token, Verifier verifier) throws InvalidTokenException{
        String[] parts = token.split("\\.");
        if(parts.length != 3) {
            throw new InvalidTokenException("Invalid jwt: length of parts expect 3 but actual "+parts.length+", token:"+token);
        }

        String content   = parts[0] + "." + parts[1];
        String payload   = parts[1];
        String signature = parts[2];

        if(verifier.verifySignature(content,payload,signature)){
            try{
                String json = new String(Base64.urlDecode(payload), UTF_8);
                return JSON.decodeToMap(json);
            }catch(UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }else{
            //不正确返回null
            return null;
        }
    }
    
    /** 签名验证器 **/
    interface Verifier {
        boolean verifySignature(String content, String payload, String signature) throws InvalidTokenException;
    }
}
