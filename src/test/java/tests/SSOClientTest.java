package tests;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sun.org.apache.xpath.internal.operations.Bool;
import net.bingosoft.oss.ssoclient.SSOClient;
import net.bingosoft.oss.ssoclient.SSOConfig;
import net.bingosoft.oss.ssoclient.exception.InvalidTokenException;
import net.bingosoft.oss.ssoclient.exception.TokenExpiredException;
import net.bingosoft.oss.ssoclient.model.Authentication;
import net.bingosoft.oss.ssoclient.spi.CacheProvider;
import net.bingosoft.oss.ssoclient.spi.TokenProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

/**
 * Created by kael on 2017/4/17.
 */
public class SSOClientTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9999);
    
    private static final String baseUrl = "http://localhost:9999/";
    private SSOClient client = new SSOClient(new SSOConfig().autoConfigureUrls(baseUrl));
    
    String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDASOjIWexLpnXiJNJF2pL6NzP\n" +
            "fBoF0tKEr2ttAkJ/7f3uUHhj2NIhQ01Wu9OjHfXjCvQSXMWqqc1+O9G1UwB2Xslb\n" +
            "WNwEZFMwmQdP5VleGbJLR3wOl3IzdggkxBJ1Q9rXUlVtslK/CsMtkwkQEg0eZDH1\n" +
            "VeJXqKBlEhsNckYIGQIDAQAB";
    String jwtToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9." +
            "eyJjbGllbnRfaWQiOiJjb25zb2xlIiwidXNlcl9pZCI6IjQ" +
            "zRkU2NDc2LUNEN0ItNDkzQi04MDQ0LUM3RTMxNDlEMDg3Ni" +
            "IsInVzZXJuYW1lIjoiYWRtaW4iLCJzY29wZSI6InBlcm0iL" +
            "CJleHBpcmVzX2luIjoyMzY5NCwiZXhwaXJlcyI6MTQ5MjAw" +
            "Mzc1MjAwMCwiZW5hYmxlZCI6MSwiZXhwIjoxNDkyMDE2MDU" +
            "3MjMyfQ.rig2Y67pkpxxfJxZD9gyKCCwQK5K9bS5w6FcDhn" +
            "kJWc8FEXZEn3kICByb2W9PivouRc5l2_9N4dVXyEH1s2k17" +
            "Jp9aAWU7AFEWwtjdRQe7UIjCxock--FOUzuUKZhrI1tgeVH" +
            "P4p-NNnkh-at43NxEI63HLOKvCo67R3QgK3wrg";
    @Before
    public void before(){
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(publicKey)));
    }
    @After
    public void after(){
        wireMockRule.stop();
    }
    
    @Test
    public void testVerifyAccessToken() throws InterruptedException {
        
        // 正确校验
        Authentication authc = client.verifyAccessToken(jwtToken);
        assertAuthc(authc);
        
        // 缓存
        Assert.assertTrue(authc == client.verifyAccessToken(jwtToken));
        
        // 过期
        authc.setExpiresIn(10);
        Thread.sleep(15);
        Assert.assertTrue(authc != client.verifyAccessToken(jwtToken));
        
        // 错误的jwt格式
        boolean invalidToken = false;
        try {
            client.verifyAccessToken("error.jwt");
        } catch (InvalidTokenException e) {
            invalidToken = true;
        }
        Assert.assertTrue(invalidToken);
        // 错误的jwt校验
        invalidToken = false;
        try {
            client.verifyAccessToken(jwtToken.replace("iJ","ab"));
        } catch (InvalidTokenException e) {
            invalidToken = true;
        }
        Assert.assertTrue(invalidToken);
        
        // jwt签名错误
        invalidToken = false;
        try {
            client.verifyAccessToken(jwtToken.replace("63HLOKvCo67R3QgK3wrg","ab"));
        } catch (InvalidTokenException e) {
            invalidToken = true;
        }
        Assert.assertTrue(invalidToken);
    }
    @Test
    public void testUnsupportedOperation(){
        // Bearer类型的at校验
        boolean unsupported = false;
        try {
            client.verifyAccessToken(UUID.randomUUID().toString());
        }catch (UnsupportedOperationException e){
            unsupported = true;
        }
        Assert.assertTrue(unsupported);

        // 获取at对象
        unsupported = false;
        try {
            client.obtainAccessToken(UUID.randomUUID().toString());
        }catch (UnsupportedOperationException e){
            unsupported = true;
        }
        Assert.assertTrue(unsupported);

        // 校验id token
        unsupported = false;
        try {
            client.verifyIdToken(UUID.randomUUID().toString());
        }catch (UnsupportedOperationException e){
            unsupported = true;
        }
        Assert.assertTrue(unsupported);
        
    }
    
    protected void assertAuthc(Authentication authc){
        Assert.assertEquals("43FE6476-CD7B-493B-8044-C7E3149D0876", authc.getUserId());
        Assert.assertEquals("admin", authc.getUsername());
        Assert.assertEquals("perm", authc.getScope());
        Assert.assertEquals("console", authc.getClientId());
        Assert.assertEquals(23694, authc.getExpiresIn());
    }
    @Test
    public void testCacheProvider(){
        final Map<String, Boolean> used = new HashMap<String, Boolean>();
        SSOClient client = new SSOClient();
        client.setConfig(new SSOConfig(baseUrl));
        CacheProvider provider = new CacheProvider() {
            @Override
            public <T> T get(String key) {
                used.put("get",true);
                Authentication authentication = new Authentication();
                authentication.setExpiresIn(1);
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return (T)authentication;
            }

            @Override
            public void put(String key, Object item, long expires) {
                used.put("put",true);
            }

            @Override
            public void remove(String key) {
                used.put("remove",true);
            }
        };
        client.setCacheProvider(provider);
        Assert.assertTrue(provider == client.getCacheProvider());
        client.verifyAccessToken(jwtToken);
        Assert.assertTrue(used.get("get"));
        Assert.assertTrue(used.get("put"));
        Assert.assertTrue(used.get("remove"));
    }
    @Test
    public void testTokenProvider(){
        final Map<String, Boolean> used = new HashMap<String, Boolean>();
        SSOClient client = new SSOClient();
        client.setConfig(new SSOConfig(baseUrl));
        TokenProvider provider = new TokenProvider() {
            @Override
            public Authentication verifyJwtAccessToken(
                    String accessToken) throws InvalidTokenException, TokenExpiredException {
                used.put("verifyJwtAccessToken",true);
                return new Authentication();
            }

            @Override
            public Authentication verifyBearerAccessToken(String accessToken) {
                used.put("verifyBearerAccessToken",true);
                return new Authentication();
            }
        };
        client.setTokenProvider(provider);
        Assert.assertTrue(provider == client.getTokenProvider());
        client.verifyAccessToken(jwtToken);
        client.verifyAccessToken(UUID.randomUUID().toString());
        Assert.assertTrue(used.get("verifyJwtAccessToken"));
        Assert.assertTrue(used.get("verifyBearerAccessToken"));
    }
    
}
