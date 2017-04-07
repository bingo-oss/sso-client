package bingoee.sso.client.rs.impl;


import bingoee.sso.client.rs.Authenticator;
import bingoee.sso.client.rs.Principal;

import java.net.URL;

/**
 * Created by kael on 2017/4/7.
 */
class AuthenticatorImpl implements Authenticator {

    private String publicKey;

    public AuthenticatorImpl(URL publicKeyUrl) {
        this(null, publicKeyUrl);
    }

    public AuthenticatorImpl(String publicKey) {
        this(publicKey, null);
    }

    public AuthenticatorImpl(String publicKey, URL publicKeyUrl) {
        this.publicKey = publicKey;
    }

    @Override
    public Principal verifyToken(String token) throws Throwable {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("token can not be null or empty");
        }
        if (token.contains(".")) {
            return decodeToPrincipal(parseJwtToken(token));
        }
        return decodeToPrincipal(parseAccessToken(token));
    }

    protected Principal decodeToPrincipal(String json) {
        // TODO decode json
        return null;
    }

    protected String parseJwtToken(String token) {
        
        return null;
    }

    protected String parseAccessToken(String token) {
        throw new IllegalArgumentException("not support access token");
    }
    
}
