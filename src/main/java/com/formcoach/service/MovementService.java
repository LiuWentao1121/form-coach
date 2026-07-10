package com.formcoach.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.formcoach.entity.Movement;

import java.util.List;

public interface MovementService {

    List<Movement> listAll();

    Page<Movement> page(int page, int size, String category);

    Movement getById(Long id);

    Movement create(Movement movement);

    void update(Movement movement);

    void delete(Long id);
}
