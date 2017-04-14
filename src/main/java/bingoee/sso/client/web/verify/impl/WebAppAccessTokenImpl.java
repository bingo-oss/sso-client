package bingoee.sso.client.web.verify.impl;

import bingoee.sso.client.web.verify.WebAppAccessToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kael on 2017/4/14.
 */
public class WebAppAccessTokenImpl implements WebAppAccessToken {
    
    private String at;
    private String rt;
    private long createdAt;
    private int expiresIn;
    private Map<String, Object> ext = new HashMap<>();
    
    public WebAppAccessTokenImpl(String at, String rt, int expiresIn) {
        this.at = at;
        this.rt = rt;
        this.createdAt = System.currentTimeMillis();
        this.expiresIn = expiresIn;
    }

    @Override
    public String getAccessToken() {
        return at;
    }

    @Override
    public String getRefreshToken() {
        return rt;
    }

    @Override
    public int getExpiresIn() {
        int expired = (int)(System.currentTimeMillis()-createdAt);
        return expiresIn - expired;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public Object get(String k) {
        return ext.get(k);
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(ext);
    }
    
    public void put(String k, Object v){
        ext.put(k,v);
    }
    
}
