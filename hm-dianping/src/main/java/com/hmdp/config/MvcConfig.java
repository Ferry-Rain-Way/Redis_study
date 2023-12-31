package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @Version 1.0
 * @ClassName : MvcConfig
 * @Author : GUO_HONG_YU
 * @Description: MVC配置
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    /**
     * 该类本身不使用stringRedisTemplate对象
     * 但由于需要在RefreshTokenInterceptor类中使用,
     * 且该对象使我们new 出来的,不在Spring的管理范围内
     * 所以无法注入stringRedisTemplate对象
     * 因为需要提前在本类中进行注入,通过RefreshTokenInterceptor的构造传入
     */
    @Resource
    StringRedisTemplate stringRedisTemplate;
    /**
     * 配置拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /*
           order越小,权限越高,越先执行
           (1) 对于拦截链的设置,在默认情况下所有拦截器的order属性都为0,按照添加顺序执行拦截
           (2) 更加稳妥的做法是在注册拦截器后,添加`.order(number)`设置执行顺序
         */
        // 注册token刷新拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).order(0);
        // 注册拦截器,并排除免登录可进入的路径
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                ).order(1);
    }
}
