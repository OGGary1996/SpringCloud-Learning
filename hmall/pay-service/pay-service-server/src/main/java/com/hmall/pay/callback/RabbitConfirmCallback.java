package com.hmall.pay.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitConfirmCallback implements RabbitTemplate.ConfirmCallback{
    @Override
    public void confirm(@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause) {
        if (ack) { // 消息成功到达交换机
            log.info("消息成功到达交换机，消息ID：{}", correlationData != null ? correlationData.getId() : "null");
        } else { // 消息未到达交换机
            log.warn("消息未到达交换机，消息ID：{}，失败原因：{}", correlationData != null ? correlationData.getId() : "null", cause);
        }
    }
}
