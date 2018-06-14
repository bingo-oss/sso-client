package tests;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import net.bingosoft.oss.ssoclient.internal.HttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author kael.
 */
public class HttpClientTest {
    private static final String baseUrl = "http://localhost:9999";
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9999);
    @Before
    public void before(){
        stubFor(post("/200").willReturn(aResponse().withStatus(200).withBody("ok")));
        stubFor(post("/300").willReturn(aResponse().withStatus(300).withBody("this is multiple choices api")));
        stubFor(post("/400").willReturn(aResponse().withStatus(400).withBody("this is bad request api")));
        stubFor(post("/500").willReturn(aResponse().withStatus(500).withBody("this is error api")));
        MappingBuilder mb = get("/get?access_token=abc")
                .willReturn(aResponse().withStatus(200).withBody("get ok"));
        stubFor(mb);
    }
    
    @Test
    public void testPost(){
        // 正常获取结果
        Map<String, String> p = new HashMap<String, String>();
        p.put("p1","p1");
        String resp = HttpClient.post(baseUrl+"/200",p,null);
        Assert.assertEquals("ok",resp);
        // 返回500
        boolean error = false;
        String errorMsg = null;
        try{
            HttpClient.post(baseUrl+"/500",p,null);
        }catch (RuntimeException e){
            error = true;
            errorMsg = e.getMessage();
        }
        Assert.assertTrue(error);
        Assert.assertTrue(errorMsg.contains("this is error api"));
        
        // 返回400
        error = false;
        errorMsg = null;
        try{
            HttpClient.post(baseUrl+"/400",p,null);
        }catch (RuntimeException e){
            error = true;
            errorMsg = e.getMessage();
        }
        Assert.assertTrue(error);
        Assert.assertTrue(errorMsg.contains("this is bad request api"));

        // 返回300
        Assert.assertTrue(HttpClient.post(baseUrl+"/300",p,null).contains("this is multiple choices api"));
    }
    
    @Test
    public void testGet(){
        String res = HttpClient.get(baseUrl+"/get?access_token=abc");
        Assert.assertEquals("get ok",res);
    }
}
