package bingoee.sso.client.web.verify;

/**
 * Created by kael on 2017/4/13.
 */
public interface TokenManager {
    IdToken verifyIdToken(String idToken);
    WebAppAccessToken fetchAccessToken(String code);
}
