package bingoee.sso.client.web;

/**
 * Created by kael on 2017/4/13.
 */
public interface WebAppConfig {
    String getSSOEndpoint();
    String getSSOInnerEndpoint();
    
    String getAuthorizationEndpoint();
    String getLogoutEndpoint();
    
    String getSSOTokenEndpoint();
    String getSSOInnerTokenEndpoint();
    
    String getSSOPublicKeyEndpoint();
    String getSSOInnerPublicKeyEndpoint();
    
    Client getClient();
}
