# AradMMO

[中文](README.md) | [English](README.en.md)

AradMMO 是一个面向 Paper/Folia 的原创 RPG/MMO 基础插件，目标是提供“可配置、可扩展、可运营”的核心系统。

- 版本：0.1.0-SNAPSHOT
- Java：25+
- Paper API：26.1.2.build.53-stable
- 兼容：Paper 26.1+、Folia（已声明 folia-supported）

## 项目亮点

- 模块化结构：core / item / rpg / eco 等子系统分层
- 配置驱动：职业、属性、元素、状态、装备规则均可通过 YAML 调整
- 聊天子系统：支持规则化聊天格式与跨服路由组管理
- 装备子系统：模板发放、套装统计、防具类型与职业精通计算
- 运维友好：统一 `/am` 根命令，含调试命令与运行时重载

## 当前功能概览

### Core

- 玩家档案：等级、经验、金币、VIP、职业、属性、技能、元素等
- 指令系统：统一 `/am` 子命令体系
- 多语言：zh_cn / en_us，基于 Adventure MiniMessage
- 聊天系统：
  - 聊天开关与重载
  - 跨服路由组查看、切换、保存
  - 基于规则的聊天格式渲染

### RPG

- 职业分阶段定义（stage-0/stage-1/stage-2）
- 等级与点数成长
- 属性与元素系统（可配置）
- 技能等级、施放与冷却
- 战斗计算（属性、元素、技能增益）

### Item/Equipment

- 装备槽位与装备面板
- 装备模板生成（含稀有度颜色、DNF 风格 Lore）
- 防具类型系统（CLOTH/LEATHER/LIGHT/HEAVY/PLATE）
- 套装件数加成（按 set-id）
- 职业防具精通：
  - 精通件数加成
  - 精通分型加成
  - 混穿惩罚
  - 护甲精通被动技能倍率（按职业阶段映射等级并套倍率）
- 调试命令：`/am equip debug armor [player]` 可查看件数、加成来源、被动等级与倍率

## 快速开始

### 1. 构建

```powershell
mvn clean package
```

产物路径：target/AradMMO-0.1.0-SNAPSHOT.jar

### 2. 安装

1. 将构建产物放入服务端 plugins 目录
2. 首次启动后会在插件目录生成默认配置
3. 按需调整配置后执行 `/am reload`

### 3. 可选软依赖

- MythicLib（provided）
- MMOItems-API（provided）

未安装时插件会按软依赖策略降级，不阻塞基础功能启动。

## 截图区（可替换）

你可以先用占位图结构发布，后续把同名文件替换为真实截图。

建议截图清单：

- 主菜单（/am menu）
- 装备面板（/am equip）
- 防具调试输出（/am equip debug armor）
- 聊天跨服路由管理（/am chat route list）

推荐将图片放到仓库目录：docs/images

示例引用（发布后可正常显示）：

```markdown
![主菜单](docs/images/menu-overview.png)
![装备面板](docs/images/equip-panel.png)
![防具调试](docs/images/armor-debug.png)
![跨服路由](docs/images/chat-route.png)
```

## 配置示例区

### 1. 护甲精通被动配置示例

路径：src/main/resources/item/equipment/armor/system.yml

```yml
class-armor-mastery:
  enabled: true
  strict-type-bonus: true

  passive-skill-level-by-stage:
    "0": 1
    "1": 2
    "2": 3

  passive-skill-multiplier-by-level:
    "1": 0.85
    "2": 1.00
    "3": 1.15
```

### 2. 套装加成示例

路径：src/main/resources/item/equipment/armor/system.yml

```yml
set-bonuses:
  iron-guard:
    "2":
      phys-def: 80
    "3":
      max-hp: 220
    "5":
      max-hp: 420
      phys-def: 120
```

### 3. 聊天路由组示例

路径：src/main/resources/config/zh_cn/chat.yml（按你的实际文件为准）

```yml
bridge:
  enabled: true
  server-id: lobby-1
  route-groups:
    public:
      targets: ["lobby-2", "game-1"]
    staff:
      targets: ["admin-1"]
```

## 常见问题（FAQ）

### Q1：为什么构建失败，提示 Java 版本不匹配？

A：本项目使用 Java 25 编译（maven.compiler.release=25）。请确认本机和 CI 使用 Java 25+。

### Q2：服务端启动后提示找不到 MythicLib / MMOItems？

A：这两个是软依赖。未安装时相关联动功能会降级，基础功能可运行。若要启用对应联动，请安装兼容版本。

### Q3：修改配置后为什么没有生效？

A：先确认 YAML 缩进和键名正确，再执行 /am reload。若仍无效，建议重启服务端并检查控制台报错。

### Q4：护甲精通被动倍率怎么验证？

A：给角色穿上对应防具后执行 /am equip debug armor [player]，查看 stage、passive level、multiplier 与加成条目是否匹配。

### Q5：跨服聊天路由切换后重启丢失怎么办？

A：切换后执行对应保存命令（chat route save）。如仍丢失，检查配置文件写入权限。

## 常用命令

以下仅列运营中最常用的一部分：

- `/am help`
- `/am reload`
- `/am profile [player]`
- `/am menu`
- `/am chat <on|off|reload|route>`
- `/am chat route list`
- `/am chat route <group|clear|save>`
- `/am equip`
- `/am equip debug armor [player]`
- `/am item give <player> <templateId>`

完整命令与权限请查看：src/main/resources/paper-plugin.yml

## 关键配置目录

- src/main/resources/config
  - zh_cn / en_us：语言与职业配置
- src/main/resources/item/equipment
  - armor/system.yml：防具规则（类型、套装、精通、被动倍率）
  - armor/templates.yml：防具模板
  - stat-templates.yml：通用属性模板
- src/main/resources/paper-plugin.yml
  - 插件元信息、命令、权限声明

运行时生成目录（服务器 plugins 下）会包含玩家档案等动态数据。

## 防具精通被动机制说明

在 `item/equipment/armor/system.yml` 的 `class-armor-mastery` 下：

- `passive-skill-level-by-stage`：职业阶段 -> 被动等级
- `passive-skill-multiplier-by-level`：被动等级 -> 加成倍率

计算流程：

1. 解析玩家职业精通类型与职业阶段
2. 按阶段得到被动等级
3. 按等级得到倍率
4. 对精通相关加成（含分型加成）统一按倍率缩放

建议用 `/am equip debug armor` 验证配置是否按预期生效。

## 架构说明

架构文档见：ARCHITECTURE.md

## 开发计划（Roadmap）

- 数据持久化增强（SQLite/MySQL）
- 跨服档案/状态同步
- 更完整技能树与任务线
- 经济与副本玩法深化

## 许可证

当前仓库未附带明确开源许可证。若计划公开发布，建议补充 LICENSE 文件并在本 README 声明许可条款。