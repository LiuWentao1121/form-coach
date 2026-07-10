package com.formcoach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.formcoach.entity.Movement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MovementMapper extends BaseMapper<Movement> {
}
