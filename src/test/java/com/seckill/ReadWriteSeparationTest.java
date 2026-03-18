package com.seckill;

import com.seckill.service.SeckillProductService;
import com.seckill.vo.Result;
import com.seckill.vo.SeckillProductVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 读写分离功能测试
 */
@SpringBootTest
public class ReadWriteSeparationTest {

    @Autowired
    private SeckillProductService productService;

    /**
     * 测试读操作使用从库
     */
    @Test
    public void testReadFromSlave() {
        System.out.println("=== 测试读操作（应使用从库）===");
        
        // 查询商品列表
        Result<java.util.List<SeckillProductVO>> listResult = productService.listProducts();
        System.out.println("商品总数：" + listResult.getData().size());
        
        // 查询单个商品
        Result<SeckillProductVO> detailResult = productService.getProductById(1L);
        if (detailResult.getData() != null) {
            System.out.println("商品详情：" + detailResult.getData().getProductName());
        }
    }

    /**
     * 测试写操作使用主库
     */
    @Test
    public void testWriteToMaster() {
        System.out.println("\n=== 测试写操作（应使用主库）===");
        
        // 执行秒杀操作（写操作）
        Result<Boolean> seckillResult = productService.seckillProduct(1L);
        System.out.println("秒杀结果：" + seckillResult.getMsg());
        
        // 验证库存是否减少（读操作，应该从从库读取）
        Result<SeckillProductVO> productResult = productService.getProductById(1L);
        if (productResult.getData() != null) {
            System.out.println("当前库存：" + productResult.getData().getStock());
        }
    }

    /**
     * 测试读写分离效果
     */
    @Test
    public void testReadWriteSeparation() {
        System.out.println("\n=== 综合测试读写分离 ===");
        
        // 多次读操作
        for (int i = 0; i < 3; i++) {
            Result<SeckillProductVO> result = productService.getProductById(1L);
            if (result.getData() != null) {
                System.out.println("第 " + (i + 1) + " 次读取：" + result.getData().getProductName());
            }
        }
        
        // 一次写操作
        Result<Boolean> writeResult = productService.seckillProduct(2L);
        System.out.println("写操作结果：" + writeResult.getMsg());
        
        // 再次读操作
        Result<SeckillProductVO> readResult = productService.getProductById(2L);
        if (readResult.getData() != null) {
            System.out.println("写后读取库存：" + readResult.getData().getStock());
        }
    }
}
