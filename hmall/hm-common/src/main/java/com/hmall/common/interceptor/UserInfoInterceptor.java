package com.hmall.common.interceptor;

import com.hmall.common.utils.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
* 本类用于在请求到达所有的微服务之前，统一从请求头中获取到 gateway-service 过滤器中存入到请求头中的用户信息，并且存入到 ThreadLocal 中，供后续的业务逻辑使用
* */
public class UserInfoInterceptor implements HandlerInterceptor {

    /*
    * 前置处理：
    *  1. 从请求头中获取用户id
    *    1.1 如果没有获取到，说明 gateway-service中直接放行了请求，没有设置用户信息，这里直接放行
    *    1.2 如果获取到了，说明 gateway-service 中鉴权通过，设置了用户信息
    *  2. 转换为 Long 类型
    *  3. 存入到 ThreadLocal 中
    *  4. 放行请求
    * */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头中获取用户id
        String userInfo = request.getHeader("user-info");
        if (userInfo == null || userInfo.isEmpty()) {
            return true;
        }

        // 2. 转换为 Long 类型
        Long userId = Long.valueOf(userInfo);
        // 3. 存入到 ThreadLocal 中
        UserContext.setUser(userId);
        // 4. 放行请求
        return true;
    }

    /*
    * 后置处理器：
    *  1. 移除 ThreadLocal 中的用户信息，防止内存泄漏
    * */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 1. 移除 ThreadLocal 中的用户信息，防止内存泄漏
        UserContext.removeUser();
    }
}
