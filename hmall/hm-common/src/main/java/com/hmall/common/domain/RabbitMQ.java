package com.hmall.common.domain;

public class RabbitMQ {
    // 正常交换机、路由键、队列
    // 1. 支付结果交换机
    public static final String EXCHANGE_PAY_DIRECT = "pay.direct";
    public static final String ROUTING_PAY_SUCCESS = "pay.success";
    public static final String QUEUE_TRADE_PAY_SUCCESS = "trade.pay.success";
    // 2. 超时取消订单交换机
    public static final String EXCHANGE_ORDER_DELAY = "order.delay.direct";
    public static final String ROUTING_ORDER_DELAY = "order.delay";
    public static final String QUEUE_ORDER_DELAY = "trade.order.delay";


    // 死信交换机、路由键、队列
    public static final String EXCHANGE_DLX_DIRECT = "dlx.direct";
    public static final String ROUTING_DLX = "dlx.routing";
    public static final String QUEUE_DLX = "dlx.queue";
}
