package com.hmall.trade.config;

import com.hmall.common.domain.RabbitMQ;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
* 本配置类用于配置 RabbitMQ 死信队列相关内容
* */
@Configuration
public class RabbitDlxConfig {
    /*
    * 配置死信队列相关内容
    * */
    // 1. 死信交换机
    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(RabbitMQ.EXCHANGE_DLX_DIRECT)
                .durable(true)
                .build();
    }
    // 2. 死信队列
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(RabbitMQ.QUEUE_DLX)
                .build();
    }
    // 3. 绑定死信交换机与死信队列
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with(RabbitMQ.ROUTING_DLX);
    }

    /*
    * 配置正常队列相关内容
    * */
    // 1：支付结果
    // 1.1 正常交换机: 支付结果交换机
    @Bean
    public DirectExchange normalExchange() {
        return ExchangeBuilder.directExchange(RabbitMQ.EXCHANGE_PAY_DIRECT)
                .durable(true)
                .build();
    }
    // 1.2 正常队列1，配置死信交换机相关参数
    @Bean
    public Queue normalQueue() {
        return QueueBuilder.durable(RabbitMQ.QUEUE_TRADE_PAY_SUCCESS)
                // 核心配置1: 设置死信交换机
                .deadLetterExchange(RabbitMQ.EXCHANGE_DLX_DIRECT)
                // 核心配置2: 设置死信路由键
                .deadLetterRoutingKey(RabbitMQ.ROUTING_DLX)
                .build();
    }
    // 1.3 绑定正常交换机与正常队列
    @Bean
    public Binding normalBinding() {
        return BindingBuilder.bind(normalQueue())
                .to(normalExchange())
                .with(RabbitMQ.ROUTING_PAY_SUCCESS);
    }

    // 2：超时取消订单
    // 2.1 延迟交换机: 超时取消订单交换机
    @Bean
    public DirectExchange delayOrderExchange() {
        return ExchangeBuilder.directExchange(RabbitMQ.EXCHANGE_ORDER_DELAY)
                .durable(true)
                .build();
    }
    // 2.2 延迟队列2，配置死信交换机相关参数
    @Bean
    public Queue delayOrderQueue() {
        return QueueBuilder.durable(RabbitMQ.QUEUE_ORDER_DELAY)
                // 核心配置1: 设置死信交换机
                .deadLetterExchange(RabbitMQ.EXCHANGE_DLX_DIRECT)
                // 核心配置2: 设置死信路由键
                .deadLetterRoutingKey(RabbitMQ.ROUTING_DLX)
                // 核心配置3: 设置消息过期时间，单位毫秒，15分钟
                .ttl(15 * 60 * 1000)
                .build();
    }
    // 2.3 绑定正常交换机与正常队列
    @Bean
    public Binding normalOrderBinding() {
        return BindingBuilder.bind(delayOrderQueue())
                .to(delayOrderExchange())
                .with(RabbitMQ.ROUTING_ORDER_DELAY);
    }
}
