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

/**
 * @since 3.0.1
 */
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
            returnUrl = Urls.getServerContextUrl(req);
            returnUrl = returnUrl + getContextPathOfReverseProxy(req);
            String state = getStateQueryParam(req,resp);
            if(!Strings.isEmpty(state)){
                returnUrl = Urls.appendQueryString(returnUrl,"__state__",state);
            }
        }

        logoutUrl = Urls.appendQueryString(logoutUrl,POST_LOGOUT_REDIRECT_URI_PARAM,returnUrl);
        return logoutUrl;
    }

    /**
     * 获取请求访问的uri，默认情况下是<code>req.getContextPath()</code>
     * 如果这个应用是通过反向代理（如：网关）的话，这里的返回值就不一定正确，此时需要重写这个方法。
     *
     * 返回当前应用的contextPath
     *
     * 示例：
     * <pre>
     *     不经过反向代理：return req.getContextPath()
     *     经过反向代理： return "/proxyPath"
     * </pre>
     *
     */
    protected String getContextPathOfReverseProxy(HttpServletRequest req){
        return req.getContextPath();
    }
    
    /**
     * 在SSO注销之后，默认会跳转回到应用根地址，这个时候有可能由于缓存导致浏览器不会自动跳转到登录页。
     * 
     * 这里返回一个随机状态码让浏览器不使用缓存页面。
     * 
     * 默认状态码是当前是时间毫秒数，如果不希望增加这个状态码，可以重写这个方法返回一个空值。
     * 
     */
    protected String getStateQueryParam(HttpServletRequest req, HttpServletResponse resp){
        return String.valueOf(System.currentTimeMillis());
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
