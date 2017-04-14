package bingoee.sso.client;

import java.util.Objects;

/**
 * Created by kael on 2017/4/13.
 */
public class Strings {
    public static boolean isEmpty(String str){
        return str == null || str.trim().isEmpty();
    }
    
    public static String[] split(String str, String separator){
        if(isEmpty(str)){
            return new String[]{};
        }
        return str.split(separator);
    }
    
    public static String nullOrToString(Object o){
        if(o == null){
            return null;
        }
        return Objects.toString(o);
    }
}
