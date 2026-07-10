package com.formcoach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coach_angle_result")
public class AngleResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Integer frameIndex;
    private String jointName;
    private BigDecimal angleValue;
    private String angleStatus;     // NORMAL / WARN / ERROR
    private LocalDateTime createdAt;
}
