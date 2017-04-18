package tests;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import net.bingosoft.oss.ssoclient.SSOClient;
import net.bingosoft.oss.ssoclient.SSOConfig;
import net.bingosoft.oss.ssoclient.model.Authentication;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Created by kael on 2017/4/17.
 */
public class SSOClientTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9999);
    
    private static final String baseUrl = "http://localhost:9999";
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
    @Test
    public void testVerifyAccessToken(){
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(publicKey)));
        Authentication authc = client.verifyAccessToken(jwtToken);
        assertAuthc(authc);
        Assert.assertTrue(authc == client.verifyAccessToken(jwtToken));
    }
    
    protected void assertAuthc(Authentication authc){
        Assert.assertEquals("43FE6476-CD7B-493B-8044-C7E3149D0876", authc.getUserId());
        Assert.assertEquals("admin", authc.getUsername());
        Assert.assertEquals("perm", authc.getScope());
        Assert.assertEquals("console", authc.getClientId());
        Assert.assertEquals(23694, authc.getExpiresIn());
    }
    @Test
    public void testAuthenticationExpired() throws InterruptedException {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(publicKey)));
        Authentication authc = client.verifyAccessToken(jwtToken);
        assertAuthc(authc);
        authc.setExpiresIn(10);
        Thread.sleep(15);
        Assert.assertTrue(authc != client.verifyAccessToken(jwtToken));
    }
    
}
