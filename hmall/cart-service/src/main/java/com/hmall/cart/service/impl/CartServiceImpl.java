package com.hmall.cart.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.cart.domain.dto.CartFormDTO;
import com.hmall.cart.domain.po.Cart;
import com.hmall.cart.domain.vo.CartVO;
import com.hmall.cart.mapper.CartMapper;
import com.hmall.cart.service.ICartService;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.item.api.client.ItemClient;
import com.hmall.item.api.dto.ItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单详情表 服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor // Lombok - 自动生成有参构造器，并且只使用必须放在构造函数中的字段进行初始化（final字段）
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {

    // private final IItemService itemService;
    // private final RestTemplate restTemplate;
    // private final DiscoveryClient discoveryClient;
    // 优化为使用openfeign
    private final ItemClient itemClient;


    @Override
    public void addItem2Cart(CartFormDTO cartFormDTO) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();

        // 2.判断是否已经存在
        if(checkItemExists(cartFormDTO.getItemId(), userId)){
            // 2.1.存在，则更新数量
            baseMapper.updateNum(cartFormDTO.getItemId(), userId);
            return;
        }
        // 2.2.不存在，判断是否超过购物车数量
        checkCartsFull(userId);

        // 3.新增购物车条目
        // 3.1.转换PO
        Cart cart = BeanUtils.copyBean(cartFormDTO, Cart.class);
        // 3.2.保存当前用户
        cart.setUserId(userId);
        // 3.3.保存到数据库
        save(cart);
    }

    @Override
    public List<CartVO> queryMyCarts() {
        // 1.查询我的购物车列表
        List<Cart> carts = lambdaQuery().eq(Cart::getUserId, 1L /*TODO: 拆分为微服务，无法调用ThreadLocal，需要优化 UserContext.getUser()*/).list();
        if (CollUtils.isEmpty(carts)) {
            return CollUtils.emptyList();
        }

        // 2.转换VO
        List<CartVO> vos = BeanUtils.copyList(carts, CartVO.class);

        // 3.处理VO中的商品信息
        handleCartItems(vos);

        // 4.返回
        return vos;
    }

    private void handleCartItems(List<CartVO> vos) {
        // 1.获取商品id
        Set<Long> itemIds = vos.stream().map(CartVO::getItemId).collect(Collectors.toSet());
        // 2.查询商品
        // 因为拆分了微服务，而这里需要注入商品服务的接口，所以先注释掉，等微服务课程讲到再补充完整
        // List<ItemDTO> items = itemService.queryItemByIds(itemIds);
        // 这里使用RestTemplate或者WebClient调用商品服务的接口，取代上面的本地注入调用的方式
//          // 2.1 通过DiscoveryClient获取商品服务的实例列表
//        List<ServiceInstance> instances = discoveryClient.getInstances("item-service");
//        if (instances == null || instances.isEmpty()) {
//            return;
//        }
//          // 2.2 选择一个实例（这里简单地随机选择实例，实际中可以使用负载均衡策略）
//        ServiceInstance itemService = instances.get(RandomUtil.randomInt(instances.size()));
            // 2.3 构建请求URL
//        String url = itemService.getUri() + "/items?ids={ids}";

//        // 2.1-2.3 优化
//        // 2.3 构建请求URL, 这里直接使用添加了@LoadBalanced注解的RestTemplate，可以直接使用服务名称调用，不需要再通过DiscoveryClient获取实例
//        String url = "http://item-service/items?ids={ids}";
//
//          // 2.4 使用RestTemplate调用商品服务的接口，获取相应体
//        ResponseEntity<List<ItemDTO>> responseEntity = restTemplate.exchange(
//                url,
//                HttpMethod.GET,
//                null, // 请求体为空，GET请求没有请求体
//                new ParameterizedTypeReference<List<ItemDTO>>() {
//                }, // 响应类型是 List<ItemDTO>
//                Map.of("ids", CollUtils.join(itemIds, ",")) // URL参数ids,并且使用hutool的CollUtils.join方法将Set转换为逗号分隔的字符串
//        );
//          // 2.5 解析响应体
//        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
//            return ;
//        }
//        List<ItemDTO> items = responseEntity.getBody();
//
//        if (CollUtils.isEmpty(items)) {
//            return;
//        }

        // 2.1-2.3 优化：使用openfeign优化调用商品服务
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds.stream().toList());
        if (items == null || items.isEmpty()) {
            return;
        }

        // 3.转为 id 到 item的map
        Map<Long, ItemDTO> itemMap = items.stream().collect(Collectors.toMap(ItemDTO::getId, Function.identity()));
        // 4.写入vo
        for (CartVO v : vos) {
            ItemDTO item = itemMap.get(v.getItemId());
            if (item == null) {
                continue;
            }
            v.setNewPrice(item.getPrice());
            v.setStatus(item.getStatus());
            v.setStock(item.getStock());
        }
    }

    @Override
    public void removeByItemIds(Collection<Long> itemIds) {
        // 1.构建删除条件，userId和itemId
        QueryWrapper<Cart> queryWrapper = new QueryWrapper<Cart>();
        queryWrapper.lambda()
                .eq(Cart::getUserId, UserContext.getUser())
                .in(Cart::getItemId, itemIds);
        // 2.删除
        remove(queryWrapper);
    }

    private void checkCartsFull(Long userId) {
        int count = lambdaQuery().eq(Cart::getUserId, userId).count();
        if (count >= 10) {
            throw new BizIllegalException(StrUtil.format("用户购物车课程不能超过{}", 10));
        }
    }

    private boolean checkItemExists(Long itemId, Long userId) {
        int count = lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getItemId, itemId)
                .count();
        return count > 0;
    }
}
