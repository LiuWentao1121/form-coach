package com.formcoach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coach_training_session")
public class TrainingSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long movementId;
    private Integer score;
    private Integer durationSeconds;
    private Integer repCount;
    private String errorSummary;    // JSON
    private String avgAngles;       // JSON
    private LocalDateTime createdAt;
}
