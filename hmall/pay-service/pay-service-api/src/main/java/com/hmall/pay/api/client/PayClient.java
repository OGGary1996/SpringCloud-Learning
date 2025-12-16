package com.hmall.pay.api.client;

import com.hmall.pay.api.config.PayClientFallbackConfig;
import com.hmall.pay.api.dto.PayOrderDTO;
import com.hmall.pay.api.fallback.PayClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/*
*
* */
@FeignClient(name = "pay-service", configuration = PayClientFallbackConfig.class, fallbackFactory = PayClientFallback.class)
public interface PayClient {
     /**
     * 根据 交易订单id 查询支付单
     * @param id 业务订单id
     * @return 支付单信息
     */
     @GetMapping("/pay-orders/biz/{id}")
     PayOrderDTO queryPayOrderByBizOrderNo(@PathVariable("id") Long id);

}
