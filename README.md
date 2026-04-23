# 🎬 TitlePro

**A powerful, highly customizable title display mod for Minecraft Forge 1.20.1.**

> Author: **XioYim** · Version: **1.0.0** · Loader: **Forge 1.20.1** · Language: Java 17

---

## 📖 Introduction

TitlePro lets you create, configure, and send fully customized on-screen titles to any player — with rich text styling, smooth animations, background effects, and optional chained commands — all through an intuitive in-game GUI or a single command.

Whether you're building an adventure map, running a server event, or just adding polish to your world, TitlePro gives you pixel-level control over every title you display.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🎨 Rich Text | Full JSON text component support — colors, bold, italic, and § color codes |
| 📐 Positioning | Offset the title anywhere on screen (X / Y) |
| 🔢 Scale | Resize the title from tiny to huge |
| ⏱️ Timing | Fine-tune stay time, fade-in, and fade-out in seconds |
| 📏 Line Spacing | Control vertical spacing between stacked titles |
| 🔃 Stack Direction | New titles stack upward or downward |
| 🟦 Background | Colored background panel with alpha, padding, and Y-offset |
| 🌑 Shadow | Drop shadow with configurable X/Y offset |
| 🎞️ Exit Slide | Smooth slide-out animation with adjustable speed |
| ↔️ Text Align | Left / Center / Right alignment for each title |
| ➕ Extra Command | Automatically run any command when the title is sent |
| 💾 Schemes | Save and load full configurations as named JSON files |
| 📋 Clipboard | Import commands from clipboard; export via "Generate & Copy" |
| 👁️ Preview | Live in-GUI title preview before sending |

---

## 🚀 Getting Started

1. Install **Forge 1.20.1**.
2. Drop the `titlepro-*.jar` into your `mods/` folder.
3. Launch the game.
4. Open the GUI with `/titlepro` (no arguments), **or** bind a key in Controls.
5. Design your title, then click **Send to Self** to preview it in-game.

---

### 🔘 Right-Column Buttons

| Button | Action |
|---|---|
| Import from File | Browse and load a saved scheme from `config/titlepro/schemes/` |
| Import from Clipboard | Parse a `/titlepro` command from the clipboard and fill all fields |
| Preview | Play a live preview of the current title inside the GUI |
| Send to Self | Send the title to yourself immediately |
| Generate & Copy | Build the full `/titlepro` command and copy it to clipboard |
| Reset | Reset all fields to default values |
| Stack Direction | Toggle stacking direction (Up / Down) |
| Align | Cycle text alignment (Left → Center → Right) |
| Export Scheme | Save the current configuration as a named scheme file |

### ⚡ Extra Command Row

Enable the **Extra Command** toggle to attach an additional command that fires automatically every time the title is sent. The command is embedded in the generated `/titlepro` command string and is also executed by `/titleprosend`.

---

## 📜 Commands

### `/titlepro` — Send a Title

```
/titlepro <targets> <text> <offsetX> <offsetY> <scale>
          <stay_s> <fadeIn_s> <fadeOut_s> <spacing> <up|down>
          <#bgColor> <bgAlpha%> <bgPadX> <bgPadY> <bgOffsetY>
          <exitSlide:true|false> <exitSpeed>
          <bgType:shadow|bg> <shadowOffX> <shadowOffY>
          <textAlign:left|center|right>
          [extraCmdEnabled:true|false] [extraCmd...]
```

> **Permission required:** operator level 2

| Parameter | Type | Description |
|---|---|---|
| `targets` | Entity selector | Who receives the title (`@a`, `@p`, player name, …) |
| `text` | JSON / string | Title text — JSON component `{"text":"…"}` or bare string |
| `offsetX` / `offsetY` | Integer | Screen position offset in pixels |
| `scale` | Float | Size multiplier (`1.5` = 150%) |
| `stay_s` | Float | Seconds the title stays visible |
| `fadeIn_s` / `fadeOut_s` | Float | Fade-in / fade-out duration in seconds |
| `spacing` | Integer | Vertical spacing between stacked titles |
| `up\|down` | String | Stack direction for multiple simultaneous titles |
| `#bgColor` | Hex color | Background panel color, e.g. `#000000` |
| `bgAlpha%` | 0–100 | Background opacity percentage |
| `bgPadX` / `bgPadY` | Integer | Background horizontal / vertical padding |
| `bgOffsetY` | Float | Background vertical offset |
| `exitSlide` | Boolean | Enable slide-out exit animation |
| `exitSpeed` | Float | Speed of the exit slide |
| `bgType` | `shadow`\|`bg` | Render mode: drop shadow or background panel |
| `shadowOffX` / `shadowOffY` | Float | Shadow X and Y offset |
| `textAlign` | `left`\|`center`\|`right` | Horizontal alignment of the title text |
| `extraCmdEnabled` | Boolean | *(Optional)* Execute an extra command on send |
| `extraCmd` | String… | *(Optional)* The extra command (rest of the line) |

---

### `/titleprosend` — Send a Saved Scheme

```
/titleprosend <targets> <schemeName>
```

> **Permission required:** operator level 2
> **Tab completion:** scheme names auto-complete from saved files ✅

| Parameter | Description |
|---|---|
| `targets` | Entity selector for recipients |
| `schemeName` | Name of a saved scheme (`.json` extension optional) |

Loads `config/titlepro/schemes/<schemeName>.json` and sends it to all targets. If the scheme includes an extra command, it runs on the server.

---

## 💡 Examples

### 🎉 Basic welcome title
```
/titlepro @a {"text":"Welcome!","color":"gold","bold":true} 0 -30 1.5 4.0 0.5 1.0 14 up #000000 50 6 4 0 false 1.0 shadow 2.0 2.0 center
```

### 🌈 Multi-color text using § codes
```
/titlepro @p {"text":"\u00a7eHello \u00a76World\u00a7r!"} 0 0 1.0 3.0 0.5 0.5 14 up #000000 0 0 0 0 false 1.0 shadow 0.0 0.0 center
```
> 💡 Inside JSON text components, write `§` as `\u00a7` (e.g. `\u00a7e` = yellow, `\u00a76` = gold, `\u00a7c` = red).

### 📤 Send a saved scheme to all players
```
/titleprosend @a my_announcement
```

### 🔗 Title with an extra command (play a sound on send)
```
/titlepro @a {"text":"Round Start!","color":"red","bold":true} 0 -20 2.0 3.0 0.5 0.5 14 up #000000 60 8 4 0 false 1.0 bg 0.0 0.0 center true playsound minecraft:entity.ender_dragon.growl master @a
```
> The extra command runs server-side at the same moment the title is sent.

### 🗃️ Export and reuse a scheme
1. Configure a title in the GUI.
2. Click **Export Scheme**, enter a name (e.g. `boss_intro`), click **Export**.
3. From then on: `/titleprosend @a boss_intro` — done! 🎊

---

## 📁 File Structure

```
config/
└── titlepro/
    ├── titlepro-common.toml      ← mod settings
    └── schemes/
        ├── welcome.json
        ├── boss_intro.json
        └── ...
```

### Scheme JSON Format

```json
{
  "text": "",
  "extra": [
    {"bold": false, "italic": false, "color": "#FFAA00", "text": "Title"},
    {"bold": false, "italic": false, "color": "#55FFFF", "text": "\u00a7cP\u00a7a\u00a7or\u00a7ro"}
  ],
  "offsetX": 0,
  "offsetY": 0,
  "scale": 1.0,
  "stay": 5000,
  "fadeIn": 0,
  "fadeOut": 1000,
  "spacing": 14,
  "stackUp": false,
  "bgColor": 0,
  "bgAlpha": 128,
  "bgPaddingX": 3,
  "bgPaddingY": 1,
  "bgOffsetY": -0.3,
  "exitSlide": true,
  "exitSpeed": 1.0,
  "bgType": 1,
  "shadowOffsetX": 0.4,
  "shadowOffsetY": 0.4,
  "textAlign": 1,
  "extraCmdEnabled": false,
  "extraCmd": ""
}
```

> 🔍 `bgType`: `0` = shadow, `1` = background panel. `textAlign`: `0` = left, `1` = center, `2` = right. `stay`, `fadeIn`, `fadeOut` are in **milliseconds**.

---

## ⚙️ Configuration

`config/titlepro/titlepro-common.toml` is auto-generated on first launch and contains server-side settings for the mod.

---

## 🔑 Permissions

| Action | Required Level |
|---|---|
| Open GUI | Any player |
| `/titlepro` | Op level 2 |
| `/titleprosend` | Op level 2 |

---

## 📝 Notes & Tips

- 💬 **§ color codes** in JSON components must be written as `\u00a7` (e.g. `\u00a7e` = yellow). The GUI handles this conversion automatically during import and export — you never need to escape them manually.
- 🖱️ **Click inside the text box** to move the cursor to the exact position you clicked.
- 📋 **Clipboard Import** accepts a full `/titlepro` command — paste and click to fill all fields instantly.
- 🌐 **Unicode scheme names** (including Chinese) are supported at the file level. If your IME doesn't work in-game, copy the name from another app and **Ctrl+V** paste it into the Export dialog.
- ⚠️ Time values in the GUI are in **seconds**; they are stored in scheme files as **milliseconds**.

---

---

# 🎬 TitlePro（中文说明）

**适用于 Minecraft Forge 1.20.1 的强大自定义标题展示模组。**

> 作者：**XioYim** · 版本：**1.0.0** · 加载器：**Forge 1.20.1** · 语言：Java 17

---

## 📖 简介

TitlePro 允许你通过直观的游戏内 GUI 或一条命令，向任意玩家发送完全自定义的屏幕标题——支持富文本样式、流畅动画、背景特效，以及可选的联动命令。

无论是制作冒险地图、举办服务器活动，还是为你的世界增添质感，TitlePro 都能让你对每一条标题的展示方式拥有像素级别的掌控。

---

## ✨ 功能一览

| 功能 | 说明 |
|---|---|
| 🎨 富文本 | 完整 JSON 文本组件支持——颜色、加粗、斜体，以及 § 颜色代码 |
| 📐 位置偏移 | 将标题定位到屏幕任意位置（X / Y） |
| 🔢 缩放 | 任意缩放标题大小 |
| ⏱️ 时间控制 | 精细调整停留时长、淡入、淡出（单位：秒） |
| 📏 行距 | 控制叠加标题之间的纵向间距 |
| 🔃 叠加方向 | 新标题向上或向下叠加 |
| 🟦 背景 | 彩色背景面板，支持透明度、内边距、纵向偏移 |
| 🌑 阴影 | 带可配置 X/Y 偏移的投影效果 |
| 🎞️ 退出滑动 | 平滑滑出动画，速度可调 |
| ↔️ 对齐方式 | 标题文本左对齐 / 居中 / 右对齐 |
| ➕ 额外命令 | 发送标题时自动执行任意附加命令 |
| 💾 方案管理 | 将完整配置保存/加载为命名 JSON 文件 |
| 📋 剪贴板 | 从剪贴板导入命令；"生成并复制"一键导出完整命令 |
| 👁️ 实时预览 | 在 GUI 内即时预览标题效果 |

---

## 🚀 快速开始

1. 安装 **Forge 1.20.1**。
2. 将 `titlepro-*.jar` 放入 `mods/` 文件夹。
3. 启动游戏。
4. 执行 `/titlepro`（不加参数）打开 GUI，**或**在控制设置中绑定快捷键。
5. 设计好标题后，点击**发送给自己**在游戏中预览效果。

---

### 🔘 右列按钮说明

| 按钮 | 功能 |
|---|---|
| 从配置文件中导入 | 浏览并导入 `config/titlepro/schemes/` 中的已保存方案 |
| 从剪贴板导入 | 解析剪贴板中的 `/titlepro` 命令并自动填充所有字段 |
| 预览标题 | 在 GUI 中播放当前标题的实时预览 |
| 发送给自己 | 立即将标题发送给自己 |
| 生成并复制 | 生成完整 `/titlepro` 命令并复制到剪贴板 |
| 重置内容 | 将所有字段重置为默认值 |
| 叠加方向 | 切换标题叠加方向（向上 / 向下） |
| 对齐方式 | 循环切换文本对齐（左 → 居中 → 右） |
| 导出方案 | 将当前配置保存为命名方案文件 |

### ⚡ 额外命令行

开启**额外命令**开关后，可填写一条附加命令，在每次发送标题时自动执行。该命令会被嵌入生成的 `/titlepro` 命令字符串中，使用 `/titleprosend` 时同样触发。

---

## 📜 命令说明

### `/titlepro` — 发送标题

```
/titlepro <目标> <文本> <X偏移> <Y偏移> <缩放>
          <停留秒> <淡入秒> <淡出秒> <行距> <up|down>
          <#背景色> <透明度%> <横向边距> <纵向边距> <背景Y偏移>
          <退出滑动:true|false> <退出速度>
          <背景类型:shadow|bg> <阴影X> <阴影Y>
          <对齐:left|center|right>
          [额外命令开关:true|false] [额外命令...]
```

> **所需权限：** OP 等级 2

| 参数 | 类型 | 说明 |
|---|---|---|
| `目标` | 实体选择器 | 接收标题的玩家（`@a`、`@p`、玩家名等） |
| `文本` | JSON / 字符串 | 标题文本——JSON 组件 `{"text":"…"}` 或纯文本 |
| `X偏移` / `Y偏移` | 整数 | 屏幕位置像素偏移量 |
| `缩放` | 浮点数 | 大小倍率（`1.5` = 150%） |
| `停留秒` | 浮点数 | 标题停留显示的秒数 |
| `淡入秒` / `淡出秒` | 浮点数 | 淡入 / 淡出持续秒数 |
| `行距` | 整数 | 叠加标题之间的纵向间距 |
| `up\|down` | 字符串 | 多条标题的叠加方向 |
| `#背景色` | 十六进制颜色 | 背景面板颜色，如 `#000000` |
| `透明度%` | 0–100 | 背景不透明度百分比 |
| `横向/纵向边距` | 整数 | 背景水平 / 垂直内边距 |
| `背景Y偏移` | 浮点数 | 背景纵向偏移量 |
| `退出滑动` | 布尔值 | 是否启用滑出退出动画 |
| `退出速度` | 浮点数 | 滑出动画速度 |
| `背景类型` | `shadow`\|`bg` | 渲染模式：投影或背景面板 |
| `阴影X` / `阴影Y` | 浮点数 | 阴影 X / Y 偏移量 |
| `对齐` | `left`\|`center`\|`right` | 标题文本水平对齐方式 |
| `额外命令开关` | 布尔值 | *（可选）* 是否执行附加命令 |
| `额外命令` | 字符串… | *（可选）* 要执行的附加命令（剩余全部内容） |

---

### `/titleprosend` — 发送已保存的方案

```
/titleprosend <目标> <方案名>
```

> **所需权限：** OP 等级 2
> **Tab 补全：** 自动补全已保存的方案文件名 ✅

| 参数 | 说明 |
|---|---|
| `目标` | 接收方的实体选择器 |
| `方案名` | 已保存方案的名称（`.json` 后缀可选） |

加载 `config/titlepro/schemes/<方案名>.json` 并发送给所有目标玩家。若方案中包含额外命令，则在服务端执行。

---

## 💡 使用示例

### 🎉 基础欢迎标题
```
/titlepro @a {"text":"欢迎加入！","color":"gold","bold":true} 0 -30 1.5 4.0 0.5 1.0 14 up #000000 50 6 4 0 false 1.0 shadow 2.0 2.0 center
```

### 🌈 使用 § 颜色代码的多色文本
```
/titlepro @p {"text":"\u00a7e你好 \u00a76世界\u00a7r！"} 0 0 1.0 3.0 0.5 0.5 14 up #000000 0 0 0 0 false 1.0 shadow 0.0 0.0 center
```
> 💡 JSON 组件中 `§` 须写作 `\u00a7`（黄色 `\u00a7e`、金色 `\u00a76`、红色 `\u00a7c`）。
> GUI 在导入/导出时会自动处理转义，无需手动修改。

### 📤 向所有玩家发送已保存方案
```
/titleprosend @a <方案名（仅支持英文）>
```

### 🔗 带附加命令的标题（发送时同步播放音效）
```
/titlepro @a {"text":"回合开始！","color":"red","bold":true} 0 -20 2.0 3.0 0.5 0.5 14 up #000000 60 8 4 0 false 1.0 bg 0.0 0.0 center true playsound minecraft:entity.ender_dragon.growl master @a
```
> 附加命令在服务端执行，与标题同步触发。

### 🗃️ 导出方案并复用
1. 在 GUI 中配置好标题。
2. 点击**导出方案**，输入名称（如 `boss_intro`），点击**导出**。
3. 之后只需执行 `/titleprosend @a boss_intro` 即可一键复用！🎊

---

## 📁 文件结构

```
config/
└── titlepro/
    ├── titlepro-common.toml      ← 模组配置文件
    └── schemes/
        ├── welcome.json
        ├── boss_intro.json
        └── ...
```

### 方案 JSON 格式

```json
{
  "text": "",
  "extra": [
    {"bold": false, "italic": false, "color": "#FFAA00", "text": "Title"},
    {"bold": false, "italic": false, "color": "#55FFFF", "text": "\u00a7cP\u00a7a\u00a7or\u00a7ro"}
  ],
  "offsetX": 0,
  "offsetY": 0,
  "scale": 1.0,
  "stay": 5000,
  "fadeIn": 0,
  "fadeOut": 1000,
  "spacing": 14,
  "stackUp": false,
  "bgColor": 0,
  "bgAlpha": 128,
  "bgPaddingX": 3,
  "bgPaddingY": 1,
  "bgOffsetY": -0.3,
  "exitSlide": true,
  "exitSpeed": 1.0,
  "bgType": 1,
  "shadowOffsetX": 0.4,
  "shadowOffsetY": 0.4,
  "textAlign": 1,
  "extraCmdEnabled": false,
  "extraCmd": ""
}
```

> 🔍 `bgType`：`0` = 投影模式，`1` = 背景面板模式。`textAlign`：`0` = 左对齐，`1` = 居中，`2` = 右对齐。
> `stay`、`fadeIn`、`fadeOut` 在方案文件中以**毫秒**为单位，GUI 中显示为**秒**。

---

## ⚙️ 配置文件

`config/titlepro/titlepro-common.toml` 在首次启动时自动生成，包含模组的服务端配置项。

---

## 🔑 权限说明

| 操作 | 所需等级 |
|---|---|
| 打开 GUI | 任意玩家 |
| `/titlepro` | OP 等级 2 |
| `/titleprosend` | OP 等级 2 |

---

## 📝 使用须知与技巧

- 💬 **§ 颜色代码** 在 JSON 组件中必须写成 `\u00a7`（如黄色 `\u00a7e`）。GUI 的导入/导出功能会自动处理转义，无需手动操作。
- 🖱️ 在文本输入框内**鼠标点击**即可将光标移动到点击位置。
- 📋 **剪贴板导入**支持完整的 `/titlepro` 命令字符串——粘贴后点击即可自动填充所有字段。
- 🌐 方案文件名支持中文等 Unicode 字符。若游戏内输入法（IME）无法正常使用，可在其他软件中复制中文名称后用 **Ctrl+V** 粘贴到导出对话框的名称输入框。
- ⚠️ GUI 中时间单位为**秒**；方案文件中存储的是**毫秒**，不要手动编辑混淆。

---

## 📄 许可证 / License

本模组仅供个人与服务器使用。如需转载或二次修改，请注明作者 **XioYim**。
