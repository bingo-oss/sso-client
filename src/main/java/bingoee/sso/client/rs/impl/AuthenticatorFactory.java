package bingoee.sso.client.rs.impl;


import bingoee.sso.client.rs.Authenticator;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by kael on 2017/4/7.
 */
public class AuthenticatorFactory {
    
    public static Authenticator generateByPublicKey(String publicKey){
        return new AuthenticatorImpl(publicKey);
    }
    
    public static Authenticator generateByPublicKeyUrl(String publicKeyUrl) throws MalformedURLException {
        URL url = new URL(publicKeyUrl);
        return new AuthenticatorImpl(url);
    }
    
}
