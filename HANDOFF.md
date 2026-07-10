# AradCore 开发交接

## 目标原则

- 不做只能转发命令的桥接插件；AradCore 是服务器规则的唯一权威层。
- MMOItems 保存物品模板，MMOCore 保存角色/职业数据，CoreTools 提供 GUI。
- 所有装备操作必须是幂等的、可回滚的、可审计的。
- 装备基础值与成长值分层：`最终值 = 基础模板值 × 品级 + 强化/增幅/锻造 + 宝石/词条`。

## 当前数据协议（PDC）

- `aradcore:grade_percent`：整数品级。
- `aradcore:grade_schema`：数据结构版本，目前为 1。
- `aradcore:base_<normalized-tag>`：每项不可变原始数值。
- `aradcore:grade_lore`：表明插件管理过品级 Lore。

不要删除或更名这些键；未来升级必须写 migration。

## 建议模块结构

```text
aradcore-api          公共服务、事件、DTO
aradcore-equipment    品级、强化、增幅、锻造、修理、镶嵌
aradcore-combat       力量/智力/体力/精神与伤害公式
aradcore-progression  转职、觉醒、成长与解锁
aradcore-integrations MMOItems/MMOCore/CoreTools/PAPI 适配器
aradcore-audit        操作日志、回滚、管理员工具
```

## 必须补的测试

- 同一物品调用 `ensure` 多次数值不变。
- 50%→80%→50% 后数值完全回到第一次 50%。
- 服务器重启、交易、死亡、容器移动后 PDC 不丢失。
- 宝石、强化和品级操作顺序不同但最终公式一致。
- 堆叠、拆分、铁砧、漏斗、创造模式复制不能绕过规则。
- 任何扣费失败都不能改变物品；任何物品写入失败都要退回货币与材料。

## 与现服对应

- 服务器：Paper 26.2。
- MMOItems：6.10.1 开发版。
- MMOCore：1.13.1 开发版。
- MythicLib：1.7.1 开发版。
- CoreTools：1.4.3-SNAPSHOT。
- 设计依据：仓库根目录 `装备系统设计文档.md`。

## 已知代码债

- `GradeListener` 全背包扫描用于原型验证，规模化前改成事件物品定点处理和去抖队列。
- Lore 目前假设管理行在首行。应改为 Adventure component 标记或重新走 MMOItems lore builder。
- 反射异常当前只写控制台；应限频并输出结构化审计日志。
