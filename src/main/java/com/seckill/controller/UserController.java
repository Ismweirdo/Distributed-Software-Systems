package com.seckill.controller;

import com.seckill.service.UserService;
import com.seckill.vo.Result;
import com.seckill.vo.UserLoginVO;
import com.seckill.vo.UserRegisterVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody UserRegisterVO vo) {
        return userService.register(vo);
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody UserLoginVO vo) {
        return userService.login(vo);
    }
}