package com.hmdp.interceptor;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Version 1.0
 * @ClassName : LoginInterceptor
 * @Author : GUO_HONG_YU
 * @Description: 刷新token
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {
    /**
     * 当前对象在使用时使我们new出的,所以不在Spring容器管理范围内
     * 因而此处无法进行注入,而是通过构造方法传递
     */
    StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     *前置登录拦截器
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
      /*
           1.从session中获取用户
            HttpSession session = request.getSession()
            UserDTO user = (UserDTO) session.getAttribute("user")
        */

        // 1.获取请求头中的token
        String suffixToken = request.getHeader("authorization");
        String token = RedisConstants.LOGIN_USER_KEY + suffixToken;

        // 2.从Redis中获取用户
        HashOperations<String, Object, Object> hOpera = stringRedisTemplate.opsForHash();
        Map<Object, Object> userMap = hOpera.entries(token);

        // 3.判断token以及用户是否存在
        if (StrUtil.isBlank(suffixToken)||userMap.isEmpty()) {
            // 用户不存在 → 拦截
            // status:401:未经授权
            // response.setStatus(401);
            return true;
        }
        // 4.保存用户到ThreadLocal
            // 将Hash数据(userMap) 转成userDTO对象
             /*基础写法
        UserDTO userDTO = new UserDTO();
        userDTO.setId(Long.valueOf(userMap.get("id").toString()));
        userDTO.setNickName(userMap.get("nickName").toString());
        userDTO.setIcon(userMap.get("icon").toString());*/
             // 工具类写法
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //保存
        UserHolder.saveUser(userDTO);

        // 5.刷新token有效期
        stringRedisTemplate.expire(token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 6.放行
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
