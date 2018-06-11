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

package net.bingosoft.oss.ssoclient.model;

import java.io.Serializable;

/**
 * @since 3.0.1
 */
public class AccessToken implements Serializable {
    private static final long serialVersionUID = -6043645669081209490L;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    /**
     * 过期时间，单位是秒
     * 指的是距离标准日期1970-01-01T00:00:00Z UTC的秒数
     */
    private long expires;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    /**
     * 设置从当前时间开始，再过<code>expiresInWithSecond</code>秒这个accessToken就会过期
     * 
     * @param expiresInWithSecond 从现在开始到过期剩余的秒数
     */
    public void setExpiresInFromNow(int expiresInWithSecond){
        this.expires = System.currentTimeMillis()/1000L+expiresInWithSecond;
    }

    /**
     * 判断是否过期，已过期返回true，未过期返回false
     */
    public boolean isExpired(){
        return System.currentTimeMillis()/1000L >= this.expires;
    }
}
