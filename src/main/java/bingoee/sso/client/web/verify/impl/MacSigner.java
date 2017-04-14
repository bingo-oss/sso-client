package bingoee.sso.client.web.verify.impl;

import bingoee.sso.client.CharsetName;
import bingoee.sso.client.Strings;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by kael on 2017/4/13.
 */
public class MacSigner {
    public static final String ALG_HMACSHA256 = "HMACSHA256";
    public static final String ALG_HS256      = "HS256";
    public static final String CLAIM_EXPIRATION_TIME = "exp";
    
    protected final String     jwtAlgorithm;
    protected final String     macAlgorithm;
    protected final SecretKey secretKey;
    
    protected int defaultExpires              = 3600;

    protected Verifier verifier;
    
    public MacSigner(String secret) {
        this(ALG_HS256, ALG_HMACSHA256, new SecretKeySpec(secret.getBytes(), ALG_HMACSHA256));
    }

    public MacSigner(String secret, int defaultExpires) {
        this(ALG_HS256, ALG_HMACSHA256, new SecretKeySpec(secret.getBytes(), ALG_HMACSHA256));
        this.defaultExpires = defaultExpires;
    }

    public MacSigner(String jwtAlgorithm, String macAlgorithm, SecretKey secretKey) {
        this.jwtAlgorithm = jwtAlgorithm;
        this.macAlgorithm = macAlgorithm;
        this.secretKey    = secretKey;
    }
    
    public Map<String, Object>  verify(String token){
        if(null == verifier) {
            verifier = new Verifier();
        }
        return verifier.verify(token);
    }
    
    
    protected class Verifier {
        protected boolean verifySignature(String content, String signature) {
            return base64UrlEncode(signToBytes(content)).equals(signature);
        }
        protected Map<String, Object> verify(String token) {
            
            String[] parts = Strings.split(token, "\\.");
            if (parts.length < 2 || parts.length > 3) {
                throw new TokenVerifyException(TokenVerifyException.ErrorCode.INVALID_TOKEN, "Invalid jwt token, wrong number of parts: " + parts.length);
            }

            String content;
            String payload;
            String signature;

            if (parts.length == 2) {
                content = parts[0];
                payload = parts[0];
                signature = parts[1];
            } else {
                content = parts[0] + "." + parts[1];
                payload = parts[1];
                signature = parts[2];
            }

            if (payload.isEmpty() || signature.isEmpty()) {
                throw new TokenVerifyException(TokenVerifyException.ErrorCode.INVALID_TOKEN, "Invalid jwt token, both payload and signature parts must not be empty");
            }

            return verify(content, payload, signature);
        }
        protected Map<String, Object> verify(String content, String payload, String signature) {
            if (!verifySignature(content, signature)) {
                throw new TokenVerifyException(TokenVerifyException.ErrorCode.INVALID_SIGNATURE, "Signature verification failed");
            }

            JSONObject json;
            try {
                json = JSON.parseObject(base64UrlDecodeToString(payload));
            } catch (Exception e) {
                throw new TokenVerifyException(TokenVerifyException.ErrorCode.INVALID_PAYLOAD, "Parse payload as json object failed, " + e.getMessage());
            }
            
            //get claims
            Map<String, Object> claims = json.toJavaObject(Map.class);

            //verify expiration
            verifyExpiration(claims);

            return claims;
        }
    }

    protected byte[] signToBytes(String payload) {
        return sign(macAlgorithm,secretKey,payload);
    }
    
    static byte[] sign(String alg, SecretKey key, String data) {
        try {
            Mac mac = Mac.getInstance(alg);
            mac.init(key);
            return mac.doFinal(data.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Error signing data using algorithm '" + alg + ", " + e.getMessage(), e);
        }
    }
    public static String base64UrlEncode(byte[] data) {
        String encoded = new String(java.util.Base64.getUrlEncoder().encode(data));
        //removes all the '=' characters
        StringBuilder sb = new StringBuilder(encoded);
        while (sb.charAt(sb.length() - 1) == '=') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
    protected static byte[] base64UrlDecode(String encoded) throws UnsupportedEncodingException {
        for (int j = 0; j < encoded.length() % 4; j++) {
            encoded = encoded + "=";
        }
        byte[] bytes = encoded.getBytes(CharsetName.UTF8);
        byte[] decodes = java.util.Base64.getUrlDecoder().decode(bytes);
        return decodes == null ? new byte[]{} : decodes;
    }

    protected static String base64UrlDecodeToString(String encoded) throws UnsupportedEncodingException {
        return new String(base64UrlDecode(encoded), CharsetName.UTF8);
    }
    protected void verifyExpiration(Map<String, Object> claims) {
        Object exp = claims.get(CLAIM_EXPIRATION_TIME);
        if (null != exp && exp instanceof Long) {
            long expirationTime = (Long) exp;
            if (expirationTime > 0 && System.currentTimeMillis() > expirationTime) {
                throw new TokenExpiredException("Token expired");
            }
        }
    }
}
