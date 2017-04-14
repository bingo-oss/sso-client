package bingoee.sso.client.web;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * Created by kael on 2017/4/14.
 */
public class WebAppConfigFactory {
    public static WebAppConfig generateByServletConfig(ServletConfig config) throws ServletException {
        return new WebAppConfigImpl(config);
    }
}
