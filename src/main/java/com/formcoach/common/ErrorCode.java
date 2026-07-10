package com.formcoach.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 通用
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 token 已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    PASSWORD_WRONG(1003, "用户名或密码错误"),
    PASSWORD_TOO_SHORT(1004, "密码长度不能少于6位"),

    // 动作模块 2xxx
    MOVEMENT_NOT_FOUND(2001, "动作不存在"),
    MOVEMENT_NAME_EXISTS(2002, "动作名称已存在"),

    // 训练模块 3xxx
    SESSION_NOT_FOUND(3001, "训练会话不存在"),
    SESSION_ALREADY_ENDED(3002, "训练会话已结束"),
    INVALID_FRAME_DATA(3003, "无效的关节点数据"),

    // 报告模块 4xxx
    REPORT_NOT_FOUND(4001, "训练报告不存在"),

    // 系统
    RATE_LIMIT(5001, "请求过于频繁，请稍后再试"),
    WEBSOCKET_ERROR(5002, "WebSocket 通信异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
