package bingoee.sso.client.rs;

/**
 * Created by kael on 2017/4/7.
 */
public interface Authenticator {
    /**
     * 传入一个token，解析出这个token携带的用户信息，并返回用户信息对象，如果解析过程出现异常，需要抛出异常信息
     * @param token token，可以是jwt token，也可以是普通的access token.
     * @return 用户信息
     * @throws Throwable 异常信息
     */
    Principal verifyToken(String token) throws Throwable;
}
