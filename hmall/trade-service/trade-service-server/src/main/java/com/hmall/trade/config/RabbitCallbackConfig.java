package com.hmall.trade.config;

import com.hmall.trade.callback.RabbitConfirmCallback;
import com.hmall.trade.callback.RabbitReturnCallback;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitCallbackConfig {
    /*
    * 默认的 RabbitTemplate 中已经集成了Confirm和Return回调机制
    * 需要显示将callback中的回调类与RabbitTemplate进行关联
    * */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            RabbitConfirmCallback confirmCallback,
            RabbitReturnCallback returnCallback
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 显式绑定回调类
        rabbitTemplate.setConfirmCallback(confirmCallback);
        rabbitTemplate.setReturnsCallback(returnCallback);
        // 开启mandatory标志，确保消息无法路由时触发Return回调
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }
}
