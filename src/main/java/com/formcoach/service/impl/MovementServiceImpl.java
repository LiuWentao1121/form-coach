package com.formcoach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.formcoach.common.BusinessException;
import com.formcoach.common.ErrorCode;
import com.formcoach.entity.Movement;
import com.formcoach.mapper.MovementMapper;
import com.formcoach.service.MovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {

    private final MovementMapper movementMapper;

    @Override
    public List<Movement> listAll() {
        return movementMapper.selectList(null);
    }

    @Override
    public Page<Movement> page(int page, int size, String category) {
        Page<Movement> p = new Page<>(page, size);
        LambdaQueryWrapper<Movement> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(Movement::getCategory, category);
        }
        wrapper.orderByDesc(Movement::getCreateTime);
        return movementMapper.selectPage(p, wrapper);
    }

    @Override
    public Movement getById(Long id) {
        Movement m = movementMapper.selectById(id);
        if (m == null) {
            throw new BusinessException(ErrorCode.MOVEMENT_NOT_FOUND);
        }
        return m;
    }

    @Override
    public Movement create(Movement movement) {
        movementMapper.insert(movement);
        return movement;
    }

    @Override
    public void update(Movement movement) {
        getById(movement.getId()); // verify exists
        movementMapper.updateById(movement);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        movementMapper.deleteById(id);
    }
}
