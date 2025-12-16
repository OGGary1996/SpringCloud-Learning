package com.hmall.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.cart.api.client.CartClient;
import com.hmall.common.domain.RabbitMQ;
import com.hmall.common.domain.dto.OrderDetailDTO;
import com.hmall.common.domain.dto.PaySuccessDTO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.item.api.client.ItemClient;
import com.hmall.item.api.dto.ItemDTO;
import com.hmall.pay.api.client.PayClient;
import com.hmall.pay.api.dto.PayOrderDTO;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.mapper.OrderMapper;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    private final CartClient cartClient;
    private final RabbitTemplate rabbitTemplate;
    private final PayClient payClient;

    @Override
    @GlobalTransactional // 开启 Seata 全局事务
    @Transactional // 开启本地事务
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds.stream().collect(Collectors.toList()));
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
        cartClient.removeByItemIds(itemIds.stream().collect(Collectors.toList()));

        // 4.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        /*
        * 补充：创建订单之后发送消息到延迟队列，用于处理订单超时未支付的场景
        * */
        CorrelationData correlationData = new CorrelationData(String.valueOf(order.getId()));
        rabbitTemplate.convertAndSend(
                RabbitMQ.EXCHANGE_ORDER_DELAY,
                RabbitMQ.ROUTING_ORDER_DELAY,
                order.getId(),
                msg -> {
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                },
                correlationData
        );
        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    /*
    * 针对监听消息队列的订单支付成功，修改订单状态的补充方法
    * 采用注解声明Exchange, Queue, RoutingKey的方式
    * */
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(name = RabbitMQ.QUEUE_TRADE_PAY_SUCCESS, durable = "true"),
//            exchange = @Exchange(name = RabbitMQ.EXCHANGE_PAY_DIRECT, type = ExchangeTypes.DIRECT, durable = "true"),
//            key = RabbitMQ.ROUTING_PAY_SUCCESS
//    ))
    // 再次优化为使用自动ACK + 本地重试 + 死信队列的方式，避免消息丢失和重复消费的问题
    @RabbitListener(queues = RabbitMQ.QUEUE_TRADE_PAY_SUCCESS)
    public void onPaySuccess(PaySuccessDTO paySuccessDTO) {
        // 创建Order对象，并且拷贝属性
        Order order = new Order();
        order.setId(paySuccessDTO.getOrderId());
        order.setStatus(paySuccessDTO.getStatus());
        order.setPayTime(paySuccessDTO.getPayTime());
        // 更新订单状态
        updateById(order);
    }


    /*
    * 补充监听死信队列（其中的延迟消息，来自延迟队列）
    * 用于在订单超时未支付时，关闭订单
    * */
    @RabbitListener(queues = RabbitMQ.QUEUE_DLX)
    public void onOrderTimeout(Long orderId) {
        // 1. 根据消息的订单ID查询订单
        Order order = getById(orderId);

        // 2. 查看订单的状态
          // 2.1 如果订单状态是已支付，则说明已经支付成功，本方法终止
        if (order == null || order.getStatus() == 1) {
            return;
        }
          // 2.2 如果订单状态是未支付，则需要二次确认支付流水中的支付状态
        // 3. 判断支付流水中的支付状态
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(orderId);
        // 3.1 如果支付流水中是已支付，则说明支付成功，需要调用上面的 markOrderPaySuccess 方法，修改订单状态为已支付
        if (payOrderDTO != null && payOrderDTO.getStatus() == 3) {
            markOrderPaySuccess(orderId);
            return;
        }
          // 3.2 如果支付流水中还是未支付，则说明订单确实超时未支付，需要关闭订单
        // 4. 修改订单状态为已关闭，并恢复库存
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(5); // 已关闭
        updateById(updateOrder);
        // 恢复库存
        // 逻辑：查询订单详情，获取商品ID和数量，调用itemClient中的减库存的方法，只是将数量变为负数即可
        List<OrderDetail> details = detailService.getByOrderId(orderId);
        // 将 OrderDetail 转换为 OrderDetailDTO
        List<OrderDetailDTO> detailDTOS = BeanUtils.copyList(details, OrderDetailDTO.class);
        // 将其中的 num 变为负数
        detailDTOS.stream().forEach(detailDTO -> detailDTO.setNum(-detailDTO.getNum()));
        // 调用远程方法恢复库存
        itemClient.deductStock(detailDTOS);
    }




    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
