package tests;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.RsaProvider;
import net.bingosoft.oss.ssoclient.SSOClient;
import net.bingosoft.oss.ssoclient.SSOConfig;
import net.bingosoft.oss.ssoclient.SSOUtils;
import net.bingosoft.oss.ssoclient.exception.InvalidCodeException;
import net.bingosoft.oss.ssoclient.exception.InvalidTokenException;
import net.bingosoft.oss.ssoclient.exception.TokenExpiredException;
import net.bingosoft.oss.ssoclient.internal.Base64;
import net.bingosoft.oss.ssoclient.internal.JSON;
import net.bingosoft.oss.ssoclient.model.AccessToken;
import net.bingosoft.oss.ssoclient.model.Authentication;
import net.bingosoft.oss.ssoclient.spi.CacheProvider;
import net.bingosoft.oss.ssoclient.spi.TokenProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

/**
 * Created by kael on 2017/4/17.
 */
public class SSOClientTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9999);
    
    private static final String baseUrl = "http://localhost:9999/";
    private SSOClient client;
    
    private KeyPair keyPair = RsaProvider.generateKeyPair();
    
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
    
    String idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
            "eyJhdWQiOiJ0ZXN0Iiwic3ViIjoiNWYxOWY3ZTgtODkwYS" +
            "00NDcxLTliZDEtNzllMTc5MWVlOTU3IiwiZXhwIjoxNDky" +
            "Njc3MTE1LCJuYW1lIjoiYWRtaW4iLCJsb2dpbl9uYW1lIj" +
            "oiYWRtaW4ifQ.GzENvWVdmgknI5eerApk8DupOxCyz3hsL" +
            "QCTVEFfvT8";
    
    String authCode = UUID.randomUUID().toString().replace("-","");
    String basicHeader;
    
    @Before
    public void before(){
        String pk = org.apache.commons.codec.binary.Base64.encodeBase64String(keyPair.getPublic().getEncoded());
        
        stubFor(get("/publickey").willReturn(aResponse().withStatus(200).withBody(pk)));
        
        JwtBuilder builder = jwtBuilder(System.currentTimeMillis()+3600*1000L)
                .signWith(SignatureAlgorithm.RS256,keyPair.getPrivate());
        jwtToken = builder.compact();
        
        SSOConfig config = new SSOConfig().autoConfigureUrls(baseUrl);
        config.setClientId("test");
        config.setClientSecret("test_secret");
        config.setRedirectUri("http://www.example.com");
        client = new SSOClient(config);
        
        basicHeader = SSOUtils.encodeBasicAuthorizationHeader(config.getClientId(),config.getClientSecret());
    }
    
    @After
    public void after(){
        wireMockRule.stop();
    }
    
    @Test
    public void testVerifyAccessToken() throws InterruptedException, UnsupportedEncodingException {
        
        // 正确校验
        Authentication authc = client.verifyAccessToken(jwtToken);
        assertAuthc(authc);
        
        // 缓存
        Assert.assertTrue(authc == client.verifyAccessToken(jwtToken));
        
        // 过期
        authc.setExpires(10);
        Thread.sleep(15);
        Assert.assertTrue(authc != client.verifyAccessToken(jwtToken));
        
        // jwt扩展属性
        Map<String,Object> ext = new HashMap<String, Object>();
        ext.put("ext1","ext1");
        ext.put("ext2","ext2");
        JwtBuilder builder = jwtBuilder(System.currentTimeMillis()+1000*3600,ext)
                .signWith(SignatureAlgorithm.RS256,keyPair.getPrivate());
        authc = client.verifyAccessToken(builder.compact());
        Assert.assertEquals("ext1",authc.getAttributes().get("ext1"));
        Assert.assertEquals("ext2",authc.getAttributes().get("ext2"));
        
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
            client.verifyAccessToken(jwtToken.substring(0,jwtToken.length()-2)+"ab");
        } catch (InvalidTokenException e) {
            invalidToken = true;
        }
        Assert.assertTrue(invalidToken);
        
        // jwt签名错误
        invalidToken = false;
        try {
            client.verifyAccessToken("ab"+jwtToken.substring(10));
        } catch (InvalidTokenException e) {
            invalidToken = true;
        }
        Assert.assertTrue(invalidToken);
        
        // jwt校验过期
        boolean expiredToken = false;
        try {
            JwtBuilder builder1 = jwtBuilder(System.currentTimeMillis()-10000)
                    .signWith(SignatureAlgorithm.RS256,keyPair.getPrivate());
            client.verifyAccessToken(builder1.compact());
        } catch (TokenExpiredException e) {
            expiredToken = true;
        }
        Assert.assertTrue(expiredToken);
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
    }
    
    @Test
    public void testObtainAccessToken(){
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type","authorization_code");
        params.put("code",authCode);
        params.put("redirectUri", Base64.urlEncode(client.getConfig().getRedirectUri()));
        
        Map<String, String> resp = new HashMap<String, String>();
        resp.put("access_token","accesstoken");
        resp.put("refresh_token","refreshtoken");
        resp.put("expires_in","3600");
        resp.put("token_type","Bearer");
        
        // 正常返回
        MappingBuilder mb = post("/oauth2/token").withPostServeAction("postParams",params)
                .withHeader("Authorization", equalTo(basicHeader))
                .willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        AccessToken accessToken = client.obtainAccessToken(authCode);
        assertAccessToken(accessToken);
        
        // 无效的code
        removeStub(mb);
        mb = post("/oauth2/token").withPostServeAction("postParams",params)
                .withHeader("Authorization", equalTo(basicHeader))
                .willReturn(aResponse().withStatus(400).withBody("invalid code"));
        stubFor(mb);
        boolean invalid = false;
        String msg = null;
        try {
            client.obtainAccessToken(authCode);
        } catch (InvalidCodeException e) {
            invalid = true;
            msg = e.getMessage();
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(invalid);
        Assert.assertTrue(msg.contains("invalid code"));
        
        // 返回结果中没有access token 
        removeStub(mb);
        resp.remove("access_token");
        mb = post("/oauth2/token").withPostServeAction("postParams",params)
                .withHeader("Authorization", equalTo(basicHeader))
                .willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        invalid = false;
        msg = null;
        try {
            client.obtainAccessToken(authCode);
        } catch (InvalidCodeException e) {
            invalid = true;
            msg = e.getMessage();
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(invalid);
        Assert.assertTrue(msg.contains("invalid authorization code"));
        resp.put("access_token","accesstoken");
        
        // 返回的结果已经过期
        removeStub(mb);
        resp.remove("expires_in");
        mb = post("/oauth2/token").withPostServeAction("postParams",params)
                .withHeader("Authorization", equalTo(basicHeader))
                .willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        boolean expired = false;
        msg = null;
        try {
            client.obtainAccessToken(authCode);
        } catch (InvalidCodeException e) {
            e.printStackTrace();
        } catch (TokenExpiredException e) {
            expired = true;
            msg=e.getMessage();
        }
        Assert.assertTrue(expired);
        Assert.assertTrue(msg.contains("is expired"));
        resp.put("expires_in","3600");
        
        // 返回结果的json解析错误
        removeStub(mb);
        resp.remove("expires_in");
        mb = post("/oauth2/token").withPostServeAction("postParams",params)
                .withHeader("Authorization", equalTo(basicHeader))
                .willReturn(aResponse().withStatus(200).withBody("{errorjson}"));
        stubFor(mb);
        boolean runtimeException = false;
        try {
            client.obtainAccessToken(authCode);
        } catch (InvalidCodeException e) {
            e.printStackTrace();
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        } catch (Exception e){
            runtimeException = true;
        }
        Assert.assertTrue(runtimeException);
    }
    
    @Test
    public void testVerifyIdToken(){
        JwtBuilder builder = jwtBuilder(System.currentTimeMillis()+5*60*1000);
        builder.signWith(SignatureAlgorithm.HS256, client.getConfig().getClientSecret().getBytes());
        String idToken = builder.compact();
        
        // Jwts自校验
        String userId = Jwts.parser().setSigningKey(client.getConfig().getClientSecret().getBytes())
                .parseClaimsJws(idToken).getBody().get("user_id").toString();
        Assert.assertEquals("43FE6476-CD7B-493B-8044-C7E3149D0876",userId);


        // 正常校验通过
        Authentication authc = client.verifyIdToken(idToken);
        assertAuthc(authc);
        
        // idToken过期
        builder.setExpiration(new Date(System.currentTimeMillis()-5*60*1000));
        idToken = builder.compact();
        boolean expired = false;
        try {
            client.verifyIdToken(idToken);
        } catch (InvalidTokenException e) {
            e.printStackTrace();
        } catch (TokenExpiredException e) {
            expired = true;
        }
        Assert.assertTrue(expired);
        
        // idToken无效
        boolean invalid = false;
        try {
            client.verifyIdToken("error.idtoken");
        } catch (InvalidTokenException e) {
            invalid = true;
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(invalid);
        
    }
    
    protected void assertAuthc(Authentication authc){
        Assert.assertEquals("43FE6476-CD7B-493B-8044-C7E3149D0876", authc.getUserId());
        Assert.assertEquals("admin", authc.getUsername());
        Assert.assertEquals("perm", authc.getScope());
        Assert.assertEquals("console", authc.getClientId());
    }
    
    protected void assertAccessToken(AccessToken at){
        Assert.assertEquals("accesstoken",at.getAccessToken());
        Assert.assertEquals("refreshtoken",at.getRefreshToken());
        Assert.assertEquals("Bearer",at.getTokenType());
        Assert.assertEquals(3600,at.getExpires()-System.currentTimeMillis()/1000L);
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
                authentication.setExpires(1);
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

            @Override
            public Authentication verifyIdToken(String idToken) throws InvalidTokenException, TokenExpiredException {
                used.put("verifyIdToken",true);
                return new Authentication();
            }

            @Override
            public AccessToken obtainAccessTokenByAuthzCode(
                    String authzCode) throws InvalidCodeException, TokenExpiredException {
                used.put("obtainAccessTokenByAuthzCode",true);
                return new AccessToken();
            }
        };
        client.setTokenProvider(provider);
        Assert.assertTrue(provider == client.getTokenProvider());
        client.verifyAccessToken(jwtToken);
        client.verifyAccessToken(UUID.randomUUID().toString());
        client.verifyIdToken(jwtToken);
        client.obtainAccessToken(authCode);
        Assert.assertTrue(used.get("verifyJwtAccessToken"));
        Assert.assertTrue(used.get("verifyBearerAccessToken"));
        Assert.assertTrue(used.get("verifyIdToken"));
        Assert.assertTrue(used.get("obtainAccessTokenByAuthzCode"));
    }
    
    protected JwtBuilder jwtBuilder(long exp, Map<String, Object> ext){
        JwtBuilder jwt = Jwts.builder()
                .claim("user_id","43FE6476-CD7B-493B-8044-C7E3149D0876")
                .claim("scope","perm")
                .claim("client_id","console")
                .claim("username","admin");
        if(ext != null){
            for (Entry<String, Object> entry : ext.entrySet()){
                jwt.claim(entry.getKey(),entry.getValue());
            }
        }
        jwt.setExpiration(new Date(exp));
        return jwt;
    }
    
    protected JwtBuilder jwtBuilder(long exp){
        return jwtBuilder(exp,null);
    }
    
}
