package bingoee.sso.client.web.servlet;

import bingoee.sso.client.Strings;
import bingoee.sso.client.Urls;
import bingoee.sso.client.web.WebAppConfig;
import bingoee.sso.client.web.WebAppConfigFactory;
import bingoee.sso.client.web.Webs;
import bingoee.sso.client.web.verify.IdToken;
import bingoee.sso.client.web.verify.TokenManager;
import bingoee.sso.client.web.verify.WebAppAccessToken;
import bingoee.sso.client.web.verify.impl.TokenManagerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by kael on 2017/4/13.
 */
public abstract class AbstractSignOnServlet extends HttpServlet {

    protected static final String RETURN_URL_PARAM = "return_url";
    protected static final String POST_LOGOUT_REDIRECT_URI_PARAM = "post_logout_redirect_uri";
    
    protected WebAppConfig config;
    protected TokenManager manager;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        this.config = WebAppConfigFactory.generateByServletConfig(config);
        this.manager = TokenManagerFactory.generateManager(this.config);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();
        if(uri.endsWith("/login")){
            login(req,resp);
        }else if(uri.endsWith("/logout")){
            localLogout(req,resp);
        }else if(uri.endsWith("/oauth2_logout")){
            oauth2Logout(req,resp);
        }
    }

    protected void login(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
        String code = req.getParameter("code");
        if(Strings.isEmpty(code)){
            redirectToLogin(req,resp);
            return;
        }else {
            validateCode(req,resp);
        }
    }

    protected void oauth2Logout(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
        String logoutUrl = buildLogoutUrl(req);
        resp.sendRedirect(logoutUrl);
    }
    
    /**
     * 跳转到SSO登录页面
     * @param req
     * @param resp
     * @throws IOException
     */
    protected void redirectToLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String loginUrl = buildLoginUrl(req);
        resp.sendRedirect(loginUrl);
    }

    /**
     * 校验授权码并继续完成登录流程
     * @param req
     * @param resp
     */
    protected void validateCode(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String code = req.getParameter("code");
        String idToken = req.getParameter("id_token");

        IdToken it = this.manager.verifyIdToken(idToken);
        WebAppAccessToken token = this.manager.fetchAccessToken(code);
        localLogin(req,resp,it,token);
        String returnUrl = req.getParameter(RETURN_URL_PARAM);
        resp.sendRedirect(returnUrl);
    }

    /**
     * 用户在sso登录完成后的本地登录操作
     * @param req 登录请求
     * @param resp 登录响应
     * @param it idToken解析的结果，可以从这里获取userId和clientId
     * @param token 本次登录产生的access token，保存下来后可以用于调用其他应用
     */
    protected abstract void localLogin(HttpServletRequest req, HttpServletResponse resp, IdToken it, WebAppAccessToken token);
    protected void localLogout(HttpServletRequest req, HttpServletResponse resp){}
    
    
    protected String buildLoginUrl(HttpServletRequest req){
        String loginUrl = Urls.addQueryString(config.getAuthorizationEndpoint(),"response_type","code id_token");
        loginUrl = Urls.addQueryString(loginUrl,"client_id",config.getClient().getId());
        loginUrl = Urls.addQueryString(loginUrl,"redirect_uri",buildRedirectUri(req));
        return loginUrl;
    }
    
    protected String buildLogoutUrl(HttpServletRequest req){
        String postLogoutRedirectUri = req.getParameter(POST_LOGOUT_REDIRECT_URI_PARAM);
        if(Strings.isEmpty(postLogoutRedirectUri)){
            postLogoutRedirectUri = req.getParameter(RETURN_URL_PARAM);
        }
        if(Strings.isEmpty(postLogoutRedirectUri)){
            postLogoutRedirectUri = Webs.getServerBaseUrl(req);
        }
        
        String logoutUrl = Urls.addQueryString(config.getLogoutEndpoint(),POST_LOGOUT_REDIRECT_URI_PARAM,postLogoutRedirectUri);
        return logoutUrl;
    }
    
    protected String buildRedirectUri(HttpServletRequest req){
        String redirectUri = req.getRequestURL().toString();
        
        String returnUrl = req.getParameter(RETURN_URL_PARAM);
        if(Strings.isEmpty(returnUrl)){
            returnUrl = req.getContextPath();
        }
        redirectUri = Urls.addQueryString(redirectUri,RETURN_URL_PARAM, returnUrl);
        return redirectUri;
    }
    
}
