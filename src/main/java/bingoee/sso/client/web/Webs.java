package bingoee.sso.client.web;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by kael on 2017/4/14.
 */
public class Webs {
    public static String getServerBaseUrl(final HttpServletRequest request){
        String schema=request.getHeader("x-forwarded-proto");
        if(schema==null || "".equals(schema)){
            schema=request.getScheme();
        }
        schema+="://";
        String host =request.getHeader("host");
        if(host==null || "".equals(host)){
            host=request.getServerName() + ":" + request.getServerPort();
        }
        String url=schema+host;
        url=regularUrl(url);

        url+=request.getContextPath();
        if (url.endsWith("/")){
            url = url.substring(0, url.length()-1);
        }

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
