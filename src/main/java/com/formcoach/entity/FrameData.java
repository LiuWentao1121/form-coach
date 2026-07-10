package com.formcoach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coach_frame_data")
public class FrameData {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Integer frameIndex;
    private String jointData;       // JSON: 33 landmarks
    private Integer isErrorFrame;
    private String errorType;
    private LocalDateTime createdAt;
}
