package net.bingosoft.oss.ssoclient.internal;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class Urls {
    public static String appendQueryString(String url, String name, String value) {
        
        String append = name + "=" + encode(value);
        if (url.indexOf("?") < 0){
            return url+"?"+append;
        }else {
            return url+"&"+append;
        }
        
    }

    public static String encode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Map<String, String> parseQueryString(String url){
        Map<String, String> map = new HashMap<String, String>();
        if(null == url){
            return map;
        }
        if(url.indexOf("?") < 0){
            return map;
        }
        String queryString = url.substring(url.indexOf("?")+1);
        if(queryString.isEmpty()){
            return map;
        }
        String[] kvs = queryString.split("&");
        
        for(String kv : kvs){
            int ei = kv.indexOf("=");
            String k = kv.substring(0,ei);
            String v = kv.substring(ei+1);
            map.put(k,v);
        }
        return map;
    }
    
    public static String getServerContextUrl(final HttpServletRequest request){
        String url = getServerBaseUrl(request);
        url += request.getContextPath();
        if (url.endsWith("/")){
            url = url.substring(0, url.length()-1);
        }
        return url;
    }
    
    public static String getServerBaseUrl(final HttpServletRequest request){
        String schema=request.getHeader("x-forwarded-proto");
        if(schema==null || "".equals(schema)){
            schema=request.getScheme();
        }
        schema+="://";
        String host =request.getHeader("x-forwarded-host");
        if(host==null || "".equals(host)){
            host=request.getServerName() + ":" + request.getServerPort();
        }
        String url=schema+host;
        url=regularUrl(url);

        return url;
    }

    private static String regularUrl(String url){
        //remove default port
        url += "/";
        if(url.startsWith("https") || url.startsWith("HTTPS")){
            url = url.replaceFirst(":443/", "/");
        }else{
            url = url.replaceFirst(":80/", "/");
        }

        return url.substring(0,url.length()-1);
    }
}
