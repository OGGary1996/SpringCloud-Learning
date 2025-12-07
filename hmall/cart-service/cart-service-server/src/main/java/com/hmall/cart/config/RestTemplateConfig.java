package com.hmall.cart.config;/*
package com.hmall.cart.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

*/
/*
* 本配置类用于在未开始学习微服务互相调用之前，使用RestTemplate进行远程调用的配置
* *//*

@Configuration
public class RestTemplateConfig {
    @Bean
    @LoadBalanced // 开启负载均衡功能, 使RestTemplate具备通过服务名称调用服务的能力, 不需要再通过DiscoveryClient手动获取服务实例
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
*/
