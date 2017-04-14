package bingoee.sso.client.web.verify.impl;

import bingoee.sso.client.web.WebAppConfig;
import bingoee.sso.client.web.verify.TokenManager;

/**
 * Created by kael on 2017/4/13.
 */
public class TokenManagerFactory {
    
    public static TokenManager generateManager(WebAppConfig config){
        return new TokenManagerImpl(config);
    }
}
