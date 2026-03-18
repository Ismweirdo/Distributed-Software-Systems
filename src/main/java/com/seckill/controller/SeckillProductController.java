package com.seckill.controller;

import com.seckill.service.SeckillProductService;
import com.seckill.vo.Result;
import com.seckill.vo.SeckillProductVO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Validated
@RequiredArgsConstructor
public class SeckillProductController {

    private final SeckillProductService productService;

    @GetMapping("/list")
    public Result<List<SeckillProductVO>> listProducts() {
        return productService.listProducts();
    }

    @GetMapping("/{id}")
    public Result<SeckillProductVO> getProductById(@PathVariable @NotNull @Min(1) Long id) {
        return productService.getProductById(id);
    }

    @PostMapping("/seckill/{id}")
    public Result<Boolean> seckillProduct(@PathVariable @NotNull @Min(1) Long id) {
        return productService.seckillProduct(id);
    }
}
