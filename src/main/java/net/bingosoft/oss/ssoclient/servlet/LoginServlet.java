package net.bingosoft.oss.ssoclient.servlet;

import net.bingosoft.oss.ssoclient.SSOClient;
import net.bingosoft.oss.ssoclient.internal.Strings;
import net.bingosoft.oss.ssoclient.model.AccessToken;
import net.bingosoft.oss.ssoclient.model.Authentication;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class LoginServlet extends HttpServlet{
    
    protected static final String ID_TOKEN_PARAM = "id_token";
    protected static final String AUTHZ_CODE_PARAM = "code";
    
    private SSOClient client;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        this.client = getClient(config);
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(isLogin(req)){
            gotoLoginSuccess(req,resp);
        }else {
            gotoLogin(req,resp);
        }
    }

    protected void gotoLoginSuccess(HttpServletRequest req, HttpServletResponse resp) {
        // TODO:
        String idToken = req.getParameter(ID_TOKEN_PARAM);
        String code = req.getParameter(AUTHZ_CODE_PARAM);
        Authentication authc = client.verifyIdToken(idToken);
        AccessToken token = client.obtainAccessToken(code);
        
        
    }


    protected void gotoLogin(HttpServletRequest req, HttpServletResponse resp){
        // TODO:
    }
    
    protected boolean isLogin(HttpServletRequest req){
        String idToken = req.getParameter(ID_TOKEN_PARAM);
        String accessToken = req.getParameter(AUTHZ_CODE_PARAM);
        return Strings.isEmpty(idToken) || Strings.isEmpty(accessToken);
    }
    
    /**
     * 返回一个{@link SSOClient}对象
     */
    protected abstract SSOClient getClient(ServletConfig config);

    /**
     * 用户在SSO登录成功后，进行本地登录
     */
    protected abstract void loginSuccess(HttpServletRequest req, HttpServletResponse resp, Authentication authc, AccessToken token);
}
