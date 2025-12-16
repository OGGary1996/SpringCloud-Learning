package com.hmall.pay.api.config;

import com.hmall.pay.api.fallback.PayClientFallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayClientFallbackConfig {
    /*
    * 配置类，用于指定 PayClient 的降级处理类 PayClientFallback
    * */
    @Bean
    public PayClientFallback payClientFallback() {
        return new PayClientFallback();
    }
}
