package net.bingosoft.oss.ssoclient.servlet;

import net.bingosoft.oss.ssoclient.SSOClient;
import net.bingosoft.oss.ssoclient.internal.Strings;
import net.bingosoft.oss.ssoclient.internal.Urls;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public abstract class AbstractLogoutServlet extends HttpServlet {

    protected static final String POST_LOGOUT_REDIRECT_URI_PARAM = "post_logout_redirect_uri";
    
    private SSOClient client;

    @Override
    public void init(ServletConfig config) throws ServletException {
        client = getClient(config);
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(isLocalLogoutRequest(req,resp)){
            localLogout(req,resp);
        }else {
            redirectToSSOLogout(req,resp);
        }
    }

    /**
     * 
     * 注销请求分两种：
     * 
     * <ul>
     *     <li>注销本地</li>
     *     <li>注销SSO</li>
     * </ul>
     * 
     * 这里返回<code>true</code>表示当前请求是<strong>注销本地</strong>请求，返回<code>false</code>
     * 表示是<strong>注销SSO</strong>请求。
     * 
     * 默认情况下：
     * <ul>
     *     <li>请求路径以<code>/oauth2_logout</code>结尾认为是<strong>注销SSO</strong></li>
     *     <li>其他所有情况都认为是<strong>注销本地</strong></li>
     * </ul>
     *     
     * 注销本地的请求地址，在SSO注册应用的时候需要填写，可以根据注册应用上填写的注销地址(logout_uri)重写这个方法来判断是否
     * 注销本地的请求，所有非注销本地请求的注销请求都会跳转到SSO进行注销。
     * 
     */
    protected boolean isLocalLogoutRequest(HttpServletRequest req, HttpServletResponse resp) {
        return !req.getRequestURI().endsWith("/oauth2_logout");
    }

    protected void redirectToSSOLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ssoLogoutUrl = buildSSOLogoutUrl(req,resp);
        resp.sendRedirect(ssoLogoutUrl);
    }
    
    protected String buildSSOLogoutUrl(HttpServletRequest req, HttpServletResponse resp) {
        String logoutUrl = client.getConfig().getOauthLogoutEndpoint();

        String returnUrl = req.getParameter(POST_LOGOUT_REDIRECT_URI_PARAM);
        if(Strings.isEmpty(returnUrl)){
            returnUrl = Urls.getServerBaseUrl(req);
            returnUrl = Urls.appendQueryString(returnUrl,"__time__",String.valueOf(System.currentTimeMillis()));
        }

        logoutUrl = Urls.appendQueryString(logoutUrl,POST_LOGOUT_REDIRECT_URI_PARAM,returnUrl);
        return logoutUrl;
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req,resp);
    }


    /**
     * 返回一个{@link SSOClient}对象
     */
    protected abstract SSOClient getClient(ServletConfig config) throws ServletException ;

    /**
     * 进行本地注销
     */
    protected abstract void localLogout(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
}
