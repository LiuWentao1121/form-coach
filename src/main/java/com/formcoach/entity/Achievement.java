package com.formcoach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coach_achievement")
public class Achievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String achievementType;
    private LocalDateTime unlockedAt;
}
