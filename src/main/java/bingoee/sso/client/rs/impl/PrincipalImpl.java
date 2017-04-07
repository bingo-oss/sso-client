package bingoee.sso.client.rs.impl;


import bingoee.sso.client.rs.Principal;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kael on 2017/4/7.
 */
class PrincipalImpl implements Principal {

    private String id;
    private String username;
    private String scope;
    private String clientId;
    private int expiresIn;
    private long expires;
    private Map<String, Object> properties = new HashMap<String, Object>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public int getExpiresIn() {
        return expiresIn;
    }

    @Override
    public long getExpires() {
        return expires;
    }

    @Override
    public Object get(String fieldName) {
        return properties.get(fieldName);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void set(String fieldName, Object value){
        properties.put(fieldName,value);
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }
}
