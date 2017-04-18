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

/**
 * {@link SSOClient}的配置信息类。
 */
public class SSOConfig {

    protected String clientId;
    protected String clientSecret;
    protected String publicKeyEndpointUrl;
    protected String tokenInfoEndpointUrl;

    //todo :

    public SSOConfig() {
        this(null,null);
    }

    public SSOConfig(String clientId, String clientSecret) {
        this.clientId     = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * @see {@link #autoConfigureUrls(String)}.
     */
    public SSOConfig(String serverUrl) {
        this.autoConfigureUrls(serverUrl);
    }

    /**
     * todo : doc
     */
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * todo : doc
     */
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * todo : doc
     */
    public String getPublicKeyEndpointUrl() {
        return publicKeyEndpointUrl;
    }

    public void setPublicKeyEndpointUrl(String publicKeyEndpointUrl) {
        this.publicKeyEndpointUrl = publicKeyEndpointUrl;
    }

    /**
     * todo : doc
     */
    public String getTokenInfoEndpointUrl() {
        return tokenInfoEndpointUrl;
    }

    public void setTokenInfoEndpointUrl(String tokenInfoEndpointUrl) {
        this.tokenInfoEndpointUrl = tokenInfoEndpointUrl;
    }

    /**
     * 指定SSO服务器的基础地址,自动配置其他的地址属性。
     *
     * <p/>
     * 示例:
     * <pre>
     *
     *      String baseUrl = "https://sso.example.com/v3/oauth2";
     *
     *      SSOConfig config = new SSOConfig().autoConfigure(baseUrl);
     *
     * </pre>
     */
    public SSOConfig autoConfigureUrls(String baseUrl) {
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        //todo :
        this.setPublicKeyEndpointUrl(baseUrl + "/get_public_key");
        this.setTokenInfoEndpointUrl(baseUrl + "/tokeninfo");
        return this;
    }
}