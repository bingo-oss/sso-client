[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.bingosoft.oss/sso-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.bingosoft.oss/sso-client/badge.svg) [![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

# 品高单点登录客户端(Java)

品高单点登录遵循[OAuth 2.0](https://tools.ietf.org/html/rfc6749)和[Open ID Connect Core](http://openid.net/specs/openid-connect-core-1_0.html)协议，在继续阅读前请先了解一下相关的概念。

## 安装
**Maven**

```xml
<dependency>
	<groupId>net.bingosoft.oss</groupId>
	<artifactId>sso-client</artifactId>
	<version>[3.0.1,]</version>
</dependency>
```

## 运行环境

|SDK 版本 | Java 版本|
| ------ | -------  |
|3.x.x   |6+        |

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

> 注：如何注册一个应用不在此文档中描述

## 使用

### 1. 配置`SSOClient`对象

配置`SSOClient`对象需要先构造一个`SSOConfig`对象，示例如下：

```java
// 创建SSOConfig对象
SSOConfig config = new SSOConfig();
// 设置应用标识
config.setClientId("clientId");
// 设置应用密钥
config.setClientSecret("clientSecret");
// 设置回调地址
ssoConfig.setRedirectUri(redirectUri);
// 根据SSO地址自动配置其他地址
config.autoConfigureUrls("http://sso.example.com");

// 创建client对象
SSOClient client = new SSOClient(config);
```

### 2. 身份认证 (Authentication)

在Restful API中，对于遵循[OAuth 2.0](https://tools.ietf.org/html/rfc6749)标准协议的请求，使用如下方式校验用户身份：

```java
HttpServletRequest req;

// 获取access token
String accessToken = SSOUtils.extractAccessToken(req);

Authentication authc = null;
try{
    authc = client.verifyAccessToken(accessToken);            
}catch (InvalidTokenException e){
    // 处理access token无效的情况
}catch (TokenExpiredException e){
    // 处理access token过期的情况
}
// userId:用户ID,username:用户登录名(loginName),clientId:应用ID,scope:授权列表，expires:过期时间
String userId = authc.getUserId();
String username = authc.getUsername();
String client = authc.getClientId();
String scope = authc.getScope();
// 获取access token的过期时间，这个过期时间指的是距离标准日期1970-01-01T00:00:00Z UTC的秒数
long expires = authc.getExpires();
```

### 3. 登录注销 (Login & Logout)

对于普通的web应用，使用SDK按照如下方式接入品高SSO实现单点登录和单点注销。

#### 3.1 登录

1. 实现一个`AbstractLoginServlet`的HttpServlet

```java
package demo;

//...

public class LoginServlet extends net.bingosoft.oss.ssoclient.servlet.AbstractLoginServlet {
    @Override
    protected SSOClient getClient(ServletConfig config) {
        // 返回一个配置好的SSOClient对象
        return new SSOClient();
    }
    @Override
    protected void localLogin(HttpServletRequest req, HttpServletResponse resp, Authentication authc,
                              AccessToken token) {
        // 省略本地登录代码...
    }
}
```

2. 在`web.xml`中配置这个实现类的访问路径

```xml
<servlet>
    <servlet-name>ssologin</servlet-name>
    <servlet-class>demo.LoginServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>ssologin</servlet-name>
    <!-- 这个servlet的访问地址，非必要的情况建议不要修改 -->
    <url-pattern>/ssoclient/login</url-pattern>
</servlet-mapping>
```

3. 设置登录跳转地址

在web应用中，对所有需要登录的请求重定向到`/ssoclient/login`这个地址上进行单点登录。

> 注：可以通过设置return_url参数决定登录完成后跳转的地址,参数值是经过url编码的地址，如：  
> `return_url=http%3A%2F%2Flocalhost%3A8080%2Fdemo`

到这里接入配置完成。

#### 3.2 注销

单点注销只要跳转到SSO注销地址注销即可。

```java
// HttpServletResponse resp;
// SSOClient client;
// 注销后的返回地址
String returnUrl = "http://www.example.com";
String ssoLogoutUrl = SSOUtils.getSSOLogoutUrl(client,returnUrl);
resp.sendRedirect(ssoLogoutUrl);
```

### 4. 获取访问令牌 (Obtain Access Token)

访问令牌(access token)是用来代表请求发起者的身份的，一般来说访问令牌有两种可能：

* **仅代表应用身份**：用于调用只需要验证应用身份的服务 
* **同时代表用户和应用的身份**：用于调用同时需要验证用户身份和应用身份的服务，也可以用于只校验应用的情况

访问令牌一般有三个属性：

```java
AccessToken token = new AccessToken();

// 访问令牌，真正代表用户和应用身份的令牌
String accessToken = token.getAccessToken();

// 刷新令牌，当这个访问令牌过期后，可以用刷新令牌换取新的访问令牌
String refreshToken = token.getRefreshToken();

// 过期时间，指的是距离标准日期1970-01-01T00:00:00Z UTC的秒数
long expires = token.getExpires();
```

访问令牌的获取有如下几种方式。

#### 4.1 通过授权码(Authorization Code)获取新的访问令牌

授权码的获取可以参考[OpenId Connect CodeFlowSteps](http://openid.net/specs/openid-connect-core-1_0.html#CodeFlowSteps)。
获取到授权码`code`后，可以利用`code`获取访问令牌：

```java
AccessToken token = client.obtainAccessTokenByCode(code);
```

这个令牌代表的身份由授权码决定，一般是代表用户身份和生成这个`code`的应用身份。

> 注：这里使用client对象验证授权码生成访问令牌，因此访问令牌代表的是client的身份和用户身份。

#### 4.2 通过应用凭证(client credentials)获取访问令牌

当我们的服务需要调用另一个服务的时候，如果被调用的服务需要并且**只需要确认client身份**，这个时候可以使用仅代表client身份的访问令牌：

```java
AccessToken token = client.obtainAccessTokenByClientCredentials();
```

这里使用client自己的访问凭证获取访问令牌，这个令牌只能代表client自己的身份。

#### 4.3 通过已有的访问令牌获取新的访问令牌

当我们的服务需要调用另一个服务，并且被调用的服务**需要同时验证用户身份和client身份**的时候，这个时候我们需要一个能代表用户身份和client身份的访问令牌。

在我们的服务接收请求的时候，已经获取到一个代表用户身份的访问令牌。

```java
// HttpServletRequest req;
String accessToken = SSOUtils.extractAccessToken(req);
AccessToken clientAndUser = client.obtainAccessTokenByToken(accessToken);
```

这里`accessToken`代表的是用户身份，`clientAndUser`代表的是用户身份和client的身份。
使用`clientAndUser`这个访问令牌就可以调用另一个服务了。

## 扩展

### 自定义缓存

SDK中提供了简单的access token校验缓存实现，在实际应用中可以根据需求定制CacheProvider。

定制CacheProvider需要实现`CacheProvider`接口，并用实现类的对象覆盖默认的CacheProvider，示例如下：

```java
class CustomCacheProvider implements net.bingosoft.oss.ssoclient.spi.CacheProvider{
    // 省略实现代码...
}
```

使用定制的CacheProvider对象

```java
SSOClient client = new SSOClient(config);
client.setCacheProvider(new CustomCacheProvider());
```

## 常见问题

**问：配置好单点登录后，在跳转到SSO时浏览器收到`invalid_request:invalid redirect_uri`错误。**

答：这是由于注册应用时设置的回调地址(redirect_uri)不能匹配SDK生成的回调地址导致的，SDK生成的回调地址一般是如下格式：

```
http(s)://${domain}:${port}/${contextPath}/ssoclient/login?${queryString}
如：http://www.example.com:80/demo/ssoclient/login?name=admin
```

请自行查阅SSO应用注册相关文档，确认应用的回调地址能匹配SDK生成的回调地址，如：

```
http://www.example.com:80/demo/ssoclient/**
```

----

**问：配置好单点登录后，访问应用后出现重定向次数过多**

答：检查是否已经忽略`/ssoclient/login`这个地址的登录校验。

----

**问：配置好单点登录后，登录时抛出`Connection refused: connect[xxx]`**

答：检查`SSOConfig.getTokenEndpointUrl()`返回的地址，在web应用部署的服务器是否可以访问。

----

**问：配置好单点登录后，登录时抛出`HTTP Status 500 - parse json error`**

答：检查`SSOConfig.getTokenEndpointUrl()`的返回值：
* 是否正确，如果json是一串html代码，很有可能是这个地址配错了。
* 返回结果是否正确的json。

----

**问：跳转到SSO注销完成后，为什么本地登录没有被注销？**

答：SSO注销完成后，会根据应用在SSO注册的注销地址(logout_uri)向应用发注销请求，如果本地注销不了，需要检查：
* 应用注册的注销地址是不是应用配置的本地注销地址
* 应用本地注销的地址是否被其他拦截器拦截

----

**问：配置好单点登录后，如果使用了反向代理，SDK登录重定向的url不对怎么办？**

答：使用反向代理的时候，要配置代理:
* 在请求头设置`host`请求头为代理服务器地址
* 设置`x-forwarded-proto`请求头为访问协议(http或https)
* 根据代理后的contextPath重写`AbstractLoginServlet.getContextPathOfReverseProxy(req)`方法

