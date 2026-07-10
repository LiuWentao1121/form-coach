package com.formcoach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coach_movement")
public class Movement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String category;
    private String description;
    private String standardAngles;  // JSON
    private String tips;            // JSON
    private String imageUrl;
    private String videoUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
