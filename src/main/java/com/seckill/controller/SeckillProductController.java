package com.seckill.controller;

import com.seckill.service.SeckillProductService;
import com.seckill.vo.Result;
import com.seckill.vo.SeckillProductVO;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class SeckillProductController {

    private final SeckillProductService productService;

    @GetMapping("/list")
    public Result<List<SeckillProductVO>> listProducts() {
        return productService.listProducts();
    }

    @GetMapping("/{id}")
    public Result<SeckillProductVO> getProductById(@PathVariable @NotNull Long id) {
        return productService.getProductById(id);
    }

    @PostMapping("/seckill/{id}")
    public Result<Boolean> seckillProduct(@PathVariable @NotNull Long id) {
        return productService.seckillProduct(id);
    }
}
