package com.hmall.cart.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignLogConfig {
    /*
    * 本类用于配置Feign客户端的日志级别。
    * */
    @Bean
    public Logger.Level feignLoggerLevel() {
        // 设置Feign日志级别为FULL，记录请求和响应的完整信息
        return Logger.Level.FULL;
    }
}
