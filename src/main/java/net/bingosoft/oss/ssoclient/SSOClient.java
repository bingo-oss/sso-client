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

package net.bingosoft.oss.ssoclient;

import net.bingosoft.oss.ssoclient.exception.InvalidTokenException;
import net.bingosoft.oss.ssoclient.exception.TokenExpiredException;
import net.bingosoft.oss.ssoclient.model.AccessToken;
import net.bingosoft.oss.ssoclient.model.Authentication;
import net.bingosoft.oss.ssoclient.spi.CacheProvider;
import net.bingosoft.oss.ssoclient.spi.CacheProviderImpl;
import net.bingosoft.oss.ssoclient.spi.TokenProvider;
import net.bingosoft.oss.ssoclient.spi.TokenProviderImpl;

import javax.servlet.http.HttpServletRequest;

/**
 * 客户端入口类,所有公开的操作都从这个入口类进行调用。
 */
public class SSOClient {

    protected SSOConfig     config;
    protected CacheProvider cacheProvider;
    protected TokenProvider tokenProvider;

    public SSOClient() {

    }

    public SSOClient(SSOConfig config) {
        this.config = config;
    }

    /**
     * 验证访问令牌并返回认证信息
     *
     * <p/>
     * 在Servlet容器中可以通过调用{@link SSOUtils#extractAccessToken(HttpServletRequest)}获取到访问令牌。
     *
     * @throws InvalidTokenException
     * @throws TokenExpiredException
     */
    public Authentication verifyAccessToken(String accessToken) throws InvalidTokenException, TokenExpiredException{
        CacheProvider cp = cp();

        Authentication authc = cp.get(accessToken);
        if(null != authc) {
            if(!authc.isExpired()) {
                return authc;
            }else{
                cp.remove(accessToken);
            }
        }

        //check is jwt token?
        boolean jwt = checkJwtToken(accessToken);
        if(jwt) {
            authc = tp().verifyJwtAccessToken(accessToken);
        }else{
            authc = tp().verifyBearerAccessToken(accessToken);
        }

        cp.put(accessToken, authc, authc.getExpiresIn());

        return authc;
    }

    /**
     * 验证登录后获取到的Id Token并返回认证信息。
     *
     * @throws InvalidTokenException
     * @throws TokenExpiredException
     */
    public Authentication verifyIdToken(String idToken) throws InvalidTokenException, TokenExpiredException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * 验证登录后获取到的授权码并返回访问令牌信息
     *
     * @throws InvalidTokenException
     * @throws TokenExpiredException
     */
    public AccessToken obtainAccessToken(String authorizationCode) throws InvalidTokenException, TokenExpiredException {
        throw new UnsupportedOperationException("Not implemented");
    }

    protected boolean checkJwtToken(String accessToken) {
        return accessToken.contains(".");
    }

    protected final CacheProvider cp() {
        if(null == cacheProvider) {
            synchronized (this) {
                if(null == cacheProvider) {
                    cacheProvider = new CacheProviderImpl();
                }
            }
        }
        return cacheProvider;
    }

    protected final TokenProvider tp() {
        if(null == tokenProvider) {
            synchronized (this) {
                if(null == tokenProvider) {
                    tokenProvider = new TokenProviderImpl();
                }
            }
        }
        return tokenProvider;
    }

    //================================ getters & setters ======================================

    public SSOConfig getConfig() {
        return config;
    }

    public void setConfig(SSOConfig config) {
        this.config = config;
    }

    public CacheProvider getCacheProvider() {
        return cacheProvider;
    }

    public void setCacheProvider(CacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    public TokenProvider getTokenProvider() {
        return tokenProvider;
    }

    public void setTokenProvider(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

}