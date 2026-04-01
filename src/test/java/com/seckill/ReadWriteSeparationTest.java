package com.seckill;

import com.seckill.service.SeckillProductService;
import com.seckill.vo.Result;
import com.seckill.vo.SeckillProductVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.springframework.core.env.Environment;
import org.junit.jupiter.api.Assumptions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@SpringBootTest
public class ReadWriteSeparationTest {

    @Autowired
    private SeckillProductService productService;

    @Autowired
    private Environment environment;

    @Test
    public void testReadFromSlave() {
        String before = DynamicDataSourceContextHolder.peek();
        assertEquals(null, before);

        Result<java.util.List<SeckillProductVO>> listResult = productService.listProducts();
        assertNotNull(listResult);
        assertEquals(200, listResult.getCode());
        assertNotNull(listResult.getData());

        Result<SeckillProductVO> detailResult = productService.getProductById(1L);
        assertNotNull(detailResult);
        assertEquals(200, detailResult.getCode());
        assertNotNull(detailResult.getData());

        String after = DynamicDataSourceContextHolder.peek();
        assertEquals(null, after);
    }

    @Test
    public void testReplicationStatus() {
        String io1 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_1", "Replica_IO_Running");
        String sql1 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_1", "Replica_SQL_Running");
        String io2 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_2", "Replica_IO_Running");
        String sql2 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_2", "Replica_SQL_Running");

        Assumptions.assumeTrue("Yes".equals(io1) && "Yes".equals(sql1)
                && "Yes".equals(io2) && "Yes".equals(sql2), "当前环境未启用主从复制，跳过复制链路断言");

        assertEquals("Yes", io1, "slave_1 复制 IO 线程未运行");
        assertEquals("Yes", sql1, "slave_1 复制 SQL 线程未运行");
        assertEquals("Yes", io2, "slave_2 复制 IO 线程未运行");
        assertEquals("Yes", sql2, "slave_2 复制 SQL 线程未运行");
    }

    @Test
    public void testWriteToMaster() {
        String beforeWrite = DynamicDataSourceContextHolder.peek();
        assertEquals(null, beforeWrite);

        Result<Long> seckillResult = productService.seckillProduct(1L, uniqueUserId());
        assertNotNull(seckillResult);
        assertEquals(200, seckillResult.getCode());

        String afterWrite = DynamicDataSourceContextHolder.peek();
        assertEquals(null, afterWrite);

        Result<SeckillProductVO> productResult = productService.getProductById(1L);
        assertNotNull(productResult);
        assertEquals(200, productResult.getCode());
        assertNotNull(productResult.getData());
    }

    @Test
    public void testReadWriteSeparation() {
        for (int i = 0; i < 3; i++) {
            Result<SeckillProductVO> result = productService.getProductById(1L);
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
        }

        Result<Long> writeResult = productService.seckillProduct(2L, uniqueUserId());
        assertNotNull(writeResult);
        assertEquals(200, writeResult.getCode());

        Result<SeckillProductVO> readResult = productService.getProductById(2L);
        assertNotNull(readResult);
        assertEquals(200, readResult.getCode());
        assertNotNull(readResult.getData());
    }

    @Test
    public void testWriteReplicatedToSlaves() {
        Long productId = 1L;
        String io1 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_1", "Replica_IO_Running");
        String sql1 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_1", "Replica_SQL_Running");
        String io2 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_2", "Replica_IO_Running");
        String sql2 = queryReplicaStatus("spring.datasource.dynamic.datasource.slave_2", "Replica_SQL_Running");
        Assumptions.assumeTrue("Yes".equals(io1) && "Yes".equals(sql1)
                && "Yes".equals(io2) && "Yes".equals(sql2), "当前环境未启用主从复制，跳过复制一致性断言");

        Integer beforeMaster = queryStock("spring.datasource.dynamic.datasource.master", productId);
        assertNotNull(beforeMaster);
        assertTrue(beforeMaster > 0, "测试商品库存不足，无法验证写入");

        Result<Long> writeResult = productService.seckillProduct(productId, uniqueUserId());
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

    private long uniqueUserId() {
        return System.currentTimeMillis() + (long) (Math.random() * 10000);
    }
}
