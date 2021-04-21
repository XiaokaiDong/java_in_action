package io.tt.oauth.controller;

import com.alibaba.fastjson.JSONObject;
import io.tt.oauth.domain.OAuth2Client;
import io.tt.oauth.util.URLParamsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;


/**
 * 码云OAuth2 认证基本流程控制器
 * 参见 https://gitee.com/api/v5/oauth_doc#/
 */
@RestController
@Slf4j
public class OAuthController {

    @Autowired
    private OAuth2Client oAuth2Client;

    @Autowired
    private RestTemplate restTemplate;

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

        //读取用户资料
        URI userInfoUri = UriComponentsBuilder
                .fromUriString(oAuth2Client.getUserInfoEndPoint())
                .build(accessToken);
        

        ResponseEntity<String> respUserInfo = restTemplate.getForEntity(userInfoUri, String.class);
        log.info("获取到的用户信息返回码：{}", respUserInfo.getStatusCode());
        log.info("获取到的用户信息为：{}", respUserInfo.getBody());
    }
}
