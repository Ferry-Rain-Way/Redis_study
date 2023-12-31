package com.hmdp.interceptor;


import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @Version 1.0
 * @ClassName : LoginInterceptor
 * @Author : GUO_HONG_YU
 * @Description: 登录拦截器
 */
public class LoginInterceptor  implements HandlerInterceptor {
    /**
     *前置登录拦截器
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 判断ThreadLocal中是否存在用户
        if (UserHolder.getUser() == null) {
            // 用户不存在 → 拦截
            // status:401:未经授权
            response.setStatus(401);
        }
        // 存在则放行
        return true;
    }

    /**
     * 后置登录拦截器
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户,避免内存泄漏
        UserHolder.removeUser();
    }
}
