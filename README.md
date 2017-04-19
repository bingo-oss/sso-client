[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

# 品高单点登录客户端(Java)

品高单点登录遵循[OAuth 2.0](https://tools.ietf.org/html/rfc6749)和[Open ID Connect Core](http://openid.net/specs/openid-connect-core-1_0.html)协议，在继续阅读前请先了解一下相关的概念。

## 安装
**Maven**

```xml
<dependency>
	<groupId>net.bingosoft.oss</groupId>
	<artifactId>sso-client</artifactId>
	<version>[3.0.0,]</version>
</dependency>
```

## 运行环境

|SDK 版本 | Java 版本|备注|
| ------- | -------  | ---- |
|未发布   | 6+       |最新稳定版|
|3.0.0-SNAPSHOT|6+|最新快照版|

## 外部依赖
|名称      | 版本    | 依赖说明|      
| ------- | ------- | ------- |  
|fastjson | 1.2.31+ | JSON解析 |
|slf4j    | 1.7.5+  | 程序日志 |      
|commons-codec | 1.10+ | Base64解码 |

## 前提条件

在使用之前需要到单点登录服务中注册一个应用，申请以下参数：

|参数           | 必须 | 说明 |
| -------      | ---- | -------- | 
|client_id     | 是    | 应用的标识 |
|client_secret | 是    | 应用的密钥 |
|redirect_uri  | 否    | 应用登录后返回的地址，用到登录功能才需要注册 |

`注：如何注册一个应用不在此文档中描述`

## 使用

### 1. 身份认证 (Authentication)

当需要开发应用作为资源服务器对外提供Restful服务时，对于遵循[OAuth 2.0](https://tools.ietf.org/html/rfc6749)标准协议的请求，如果需要校验
用户身份，可以使用如下方式：

```java
import net.bingosoft.oss.ssoclient.*;
import net.bingosoft.oss.ssoclient.model.Authentication;
import net.bingosoft.oss.ssoclient.exception.InvalidTokenException;
import net.bingosoft.oss.ssoclient.exception.TokenExpiredException;

public class Api extends javax.servlet.http.HttpServlet{
    
    protected SSOClient client;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 获取access token
        String accessToken = SSOUtils.extractAccessToken(req);
        // 验证access token
        Authentication authc = null;
        try{
            authc = client.verifyAccessToken(accessToken);            
        }catch (InvalidTokenException e){
            // 处理access token无效的情况
        }catch (TokenExpiredException e){
            // 处理access token过期的情况
        }
        // 获取用户id
        String userId = authc.getUserId();
        // 获取客户端应用id
        String client = authc.getClientId();
        // 获取access token的授权列表
        String scope = authc.getScope();
        // 获取access token的过期时间，这个过期时间指的是距离标准日期1970-01-01T00:00:00Z UTC的秒数
        long expires = authc.getExpires();
        
        // 根据Authentication获取用户其他信息的业务代码省略...
        
        // 返回处理成功的结果
        resp.getWriter().write("ok");
    }
}
```

### 2. 登录注销 (Login & Logout)

todo : 简要描述登录注销适用场景

#### 2.1 登录

todo

#### 2.2 注销

todo

### 3. 获取访问令牌 (Obtain Access Token)

todo : 简要描述获取访问令牌的适用场景

#### 3.1 通过授权码获取新的访问令牌

todo

#### 3.2 通过已有的访问令牌获取新的访问令牌

todo 

#### 3.3 通过id_token获取新的访问令牌

todo

## 扩展

### 自定义缓存

todo

### 自定义Http Client

todo

## 常见问题

todo


