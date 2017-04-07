package bingoee.sso.client.rs;

/**
 * Created by kael on 2017/4/7.
 */
public interface Principal {
    /**
     * 返回用户ID，没有用户信息则返回null
     */
    String getId();

    /**
     * 返回用户登录名（loginName）
     */
    String getUsername();

    /**
     * 返回用户的scope列表
     */
    String getScope();

    /**
     * 返回clientId，没有则返回null
     */
    String getClientId();

    /**
     * 返回收到这个token时距离过期还有多长时间
     */
    int getExpiresIn();

    /**
     * 返回这个token的过期时间
     */
    long getExpires();

    /**
     * 返回指定属性名的属性，没有则返回null
     * @param fieldName
     */
    Object get(String fieldName);
}
