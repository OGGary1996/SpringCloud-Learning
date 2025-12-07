package com.hmall.item.api.config;

import com.hmall.item.api.fallback.ItemClientFallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ItemClientFallbackConfig {
    /*
    * 配置ItemClient的降级处理类
    * */
    @Bean
    public ItemClientFallback itemClientFallback() {
        return new ItemClientFallback();
    }
}
