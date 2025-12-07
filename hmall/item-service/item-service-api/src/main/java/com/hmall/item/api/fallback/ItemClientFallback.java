package com.hmall.item.api.fallback;

import com.hmall.common.domain.dto.OrderDetailDTO;
import com.hmall.item.api.client.ItemClient;
import com.hmall.item.api.dto.ItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ItemClientFallback implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {

            @Override
            public List<ItemDTO> queryItemByIds(List<Long> ids) {
                log.error("调用item-service服务查询商品信息失败：", cause);
                // 返回一个空列表，表示没有查询到任何商品信息
                return Collections.emptyList();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                log.error("调用item-service服务扣减库存失败：", cause);
                // 这里可以选择抛出一个自定义异常，或者进行其他的降级处理
                throw new RuntimeException(cause);
            }
        };
    }
}
