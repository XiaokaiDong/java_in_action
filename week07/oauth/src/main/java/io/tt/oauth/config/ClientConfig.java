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
