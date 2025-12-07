package com.hmall.common.config;

import com.hmall.common.interceptor.UserInfoInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
* 本类用于配置Spring MVC相关的设置
* 注意：
*  1. 本类位于 com.hmall.common.config 包中，但其他的微服务模块的包名是 com.hmall.[moduleName]
*  2. 所以本类无法被其他的微服务扫描到，默认只能扫描相同包名的类
*  3. 想要本配置类对所有的微服务都生效，则需要依赖SpringBoot的自动装配机制：
*  4. 在resource目录下定义META-INF/spring.factories文件，并且将本类的全类名添加进去
*  5. 在所有微服务的启动类中@SpringBootApplication注解中其实包含了@EnableAutoConfiguration注解，会自动扫描所有模块的META-INF/spring.factories文件
*  5. 这样Spring Boot在启动时会自动加载本配置类，从而实现配置的共享
* */
@Configuration
/*
* 注意：
*  1. 由于自动装配的原理，本配置类也会对gateway-service生效，而gateway-service中的过滤器会与拦截器冲突
*  2. 所以需要使用@Conditional注解来限制本配置类在gateway-service中不生效
*  3. gateway-service与其他所有模块的核心区别是，gateway-service模块并不依赖SpringMVC，而是基于响应式编程模型的WebFlux
*  4. 因此可以通过 SpringMVC的核心类DispatcherServlet类是否存在来决定是否生效
* */
@ConditionalOnClass(DispatcherServlet.class) // 在类路径中存在DispatcherServlet类时，本配置类才生效
public class MvcConfig implements WebMvcConfigurer {

    /*
    * 添加适用于所有微服务的拦截器
    * 注意：
    *  1.这里无需再添加拦截路径，因为默认会拦截所有请求
    *  2.实际上的鉴权操作已经在gateway-service中的过滤器完成，这里只是为了将用户信息传递到各个微服务中
    * */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserInfoInterceptor());
    }
}
