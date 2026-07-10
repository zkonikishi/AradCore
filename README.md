# AradCore

阿拉德服务器的模块化 RPG 核心。当前 `0.1.0-SNAPSHOT` 实现 DNF 风格装备统一品级。

## 已实现

- 首次进入玩家背包的 MMOItems 装备随机产生 1–100% 品级。
- 同一品级统一缩放配置中的全部基础属性。
- PDC 永久保存每项原始值，重调始终从原值计算，不会重复缩放。
- 品级概率区间、属性标签、Lore 和颜色全部可配置。
- `/aradcore inspect` 查看手持物的 MythicLib 标签、品级和当前值。
- `/aradcore roll` 重调；`/aradcore set <百分比>` 调试指定品级。
- 使用反射接入 MythicLib，降低开发版 API 变化导致的编译耦合。

## 重要限制

1. 当前依靠背包事件发现新装备，不是 MMOItems 生成管线的原子钩子。后续应直接监听 `ItemBuildEvent` 并在事件末尾处理。
2. `scaled-tags` 的实际标签必须用服务器上的 `/aradcore inspect` 校准。默认值基于 MMOItems 常用命名。
3. 第一版只负责数值及 Lore；重调材料、金币扣除和 CoreTools GUI 尚未接入。
4. 已经被旧配置生成过且没有 AradCore 原始值的装备，首次接管会把“当前值”视为原始值。正式服迁移前必须制定迁移策略。

## 构建

工程使用 Java 21 与 Gradle。机器当前没有全局 Gradle/Maven，可添加 Gradle Wrapper 后执行：

```text
gradlew clean build
```

产物位于 `build/libs/AradCore-0.1.0-SNAPSHOT.jar`。

## 下一步优先级

1. 在测试服用 `/aradcore inspect` 确定所有 MMOItems 实际 NBT 标签。
2. 接入 `ItemBuildEvent`，增加生成原因白名单及模板排除规则。
3. 加入品级调整券、材料/金币事务、失败回滚、审计日志。
4. CoreTools 打开 GUI，AradCore 只负责服务与规则。
5. 后续模块：强化、增幅、锻造、耐久修理、宝石槽、继承、词条洗练。

详细交接见 `HANDOFF.md`。
