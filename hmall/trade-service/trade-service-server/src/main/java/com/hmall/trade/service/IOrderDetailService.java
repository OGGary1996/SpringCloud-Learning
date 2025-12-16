package com.hmall.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.trade.domain.po.OrderDetail;

import java.util.List;

/**
 * <p>
 * 订单详情表 服务类
 * </p>
 */
public interface IOrderDetailService extends IService<OrderDetail> {
    // 根据 orderId 查询订单详情
    List<OrderDetail> getByOrderId(Long orderId);

}
