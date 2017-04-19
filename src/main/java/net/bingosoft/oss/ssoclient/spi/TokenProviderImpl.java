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

package net.bingosoft.oss.ssoclient.spi;

import net.bingosoft.oss.ssoclient.SSOConfig;
import net.bingosoft.oss.ssoclient.exception.InvalidTokenException;
import net.bingosoft.oss.ssoclient.exception.TokenExpiredException;
import net.bingosoft.oss.ssoclient.internal.Base64;
import net.bingosoft.oss.ssoclient.internal.HttpClient;
import net.bingosoft.oss.ssoclient.internal.JWT;
import net.bingosoft.oss.ssoclient.internal.Strings;
import net.bingosoft.oss.ssoclient.model.Authentication;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.Map.Entry;

public class TokenProviderImpl implements TokenProvider {

    private final SSOConfig config;

    private RSAPublicKey publicKey;

    public TokenProviderImpl(SSOConfig config) {
        this.config = config;

        this.refreshPublicKey();
    }

    @Override
    public Authentication verifyJwtAccessToken(String accessToken) throws InvalidTokenException {
        Map<String, Object> map = JWT.verity(accessToken, publicKey);
        if(null == map) {
            map = retryVerify(accessToken);
            if(null == map) {
                throw new InvalidTokenException("Incorrect token : " + accessToken);
            }
        }

        //验证通过
        Authentication authentication = new Authentication();
        authentication.setUserId((String)map.remove("user_id"));
        authentication.setUsername((String)map.remove("username"));
        authentication.setClientId((String)map.remove("client_id"));
        authentication.setScope((String)map.remove("scope"));

        String expires = Strings.nullOrToString(map.remove("exp"));
        authentication.setExpires(expires == null ? 0 : Long.parseLong(expires));
        
        if(authentication.isExpired()){
            throw new TokenExpiredException(accessToken);
        }
        
        for (Entry<String, Object> entry : map.entrySet()){
            authentication.setAttribute(entry.getKey(),entry.getValue());
        }
        
        return authentication;
    }

    @Override
    public Authentication verifyBearerAccessToken(String accessToken) {
        throw new UnsupportedOperationException("Not implemented");
    }

    protected Map<String,Object> retryVerify(String accessToken) {
        //先刷新public key
        refreshPublicKey();

        //再verify一次
        return JWT.verity(accessToken, publicKey);
    }

    protected void refreshPublicKey() {
        String publicKeyBase64 = HttpClient.get(config.getPublicKeyEndpointUrl());

        this.publicKey = decodePublicKey(publicKeyBase64);
    }

    private static RSAPublicKey decodePublicKey(String base64) {
        try{
            X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.mimeDecode(base64));
            KeyFactory f = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) f.generatePublic(spec);
        }catch (Exception e) {
            throw new RuntimeException("Decode public key error", e);
        }
    }
}
