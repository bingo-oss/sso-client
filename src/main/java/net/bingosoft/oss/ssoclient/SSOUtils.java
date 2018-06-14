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

import net.bingosoft.oss.ssoclient.internal.Base64;
import net.bingosoft.oss.ssoclient.internal.Strings;
import net.bingosoft.oss.ssoclient.internal.Urls;

import javax.servlet.http.HttpServletRequest;

/**
 * 工具类。
 */
public class SSOUtils {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER               = "Bearer";
    public static final String BASIC                = "Basic";
    public static final String ACCESS_TOKEN_PARAM_NAME   = "access_token";

    public static final String POST_LOGOUT_REDIRECT_URI_PARAM = "post_logout_redirect_uri";
    /**
     * 从{@link HttpServletRequest}对象中解析accessToken，这里的accessToken是放在名为Authorization的请求头里。
     *
     * 如果没有名为Authorization的请求头，则返回null
     *
     * <p/>
     * 示例：
     * <pre>
     *      Authorization: Bearer {accessToken}
     * </pre>
     */
    public static String extractAccessToken(HttpServletRequest request) {
        String accessToken = request.getHeader(AUTHORIZATION_HEADER);
        if(accessToken ==null || accessToken.trim().isEmpty()){
        	accessToken=request.getParameter(ACCESS_TOKEN_PARAM_NAME);
        }else{
        	accessToken = accessToken.trim();
            if(accessToken.startsWith(BEARER)){
                accessToken = accessToken.substring(BEARER.length());
                accessToken= accessToken.trim();
            }
        }
        return accessToken;
    }

    /**
     * 将<code>clientId</code>和<code>clientSecret</code>组合并编码成HTTP Basic authentication需要的请求头的值。
     * 参考：https://tools.ietf.org/html/rfc6749#section-2.3.1
     *
     * 在使用授权码获取access token的时候，需要使用HTTP Basic authentication方式验证client身份。
     * 参考：https://tools.ietf.org/html/rfc6749#section-4.1.3
     *
     */
    public static String encodeBasicAuthorizationHeader(String clientId, String clientSecret){
        return BASIC + " " + Base64.urlEncode(clientId+":"+clientSecret);
    }

    /**
     * 返回单点注销地址
     *
     * 应用注销的时候只要直接重定向到这个地址即可单点注销
     *
     */
    public static String getSSOLogoutUrl(SSOClient client, String returnUrl){
        String logoutUrl = client.getConfig().getOauthLogoutEndpoint();
        if(!Strings.isEmpty(returnUrl)){
            logoutUrl = Urls.appendQueryString(logoutUrl,POST_LOGOUT_REDIRECT_URI_PARAM,returnUrl);
        }
        return logoutUrl;
    }

    private static String getContextPathOfReverseProxy(HttpServletRequest req){
        return req.getContextPath();
    }

    protected SSOUtils() {}

}