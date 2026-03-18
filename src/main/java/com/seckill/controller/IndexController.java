package com.seckill.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面控制器 - 处理前端页面跳转
 */
@Controller
public class IndexController {

    /**
     * 访问根路径时转发到 index.html
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }
}
