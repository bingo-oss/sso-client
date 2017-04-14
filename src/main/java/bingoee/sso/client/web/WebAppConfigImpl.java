package bingoee.sso.client.web;

import bingoee.sso.client.Args;
import bingoee.sso.client.Strings;

import javax.servlet.ServletConfig;

/**
 * Created by kael on 2017/4/14.
 */
class WebAppConfigImpl implements WebAppConfig {
    
    private String ssoEndpoint;
    private String ssoInnerEndpoint;
    private String ssoTokenEndpoint;
    private String ssoInnerTokenEndpoint;
    private String authorizationEndpoint;
    private String logoutEndpoint;
    private String ssoPublicKeyEndpoint;
    private String ssoInnerPublicKeyEndpoint;
    private Client client;

    public WebAppConfigImpl(ServletConfig config) {
        ssoEndpoint = config.getInitParameter("sso.endpoint");
        Args.notEmpty(ssoEndpoint,"ssoEndpoint can not be null or empty");
        
        if(ssoEndpoint.endsWith("/")){
            ssoEndpoint = ssoEndpoint.substring(0,ssoEndpoint.length()-1);
        }
        
        ssoInnerEndpoint = config.getInitParameter("sso.inner.endpoint");
        if(Strings.isEmpty(ssoInnerEndpoint)){
            ssoInnerEndpoint = ssoEndpoint;
        }
        
        authorizationEndpoint = ssoEndpoint +"/oauth2/authorize";
        logoutEndpoint = ssoEndpoint + "/oauth2/logout";
        
        ssoPublicKeyEndpoint = ssoEndpoint + "/get_public_key";
        ssoInnerPublicKeyEndpoint = ssoInnerEndpoint + "/get_public_key";
                
        ssoTokenEndpoint = ssoEndpoint + "/oauth2/token";
        ssoInnerTokenEndpoint = ssoInnerEndpoint + "/oauth2/token";

        final String clientId = config.getInitParameter("clientId");
        final String clientSecret = config.getInitParameter("clientSecret");
        if(Strings.isEmpty(clientId)){
            
        }
        Args.notEmpty(clientId,"clientId can not be empty");
        Args.notEmpty(clientSecret,"clientSecret can not be empty");
        client = new Client() {
            @Override
            public String getId() {
                return clientId;
            }

            @Override
            public String getSecret() {
                return clientSecret;
            }
        };
    }

    @Override
    public String getSSOEndpoint() {
        return ssoEndpoint;
    }

    @Override
    public String getSSOInnerEndpoint() {
        return ssoInnerEndpoint;
    }

    @Override
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    @Override
    public String getLogoutEndpoint() {
        return logoutEndpoint;
    }

    @Override
    public String getSSOPublicKeyEndpoint() {
        return ssoPublicKeyEndpoint;
    }

    @Override
    public String getSSOInnerPublicKeyEndpoint() {
        return ssoInnerPublicKeyEndpoint;
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public String getSSOTokenEndpoint() {
        return ssoTokenEndpoint;
    }

    @Override
    public String getSSOInnerTokenEndpoint() {
        return ssoInnerTokenEndpoint;
    }
}
