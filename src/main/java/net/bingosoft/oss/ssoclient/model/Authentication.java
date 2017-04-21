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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证信息
 */
public class Authentication {

    protected String userId;
    protected String username;
    protected String clientId;
    protected String scope;
    /**
     * 过期时间，指的是距离标准日期1970-01-01T00:00:00Z UTC的秒数
     * 单位：秒
     * 参考：http://self-issued.info/docs/draft-ietf-oauth-json-web-token.html#expDef
     */
    protected long   expires;
    
    /* ======= Nonstandard attribute ======= */
    protected Map<String, Object> ext = new HashMap<String, Object>();
    
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public long getExpires() {
        return expires;
    }

    public long getExpiresMs() {
        return expires * 1000L;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public void setAttribute(String key, Object val){
        ext.put(key,val);
    }
    
    public Map<String,Object> getAttributes(){
        return Collections.unmodifiableMap(ext);
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis()/1000L >= expires;
    }
}
