package bingoee.sso.client;

/**
 * Created by kael on 2017/4/14.
 */
public class Args {
    public static void notEmpty(String arg, String message){
        if(arg == null || arg.trim().isEmpty()){
            throw new IllegalArgumentException(message);
        }
    }
}
