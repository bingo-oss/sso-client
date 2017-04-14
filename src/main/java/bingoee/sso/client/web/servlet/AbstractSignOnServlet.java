package bingoee.sso.client.web.servlet;

import bingoee.sso.client.Strings;
import bingoee.sso.client.Urls;
import bingoee.sso.client.web.WebAppConfig;
import bingoee.sso.client.web.WebAppConfigFactory;
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
        String code = req.getParameter("code");
        if(Strings.isEmpty(code)){
            redirectToLogin(req,resp);
            return;
        }else {
            validateCode(req,resp);
        }
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
    protected void validateCode(HttpServletRequest req, HttpServletResponse resp){
        String code = req.getParameter("code");
        String idToken = req.getParameter("id_token");

        IdToken it = this.manager.verifyIdToken(idToken);
        WebAppAccessToken token = this.manager.fetchAccessToken(code);
        
        
        
    }
    
    protected abstract void loginSuccess(HttpServletRequest req, HttpServletResponse resp,IdToken it,WebAppAccessToken token);
    
    protected String buildLoginUrl(HttpServletRequest req){
        String loginUrl = Urls.addQueryString(config.getAuthorizationEndpoint(),"response_type","code id_token");
        loginUrl = Urls.addQueryString(loginUrl,"client_id",config.getClient().getId());
        loginUrl = Urls.addQueryString(loginUrl,"redirect_uri",buildRedirectUri(req));
        return loginUrl;
    }
    
    protected String buildRedirectUri(HttpServletRequest req){
        return req.getRequestURL().toString();
    }
    
}
