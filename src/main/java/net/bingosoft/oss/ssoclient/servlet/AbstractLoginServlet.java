package net.bingosoft.oss.ssoclient.servlet;

import net.bingosoft.oss.ssoclient.SSOClient;
import net.bingosoft.oss.ssoclient.internal.Strings;
import net.bingosoft.oss.ssoclient.internal.Urls;
import net.bingosoft.oss.ssoclient.model.AccessToken;
import net.bingosoft.oss.ssoclient.model.Authentication;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.UUID;

/**
 * @since 3.0.1
 */
public abstract class AbstractLoginServlet extends HttpServlet{

    protected static final String ID_TOKEN_PARAM                 = "id_token";
    protected static final String AUTHZ_CODE_PARAM               = "code";

    private SSOClient client;

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.client = getClient(config);
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(isRedirectedFromSSO(req)){
            gotoLocalLogin(req,resp);
        }else {
            redirectToSSOLogin(req,resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req,resp);
    }

    protected void redirectToSSOLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String redirectUri = buildRedirectUri(req,resp);
        String loginUrl = buildLoginUrl(req,resp,redirectUri);

        resp.sendRedirect(loginUrl);

    }

    protected void gotoLocalLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if(checkOauth2LoginState(req,resp)){
            String idToken = req.getParameter(ID_TOKEN_PARAM);
            String code = req.getParameter(AUTHZ_CODE_PARAM);

            Authentication authc = client.verifyIdToken(idToken);
            AccessToken token = client.obtainAccessTokenByCode(code);

            localLogin(req,resp,authc,token);

            String returnUrl = req.getParameter("return_url");
            if(Strings.isEmpty(returnUrl)){
                returnUrl = Urls.getServerBaseUrl(req)+getContextPathOfReverseProxy(req);
                if(returnUrl.endsWith("//")){
                    returnUrl.substring(0,returnUrl.length()-1);
                }
            }
            resp.sendRedirect(returnUrl);
        }else {
            resp.sendError(HttpURLConnection.HTTP_BAD_REQUEST,"state has been change!");
        }
    }

    /**
     * OAuth登录过程需要校验<code>state</code>参数是否变化。
     *
     * 默认情况下这个<code>state</code>是调整到SSO登录时生成的随机码，如果这个状态被改变说明请求可能被篡改。
     *
     * 校验通过返回<code>true</code>，不通过返回false。
     *
     * @see #setOauth2LoginState(HttpServletRequest, HttpServletResponse, String)
     */
    protected boolean checkOauth2LoginState(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String state = req.getParameter("state");
        String sessionState = (String) req.getSession().getAttribute("oauth2_login_state");
        if(!Strings.equals(sessionState,state)){
            return false;
        }
        return true;
    }

    /**
     * 设置跳转到登录页面时的<code>state</code>参数。
     *
     * 默认生成一串随机{@link UUID},并去掉'-'符号作为<code>state</code>。
     *
     * @see #checkOauth2LoginState(HttpServletRequest, HttpServletResponse)
     */
    protected String setOauth2LoginState(HttpServletRequest req, HttpServletResponse resp, String authzEndpoint){
        String state = UUID.randomUUID().toString().replace("-","");
        req.getSession().setAttribute("oauth2_login_state", state);
        authzEndpoint = Urls.appendQueryString(authzEndpoint,"state",state);
        return authzEndpoint;
    }

    protected String buildLoginUrl(HttpServletRequest req, HttpServletResponse resp, String redirectUri) {
        String authzEndpoint = client.getConfig().getAuthorizationEndpointUrl();
        authzEndpoint = Urls.appendQueryString(authzEndpoint,"response_type","code id_token");
        authzEndpoint = Urls.appendQueryString(authzEndpoint,"client_id",client.getConfig().getClientId());
        authzEndpoint = Urls.appendQueryString(authzEndpoint,"redirect_uri",redirectUri);
        if(!Strings.isEmpty(req.getParameter("login_token"))){
            authzEndpoint = Urls.appendQueryString(authzEndpoint,"login_token",req.getParameter("login_token"));
        }
        authzEndpoint = setOauth2LoginState(req,resp,authzEndpoint);
        return authzEndpoint;
    }

    /**
     * 构造SSO登录完成后的回调url，一般情况下，在注册SSO应用的时候，需要保证这个uri可以通过SSO的验证。
     * 这个方法构造的url一般是如下格式：
     *
     * <pre>
     *     http(s)://${domain}:${port}/${contextPath}/ssoclient/login?${queryString}
     *     示例：
     *     http://www.example.com:80/demo/ssoclient/login?name=admin
     * </pre>
     *
     * 一般情况下要求注册client的时候，填写的回调地址(redirect_uri)必须能够验证这里构造的url实现自动完成登录的过程。
     *
     * 如果由于其他原因，回调地址不能设置为匹配这个地址的表达式，请重写这个方法，并自己处理登录完成后的回调请求。
     */
    protected String buildRedirectUri(HttpServletRequest req, HttpServletResponse resp){
        String baseUrl = Urls.getServerBaseUrl(req);

        String requestUri = parseRequestUriWithoutContextPath(req);
        String current = baseUrl + getContextPathOfReverseProxy(req) + requestUri;

        String queryString = req.getQueryString();
        if(Strings.isEmpty(queryString)){
            return current;
        }else {
            return current+"?"+queryString;
        }
    }

    /**
     * 返回一个不包含contextPath的请求路径，如:<code>/ssoclient/login</code>
     */
    protected String parseRequestUriWithoutContextPath(HttpServletRequest req){
        String requestUri = req.getRequestURI();
        String contextPath = req.getContextPath();
        requestUri = requestUri.substring(contextPath.length());
        if(requestUri.startsWith("/")){
            return requestUri;
        }else {
            return "/"+requestUri;
        }
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

    protected boolean isRedirectedFromSSO(HttpServletRequest req){
        String idToken = req.getParameter(ID_TOKEN_PARAM);
        String accessToken = req.getParameter(AUTHZ_CODE_PARAM);
        return !Strings.isEmpty(idToken) && !Strings.isEmpty(accessToken);
    }



    /**
     * 返回一个{@link SSOClient}对象
     */
    protected abstract SSOClient getClient(ServletConfig config) throws ServletException ;

    /**
     * 用户在SSO登录成功后，进行本地登录
     */
    protected abstract void localLogin(HttpServletRequest req, HttpServletResponse resp, Authentication authc, AccessToken token) throws ServletException, IOException ;
}
