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

import java.io.UnsupportedEncodingException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

public class JWT {

    public static final String UTF_8 = "UTF-8";
    public static final String ALGORITHM = "SHA256withRSA";

    /**
     * 如果token验证不正确,返回<code>null</code>
     *
     * @throws InvalidTokenException 如果token的格式不正确
     */
    public static Map<String, Object> verity(String token, RSAPublicKey pk) throws InvalidTokenException {
        String[] parts = token.split("\\.");
        if(parts.length != 3) {
            throw new InvalidTokenException("Invalid jwt token"); //todo : detail error message.
        }

        String content   = parts[0] + "." + parts[1];
        String payload   = parts[1];
        String signature = parts[2];

        if(verifySignature(content,signature,pk)){
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

    /**
     * @throws InvalidTokenException 如果签名的格式不正确
     */
    private static boolean verifySignature(String content, String signed, RSAPublicKey pk) throws InvalidTokenException {
        try {
            byte[] signedData = Base64.urlDecode(signed);
            byte[] contentData = content.getBytes(JWT.UTF_8);

            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(pk);
            signature.update(contentData);

            return signature.verify(signedData);
        }catch (SignatureException e) {
            throw new InvalidTokenException("Invalid signature", e);
        }catch (Exception e) {
            throw new RuntimeException("Verify signature error", e);
        }
    }
}
