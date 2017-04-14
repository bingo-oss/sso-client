package bingoee.sso.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by kael on 2017/4/13.
 */
public class Urls {
    public static String addQueryString(String url, String name, String value){
        int qsi = url.indexOf("?");
        try {
            String append = URLEncoder.encode(name,CharsetName.UTF8) + "=" +URLEncoder.encode(value,CharsetName.UTF8);
            if(qsi > 0){
                if(qsi == url.length()-1){
                    return url+append;
                }else {
                    return url+"&"+append;
                }
            }else {
                return url+"?"+append;
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
