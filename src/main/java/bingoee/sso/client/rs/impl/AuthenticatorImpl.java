package bingoee.sso.client.rs.impl;


import bingoee.sso.client.Base64;
import bingoee.sso.client.CharsetName;
import bingoee.sso.client.Strings;
import bingoee.sso.client.rs.Authenticator;
import bingoee.sso.client.rs.Principal;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

/**
 * Created by kael on 2017/4/7.
 */
class AuthenticatorImpl implements Authenticator {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER               = "Bearer";
    
    private static final Logger log = LoggerFactory.getLogger(AuthenticatorImpl.class);
    
    private String publicKey;
    private URL publicKeyUrl;

    public AuthenticatorImpl(URL publicKeyUrl) {
        this(null, publicKeyUrl);
    }

    public AuthenticatorImpl(String publicKey) {
        this(publicKey, null);
    }

    public AuthenticatorImpl(String publicKey, URL publicKeyUrl) {
        this.publicKey = publicKey;
        this.publicKeyUrl = publicKeyUrl;
    }

    @Override
    public Principal verifyToken(String token) throws Throwable {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("token can not be null or empty");
        }
        if (token.contains(".")) {
            return decodeToPrincipal(parseJwtToken(token));
        }
        return decodeToPrincipal(parseAccessToken(token));
    }

    @Override
    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if(header == null || header.trim().isEmpty()){
            return null;
        }
        header = header.trim();
        if(header.startsWith(BEARER)){
            header = header.substring(BEARER.length());
            return header.trim();
        }else {
            return header;
        }
    }

    protected Principal decodeToPrincipal(String json) {
        
        log.debug("parse json :{}", json);
        
        if(json == null || json.trim().isEmpty()){
            return null;
        }
        JSONObject object = JSON.parseObject(json);
        PrincipalImpl principal = new PrincipalImpl();
        
        Object id = object.remove("user_id");
        Object username = object.remove("username");
        Object scope = object.remove("scope");
        Object clientId = object.remove("client_id");
        Object expiresIn = object.remove("expires_in");
        Object expires = object.remove("expires");
        
        principal.setId(Strings.nullOrToString(id));
        principal.setUsername(Strings.nullOrToString((username)));
        principal.setScope(Strings.nullOrToString((scope)));
        principal.setClientId(Strings.nullOrToString((clientId)));
        if(expiresIn != null){
            principal.setExpiresIn(Integer.parseInt(Strings.nullOrToString((expiresIn))));
        }
        if(expires != null){
            principal.setExpires(Long.parseLong(Strings.nullOrToString((expires))));
        }
        
        for (Map.Entry<String, Object> entry : object.entrySet()){
            principal.set(entry.getKey(),entry.getValue());
        }
        
        return principal;
    }
    
    protected String parseJwtToken(String token) throws UnsupportedEncodingException, InvalidKeySpecException, SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        String[] parts = token.split("\\.");

        String content;
        String payload;
        String signature;

        content = parts[0] + "." + parts[1];
        payload = parts[1];
        signature = parts[2];
        //验证token
        if (!verifySignature(content, signature)) {
            // 验证失败
        } else {
            // 验证成功
            
            byte[] decodes = Base64.decode(payload.getBytes(CharsetName.UTF8));
            String info = new String(decodes);
            return info;
        }
        return null;
    }

    protected String parseAccessToken(String token) {
        throw new IllegalArgumentException("not support access token");
    }
    
    // 用公钥校验token的有效性
    protected boolean verifySignature(String content, String signed) throws SignatureException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        byte[] signedData = Base64.decode(signed.getBytes(CharsetName.UTF8));
        byte[] contentData = content.getBytes();

        Signature signature = Signature.getInstance("SHA256withRSA");
        if(publicKey == null || publicKey.trim().isEmpty()){
            refreshPublicKey();
        }
        if(publicKey == null){
            throw new NullPointerException("publicKey is null!");
        }
        signature.initVerify(decodePublicKey(publicKey));
        signature.update(contentData);
        try {
            boolean verified = signature.verify(signedData);
            if(verified){
                return true;
            }
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        if(refreshPublicKey()){
            signature.initVerify(decodePublicKey(publicKey));
            signature.update(contentData);
            return signature.verify(signedData);
        }
        return false;
    }
    // 生成校验用的公钥
    protected RSAPublicKey decodePublicKey(String base64) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(base64));
        KeyFactory f = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) f.generatePublic(spec);
    }
    protected boolean refreshPublicKey(){
        if(publicKeyUrl == null){
            return false;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection)publicKeyUrl.openConnection();
            connection.setConnectTimeout(3000);
            connection.connect();
            InputStream is = connection.getInputStream();
            StringBuilder sb = new StringBuilder();
            do{
                int i = is.read();
                if(i == -1){
                    break;
                }
                sb.append((char)i);
            }while (true);
            if(sb.length() > 0){
                this.publicKey = sb.toString();
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
    
}
