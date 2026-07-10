package com.formcoach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.formcoach.entity.TrainingSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TrainingSessionMapper extends BaseMapper<TrainingSession> {

    @Select("SELECT DATE(created_at) as date, SUM(duration_seconds) as seconds " +
            "FROM coach_training_session WHERE user_id = #{userId} " +
            "AND created_at >= #{startDate} GROUP BY DATE(created_at)")
    List<Map<String, Object>> getHeatmapData(Long userId, String startDate);

    @Select("SELECT COUNT(DISTINCT DATE(created_at)) FROM coach_training_session " +
            "WHERE user_id = #{userId}")
    int countTrainingDays(Long userId);

    @Select("SELECT COALESCE(SUM(duration_seconds), 0) FROM coach_training_session " +
            "WHERE user_id = #{userId}")
    int sumTrainingSeconds(Long userId);
}
