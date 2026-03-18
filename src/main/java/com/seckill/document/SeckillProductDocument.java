package com.seckill.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 ES 文档实体
 */
@Data
@Document(indexName = "seckill_product")
public class SeckillProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart")
    private String productName;

    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal originalPrice;

    @Field(type = FieldType.Double)
    private BigDecimal seckillPrice;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Field(type = FieldType.Keyword)
    private String status; // 秒杀状态：UPCOMING(即将开始), ONGOING(进行中), ENDED(已结束)
}
