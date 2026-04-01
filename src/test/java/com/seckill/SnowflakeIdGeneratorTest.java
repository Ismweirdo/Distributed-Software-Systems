package com.seckill;

import com.seckill.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SnowflakeIdGeneratorTest {

    @Test
    void shouldGenerateIncreasingIds() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1L, 1L);
        long first = generator.nextId();
        long second = generator.nextId();
        Assertions.assertTrue(second > first);
    }
}

