package com.hmall.item.api.client;

import com.hmall.item.api.dto.ItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "item-service")
public interface ItemClient {

    /*
    * 注意：
    *  1. 必须与被调用的服务接口保持一致，包括请求路径、请求方法、请求参数等
    *  2. 返回值类型也必须保持一致
    * */
    @GetMapping("/items")
    List<ItemDTO> queryItemByIds(@RequestParam("ids") List<Long> ids);
}
