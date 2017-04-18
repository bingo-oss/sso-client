package net.bingosoft.oss.ssoclient.internal;

/**
 * Created by kael on 2017/4/18.
 */
public class Strings {
    public static String nullOrToString(Object o){
        if(o == null){
            return null;
        }
        return o.toString();
    }
}
