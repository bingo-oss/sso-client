package bingoee.sso.client.web.verify.impl;

import bingoee.sso.client.CharsetName;
import bingoee.sso.client.Strings;
import bingoee.sso.client.web.Client;
import bingoee.sso.client.web.WebAppConfig;
import bingoee.sso.client.web.verify.IdToken;
import bingoee.sso.client.web.verify.TokenManager;
import bingoee.sso.client.web.verify.WebAppAccessToken;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Created by kael on 2017/4/13.
 */
class TokenManagerImpl implements TokenManager {
    
    private static final Logger log = LoggerFactory.getLogger(TokenManagerImpl.class);
    
    private WebAppConfig config;
    private MacSigner signer;
    private Client client;
    
    public TokenManagerImpl(WebAppConfig config) {
        this.config = config;
        this.client = this.config.getClient();
        this.signer = new MacSigner(this.client.getSecret());
    }

    @Override
    public IdToken verifyIdToken(String idToken) {
        Map<String, Object> claim = signer.verify(idToken);
        final String clientId = Strings.nullOrToString(claim.get("aud"));
        final String userId = Strings.nullOrToString(claim.get("sub"));
        IdToken it = new IdToken() {
            @Override
            public String getClientId() {
                return clientId;
            }

            @Override
            public String getUserId() {
                return userId;
            }
        };
        return it;
    }

    @Override
    public WebAppAccessToken fetchAccessToken(String code) {
        try {
            URL url = new URL(config.getSSOInnerTokenEndpoint());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setConnectTimeout(3000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            connection.setRequestProperty("Charset", CharsetName.UTF8);
            
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=authorization_code");
            sb.append("&");
            sb.append("code="+code);
            sb.append("&");
            sb.append("client_id="+client.getId());
            sb.append("&");
            sb.append("client_secret="+client.getSecret());
            
            connection.connect();
            OutputStream os = null;
            InputStream is = null;
            InputStreamReader isr = null;
            BufferedReader reader = null;
            try{
                os = connection.getOutputStream();
                os.write(sb.toString().getBytes(CharsetName.UTF8));
                os.flush();
                int respCode = connection.getResponseCode();
                if(respCode == HttpURLConnection.HTTP_OK){
                    is = connection.getInputStream();
                    isr = new InputStreamReader(is, CharsetName.UTF8);
                    reader = new BufferedReader(isr);
                    StringBuilder json = new StringBuilder();
                    do {
                        String line = reader.readLine();
                        if(line == null){
                            break;
                        }
                        json.append(line);
                    }while (true);
                    JSONObject object = JSON.parseObject(json.toString());
                    String at = Strings.nullOrToString(object.remove("access_token"));
                    String rt = Strings.nullOrToString(object.remove("refresh_token"));
                    String exp = Strings.nullOrToString(object.remove("expires_in"));
                    int expiresIn = exp == null?0:Integer.parseInt(exp);
                    WebAppAccessTokenImpl token = new WebAppAccessTokenImpl(at,rt,expiresIn);

                    for(Map.Entry<String,Object> entry : object.entrySet()){
                        token.put(entry.getKey(),entry.getValue());
                    }
                    return token;
                }else {
                    log.error("fetch access token error: response code is " + respCode);
                    is = connection.getErrorStream();
                    isr = new InputStreamReader(is, CharsetName.UTF8);
                    reader = new BufferedReader(isr);
                    StringBuilder error = new StringBuilder();
                    do {
                        String line = reader.readLine();
                        if(line == null){
                            break;
                        }
                        error.append(line);
                    }while (true);
                    log.error("fetch access token error: response code is " + respCode);
                    log.error("fetch access token error: " + error.toString());
                    reader.close();
                    throw new IllegalStateException(error.toString());
                }
            }finally {
                if(reader != null){
                    reader.close();
                }
                if(isr != null){
                    isr.close();
                }
                if (is != null){
                    is.close();
                }
                if (os != null){
                    os.close();
                }
                connection.disconnect();
            }
        } catch (MalformedURLException e) {
            log.error("create url error",e);
        } catch (IOException e) {
            log.error("connection error",e);
        }
        return null;
    }
    
}
