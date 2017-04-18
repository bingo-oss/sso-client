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
import net.bingosoft.oss.ssoclient.exception.TokenExpiredException;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

public class JWT {
    
    public static final String UTF_8 = "UTF-8";
    public static final String ALGORITHM = "SHA256withRSA";
    
    public static Map<String, Object> verity(String jwt, String pk) throws InvalidTokenException, TokenExpiredException, InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String[] parts = jwt.split("\\.");

        String content;
        String payload;
        String signature;

        content = parts[0] + "." + parts[1];
        payload = parts[1];
        signature = parts[2];
        
        if(verifySignature(content,signature,pk)){
            String json = new String(Base64.urlDecode(payload), UTF_8);
            return JSON.decodeToMap(json);
        }
        throw new InvalidTokenException(jwt);
    }
    
    private static boolean verifySignature(String content, String signed, String pk) throws SignatureException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        byte[] signedData = Base64.urlDecode(signed);
        byte[] contentData = content.getBytes(JWT.UTF_8);

        Signature signature = Signature.getInstance(ALGORITHM);
        
        if(pk == null){
            throw new NullPointerException("publicKey is null!");
        }
        signature.initVerify(decodePublicKey(pk));
        signature.update(contentData);
        try {
            boolean verified = signature.verify(signedData);
            if(verified){
                return true;
            }
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private static RSAPublicKey decodePublicKey(String base64) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.mimeDecode(base64));
        KeyFactory f = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) f.generatePublic(spec);
    }
}
