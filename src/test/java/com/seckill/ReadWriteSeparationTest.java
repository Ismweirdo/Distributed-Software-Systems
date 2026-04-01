package com.seckill;

import com.seckill.service.SeckillProductService;
import com.seckill.vo.Result;
import com.seckill.vo.SeckillProductVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.springframework.core.env.Environment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 读写分离功能测试
 */
@SpringBootTest
public class ReadWriteSeparationTest {

    @Autowired
    private SeckillProductService productService;

    @Autowired
    private Environment environment;

    /**
     * 测试读操作使用从库
     */
    @Test
    public void testReadFromSlave() {
        System.out.println("=== 测试读操作（应使用从库）===");
        String before = DynamicDataSourceContextHolder.peek();
        assertEquals(null, before);

        // 查询商品列表
        Result<java.util.List<SeckillProductVO>> listResult = productService.listProducts();
        assertNotNull(listResult);
        assertEquals(200, listResult.getCode());
        assertNotNull(listResult.getData());
        System.out.println("商品总数：" + listResult.getData().size());
        
        // 查询单个商品
        Result<SeckillProductVO> detailResult = productService.getProductById(1L);
        assertNotNull(detailResult);
        assertEquals(200, detailResult.getCode());
        assertNotNull(detailResult.getData());
        if (detailResult.getData() != null) {
            System.out.println("商品详情：" + detailResult.getData().getProductName());
        }

        String after = DynamicDataSourceContextHolder.peek();
        assertEquals(null, after);
    }

    /**
     * 直接验证主从复制链路可用
     */
    @Test
    public void testReplicationStatus() {
        String io1 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_1", "Replica_IO_Running");
        String sql1 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_1", "Replica_SQL_Running");
        String io2 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_2", "Replica_IO_Running");
        String sql2 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_2", "Replica_SQL_Running");

        assertEquals("Yes", io1, "slave_1 复制 IO 线程未运行");
        assertEquals("Yes", sql1, "slave_1 复制 SQL 线程未运行");
        assertEquals("Yes", io2, "slave_2 复制 IO 线程未运行");
        assertEquals("Yes", sql2, "slave_2 复制 SQL 线程未运行");
    }

    /**
     * 测试写操作使用主库
     */
    @Test
    public void testWriteToMaster() {
        System.out.println("\n=== 测试写操作（应使用主库）===");
        String beforeWrite = DynamicDataSourceContextHolder.peek();
        assertEquals(null, beforeWrite);

        // 执行秒杀操作（写操作）
        Result<Boolean> seckillResult = productService.seckillProduct(1L);
        assertNotNull(seckillResult);
        assertEquals(200, seckillResult.getCode());
        System.out.println("秒杀结果：" + seckillResult.getMsg());

        String afterWrite = DynamicDataSourceContextHolder.peek();
        assertEquals(null, afterWrite);

        // 验证库存是否减少（读操作，应该从从库读取）
        Result<SeckillProductVO> productResult = productService.getProductById(1L);
        assertNotNull(productResult);
        assertEquals(200, productResult.getCode());
        assertNotNull(productResult.getData());
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
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            if (result.getData() != null) {
                System.out.println("第 " + (i + 1) + " 次读取：" + result.getData().getProductName());
            }
        }
        
        // 一次写操作
        Result<Boolean> writeResult = productService.seckillProduct(2L);
        assertNotNull(writeResult);
        assertEquals(200, writeResult.getCode());
        System.out.println("写操作结果：" + writeResult.getMsg());
        
        // 再次读操作
        Result<SeckillProductVO> readResult = productService.getProductById(2L);
        assertNotNull(readResult);
        assertEquals(200, readResult.getCode());
        assertNotNull(readResult.getData());
        if (readResult.getData() != null) {
            System.out.println("写后读取库存：" + readResult.getData().getStock());
        }
    }

    /**
     * 写主库后，校验两个从库都能读到同样结果（验证写入后复制生效）
     */
    @Test
    public void testWriteReplicatedToSlaves() {
        Long productId = 1L;
        Integer beforeMaster = queryStock("spring.datasource.dynamic.datasource.master", productId);
        assertNotNull(beforeMaster);
        assertTrue(beforeMaster > 0, "测试商品库存不足，无法验证写入");

        Result<Boolean> writeResult = productService.seckillProduct(productId);
        assertNotNull(writeResult);
        assertEquals(200, writeResult.getCode());

        Integer afterMaster = queryStock("spring.datasource.dynamic.datasource.master", productId);
        Integer afterSlave1 = queryStock("spring.datasource.dynamic.datasource.slave_1", productId);
        Integer afterSlave2 = queryStock("spring.datasource.dynamic.datasource.slave_2", productId);

        assertNotNull(afterMaster);
        assertNotNull(afterSlave1);
        assertNotNull(afterSlave2);
        assertEquals(afterMaster, afterSlave1, "slave_1 与 master 库存不一致");
        assertEquals(afterMaster, afterSlave2, "slave_2 与 master 库存不一致");
    }

    private Integer queryStock(String prefix, Long productId) {
        String url = property(prefix + ".url");
        String username = property(prefix + ".username");
        String password = property(prefix + ".password");
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("SELECT stock FROM seckill_product WHERE id = ?")) {
            statement.setLong(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("stock") : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("查询库存失败: " + prefix, e);
        }
    }

    private String queryReplicaStatus(String prefix, String field) {
        String url = property(prefix + ".url");
        String username = property(prefix + ".username");
        String password = property(prefix + ".password");
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("SHOW REPLICA STATUS");
             ResultSet rs = statement.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            return rs.getString(field);
        } catch (Exception e) {
            throw new IllegalStateException("查询复制状态失败: " + prefix, e);
        }
    }

    private String property(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少配置: " + key);
        }
        return value;
    }
}
