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
import net.bingosoft.oss.ssoclient.internal.JSON;
import net.bingosoft.oss.ssoclient.internal.Urls;
import net.bingosoft.oss.ssoclient.model.AccessToken;
import net.bingosoft.oss.ssoclient.model.Authentication;
import net.bingosoft.oss.ssoclient.spi.CacheProvider;
import net.bingosoft.oss.ssoclient.spi.TokenProvider;
import org.junit.*;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

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

        stubFor(get("/oauth2/publickey").willReturn(aResponse().withStatus(200).withBody(pk)));
        
        JwtBuilder builder = jwtBuilder(System.currentTimeMillis()+3600*1000L)
                .signWith(SignatureAlgorithm.RS256,keyPair.getPrivate());
        jwtToken = builder.compact();

        SSOConfig config = new SSOConfig().autoConfigureUrls(baseUrl);
        config.setClientId("test");
        config.setClientSecret("test_secret");
        config.setResourceName("resourceName");
        config.setRedirectUri("http://www.example.com");
        client = new SSOClient(config);

        basicHeader = SSOUtils.encodeBasicAuthorizationHeader(config.getClientId(),config.getClientSecret());
    }

    @After
    public void after(){
        wireMockRule.stop();
    }

    @Test
    public void testVerifyJwtAccessToken() throws InterruptedException, UnsupportedEncodingException {

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
    public void testVerifyBearerAccessToken(){

        String accessToken = UUID.randomUUID().toString();

        Map<String, String> resp = new HashMap<String, String>();
        resp.put("user_id","43FE6476-CD7B-493B-8044-C7E3149D0876");
        resp.put("username","admin");
        resp.put("expires_in","3600");
        resp.put("client_id","console");
        resp.put("scope","perm name user");

        MappingBuilder mb = post("/oauth2/tokeninfo")
                .willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        // 正常返回
        Authentication authc = client.verifyAccessToken(accessToken);
        assertAuthc(authc);
        //缓存
        Authentication authc1 = client.verifyAccessToken(accessToken);
        Assert.assertTrue(authc == authc1);
        // 缓存失效
        authc.setExpires(10);
        authc1 = client.verifyAccessToken(accessToken);
        Assert.assertTrue(authc != authc1);

        // access token 无效
        removeStub(mb);
        Map<String, String> error = new HashMap<String, String>();
        error.put("error","invalid_token");
        error.put("error_description","无效的token");
        mb = post("/oauth2/tokeninfo")
                .willReturn(aResponse().withStatus(400).withBody(JSON.encode(error)));
        stubFor(mb);
        boolean invalid = false;
        authc1.setExpires(10);
        try {
            client.verifyAccessToken(accessToken);
        } catch (InvalidTokenException e) {
            invalid = true;
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(invalid);

        // access token过期
        resp.remove("expires_in");
        removeStub(mb);
        mb = post("/oauth2/tokeninfo")
                .willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        boolean expires = false;
        authc1.setExpires(10);
        try {
            client.verifyAccessToken(accessToken);
        } catch (InvalidTokenException e) {
            e.printStackTrace();
        } catch (TokenExpiredException e) {
            expires = true;
        }
        Assert.assertTrue(expires);
        removeStub(mb);
    }

    @Test
    public void testObtainAccessTokenByCode(){
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type","authorization_code");
        params.put("code",authCode);
        params.put("redirectUri", Urls.encode(client.getConfig().getRedirectUri()));

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
        AccessToken accessToken = client.obtainAccessTokenByCode(authCode);
        assertAccessToken(accessToken);

        // 无效的code
        removeStub(mb);
        mb = post("/oauth2/token").withPostServeAction("postParams",params)
                .withHeader("Authorization", equalTo(basicHeader))
                .willReturn(aResponse().withStatus(400).withBody("{\"error\":\"invalid_grant\",\"error_description\":\"invalid code\"}"));
        stubFor(mb);
        boolean invalid = false;
        String msg = null;
        try {
            client.obtainAccessTokenByCode(authCode);
        } catch (InvalidCodeException e) {
            invalid = true;
            msg = e.getMessage();
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(invalid);
        Assert.assertTrue(msg.contains("invalid_grant"));
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
            client.obtainAccessTokenByCode(authCode);
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
            client.obtainAccessTokenByCode(authCode);
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
            client.obtainAccessTokenByCode(authCode);
        } catch (InvalidCodeException e) {
            e.printStackTrace();
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        } catch (Exception e){
            runtimeException = true;
        }
        Assert.assertTrue(runtimeException);
        removeStub(mb);
    }

    @Test
    public void testObtainAccessTokenByClientCredentials(){
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type","client_credentials");

        Map<String, String> resp = new HashMap<String, String>();
        resp.put("access_token","accesstoken");
        resp.put("refresh_token","refreshtoken");
        resp.put("expires_in","3600");
        resp.put("token_type","Bearer");

        Map<String, String> error = new HashMap<String, String>();
        error.put("error","invalid_grant");
        error.put("error_description","client_secret invalid");

        // 正常获取
        MappingBuilder mb = post("/oauth2/token").withPostServeAction("postParams",params)
                .withHeader("Authorization", equalTo(basicHeader))
                .willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        AccessToken accessToken = client.obtainAccessTokenByClientCredentials();
        assertAccessToken(accessToken);

        // 缓存
        AccessToken accessToken1 = client.obtainAccessTokenByClientCredentials();
        Assert.assertTrue(accessToken==accessToken1);

        // 缓存过期
        accessToken.setExpires(10);
        accessToken1 = client.obtainAccessTokenByClientCredentials();
        Assert.assertTrue(accessToken!=accessToken1);

        removeStub(mb);

        // client credentials校验错误
        client.getCacheProvider().remove("obtainAccessTokenByClientCredentials:"+client.getConfig().getClientId());
        mb.willReturn(aResponse().withStatus(401).withBody(JSON.encode(error)));
        stubFor(mb);
        boolean invalidGrant = false;
        String errorMsg = null;
        try {
            client.obtainAccessTokenByClientCredentials();
        } catch (Exception e) {
            invalidGrant = true;
            errorMsg = e.getMessage();
        }
        Assert.assertTrue(invalidGrant);
        Assert.assertTrue(errorMsg.contains("invalid_grant"));
        removeStub(mb);

        // 服务端异常
        mb.willReturn(aResponse().withStatus(500).withBody(JSON.encode("{error:\"server_error\",error_description:\"server error\"}")));
        stubFor(mb);
        boolean serverError = false;
        try {
            client.obtainAccessTokenByClientCredentials();
        } catch (Exception e) {
            serverError = true;
        }
        Assert.assertTrue(serverError);
        removeStub(mb);
        // json解析异常

        mb.willReturn(aResponse().withStatus(200).withBody(JSON.encode("{aaa}{bbb}")));
        stubFor(mb);
        boolean jsonError = false;
        errorMsg = null;
        try {
            client.obtainAccessTokenByClientCredentials();
        } catch (Exception e) {
            jsonError = true;
            errorMsg = e.getMessage();
        }
        Assert.assertTrue(jsonError);
        Assert.assertTrue(errorMsg.contains("parse json error"));
        removeStub(mb);
        // access token的json异常
        error.put("error","server_error");
        error.put("error_description","server error");
        mb.willReturn(aResponse().withStatus(200).withBody(JSON.encode(error)));
        stubFor(mb);
        boolean errorAccessTokenJson = false;
        errorMsg = null;
        try {
            client.obtainAccessTokenByClientCredentials();
        } catch (Exception e) {
            errorAccessTokenJson = true;
            errorMsg = e.getMessage();
        }
        Assert.assertTrue(errorAccessTokenJson);
        Assert.assertTrue(errorMsg.contains("server_error"));
        removeStub(mb);

        // 取到at的时候就已经过期了
        resp.put("expires_in","0");
        mb.willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        boolean expires = false;
        try {
            client.obtainAccessTokenByClientCredentials();
        } catch (TokenExpiredException e) {
            expires = true;
        }
        Assert.assertTrue(expires);

    }

    @Test
    public void testObtainAccessTokenByClientCredentialsWithToken(){
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type","token_client_credentials");

        Map<String, String> resp = new HashMap<String, String>();
        resp.put("access_token","accesstoken");
        resp.put("refresh_token","refreshtoken");
        resp.put("expires_in","3600");
        resp.put("token_type","Bearer");

        Map<String, String> error = new HashMap<String, String>();
        error.put("error","invalid_grant");
        error.put("error_description","client_secret invalid");

        JwtBuilder jwtBuilder = jwtBuilder(System.currentTimeMillis()+1000*600)
                .signWith(SignatureAlgorithm.RS256,keyPair.getPrivate());
        String jwt = jwtBuilder.compact();
        params.put("access_token",jwt);

        // 正常获取
        MappingBuilder mb = post("/oauth2/token").withPostServeAction("postParams",params)
                .withHeader("Authorization", equalTo(basicHeader))
                .willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        AccessToken accessToken = client.obtainAccessTokenByToken(jwt);
        assertAccessToken(accessToken);
        // 缓存
        AccessToken accessToken1 = client.obtainAccessTokenByToken(jwt);
        Assert.assertTrue(accessToken==accessToken1);
        // 缓存过期
        accessToken.setExpires(10);
        accessToken1 = client.obtainAccessTokenByToken(jwt);
        Assert.assertTrue(accessToken!=accessToken1);

        // jwt过期
        jwt = jwtBuilder.setExpiration(new Date(System.currentTimeMillis()-1000*60)).compact();
        boolean expires = false;
        try {
            client.obtainAccessTokenByToken(jwt);
        } catch (InvalidTokenException e) {
            e.printStackTrace();
        } catch (TokenExpiredException e) {
            expires = true;
        }
        Assert.assertTrue(expires);

        // jwt无效
        jwt = jwtBuilder.signWith(SignatureAlgorithm.RS256, RsaProvider.generateKeyPair().getPrivate())
                .setExpiration(new Date(System.currentTimeMillis()+1000*600)).compact();
        boolean invalid = false;
        try {
            client.obtainAccessTokenByToken(jwt);
        } catch (InvalidTokenException e) {
            invalid = true;
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(invalid);

        // 取到的access token已经是过期的了
        jwt = jwtBuilder.signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .setExpiration(new Date(System.currentTimeMillis()+1000*60)).compact();
        removeStub(mb);
        resp.put("expires_in","0");
        mb.willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        expires = false;
        try {
            client.obtainAccessTokenByToken(jwt);
        } catch (InvalidTokenException e) {
            e.printStackTrace();
        } catch (TokenExpiredException e) {
            expires = true;
        }
        Assert.assertTrue(expires);


        // json 解析失败
        removeStub(mb);
        mb.willReturn(aResponse().withStatus(200).withBody("error_json"));
        stubFor(mb);
        boolean errorJson = false;
        String errorMsg = null;
        try {
            client.obtainAccessTokenByToken(jwt);
        } catch (InvalidTokenException e) {
            e.printStackTrace();
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        } catch (Exception e){
            errorJson = true;
            errorMsg = e.getMessage();
        }
        Assert.assertTrue(errorJson);
        Assert.assertTrue(errorMsg.contains("parse json error"));

        // 通过普通access token换取新token
        resp.put("expires_in","3600");
        removeStub(mb);
        mb = post("/oauth2/token").withPostServeAction("postParams",params)
                .withHeader("Authorization", equalTo(basicHeader))
                .willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        accessToken = client.obtainAccessTokenByToken(UUID.randomUUID().toString());
        assertAccessToken(accessToken);
    }

    @Test
    public void testVerifyIdToken(){
        JwtBuilder builder = Jwts.builder()
                .claim("sub","43FE6476-CD7B-493B-8044-C7E3149D0876")
                .claim("aud","console")
                .claim("login_name","admin")
                .claim("name","管理员")
                .setExpiration(new Date(System.currentTimeMillis()+5*60*1000));
        builder.signWith(SignatureAlgorithm.HS256, client.getConfig().getClientSecret().getBytes());
        String idToken = builder.compact();

        // Jwts自校验
        String userId = Jwts.parser().setSigningKey(client.getConfig().getClientSecret().getBytes())
                .parseClaimsJws(idToken).getBody().get("sub").toString();
        Assert.assertEquals("43FE6476-CD7B-493B-8044-C7E3149D0876",userId);


        // 正常校验通过
        Authentication authc = client.verifyIdToken(idToken);
        assertIdTokenAuthc(authc);

        // 缓存
        Authentication authc1 = client.verifyIdToken(idToken);
        Assert.assertTrue(authc == authc1);
        // 缓存失效
        authc.setExpires(10);
        authc1 = client.verifyIdToken(idToken);
        Assert.assertTrue(authc != authc1);

        // idToken验证失败
        boolean invalid = false;
        try {
            client.verifyIdToken("aa"+idToken.substring(2));
        } catch (InvalidTokenException e) {
            invalid = true;
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(invalid);

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
        invalid = false;
        try {
            client.verifyIdToken("error.idtoken");
        } catch (InvalidTokenException e) {
            invalid = true;
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(invalid);
        // idToken不是jwtToken
        invalid = false;
        try {
            client.verifyIdToken(UUID.randomUUID().toString());
        } catch (InvalidTokenException e) {
            invalid = true;
        } catch (TokenExpiredException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(invalid);
    }
    
    @Test
    public void testRefreshToken(){
        Map<String, String> params = new HashMap<String, String>();
        params.put("grant_type","token_client_credentials");

        Map<String, String> resp = new HashMap<String, String>();
        resp.put("access_token","accesstoken");
        resp.put("refresh_token","refreshtoken");
        resp.put("expires_in","3600");
        resp.put("token_type","Bearer");

        Map<String, String> error = new HashMap<String, String>();
        error.put("error","invalid_grant");
        error.put("error_description","client_secret invalid");
        
        // 正常获取
        MappingBuilder mb = post("/oauth2/token").withPostServeAction("postParams",params)
                .withHeader("Authorization", equalTo(basicHeader))
                .willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        AccessToken at = client.obtainAccessTokenByClientCredentials();
        assertAccessToken(at);
        
        
        
        // 正常刷新
        removeStub(mb);
        resp.put("access_token","accesstoken1");
        resp.put("refresh_token","refreshtoken1");
        resp.put("expires_in","3600");
        resp.put("token_type","Bearer");
        mb.willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp)));
        stubFor(mb);
        AccessToken at1 = client.refreshAccessToken(at);
        Assert.assertEquals("accesstoken1",at1.getAccessToken());
        Assert.assertEquals("refreshtoken1",at1.getRefreshToken());
        Assert.assertEquals("Bearer",at1.getTokenType());
        Assert.assertEquals(3600,at1.getExpires()-System.currentTimeMillis()/1000L);
        
        // 刷新异常
        removeStub(mb);
        mb.willReturn(aResponse().withStatus(401).withBody(JSON.encode(error)));
        stubFor(mb);
        boolean invalid = false;
        try {
            client.refreshAccessToken(at);
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
        Assert.assertEquals("perm name user", authc.getScope());
        Assert.assertEquals("console", authc.getClientId());
    }

    protected void assertIdTokenAuthc(Authentication authc){
        Assert.assertEquals("43FE6476-CD7B-493B-8044-C7E3149D0876", authc.getUserId());
        Assert.assertEquals("admin", authc.getUsername());
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

            @Override
            public AccessToken obtainAccessTokenByClientCredentials() {
                used.put("obtainAccessTokenByClientCredentials",true);
                return new AccessToken();
            }

            @Override
            public AccessToken obtainAccessTokenByClientCredentialsWithJwtToken(
                    String accessToken) throws InvalidTokenException, TokenExpiredException {
                used.put("obtainAccessTokenByClientCredentialsWithJwtToken",true);
                return new AccessToken();
            }
            @Override
            public AccessToken obtainAccessTokenByClientCredentialsWithBearerToken(
                    String accessToken) throws InvalidTokenException, TokenExpiredException {
                used.put("obtainAccessTokenByClientCredentialsWithBearerToken",true);
                return new AccessToken();
            }

            @Override
            public AccessToken refreshAccessToken(
                    AccessToken accessToken) throws InvalidTokenException, TokenExpiredException {
                used.put("refreshAccessToken",true);
                return new AccessToken();
            }
        };
        client.setTokenProvider(provider);
        Assert.assertTrue(provider == client.getTokenProvider());
        client.verifyAccessToken(jwtToken);
        client.verifyAccessToken(UUID.randomUUID().toString());
        client.verifyIdToken(jwtToken);
        client.obtainAccessTokenByCode(authCode);
        client.obtainAccessTokenByClientCredentials();
        client.obtainAccessTokenByToken(jwtToken);
        client.obtainAccessTokenByToken(UUID.randomUUID().toString());
        client.refreshAccessToken(new AccessToken());
        Assert.assertTrue(used.get("verifyJwtAccessToken"));
        Assert.assertTrue(used.get("verifyBearerAccessToken"));
        Assert.assertTrue(used.get("verifyIdToken"));
        Assert.assertTrue(used.get("obtainAccessTokenByAuthzCode"));
        Assert.assertTrue(used.get("obtainAccessTokenByClientCredentials"));
        Assert.assertTrue(used.get("obtainAccessTokenByClientCredentialsWithJwtToken"));
        Assert.assertTrue(used.get("obtainAccessTokenByClientCredentialsWithBearerToken"));
        Assert.assertTrue(used.get("refreshAccessToken"));
    }

    protected JwtBuilder jwtBuilder(long exp, Map<String, Object> ext){
        JwtBuilder jwt = Jwts.builder()
                .claim("user_id","43FE6476-CD7B-493B-8044-C7E3149D0876")
                .claim("scope","perm name user")
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
