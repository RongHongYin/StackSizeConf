# Tools（实用工具箱）

面向 **Minecraft 1.21.11 + Fabric** 的整合小工具模组。游戏内与 Mod Menu 中显示名称为 **Tools**；技术标识（`mod_id`）仍为 `stacksizeconf`，与旧配置、工程目录兼容。

---

## 功能一览

| 功能 | 说明 |
|------|------|
| **自定义堆叠** | 按倍率与封顶提高物品最大堆叠；原不可堆叠物品可用单独「基准上限」参与计算。合并规则与原版一致（物品与组件数据须一致）。 |
| **漏斗与容器** | 修正漏斗向箱子等容器合并时的堆叠上限（避免卡在 99）；可配置 **漏斗传输速度倍率**（缩短/延长每次吸入或吐出后的冷却，1.0 为原版）。 |
| **物品吸附** | 在玩家附近将掉落物吸入背包（可开关、可调范围）。 |
| **手持潜影盒** | 手持潜影盒时快捷打开内容（可配置潜行、副手、骑乘/飞行、音效、服务端校验等）。 |
| **大数字显示** | 物品栏等界面中，堆叠数量文字过长时自动缩小绘制，避免大数字（如 1024）挤出格位。 |

---

## 环境与依赖

| 项目 | 说明 |
|------|------|
| Minecraft | 1.21.11 |
| Fabric Loader | 见 `gradle.properties` 中 `loader_version` |
| Fabric API | 与工程版本一致（如 `0.141.3+1.21.11`） |
| Cloth Config | **21.11.x**（与 1.21.11 匹配；旧版 15.x 会导致配置界面崩溃） |
| 安装 | 将构建得到的 `stacksizeconf-*.jar` 放入 `.minecraft/mods` |

**单机 / 联机**：逻辑在服务端生效时，服务端与客户端建议安装同一版本模组；**`config/stacksizeconf.json`** 在联机时应与服务器一致。

---

## 如何打开配置

1. **快捷键**：默认 **O**（可在控制设置中改键）打开 **Tools** 设置界面。  
2. **Mod Menu**（若已安装）：在模组列表中进入本模组配置。  
3. **直接改文件**：游戏配置目录下的 **`config/stacksizeconf.json`**（JSON，修改后重启或按单机逻辑重载；服务端需重启或按你使用的管理工具重载）。

---

## 配置文件说明（`config/stacksizeconf.json`）

以下为常用键名（与游戏内 Cloth 界面一致；布尔/数值范围以界面与持久化校验为准）。

### 自定义堆叠

| 键名 | 含义 |
|------|------|
| `enable_stack_overrides` | 是否启用堆叠覆盖；`false` 时恢复原版堆叠。 |
| `stack_size_multiplier` | 堆叠倍率（在基准上乘算后四舍五入，再受封顶限制）。 |
| `non_stackable_base_max` | 原版最大堆叠为 1 的物品所用的基准上限。 |
| `max_stack_hard_cap` | 全局堆叠封顶。 |

**计算要点**：可堆叠物品以原版最大堆叠为基准；原为 1 的物品以 `non_stackable_base_max` 为基准；结果限制在 `[1, max_stack_hard_cap]`。

### 漏斗速度

| 键名 | 含义 |
|------|------|
| `hopper_transfer_speed_multiplier` | 传输速度倍率；`1.0` 为原版，大于 1 加快，小于 1 减慢。过大可能增加服务端负担。 |

### 物品吸附

| 键名 | 含义 |
|------|------|
| `enable_item_magnet` | 是否启用吸附。 |
| `item_magnet_range` | 吸附范围（格）。 |

### 手持潜影盒

| 键名 | 含义 |
|------|------|
| `enable_handheld_shulker_open` | 是否启用手持打开。 |
| `shulker_open_require_sneak` | 是否需要潜行。 |
| `shulker_open_allow_offhand` | 是否允许副手。 |
| `shulker_open_allow_riding_or_flying` | 是否允许骑乘/飞行时打开。 |
| `shulker_open_play_sound` | 是否播放打开音效。 |
| `shulker_open_server_validation` | 是否在服务端校验打开条件。 |

---

## 使用建议

- **平衡**：堆叠与漏斗倍率过高可能影响经济、红石节奏与部分模组逻辑，请按需调整。  
- **联机**：以服务端 `stacksizeconf.json` 为准，客户端避免使用冲突配置。  
- **关闭某类功能**：将对应 `enable_*` 设为 `false`；堆叠类可单独关闭 `enable_stack_overrides`。

---

## 开发者：构建

```bash
./gradlew build
```

产物一般在 `build/libs/`。若国内网络拉依赖失败，可在 `gradle.properties` 中按注释配置代理。

---

## 许可证

见仓库内许可证文件；`gradle.properties` 中声明为 MIT 时以仓库实际 LICENSE 为准。
