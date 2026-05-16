# AradMMO

AradMMO is an original RPG/MMO foundation plugin for Paper/Folia servers, focused on delivering a configurable, extensible, and operations-friendly core system.

- Version: 0.1.0-SNAPSHOT
- Java: 25+
- Paper API: 26.1.2.build.53-stable
- Compatibility: Paper 26.1+, Folia (folia-supported declared)

## Highlights

- Modular structure: layered subsystems such as core / item / rpg / eco
- Configuration-driven: classes, attributes, elements, status effects, and equipment rules are all YAML-based
- Chat subsystem: rule-based chat formatting and cross-server route group management
- Equipment subsystem: template-based item generation, set counting, armor types, and class armor mastery
- Ops-friendly: unified `/am` command root with debug commands and runtime reload support

## Feature Overview

### Core

- Player profiles: level, exp, gold, VIP, class, attributes, skills, elements, and more
- Command system: unified `/am` subcommand framework
- i18n: zh_cn / en_us based on Adventure MiniMessage
- Chat system:
  - chat receive toggle and reload
  - cross-server route group list/switch/save
  - rule-based chat format rendering

### RPG

- Stage-based class definitions (stage-0/stage-1/stage-2)
- Level progression and point growth
- Attribute and element systems (configurable)
- Skill leveling, casting, and cooldown
- Combat calculations (attributes, elements, skill buffs)

### Item/Equipment

- Equipment slots and equipment UI
- Template item generation (rarity colors + DNF-style lore)
- Armor type system (CLOTH/LEATHER/LIGHT/HEAVY/PLATE)
- Set bonus thresholds by set-id
- Class armor mastery:
  - mastery threshold bonuses
  - mastery type-specific bonuses
  - mismatch penalties
  - passive armor mastery multiplier (mapped by class stage and level)
- Debug command: `/am equip debug armor [player]` shows counts, triggered bonuses, passive level, and multiplier

## Quick Start

### 1. Build

```powershell
mvn clean package
```

Artifact path: `target/AradMMO-0.1.0-SNAPSHOT.jar`

### 2. Install

1. Put the built jar into your server `plugins` directory.
2. Start the server once to generate default configs.
3. Adjust configs as needed, then run `/am reload`.

### 3. Optional Soft Dependencies

- MythicLib (provided)
- MMOItems-API (provided)

If not installed, the plugin should degrade gracefully for integration features without blocking core startup.

## Common Commands

Frequently used operations:

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

For full command and permission declarations, see `src/main/resources/paper-plugin.yml`.

## Key Config Paths

- `src/main/resources/config`
  - `zh_cn` / `en_us`: localization and class configs
- `src/main/resources/item/equipment`
  - `armor/system.yml`: armor rules (types, sets, mastery, passive multipliers)
  - `armor/templates.yml`: armor templates
  - `stat-templates.yml`: generic stat templates
- `src/main/resources/paper-plugin.yml`
  - plugin metadata, commands, and permissions

Runtime-generated directories (under the server `plugins` folder) contain dynamic data such as player profiles.

## Passive Armor Mastery Mechanism

Under `class-armor-mastery` in `item/equipment/armor/system.yml`:

- `passive-skill-level-by-stage`: class stage -> passive skill level
- `passive-skill-multiplier-by-level`: passive skill level -> bonus multiplier

Calculation flow:

1. Resolve player's armor mastery type and class stage.
2. Map stage to passive skill level.
3. Map level to multiplier.
4. Apply that multiplier to mastery-related bonuses (including type-specific mastery bonuses).

Use `/am equip debug armor` to verify whether your config behaves as expected.

## Architecture

See `ARCHITECTURE.md` for architecture and module guidelines.

## Roadmap

- Enhanced persistence (SQLite/MySQL)
- Cross-server profile/state synchronization
- Richer skill tree and quest lines
- Extended economy and dungeon gameplay

## License

No explicit open-source license is included yet.
If you plan to publish this repository, it is recommended to add a `LICENSE` file and declare licensing terms in both README files.
