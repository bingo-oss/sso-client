[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

# 品高单点登录客户端

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

SDK 版本 | Java 版本
------- | -------
3.x.x   | 6+

## 外部依赖
名称      | 版本    | 依赖说明      
-------  | ------- | -------   
fastjson | 1.2.31+ | JSON解析
slf4j    | 1.7.5+  | 程序日志       
commons-codec | 1.10+ | Base64解码 

## 使用

品高单点登录遵循[OAuth 2.0](https://tools.ietf.org/html/rfc6749)和[Open ID Connect Core](http://openid.net/specs/openid-connect-core-1_0.html)协议，使用之前了解相关概念有助于更好的理解。

### 身份认证 (Authentication)

todo : 简要描述身份认证的适用场景

```java

todo : 示例代码

```

### 登录注销 (Login & Logout)

todo : 简要描述登录注销适用场景

#### 登录

todo

#### 注销

todo

### 获取访问令牌 (Obtain Access Token)

todo : 简要描述获取访问令牌的适用场景

#### 通过授权码获取新的访问令牌

todo

#### 通过已有的访问令牌获取新的访问令牌

todo 

#### 通过id_token获取新的访问令牌

todo

## 扩展

### 自定义缓存

todo

### 自定义Http Client

todo

## 常见问题

todo


