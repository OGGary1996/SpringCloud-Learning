package com.hmall.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConverterConfig {
    /*
    * 配置 RabbitMQ 使用 JSON 序列化和反序列化消息的转换器
    * 用于替换默认的 Java 序列化方式，提升跨语言兼容性和可读性
    * 注意：Jackson2Json对于jdk8中的LocalDateTime等新时间类型的支持，需要额外配置模块
    * */
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();

        // ✅ 关键 1：支持 Java 8 时间
        mapper.registerModule(new JavaTimeModule());

        // ✅ 关键 2：禁止时间戳格式（否则 LocalDateTime 会变数组）
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new Jackson2JsonMessageConverter(mapper);
    }
}
