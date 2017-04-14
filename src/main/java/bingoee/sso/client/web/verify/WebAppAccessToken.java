package bingoee.sso.client.web.verify;

import java.util.Map;

/**
 * Created by kael on 2017/4/13.
 */
public interface WebAppAccessToken {
    String getAccessToken();
    String getRefreshToken();
    int getExpiresIn();
    long getCreatedAt();
    
    Object get(String k);
    
    Map<String, Object> getProperties();
    
}
