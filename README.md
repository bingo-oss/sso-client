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

|SDK 版本 | Java 版本|
| ------ | -------  |
|3.0.0   |6+        |

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

### 1. 创建`SSOClient`对象

创建一个`SSOClient`对象需要先构造一个`SSOConfig`对象，示例如下：

```java
// SSOClient的构造器
public class ClientGenerator{
    public SSOClient generate(){
        // 创建SSOConfig对象
        SSOConfig config = new SSOConfig();
        // 设置应用标识
        config.setClientId("clientId");
        // 设置应用密钥
        config.setClientSecret("clientSecret");
        
        // 设置应用获取SSO服务器公钥（public key）的url
        // 如：http://sso.example.com/publickey
        // 这里也可以使用config.autoConfigureUrls("http://sso.example.com")自动配置
        config.setPublicKeyEndpointUrl("http://sso.example.com/publickey");
        // 创建client对象
        SSOClient client = new SSOClient(config);
        return client;
    }
}
```

### 2. 身份认证 (Authentication)

当需要开发web服务对外提供Restful服务时，对于遵循[OAuth 2.0](https://tools.ietf.org/html/rfc6749)标准协议的请求，如果需要校验用户身份，可以使用如下方式：

```java
import net.bingosoft.oss.ssoclient.*;
import net.bingosoft.oss.ssoclient.model.Authentication;
import net.bingosoft.oss.ssoclient.exception.InvalidTokenException;
import net.bingosoft.oss.ssoclient.exception.TokenExpiredException;

public class DemoServlet extends javax.servlet.http.HttpServlet{
    
    protected SSOClient client;
    
    @Override
    public void init() throws ServletException {
        // 使用前面的构造器创建一个SSOClient对象
        this.client = new ClientGenerator().generate();
    }
    
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
        // 获取用户登录名
        String username = authc.getUsername();
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

### 3. 登录注销 (Login & Logout)

todo : 简要描述登录注销适用场景

#### 3.1 登录

todo

#### 3.2 注销

todo

### 4. 获取访问令牌 (Obtain Access Token)

todo : 简要描述获取访问令牌的适用场景

#### 4.1 通过授权码获取新的访问令牌

todo

#### 4.2 通过已有的访问令牌获取新的访问令牌

todo 

#### 4.3 通过id_token获取新的访问令牌

todo

## 扩展

### 自定义缓存

对于access token的校验结果，sdk中提供了简单的缓存实现`net.bingosoft.oss.ssoclient.spi.CacheProviderImpl`，在实际应用中我们可能需要根据需求定制校验缓存。

定制缓存需要实现`net.bingosoft.oss.ssoclient.spi.CacheProvider`接口，并用缓存实现类的对象覆盖默认的CacheProvider，示例如下：

```java
import net.bingosoft.oss.ssoclient.*;

// 创建新的缓存实现
class CustomCacheProvider implements net.bingosoft.oss.ssoclient.spi.CacheProvider{
    @Override
    public <T> T get(String key) {
        // 根据传入的key获取已缓存的对象，在校验access token的过程中，这里传入的key是access token
    }

    @Override
    public void put(String key, Object item, long expires) {
        // 根据传入的key和item缓存对象item，这里expires是缓存过期时间，在缓存过期后需要清理缓存
    }

    @Override
    public void remove(String key) {
        // 根据key将缓存的对象清除，在校验access token的过程中，这里传入的key是access token
    }
}

// 使用定制的缓存对象
// SSOClient的构造器
public class ClientGenerator{
    public SSOClient generate(){
        SSOConfig config = new SSOConfig();
        // 省略SSOConfig对象的配置过程
        SSOClient client = new SSOClient(config);
        
        
        // 设置client对象的CacheProvider
        client.setCacheProvider(new CustomCacheProvider());
        return client;
    }
}
```

## 常见问题

todo


