package com.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seckill.entity.User;
import com.seckill.vo.Result;
import com.seckill.vo.UserLoginVO;
import com.seckill.vo.UserRegisterVO;

public interface UserService extends IService<User> {
    Result<?> register(UserRegisterVO vo);
    Result<?> login(UserLoginVO vo);
}