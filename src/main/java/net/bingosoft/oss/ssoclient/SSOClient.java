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

import net.bingosoft.oss.ssoclient.exception.InvalidCodeException;
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
     * @throws InvalidTokenException 如果accessToken是无效的
     * @throws TokenExpiredException 如果accessToken已经过期
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

        boolean jwt = checkJwtToken(accessToken);
        if(jwt) {
            authc = tp().verifyJwtAccessToken(accessToken);
        }else{
            authc = tp().verifyBearerAccessToken(accessToken);
        }

        cp.put(accessToken, authc, authc.getExpires());

        return authc;
    }

    /**
     * 验证登录后获取到的Id Token并返回认证信息。
     *
     * @throws InvalidTokenException 如果idToken是无效的
     * @throws TokenExpiredException 如果idToken已经过期
     *
     * @since 3.0.1
     */
    public Authentication verifyIdToken(String idToken) throws InvalidTokenException, TokenExpiredException {
        CacheProvider cp = cp();
        Authentication authc = cp.get(idToken);
        if(null != authc) {
            if(!authc.isExpired()) {
                return authc;
            }else{
                cp.remove(idToken);
            }
        }
        
        //check is jwt token?
        boolean jwt = checkJwtToken(idToken);
        if(jwt) {
            authc = tp().verifyIdToken(idToken);
        }else{
            throw new InvalidTokenException("idToken is not and jwt token:"+idToken);
        }

        cp.put(idToken, authc, authc.getExpires());

        return authc;
    }

    /**
     * 验证登录后获取到的授权码并返回access token。 
     * 
     * 返回代表用户和应用的access token，这里的用户和应用由<code>authorizationCode</code>决定
     *
     * @throws InvalidTokenException
     * @throws TokenExpiredException
     */
    public AccessToken obtainAccessTokenByCode(String authorizationCode) throws InvalidCodeException, TokenExpiredException {
        AccessToken token = tp().obtainAccessTokenByAuthzCode(authorizationCode);
        return token;
    }

    /**
     * 使用<code>client_id</code>和<code>client_secret</code>通过<code>grant_type=client_credentials</code>的方式
     * 获取access token。
     *
     * 返回SSO颁发给当前应用的access token，代表当前应用的身份。
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">ClientAuthentication</a>
     *
     * @since 3.0.1
     */
    public AccessToken obtainAccessTokenByClientCredentials() throws TokenExpiredException{
        String key = "obtainAccessTokenByClientCredentials:"+config.getClientId();
        AccessToken accessToken = getAccessTokenFromCache(key);
        if (accessToken != null){
            return accessToken;
        }

        accessToken = tp().obtainAccessTokenByClientCredentials();
        cp().put(key,accessToken,accessToken.getExpires());
        return accessToken;
    }

    /**
     * 使用<code>client_id</code>和<code>client_secret</code>加上用户的access token获取一个新的access token，
     *
     * 新的access token是SSO颁发给用户和当前应用的，这里的用户由<code>accessToken</code>决定
     *
     * 返回代表用户和当前应用的身份的access token。
     *
     * @throws InvalidTokenException 如果传入的accessToken是无效的
     * @throws TokenExpiredException 如果传入的accessToken已经过期
     *
     * @since 3.0.1
     */
    public AccessToken obtainAccessTokenByToken(String accessToken) throws InvalidTokenException, TokenExpiredException{
        String key = "obtainAccessTokenByToken:"+accessToken;
        AccessToken token = getAccessTokenFromCache(key);
        if (token != null){
            return token;
        }

        boolean isJwt = checkJwtToken(accessToken);
        if(isJwt){
            token = tp().obtainAccessTokenByClientCredentialsWithJwtToken(accessToken);
        }else {
            token = tp().obtainAccessTokenByClientCredentialsWithBearerToken(accessToken);
        }
        cp().put(key,token,token.getExpires());
        return token;
    }

    /**
     * 使用<code>accessToken</code>中所带的refreshToken获取一个新的access token，
     *
     * 新的accessToken包含的信息和传入的accessToken一致
     *
     * @throws InvalidTokenException 如果传入的accessToken所带的refreshToken是无效的
     * @throws TokenExpiredException 如果传入的accessToken所带的refreshToken已经过期
     *
     * @since 3.0.3
     */
    public AccessToken refreshAccessToken(AccessToken accessToken) throws InvalidTokenException, TokenExpiredException{
        return tp().refreshAccessToken(accessToken);
    }
    
    protected AccessToken getAccessTokenFromCache(String key){
        CacheProvider cp = cp();
        AccessToken token = cp.get(key);
        if(token != null){
            if(!token.isExpired()){
                return token;
            }else {
                cp.remove(key);
            }
        }
        return null;
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
                    tokenProvider = new TokenProviderImpl(getConfig());
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