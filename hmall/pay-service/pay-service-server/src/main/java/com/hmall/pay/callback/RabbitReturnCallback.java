package com.hmall.pay.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitReturnCallback implements RabbitTemplate.ReturnsCallback{
    @Override
    public void returnedMessage(ReturnedMessage returned) {
        log.warn("消息未被路由到队列，消息ID：{}，交换机：{}，路由键：{}，回复码：{}，回复文本：{}",
                returned.getMessage().getMessageProperties().getCorrelationId(),
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText());
    }
}
