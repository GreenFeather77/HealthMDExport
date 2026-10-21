# HealthMD 导出（Health Connect → 本地文件）

一个**本地优先**的安卓健康数据导出应用：读取手机 Google Health Connect 中的数据，导出为
**Markdown（Obsidian 友好）/ CSV / JSON** 文件。数据只保存在本机，不上传任何云端。

本应用参考 [health.md](https://healthmd.app)（Cody Bontecou，AGPL-3.0）的设计理念独立实现，
面向个人本地使用。

---

## 功能

| 功能 | 说明 |
| --- | --- |
| 连接状态检测 | 自动检测 Health Connect 是否安装 / 需要更新，一键跳转 |
| 逐项权限管理 | 13 项健康指标独立显示授权状态，一键批量请求授权 |
| 时间范围选择 | 今天 / 最近 7 天 / 30 天 / 90 天 / 自定义日期 |
| 三种导出格式 | Markdown 汇总报告、CSV 按指标分文件、JSON 原始记录 |
| 导出历史 | 记录每次导出的文件位置、条数、大小，可分享 / 删除 |
| 系统分享 | 通过系统分享把文件发给 Obsidian、网盘、微信等 |
| 自动导出 | 可开启「每天自动导出最近 7 天数据」（WorkManager，保留最近 30 份） |
| 今日概览 | 渐变问候卡 + 步数/消耗/睡眠/补水四色指标卡 + 最近 7 天步数趋势图（Jetpack Compose，浅色「春野」设计体系） |

支持导出的数据指标（对应 Health Connect 权限）：

- 步数、距离、总消耗卡路里、活动卡路里、爬楼层数、睡眠、体重、身高、运动记录、体脂率、补水量（普通权限，用户授权即可）
- 心率、静息心率（**受限权限**，见下方说明）

## 安装

1. 下载 `release/HealthMDExport-v3.0.0.apk` 传到手机上（微信文件传输 / 网盘 / 数据线均可）
2. 点击安装。若提示「未知来源」，在系统设置中允许安装该来源的应用
3. 打开应用 → 「连接」页确认 Health Connect 状态：
   - 提示**未安装** → 点击「去安装」跳转应用商店安装 Google Health Connect
   - 提示**需要更新** → 点击「去更新」
   - 提示**已就绪** → 点击「请求数据权限」，在 Health Connect 里勾选要授权的指标
4. 切到「导出」页，选时间范围、格式、指标，点「开始导出」
5. 完成后点「分享文件」，发送到 Obsidian / 网盘 / 电脑

### 说明

- 需要手机装有 **Google Health Connect**（Android 14+ 部分机型内置，其他机型需从应用商店安装）
- 导出文件保存在应用目录 `Android/data/com.healthmd.exporter/files/exports/` 下
- 自动导出保存在 `.../files/auto_exports/` 下，仅保留最近 30 份

### 受限权限（心率 / 静息心率）

心率、静息心率属于 Google Play **受限权限**，需要开发者在 Play 开发者控制台提交隐私政策
并申请批准后，Health Connect 才会允许授权。本应用为侧载安装（未上架），这两项会显示
「未授权」，其余 11 项指标不受影响。如需开通，需将应用上架 Google Play 并申请该权限。

## 对接 Obsidian

1. 在应用里导出「Markdown」格式
2. 点「分享文件」→ 选择 Obsidian 或保存到手机
3. 把 `health-summary.md` 放进你的 vault（如 `E:\羽毛的知识库`）

生成的 Markdown 带 YAML frontmatter（title / provider / start_date / end_date / generated_at），
可直接被 Obsidian 的属性和双链体系使用。

## 源码构建

```bash
# 环境要求：JDK 17、Android SDK（platform 36 + build-tools 36.0.0）
./gradlew assembleDebug          # 产物在 app/build/outputs/apk/debug/app-debug.apk
```

或用 Android Studio 直接打开本目录即可。

## 许可

本工程代码以 **AGPL-3.0** 发布（见 [LICENSE](LICENSE)）。
参考项目 health.md / healthmd-cli（AGPL-3.0，https://github.com/CodyBontecou/healthmd-cli）。

### 改动说明（相对参考项目）

- **平台与语言不同**：本项目为 Android 原生应用（Kotlin + Jetpack Compose）；参考项目为 Rust 命令行工具。未复制其任何代码，仅参考「把健康数据导出为 Markdown」的功能理念。
- **UI 为全新独立实现**：Material 3 浅色「春野」设计体系、四页面（概览/连接/导出/记录）均为重新设计。
- **导出引擎、Health Connect 权限管理、导出历史、自动导出**等均为独立实现。

分发本软件（含 APK）时，须同样以 AGPL-3.0 向接收者提供本仓库完整源码。仅供个人学习与本地使用。
