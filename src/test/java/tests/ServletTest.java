package tests;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.RsaProvider;
import net.bingosoft.oss.ssoclient.SSOClient;
import net.bingosoft.oss.ssoclient.SSOConfig;
import net.bingosoft.oss.ssoclient.internal.Base64;
import net.bingosoft.oss.ssoclient.internal.JSON;
import net.bingosoft.oss.ssoclient.internal.Urls;
import net.bingosoft.oss.ssoclient.model.AccessToken;
import net.bingosoft.oss.ssoclient.model.Authentication;
import net.bingosoft.oss.ssoclient.servlet.AbstractLoginServlet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

public class ServletTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9999);
    
    private static final String LOCAL_HOST="http://test.meterware.com";
    
    private static final KeyPair keyPair = RsaProvider.generateKeyPair();
    
    private static SSOClient client;
    
    private static final Map<String, String> map = new ConcurrentHashMap<String, String>();
    
    private ServletRunner sr = new ServletRunner();
    @Before
    public void before(){
        SSOConfig c = new SSOConfig();
        c.setClientId("clientId");
        c.setClientSecret("clientSecret");
        c.setRedirectUri(LOCAL_HOST);
        c.autoConfigureUrls("http://localhost:9999");
        client = new SSOClient(c);
        
        sr.registerServlet( "/ssoclient/login", LoginServlet.class.getName());

        String pk = org.apache.commons.codec.binary.Base64.encodeBase64String(keyPair.getPublic().getEncoded());

        Map<String, String> resp = new HashMap<String, String>();
        resp.put("access_token","accesstoken");
        resp.put("refresh_token","refreshtoken");
        resp.put("expires_in","3600");
        resp.put("token_type","Bearer");
        
        stubFor(WireMock.get("/oauth2/publickey").willReturn(aResponse().withStatus(200).withBody(pk)));
        stubFor(post("/oauth2/token").willReturn(aResponse().withStatus(200).withBody(JSON.encode(resp))));
    }
    @Test
    public void testLogin() throws IOException, SAXException {
        ServletUnitClient sc = sr.newClient();
        // 跳转到登录页面
        WebRequest request = new GetMethodWebRequest(LOCAL_HOST+"/ssoclient/login");
        request.setParameter("return_url", Urls.encode(LOCAL_HOST));
        WebResponse response = sc.getResource( request );
        assertEquals(302,response.getResponseCode());
        String location = response.getHeaderField("location");
        System.out.println(location);
        assertTrue(location.contains("/oauth2/authorize"));
        Map<String, String> params = Urls.parseQueryString(location);
        assertEquals("code+id_token",params.get("response_type"));
        assertEquals("clientId",params.get("client_id"));
        assertEquals("http%3A%2F%2Flocalhost%3A0%2Fssoclient%2Flogin%3Freturn_url%3Dhttp%25253A%25252F%25252Ftest.meterware.com",params.get("redirect_uri"));
        assertNotNull(params.get("state"));
        
        // 从SSO重定向回来
        String idToken = Jwts.builder().signWith(SignatureAlgorithm.HS256, Base64.urlEncode("clientSecret"))
                .claim("sub","43FE6476-CD7B-493B-8044-C7E3149D0876")
                .claim("aud","clientId")
                .claim("login_name","admin")
                .setExpiration(new Date(System.currentTimeMillis()+1000*60))
                .compact();
        request = new GetMethodWebRequest(LOCAL_HOST+"/ssoclient/login");
        request.setParameter("state",params.get("state"));
        request.setParameter("code","code");
        request.setParameter("id_token",idToken);
        request.setParameter("return_url",LOCAL_HOST);
        
        sc.getSession(true).setAttribute("oauth2_login_state",params.get("state"));
        
        response = sc.getResource(request);
        assertEquals(302,response.getResponseCode());
        location = response.getHeaderField("location");
        assertEquals(LOCAL_HOST,location);
        
    }
    
    public static class LoginServlet extends AbstractLoginServlet{
        @Override
        protected SSOClient getClient(ServletConfig config) throws ServletException {
            return client;
        }

        @Override
        protected void localLogin(HttpServletRequest req, HttpServletResponse resp, Authentication authc,
                                  AccessToken token) throws ServletException, IOException {
            Assert.assertEquals("clientId",authc.getClientId());
            Assert.assertEquals("43FE6476-CD7B-493B-8044-C7E3149D0876",authc.getUserId());
            Assert.assertEquals("accesstoken",token.getAccessToken());
        }
    }
}
