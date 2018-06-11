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
import net.bingosoft.oss.ssoclient.SSOUtils;
import net.bingosoft.oss.ssoclient.exception.HttpException;
import net.bingosoft.oss.ssoclient.exception.InvalidCodeException;
import net.bingosoft.oss.ssoclient.exception.InvalidTokenException;
import net.bingosoft.oss.ssoclient.exception.TokenExpiredException;
import net.bingosoft.oss.ssoclient.internal.*;
import net.bingosoft.oss.ssoclient.model.AccessToken;
import net.bingosoft.oss.ssoclient.model.Authentication;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
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
        Map<String, Object> map = JWT.verify(accessToken, publicKey);
        if(null == map) {
            map = retryVerify(accessToken);
            if(null == map) {
                throw new InvalidTokenException("Incorrect token : " + accessToken);
            }
        }

        //验证通过
        Authentication authentication = createAuthcFromMap(map);

        if(authentication.isExpired()){
            throw new TokenExpiredException(accessToken);
        }

        return authentication;
    }

    @Override
    public Authentication verifyIdToken(String idToken) throws InvalidTokenException, TokenExpiredException {
        Map<String, Object> map = JWT.verify(idToken, config.getClientSecret());
        if(null == map){
            throw new InvalidTokenException("Incorrect token : " + idToken);
        }
        //验证通过
        Authentication authentication = createAuthcFromIdTokenMap(map);
        if(authentication.isExpired()){
            throw new TokenExpiredException(idToken);
        }
        return authentication;
    }



    @Override
    public Authentication verifyBearerAccessToken(String accessToken) throws InvalidTokenException, TokenExpiredException {
        if(Strings.isEmpty(config.getResourceName())){
            throw new IllegalStateException("resource name must not be null or empty");
        }
        String tokeninfoUrl = config.getTokenInfoEndpointUrl();
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token",accessToken);
        params.put("resource",config.getResourceName());
        Map<String, String> header = new HashMap<String, String>();
        header.put(SSOUtils.AUTHORIZATION_HEADER,SSOUtils.encodeBasicAuthorizationHeader(config.getClientId(),config.getClientSecret()));

        String json = null;
        try {
            json = HttpClient.post(tokeninfoUrl,params,header);
        } catch (HttpException e) {
            if(e.getMessage().contains("invalid_token")){
                throw new InvalidTokenException("error in obtain access token:[http code:"+e.getCode()+"] "+e.getMessage(),e);
            }
            throw e;
        }

        Map<String, Object> tokenInfoMap = JSON.decodeToMap(json);

        if(tokenInfoMap.containsKey("error")){
            throw new InvalidTokenException(tokenInfoMap.get("error")+":"+tokenInfoMap.get("error_description"));
        }

        Authentication authc = new Authentication();
        authc.setUserId((String) tokenInfoMap.remove("user_id"));
        authc.setClientId((String)tokenInfoMap.remove("client_id"));
        authc.setUsername((String)tokenInfoMap.remove("username"));
        
        String scope = (String)tokenInfoMap.remove("scope");
        authc.setScope(scope);
        String expiresIn = Strings.nullOrToString(tokenInfoMap.remove("expires_in"));
        if(null == expiresIn){
            expiresIn = "0";
        }
        authc.setExpires(System.currentTimeMillis()/1000L+Integer.parseInt(expiresIn));

        if(authc.isExpired()){
            throw new TokenExpiredException("token is expired:"+accessToken);
        }

        return authc;
    }

    @Override
    public AccessToken obtainAccessTokenByAuthzCode(String authzCode) throws InvalidCodeException, TokenExpiredException{

        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type","authorization_code");
        params.put("code",authzCode);
        params.put("redirect_uri",Base64.urlEncode(config.getRedirectUri()));

        String json;
        try {
            json = HttpClient.post(config.getTokenEndpointUrl(),params,createAuthorizationHeader());
        } catch (HttpException e) {
            if(e.getMessage().contains("invalid_grant")){
                throw new InvalidCodeException("error in obtain access token:[http code:"+e.getCode()+"] "+e.getMessage(),e);
            }
            throw e;
        }

        Map<String, Object> map;
        try {
            map = JSON.decodeToMap(json);
        } catch (Exception e) {
            throw new RuntimeException("parse json error",e);
        }

        AccessToken token = createAccessTokenFromMap(map);

        if(null == token.getAccessToken() || token.getAccessToken().isEmpty()){
            throw new InvalidCodeException("invalid authorization code["+authzCode+"]:" +
                    map.get("error") + "\n" + map.get("error_description"));
        }

        if(token.isExpired()){
            throw new TokenExpiredException("access token obtain by authorization code " +authzCode+ " is expired!");
        }

        return token;
    }

    @Override
    public AccessToken obtainAccessTokenByClientCredentials() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type","client_credentials");

        String json = HttpClient.post(config.getTokenEndpointUrl(),params,createAuthorizationHeader());

        Map<String, Object> map;
        try {
            map = JSON.decodeToMap(json);
        } catch (Exception e) {
            throw new RuntimeException("parse json error",e);
        }

        AccessToken token = createAccessTokenFromMap(map);

        if(null == token.getAccessToken() || token.getAccessToken().isEmpty()){
            throw new RuntimeException(map.get("error")+":"+map.get("error_description"));
        }

        if(token.isExpired()){
            throw new TokenExpiredException("access token obtain by client secret is expired!");
        }

        return token;
    }

    @Override
    public AccessToken obtainAccessTokenByClientCredentialsWithJwtToken(
            String accessToken) throws InvalidTokenException, TokenExpiredException {

        verifyJwtAccessToken(accessToken);

        return obtainAccessTokenByTokenClientCredentials(accessToken);
    }

    @Override
    public AccessToken obtainAccessTokenByClientCredentialsWithBearerToken(
            String accessToken) throws InvalidTokenException, TokenExpiredException {
        return obtainAccessTokenByTokenClientCredentials(accessToken);
    }

    protected AccessToken obtainAccessTokenByTokenClientCredentials(String accessToken) throws TokenExpiredException{
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type","token_client_credentials");
        params.put("access_token",accessToken);

        String json = HttpClient.post(config.getTokenEndpointUrl(),params,createAuthorizationHeader());

        Map<String, Object> map;
        try {
            map = JSON.decodeToMap(json);
        } catch (Exception e) {
            throw new RuntimeException("parse json error",e);
        }
        if(map.containsKey("error")){
            throw new InvalidTokenException("invalid token:"+map.get("error_description"));
        }
        AccessToken token = createAccessTokenFromMap(map);

        if(token.isExpired()){
            throw new TokenExpiredException("access token obtain by token p client credentials is expired!");
        }

        return token;
    }

    @Override
    public AccessToken refreshAccessToken(AccessToken accessToken) throws InvalidTokenException, TokenExpiredException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type","refresh_token");
        params.put("refresh_token",accessToken.getRefreshToken());

        String json = null;
        try {
            json = HttpClient.post(config.getTokenEndpointUrl(),params,createAuthorizationHeader());
        } catch (HttpException e) {
            String msg = e.getMessage();
            throw new InvalidTokenException(msg,e);
        }

        Map<String, Object> map;
        try {
            map = JSON.decodeToMap(json);
        } catch (Exception e) {
            throw new RuntimeException("parse json error",e);
        }
        if(map.containsKey("error")){
            throw new InvalidTokenException("invalid token:"+map.get("error_description"));
        }
        AccessToken token = createAccessTokenFromMap(map);

        if(token.isExpired()){
            throw new TokenExpiredException("refresh token is expired!");
        }

        return token;
    }

    protected Map<String,Object> retryVerify(String accessToken) {
        //先刷新public key
        refreshPublicKey();

        //再verify一次
        return JWT.verify(accessToken, publicKey);
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

    protected Authentication createAuthcFromMap(Map<String, Object> map){
        Authentication authentication = new Authentication();
        authentication.setUserId((String)map.remove("user_id"));
        authentication.setUsername((String)map.remove("username"));
        authentication.setClientId((String)map.remove("client_id"));
        
        String scope = (String)map.remove("scope");
        authentication.setScope(scope);

        String expires = Strings.nullOrToString(map.remove("exp"));
        authentication.setExpires(expires == null ? 0 : Long.parseLong(expires));
        for (Entry<String, Object> entry : map.entrySet()){
            authentication.setAttribute(entry.getKey(),entry.getValue());
        }
        return authentication;
    }

    protected Authentication createAuthcFromIdTokenMap(Map<String, Object> map) {
        Authentication authentication = new Authentication();
        authentication.setUserId((String)map.remove("sub"));
        authentication.setUsername((String)map.remove("login_name"));
        authentication.setClientId((String)map.remove("aud"));

        String scope = (String)map.remove("scope");
        authentication.setScope(scope);

        String expires = Strings.nullOrToString(map.remove("exp"));
        authentication.setExpires(expires == null ? 0 : Long.parseLong(expires));
        for (Entry<String, Object> entry : map.entrySet()){
            authentication.setAttribute(entry.getKey(),entry.getValue());
        }
        return authentication;
    }

    protected AccessToken createAccessTokenFromMap(Map<String, Object> map){
        AccessToken token = new AccessToken();
        token.setAccessToken((String)map.remove("access_token"));
        token.setRefreshToken((String)map.remove("refresh_token"));
        token.setTokenType((String)map.remove("token_type"));
        String expiresIn = Strings.nullOrToString(map.remove("expires_in"));
        token.setExpiresInFromNow(expiresIn==null?0:Integer.parseInt(expiresIn));
        return token;
    }

    protected Map<String, String> createAuthorizationHeader(){
        Map<String, String> header = new HashMap<String, String>();
        String h = SSOUtils.encodeBasicAuthorizationHeader(config.getClientId(),config.getClientSecret());
        header.put(SSOUtils.AUTHORIZATION_HEADER,h);
        return header;
    }
}
