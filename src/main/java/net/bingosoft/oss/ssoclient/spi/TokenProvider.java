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

import net.bingosoft.oss.ssoclient.exception.InvalidCodeException;
import net.bingosoft.oss.ssoclient.exception.InvalidTokenException;
import net.bingosoft.oss.ssoclient.exception.TokenExpiredException;
import net.bingosoft.oss.ssoclient.model.AccessToken;
import net.bingosoft.oss.ssoclient.model.Authentication;

public interface TokenProvider {
    
    Authentication verifyJwtAccessToken(String accessToken) throws InvalidTokenException, TokenExpiredException;

    Authentication verifyIdToken(String idToken) throws InvalidTokenException, TokenExpiredException;
    
    Authentication verifyBearerAccessToken(String accessToken) throws InvalidTokenException, TokenExpiredException;

    /**
     * 通过授权码获取access token
     * 
     * @since 3.0.1
     */
    AccessToken obtainAccessTokenByAuthzCode(String authzCode) throws InvalidCodeException,TokenExpiredException;

    /**
     * 
     * 通过<code>clientId</code>和<code>ClientSecret</code>获取access token
     * 
     * @since 3.0.1
     */
    AccessToken obtainAccessTokenByClientCredentials();

    /**
     * 通过<code>clientId</code>和<code>ClientSecret</code>加上jwt access token获取access token
     * 
     * @since 3.0.1
     */
    AccessToken obtainAccessTokenByClientCredentialsWithJwtToken(String accessToken) throws InvalidTokenException, TokenExpiredException;
    
    /**
     * 通过<code>clientId</code>和<code>ClientSecret</code>加上bearer access token获取access token
     *
     * @since 3.0.1
     */
    AccessToken obtainAccessTokenByClientCredentialsWithBearerToken(String accessToken) throws InvalidTokenException, TokenExpiredException;
    
    /**
     * 刷新accessToken
     *
     * @since 3.0.3
     */
    AccessToken refreshAccessToken(AccessToken accessToken) throws InvalidTokenException, TokenExpiredException;
}
