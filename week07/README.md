# 小马哥JAVA实战营第七周作业


## 作业内容


> 使用 Spring Boot 来实现一个整合Gitee/或者GithubOAuth2 认证


### 选择整合GiteeOAuth2 认证

> Gitee OAuth文档地址： https://gitee.com/api/v5/oauth_doc#/

选择最安全的授权码模式。

#### 以下文字从Gitee OAuth文档中摘录。

- 应用通过 浏览器 或 Webview 将用户引导到码云三方认证页面上（ GET请求 ）

```
https://gitee.com/oauth/authorize?client_id={client_id}&redirect_uri={redirect_uri}&response_type=code
```

- 用户对应用进行授权
注意: 如果之前已经授权过的需要跳过授权页面，需要在上面第一步的 URL 加上 scope 参数，且 scope 的值需要和用户上次授权的勾选的一致。如用户在上次授权了user_info、projects以及pull_requests。则步骤A 中 GET 请求应为：

```
https://gitee.com/oauth/authorize?client_id={client_id}&redirect_uri={redirect_uri}&response_type=code&scope=user_info%20projects%20pull_requests
```

- 码云认证服务器通过回调地址{redirect_uri}将 用户授权码 传递给 应用服务器 或者直接在 Webview 中跳转到携带 用户授权码的回调地址上，Webview 直接获取code即可（{redirect_uri}?code=abc&state=xyz)

```
实际测试，回调时没有发现state参数
```

- 应用服务器 或 Webview 使用 access_token API 向 码云认证服务器发送post请求传入 用户授权码 以及 回调地址（ POST请求 ）
注：请求过程建议将 client_secret 放在 Body 中传值，以保证数据安全。

```
https://gitee.com/oauth/token?grant_type=authorization_code&code={code}&client_id={client_id}&redirect_uri={redirect_uri}&client_secret={client_secret}
```

- 码云认证服务器返回 access_token
应用通过 access_token 访问 Open API 使用用户数据。

- 当 access_token 过期后（有效期为一天），你可以通过以下 refresh_token 方式重新获取 access_token（ POST请求 ）

```
#此步没有做！
https://gitee.com/oauth/token?grant_type=refresh_token&refresh_token={refresh_token}
```

#### 选择使用Spring Boot

> 为了简单（省事），没有选择MVC分层，全部功能在controller中完成^_^

- 配置文件如下

```properties
server.port=8080

oauth.client.id=37eba7a7126c5e29.....
oauth.client.secret=365ac1e46e69d....
oauth.client.oauthUrl=https://gitee.com:443/oauth/authorize
oauth.client.redirectUri=http://h5tf0hmt.xiaomy.net/oauth
oauth.client.scope="user_info emails"
oauth.client.access_token_endPoint=https://gitee.com/oauth/token?grant_type=authorization_code&code={code}&client_id={client_id}&redirect_uri={redirect_uri}&client_secret={client_secret}
oauth.client.user_info_endPoint=https://gitee.com/api/v5/user?access_token={access_token}


```

- 对应的配置类为ClientConfig

```java
package io.tt.oauth.config;

import io.tt.oauth.domain.OAuth2Client;
import io.tt.oauth.util.ssl.HttpsClientRequestFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.beans.ConstructorProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ClientConfig {
    @Bean
    @ConfigurationProperties("oauth.client")
    OAuth2Client oAuth2Client(){
        return new OAuth2Client();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = null;
        restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(1000))
                .setReadTimeout(Duration.ofMillis(1000))
                .requestFactory(() -> new HttpsClientRequestFactory())
                .build();
        restTemplate.setMessageConverters(getConverts());
        return restTemplate;
    }

    //如果不定义HttpMessageConverter，在使用RestTemplate时会报错401
    private List<HttpMessageConverter<?>> getConverts() {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        // String转换器
        StringHttpMessageConverter stringConvert = new StringHttpMessageConverter();
        List<MediaType> stringMediaTypes = new ArrayList<MediaType>() {{
            //添加响应数据格式，不匹配会报401
            add(MediaType.TEXT_PLAIN);
            add(MediaType.TEXT_HTML);
            add(MediaType.APPLICATION_JSON);
        }};
        stringConvert.setSupportedMediaTypes(stringMediaTypes);
        messageConverters.add(stringConvert);
        return messageConverters;
    }
}

```

- OAuth2Client类就是OAUTH2中的第三方服务（相对于GITEE来说）

> - "我"在GITEE上注册后，就是资源拥有者
> - GITEE是授权服务器
> - “我”的头像就是受保护资源
> - 这个程序代表第三方服务

```java
package io.tt.oauth.domain;

import lombok.Data;

@Data
public class OAuth2Client {
    private String id;
    private String secret;
    private String redirectUri;
    private String scope;
    private String oauthUrl;
    private String accessTokenEndPoint;
    private String userInfoEndPoint;
}

```

- 所有的逻辑都集中在OAuthController中

  - 用户登录页

    > 没有写前端页面（不会！），直接重定向到GITEE码云三方认证页面

    ```java
    /**
     * 将用户引导到码云三方认证页面上
     * @param response 响应，用于重定向
     */
    @GetMapping("/login_with_gitee")
    public void loginWithGitee( HttpServletResponse response) {
        log.info("登录请求开始...");

        Map<String, String> params = new HashMap<String, String>();
        params.put("response_type","code");
        params.put("redirect_uri",oAuth2Client.getRedirectUri());
        params.put("client_id", oAuth2Client.getId());

        String toOauthUrl = URLParamsUtil.appendParams(oAuth2Client.getOauthUrl(),params);//构造请求授权的URl

        log.info("重定向到授权服务：{}", toOauthUrl);

        try {
            //码云三方认证页面:
            //https://gitee.com/oauth/authorize?client_id={client_id}&redirect_uri={redirect_uri}&response_type=code
            response.sendRedirect(toOauthUrl);//授权码流程的重定向
        } catch (IOException e) {
            log.info("授权重定向失败：{}", e.getMessage());
        }
    }
    ```

  - 用户在码云三方认证页面上做完必要的登录（身份认证）后， 选择相应的权限，点击授权，携带授权码又重定向到本服务

    ```java
    /**
     * 用于用户授权后接收授权码，然后使用这个授权码获取access token.
     * 应用服务器 或 Webview 使用 access_token API 向 码云认证服务器发送post请求传入 用户授权码 以及 回调地址（ POST请求 ）
     * 注：请求过程建议将 client_secret 放在 Body 中传值，以保证数据安全。
     * url: https://gitee.com/oauth/token?grant_type=authorization_code&code={code}&client_id={client_id}&redirect_uri={redirect_uri}&client_secret={client_secret}
     * @param code 授权码
     */
    @GetMapping("/oauth")
    public void getCode(@RequestParam String code){
        Map<String, String> params = new HashMap<String, String>();

        params.put("client_secret", oAuth2Client.getSecret());

        log.info("获得授权码: [{}]", code);

        ...

    }
    ```

  - 直接使用授权码申请access token

    ```java
    URI accessTokenUri = UriComponentsBuilder
            .fromUriString(oAuth2Client.getAccessTokenEndPoint())
            .build(code, oAuth2Client.getId(),
                    oAuth2Client.getRedirectUri(),
                    oAuth2Client.getSecret());

    log.info("请求access_toke的uri为: {}", accessTokenUri);
    log.info("报文体为: {}", URLParamsUtil.mapToStr(params));

    ResponseEntity<String> respToken = restTemplate.postForEntity(accessTokenUri, URLParamsUtil.mapToStr(params) ,String.class);
    log.info("Response Status: {}, Response Headers: {}", respToken.getStatusCode(), respToken.getHeaders().toString());
    String accessTokenJson = respToken.getBody();

    log.info("获取到包含access_token的JSON字符串为: {}", accessTokenJson);

    JSONObject jsonObject1 = JSONObject.parseObject(accessTokenJson);
    String accessToken = jsonObject1.getString("access_token");

    log.info("获取到的access_token为： {}", accessToken);
    ```

  - 有了access token后，就可以调用接口了，这里以获取用户信息接口为例(API文档参见https://gitee.com/api/v5/swagger#/getV5ReposOwnerRepoStargazers?ex=no)

    > https://gitee.com/api/v5/user?access_token={access_token}

    ```java
    //读取用户资料
    URI userInfoUri = UriComponentsBuilder
            .fromUriString(oAuth2Client.getUserInfoEndPoint())
            .build(accessToken);
    

    ResponseEntity<String> respUserInfo = restTemplate.getForEntity(userInfoUri, String.class);
    log.info("获取到的用户信息返回码：{}", respUserInfo.getStatusCode());
    log.info("获取到的用户信息为：{}", respUserInfo.getBody());
    ```

#### 在码云上注册第三方应用

方法参见https://gitee.com/api/v5/oauth_doc#/。注册完成后就可以获取Client ID以及Client Secret，将其配置到程序中。

#### 内网穿透

使用网云传，可以免费申请一个域名以及隧道。http://www.neiwangchuantou.net/