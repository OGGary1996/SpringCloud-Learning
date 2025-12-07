package com.hmall.common.config;

import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultFeignConfig {
    /*
    * Feign的日志级别，这里配置为FULL，方便调试
    * 在各个微服务的yml中可以单独配置日志级别
    * */
    @Bean
    public Logger.Level fullFeignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /*
    * Feign的 RequestInterceptor
    * 作用：
    *  1. 在通过 Feign 调用其他微服务时，从请求头中获取到user-info并且传递给下游微服务
    * */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Long userId = UserContext.getUser();
                if (userId != null ){
                    template.header("user-info", userId.toString());
                }

            }
        };
    }

}
