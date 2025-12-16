package com.hmall.pay.api.fallback;

import com.hmall.pay.api.client.PayClient;
import com.hmall.pay.api.dto.PayOrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class PayClientFallback implements FallbackFactory<PayClient> {
    @Override
    public PayClient create(Throwable cause) {
        return new PayClient() {
            @Override
            public PayOrderDTO queryPayOrderByBizOrderNo(Long id) {
                log.error("调用支付服务查询支付单失败: {}", cause.getMessage(), cause);
                return null;
            }
        };
    }
}
