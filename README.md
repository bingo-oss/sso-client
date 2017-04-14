# 前言

这个工程用于品高软件的sso单点登录产品（v3版本）接入的开发sdk，使用这个sdk可以简单方便得接入品高的单点登录服务。

# 安装

* maven安装

如果使用maven构建工程，使用这个sdk只需要添加依赖：

```xml
<dependency>
	<groupId>net.bingosoft</groupId>
	<artifactId>sso-client</artifactId>
	<version>3.1.3-SNAPSHOT</version>
</dependency>
```

* 打包使用

如果不是使用maven安装工程，需要自己打包jar包，这个sdk需要三个外部依赖：

```xml
<dependency>
	<groupId>com.alibaba</groupId>
	<artifactId>fastjson</artifactId>
	<version>1.2.31</version>
</dependency>
<dependency>
	<groupId>org.slf4j</groupId>
	<artifactId>slf4j-api</artifactId>
	<version>1.7.5</version>
	<type>jar</type>
</dependency>
<dependency>
	<groupId>org.slf4j</groupId>
	<artifactId>jcl-over-slf4j</artifactId>
	<version>1.7.5</version>
	<type>jar</type>
</dependency>
```

打包的时候注意要把这几个依赖包也加入到工程依赖中。

# 接入SSO

品高得sso按照[OpenId Connect](http://openid.net/specs/openid-connect-core-1_0.html)实现，需要对该协议有一定了解。

## web端接入

web接入sso的实现流程如下：

![登录流程](./login_flow.png)

从上面的时序图中，我们可以看到，sdk在第3步开始接管应用的登录过程，一直到第12步完成登录后重定向回到应用才结束登录过程。

### 实现本地登录逻辑

在登录流程中第11步可以看到，用户完成在sso的登录之后，会进入本地登录的流程，这个过程主要是sso服务器登录完成之后，用户需要在本地应用保存登录状态，以免下次访问本地应用要重新跳转到sso登录，这里sdk已经将3-10步的流程封装好了，第11步提供了一个抽象类，需要应用自己实现本地登录的逻辑，因此应用需要实现抽象类`bingoee.sso.client.web.servlet.AbstractSignOnServlet`的抽象方法：

```java
/**
 * 用户在sso登录完成后的本地登录操作,这个接口必须实现
 * @param req 登录请求
 * @param resp 登录响应
 * @param it idToken解析的结果，可以从这里获取userId和clientId
 * @param token 本次登录产生的access token，保存下来后可以用于调用其他应用
 */
protected abstract void localLogin(HttpServletRequest req, HttpServletResponse resp, IdToken it, WebAppAccessToken token);
```

示例如下：

```java
package net.bingosoft.LoginServlet;

public class LoginServlet extends AbstractSignOnServlet {
    @Override
    protected void localLogin(HttpServletRequest req, HttpServletResponse resp, 
	IdToken it, WebAppAccessToken token) {
        req.getSession().setAttribute("user",new LoginUser(it,token));
    }
}
```

这里假设只要把登录信息设置到`session`的`attribute`中即可完成登录。

这样就完成登录逻辑了，`LoginServlet`这个类将作为处理登录请求的`servlet`。

### 配置sdk的访问路径

处理登录请求的类已经写好了，接下来我们需要配置sdk的访问路径，以便让sdk可以接管登录过程，一般来说，需要在`web.xml`中配置`servlet`来实现sdk接管请求：

```xml
<servlet>
	<servlet-name>login</servlet-name>
	<servlet-class>net.bingosoft.LoginServlet</servlet-class>
	<!-- sso的根路径，不要带/后缀 -->
	<init-param>
		<param-name>sso.endpoint</param-name>
		<param-value>http://host:port/context</param-value>
	</init-param>
	<!-- oauth2 定义的client id -->
	<init-param>
		<param-name>clientId</param-name>
		<param-value>clientId</param-value>
	</init-param>
	<!-- oauth2 定义的client secret -->
	<init-param>
		<param-name>clientSecret</param-name>
		<param-value>clientSecret</param-value>
	</init-param>
</servlet>

<servlet-mapping>
	<servlet-name>login</servlet-name>
	<url-pattern>/sso_client/*</url-pattern>
</servlet-mapping>
```

上面配置了sdk的请求路径是`/sso_client/*`，这表示sdk需要接管所有访问到`/sso_client/`这个路径下的子路径。

> **注意**：这里这个路径不能改变，另外，如果使用的应用有自己的安全拦截器，需要忽略对这个路径的拦截，因为在登录完成前，访问这个路径的请求都是没有用户身份的，如果安全拦截器拦截了这个路径，会导致请求无法被sdk接管，进入无限重定向到登录页面的循环。

这里我们还注意到几个初始化参数的配置：

* sso.endpoint：这个是sso的应用根路径，就是部署sso的地址，不需要/结尾
* clientId：在oauth2规范中，定义了应用身份，包含应用的唯一标识和应用密码，这里需要配置这个应用的应用id作为应用标识
* clientSecret：应用密码

> 应用标识和应用密码是应用的身份凭据，也是sso信任应用的基础，因此应用要接入sso，**需要先在sso申请注册应用**，并获得sso颁发的应用id和应用secret。

