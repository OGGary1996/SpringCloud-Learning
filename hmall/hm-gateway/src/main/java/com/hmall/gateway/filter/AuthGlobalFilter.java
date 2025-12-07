package com.hmall.gateway.filter;

import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;
    private final JwtTool jwtTool;
    // 用于判断path是否属于命中某个范围
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /*
    * 控制过滤器的执行顺序，数值越小，优先级越高
    * 这里设置为-100，确保在大多数过滤器之前执行
    * */
    @Override
    public int getOrder() {
        return -100;
    }

    /*
    * 用于全局鉴权过滤
    * 参数：
    *  1. ServerWebExchange exchange：包含了请求和响应的所有上下文信息，可以获取请求头、请求参数等信息
    *  2. GatewayFilterChain chain：过滤器链对象，用于将请求传递给下一个过滤器
    * 流程：
    *  1. 从exchange中获取信息（路径、请求头中的token）
    *  2. 判断是否属于白名单路径
    *    2.1 如果是，直接放行
    *    2.2 如果不是，进行下一步
    *  3. 从请求头中获取token
    *    3.1 如果token不存在，返回权限不足的响应，401
    *    3.2 如果存在，进行下一步
    *  4. 验证token的有效性（可以调用认证服务或者使用JWT等方式）
    *    4.1 如果token无效，返回权限不足
    *    4.2 如果token有效，放行请求
    *  5. 返回chain.filter(exchange)继续处理请求
    * */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 从exchange中获取请求路径和请求头中的token
        String path = exchange.getRequest().getPath().toString();
        if (isExcludedPath(path)) {
            // 2.1 如果在白名单内，直接放行
            return chain.filter(exchange);
        }
        // 2.2 如果不在白名单内，进行下一步
        log.info("请求路径{}不在白名单中，进行鉴权", path);
        // 3. 判断token是否存在
        String token = exchange.getRequest().getHeaders().getFirst("authorization");
        if (token == null || token.isEmpty()) {
            // 3.1 如果token不存在，返回权限不足的响应，401
            log.warn("请求路径{}未携带token，拒绝访问", path);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        // 3.2 如果存在，进行下一步
        log.info("请求路径{}携带token，开始验证token有效性", path);
        // 4. 验证token的有效性
        /*
        * 注意：
        *  1. 这里的parseToken方法会抛出异常，如果token无效，但是并不能被全局异常捕获。
        *  2. 因为GlobalFilter是在WebFlux的响应式编程模型下运行的,而不是在传统的Servlet模型下运行的
        *  3. 需要手动捕获异常，如果有异常则手动返回401响应
        * */
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException uex) {
            log.warn("请求路径{}携带无效token，拒绝访问", path);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        // 5. token有效，存储用户信息到请求头中，放行请求
        log.info("请求路径{}携带有效token，存储用户信息并且放行", path);
        ServerHttpRequest newRequest = exchange.getRequest().mutate()
                .header("user-info", String.valueOf(userId))
                .build();
        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
        return chain.filter(newExchange);
    }

    /*
    * 判断是否属于白名单路径的内部方法
    * */
    private boolean isExcludedPath(String path) {
        // stream anyMatch 用于判断路径是否匹配白名单中的任意一个路径，如果 AntPathMatcher 判断match，则返回true
        boolean isExcludedPath = authProperties.getExcludePaths().stream()
                .anyMatch(excludePath -> pathMatcher.match(excludePath, path));
        if (isExcludedPath) {
            log.info("请求路径{}在白名单中，放行", path);
        }
        return isExcludedPath;
    }
}
