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
import net.bingosoft.oss.ssoclient.exception.TokenExpiredException;
import net.bingosoft.oss.ssoclient.internal.HttpClient;
import net.bingosoft.oss.ssoclient.internal.JWT;
import net.bingosoft.oss.ssoclient.internal.Strings;
import net.bingosoft.oss.ssoclient.model.Authentication;

import java.util.Map;

public class TokenProviderImpl implements TokenProvider {

    private SSOConfig config;
    private String pk;

    public TokenProviderImpl(SSOConfig config) {
        this.config = config;
        this.pk = HttpClient.get(config.getPublicKeyEndpointUrl());
    }

    @Override
    public Authentication verifyJwtAccessToken(String accessToken) {
        Map<String, Object> map;
        try {
            map = JWT.verity(accessToken, pk);
        } catch (Throwable e) {
            String newPk = HttpClient.get(config.getPublicKeyEndpointUrl());
            if(newPk != null && newPk.equals(pk)){
                throw new RuntimeException(e);
            }
            pk = newPk;
            try {
                map = JWT.verity(accessToken, pk);
            } catch (Throwable e1) {
                throw new RuntimeException(e1);
            }
        }
        Authentication authentication = new Authentication();
        authentication.setUserId(Strings.nullOrToString(map.remove("user_id")));
        authentication.setClientId(Strings.nullOrToString(map.remove("client_id")));
        authentication.setScope(Strings.nullOrToString(map.remove("scope")));
        authentication.setUsername(Strings.nullOrToString(map.remove("username")));
        
        String expiresIn = Strings.nullOrToString(map.remove("expires_in"));
        authentication.setExpiresIn(expiresIn == null?0:Integer.parseInt(expiresIn));
        
        if(authentication.isExpired()){
            throw new TokenExpiredException(accessToken);
        }
        
        return authentication;
    }

    @Override
    public Authentication verifyBearerAccessToken(String accessToken) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
