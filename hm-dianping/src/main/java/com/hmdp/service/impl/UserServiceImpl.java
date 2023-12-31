package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone , HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            //不符合手机号格式
            return Result.fail("手机号格式错误");
        }
        // 2.生成验证码
        String vCode = RandomUtil.randomNumbers(6);
        /*
          <del> 3.保存验证码Session 实现</del>
          (已过时:deprecated) session.setAttribute("code",vCode);*/
        // 3.保存验证码Redis实现
        stringRedisTemplate.opsForValue().set(
                LOGIN_CODE_KEY+phone,
                vCode,
                RedisConstants.LOGIN_CODE_TTL,
                TimeUnit.MINUTES);
        //发送验证码
        log.debug("验证码:["+vCode+"]");
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            //不符合手机号格式
            return Result.fail("手机号格式错误");
        }
        /*
          <del>2.验证码校验</del>
          (已过时:deprecated) String code = (String) session.getAttribute("code");*/
        // 2.Redis实现
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        if (null == code || !code.equals(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }
        // 3.根据手机号查用户
        User user = query().eq("phone", phone).one();
        // 4.保存用户信息到数据库
        if(null == user){
            //用户不存在,创建用户,保存到数据库
           user = createUserWithPhone(phone);
           //保存到数据库(Mybatis-Plus)
           save(user);
        }
        /*
         5.保存用户到Session(BeanUtil.copyProperties复制用户)
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class)); */
        // 5.Redis实现
        HashOperations<String, Object, Object> hOpera = stringRedisTemplate.opsForHash();
            // 生成token
        String suffixToken = UUID.randomUUID().toString(true);
        String token = LOGIN_USER_KEY + suffixToken;
            // 转成Hash数据存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
      /*基础写法
        Map<String,String> map = new HashMap<>(16);
        map.put("id",userDTO.getId().toString());
        map.put("nickName",userDTO.getNickName());
        map.put("icon",userDTO.getIcon());*/
        //使用工具类
        Map<String,Object> map = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));

        hOpera.putAll(token,map);
            // 设置时效
        stringRedisTemplate.expire(token,LOGIN_USER_TTL,TimeUnit.MINUTES);
            // 在登录拦截出延长时效
        // 返回token到客户端
        return Result.ok();
    }


    private User createUserWithPhone(String phone) {
        User user = new User();
        // 1.创建昵称
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.设置手机号
        user.setPhone(phone);
        return user;
    }
}
