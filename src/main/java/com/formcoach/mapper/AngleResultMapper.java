package com.formcoach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.formcoach.entity.AngleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AngleResultMapper extends BaseMapper<AngleResult> {

    @Select("SELECT * FROM coach_angle_result WHERE session_id = #{sessionId} ORDER BY frame_index, joint_name")
    List<AngleResult> findBySessionId(Long sessionId);
}
