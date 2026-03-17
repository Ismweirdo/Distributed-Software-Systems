package com.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.service.UserService;
import com.seckill.vo.Result;
import com.seckill.vo.UserLoginVO;
import com.seckill.vo.UserRegisterVO;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public Result<?> register(UserRegisterVO vo) {
        User exist = baseMapper.findByUsername(vo.getUsername());
        if (exist != null) {
            return Result.fail("用户名已存在");
        }
        User user = new User();
        user.setUsername(vo.getUsername());
        user.setPassword(vo.getPassword());
        user.setPhone(vo.getPhone());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        save(user);
        return Result.success("注册成功");
    }

    @Override
    public Result<?> login(UserLoginVO vo) {
        User user = baseMapper.findByUsername(vo.getUsername());
        if (user == null) return Result.fail("用户不存在");
        if (!user.getPassword().equals(vo.getPassword())) return Result.fail("密码错误");
        return Result.success("login_success_" + user.getId());
    }
}