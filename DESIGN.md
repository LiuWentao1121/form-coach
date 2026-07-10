# FormCoach — AI 健身动作纠错平台

## 一句话描述
用户对着手机做动作，系统实时分析关节角度，指出动作哪里不对、怎么改。

## 目标用户
自主训练的健身爱好者（不做健身房SaaS，只做C端）

---

## 核心功能（MVP，一个月）

### 1. 动作训练
- 选择动作（深蹲/俯卧撑/平板支撑）
- 手机摄像头拍摄，画面叠加骨架线
- **本地推理**：小程序端 MediaPipe WASM 提取 33 个关节点坐标，不传视频到后端
- 后端接收关节点坐标 → 角度计算 → 实时返回纠错提示（"膝盖再打开一点"）
- 语音播报纠错提示（小程序 TTS）

### 2. 训练报告
- 一组做完后显示评分（0-100）
- 错误类型统计（膝盖内扣 5 次、核心塌陷 3 次）
- 与上次训练对比改善

### 3. 训练历史
- 日历热力图（哪天练了）
- 每次训练的关节角度变化曲线
- 体重/体脂记录

### 4. 个人中心
- 总训练次数、总时长
- 连续打卡天数
- 成就徽章

---

## 技术架构

```
┌──────────────────────────────────────────────────┐
│ 微信小程序                                        │
│ camera → MediaPipe WASM → 33 关节点 → WebSocket   │
│ 只传坐标(x/y/z)，不传视频，几十KB/帧               │
└────────────────────┬─────────────────────────────┘
                     │ WebSocket / HTTP
┌────────────────────▼─────────────────────────────┐
│ Spring Boot (form-coach)                          │
│                                                   │
│ ┌─────────────┐  ┌──────────────┐  ┌───────────┐ │
│ │ 角度计算引擎 │  │ 错误判定器    │  │ 评分引擎   │ │
│ │ 几何 + 向量 │  │ 规则+阈值     │  │ 加权汇总   │ │
│ └─────────────┘  └──────────────┘  └───────────┘ │
│                                                   │
│ ┌─────────────┐  ┌──────────────┐  ┌───────────┐ │
│ │ 用户服务     │  │ 训练记录服务  │  │ 成就服务   │ │
│ └─────────────┘  └──────────────┘  └───────────┘ │
└────────────────────┬─────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────┐
│ MySQL + Redis                                     │
│ - 用户表、训练记录表、动作表、成就表                │
│ - Redis：训练会话缓存、实时坐标暂存                │
└──────────────────────────────────────────────────┘
```

---

## 数据库设计（6张表）

### sys_user — 用户表
```
id, username, password, nickname, avatar, height(身高cm),
weight(体重kg), gender, create_time
```

### coach_movement — 动作库
```
id, name(深蹲), category(下肢), description,
standard_angles(标准角度JSON), tips(纠错文案JSON), image_url, video_url
```

### coach_training_session — 训练会话
```
id, user_id, movement_id, score, duration_seconds,
rep_count(完成次数), error_summary(错误汇总JSON),
created_at
```

### coach_frame_data — 关键帧数据（可选，量大时只保留错误帧）
```
id, session_id, frame_index,
joint_data(33个关节点坐标JSON),
is_error_frame(是否错误帧),
error_type(错误类型),
created_at
```

### coach_angle_result — 角度计算结果
```
id, session_id, frame_index, joint_name(膝关节),
angle_value(角度值), angle_status(NORMAL/WARN/ERROR),
created_at
```

### coach_achievement — 用户成就
```
id, user_id, achievement_type(连续7天/总分1000+等),
unlocked_at
```

---

## 后端模块划分

```
com.formcoach
├── controller
│   ├── AuthController          # 登录注册
│   ├── MovementController      # 动作库增删改查
│   ├── TrainingController      # 训练：接收坐标→返回纠错提示
│   ├── ReportController        # 训练报告
│   └── UserController          # 个人中心
├── service
│   ├── AngleCalculator         # 核心：关节角度计算（几何+向量数学）
│   ├── ErrorDetector           # 错误判定（规则+阈值）
│   ├── ScoreCalculator         # 评分引擎
│   ├── MovementService         # 动作库管理
│   ├── TrainingService         # 训练流程
│   └── AchievementService      # 成就检查
├── entity
│   ├── User
│   ├── Movement
│   ├── TrainingSession
│   ├── FrameData
│   ├── AngleResult
│   └── Achievement
├── mapper (MyBatis-Plus)
└── config
    ├── SecurityConfig
    ├── WebSocketConfig
    └── CorsConfig
```

---

## 核心算法：角度计算引擎

### 输入
MediaPipe Pose 33 个关节点，格式：
```json
{
  "landmarks": [
    {"x": 0.512, "y": 0.318, "z": -0.021, "visibility": 0.99},
    ...
  ]
}
```

### 计算逻辑（以深蹲膝关节为例）

```
髋关节(landmark 23/24) → 膝关节(landmark 25/26) → 踝关节(landmark 27/28)

向量1: 髋 → 膝
向量2: 膝 → 踝
角度: arccos( (v1·v2) / (|v1| * |v2|) )

膝关节角度 < 80°: 太深，提示"下蹲过深，膝盖受力过大"
膝关节角度 80°-100°: ✓ 正常
膝关节角度 > 100°: 不够深，提示"再蹲低一点"

膝盖投影超出脚尖: 提示"重心后移，膝盖不要过脚尖"
```

### 输出
```json
{
  "frameIndex": 128,
  "isCorrect": false,
  "errors": [
    {"joint": "左膝", "angle": 75, "expected": "80-100", "tip": "膝盖不要内扣，朝脚尖方向打开"},
    {"joint": "腰背", "angle": 55, "expected": "60-90", "tip": "挺直腰背，不要弓背"}
  ],
  "score": 72
}
```

---

## 开发排期（4周）

| 周 | 任务 |
|----|------|
| **W1** | 项目骨架 + 用户模块 + 数据库建表 + SecurityConfig |
| **W2** | 动作库 CRUD + 角度计算引擎 + 错误判定器 + 评分引擎 |
| **W3** | 训练会话流程 + WebSocket 实时通信 + 训练报告 |
| **W4** | 成就系统 + 压测优化 + 简历文案 |

---

## 面试能聊的点

1. **角度计算引擎怎么设计的？**
   向量几何 + 关节链模型，不是简单的 if-else，而是基于人体运动学标准角度范围

2. **不同体型的人怎么自适应？**
   初始校准帧：让用户保持标准站姿录 3 秒，建立个人基线，后续计算时用相对偏差而非绝对值

3. **小程序端为什么选 MediaPipe WASM？**
   视频不离开手机保护隐私，WASM 比纯 JS 快 3-5 倍，33 个关节点帧率能到 30fps

4. **实时性怎么保证？**
   小程序端本地推理 < 30ms/帧，通过 WebSocket 传坐标到后端 < 50ms，角度计算 < 5ms，端到端延迟 < 100ms

5. **关节点抖动怎么处理？**
   指数移动平均（EMA）平滑连续帧的坐标，抖动幅度 > 阈值才判定为新状态

---

## 简历描述

> **FormCoach — AI 健身动作纠错平台**（2026.07 - 2026.08）
> - 基于 MediaPipe Pose + 自定义向量几何算法，实现深蹲/俯卧撑/平板支撑 3 个动作的实时关节角度检测与错误判定
> - 设计多层纠错规则引擎，覆盖膝内扣、核心塌陷、弓背等 12 种常见错误姿态，单帧判定耗时 < 5ms
> - 通过 EMA 平滑算法解决关节点抖动问题，配合自适应校准机制消除不同体型用户的基准偏差
> - 采用 WebSocket 实时通信，端到端延迟 < 100ms，用户训练后即时生成评分报告与改善建议
