package com.formcoach.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.formcoach.common.PageResult;
import com.formcoach.common.Result;
import com.formcoach.entity.Movement;
import com.formcoach.service.MovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "动作库", description = "健身动作的增删改查")
@RestController
@RequestMapping("/api/movements")
@RequiredArgsConstructor
public class MovementController {

    private final MovementService movementService;

    @Operation(summary = "获取所有动作")
    @GetMapping
    public Result<?> listAll() {
        return Result.ok(movementService.listAll());
    }

    @Operation(summary = "分页查询动作（可按分类筛选）")
    @GetMapping("/page")
    public Result<PageResult<Movement>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "分类（下肢/上肢/核心）") @RequestParam(required = false) String category) {
        Page<Movement> result = movementService.page(page, size, category);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "获取动作详情")
    @GetMapping("/{id}")
    public Result<Movement> getById(@PathVariable Long id) {
        return Result.ok(movementService.getById(id));
    }

    @Operation(summary = "新增动作（管理员）")
    @PostMapping
    public Result<Movement> create(@RequestBody Movement movement) {
        return Result.ok(movementService.create(movement));
    }

    @Operation(summary = "更新动作（管理员）")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Movement movement) {
        movement.setId(id);
        movementService.update(movement);
        return Result.ok();
    }

    @Operation(summary = "删除动作（管理员）")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        movementService.delete(id);
        return Result.ok();
    }
}
