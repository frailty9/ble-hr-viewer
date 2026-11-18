<h1 align="center">BLE Heart Rate Viewer</h1>

<p align="center">
  A simple BLE heart rate client with a real-time floating overlay.<br>
  连接心率设备，在悬浮窗中实时查看。
</p>

<p align="center">
  <a href="#构建"><img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&amp;logo=android&amp;logoColor=white" alt="Android 8.0+"></a>
  <a href="#构建"><img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&amp;logo=kotlin&amp;logoColor=white" alt="Kotlin 2.0.21"></a>
  <a href="#功能"><img src="https://img.shields.io/badge/Bluetooth-BLE-0082FC?style=flat-square&amp;logo=bluetooth&amp;logoColor=white" alt="Bluetooth Low Energy"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-D4A24C?style=flat-square" alt="License: MIT"></a>
</p>

<p align="center">
  <a href="#功能">功能</a> ·
  <a href="#使用">使用</a> ·
  <a href="#构建">构建与安装</a> ·
  <a href="#检查与发布">发布检查</a> ·
  <a href="#权限与数据">权限与数据</a> ·
  <a href="#合规与发布">合规与发布</a> ·
  <a href="#许可证">许可证</a>
</p>

---

一个简单的 Android 心率查看工具，通过 BLE 接收心率设备的数据，在主界面或其他应用上方实时显示心率（Heart Rate，简称 HR）。

项目正式名称为 **BLE Heart Rate Viewer**，安装后的应用名称、应用内标题和通知标题使用 **BLE HR Viewer**。

应用作为 GATT Notify 客户端，订阅标准 Heart Rate Service（`0x180D`）下的 Heart Rate Measurement（`0x2A37`）特征。

## 功能

- 扫描、连接 BLE 心率设备，显示实时 BPM、心率趋势和设备接触状态。
- 解析 RR 间期、能量消耗与原始通知包，显示异常数据及错误原因。
- 系统悬浮窗显示心率，可拖动、长按关闭，并可调整颜色和透明度。
- 悬浮窗开启时通过前台服务保持后台连接；主界面支持横竖屏并保持屏幕常亮。

需要 Android 8.0 及以上、支持 BLE 的手机，以及能够提供标准心率服务的设备。

界面支持中文和英文，默认跟随系统语言；其他语言使用英文。在应用的“设置”页面可选择跟随系统、中文或 English，立即生效并保存；切换时原位更新页面，保留导航和动画状态。Android 13 及以上也可通过系统的应用语言设置选择。

界面提供浅色、深色主题，默认跟随系统。设置中可手动切换，立即生效并在重启后保留；跟随系统时随系统深色模式自动更新。应用主题与悬浮窗的颜色、透明度各自独立。

页面圆角会参考设备屏幕圆角：Android 12 及以上取系统提供的最大半径，换算为 dp 后减去 14dp 页面边距，并限制在 12–28dp；无有效数据、旧系统及多窗口模式使用 20dp。卡片、设备行、按钮、状态标签和三段控件采用同一套比例层级，悬浮窗样式独立。主页保留左右留白，移除上下额外留白及滚动视窗的圆角裁切，内容可滚至屏幕底边；列表末尾预留系统导航安全区，双栏布局的两列分别滚动。

## 使用

1. 确认心率设备已启用心率广播。如需手动开启，请查看设备设置或配套应用。
2. 首次启动时阅读隐私提示，同意后进入主页。
3. 点击“扫描 BLE”。首次扫描时按系统提示授权，授权后自动开始扫描。Android 11 及以下需授予定位权限并开启系统定位服务。选择设备连接，设备旁的 `HR` 标记表示其广播包含标准心率服务。
4. 点击心率卡片右上角的窗口图标开启悬浮窗。若系统要求授权，允许“显示在其他应用上层”后返回应用。
5. 拖动悬浮窗移动位置。关闭时可点击心率卡片右上角的关闭图标、长按悬浮窗约 0.6 秒，或使用通知中的关闭操作。单击悬浮窗不执行操作。

应用会记住悬浮窗的开启状态。下次打开应用且权限仍有效时自动恢复。主动关闭后，下次启动保持关闭。

悬浮窗的颜色、透明度设置会保存在本机，并在运行时立即生效。默认使用深灰色背景（30% 不透明度）和预设粉红色字体（65% 不透明度）；升级保留已有设置，“恢复默认”应用这组样式。

主页保留实时心率、心率趋势和附近设备，内容居中且最大宽度为 1120dp。扣除安全区和左右留白后，可用宽度达到 680dp 时使用双栏，左侧趋势图随可用高度扩展。附近设备卡片填充剩余空间，结果较多时可向下滚动。

点击主界面的“设置”，可选择界面语言、主题，调整悬浮窗外观和蓝牙超时，查看原始包、错误包和使用提示，并在“关于与合规”中查看版本、隐私政策、用户协议、开源许可及撤回同意。设置内容居中且最大宽度为 640dp，标题与返回按钮不随内容滚动。设置无需授予蓝牙权限即可使用；悬浮窗的“恢复默认”只重置其外观。

## 构建

| 项目 | 版本 |
| --- | --- |
| JDK | 17 或 21 |
| Gradle Wrapper | 8.13 |
| Android Gradle Plugin | 8.9.0 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.09.02 |
| Navigation Compose | 2.8.9 |
| Lifecycle | 2.8.3 |
| Android SDK | compileSdk / targetSdk 35，minSdk 26 |
| 应用版本 | 1.0.0-SNAPSHOT（versionCode 21） |
| 应用包名 | `io.github.frailty9.blehr` |

1.0.0 起应用包名为 `io.github.frailty9.blehr`。0.4.x 使用 `com.example.blehr`，Android 会将 1.0.0 作为独立应用安装，旧版的本地设置和权限不会迁移。可以保留旧版，也可以在确认新版可用后卸载旧版。

使用 Android Studio 打开项目并配置 Android SDK，或在本机的 `local.properties` 中设置 `sdk.dir`。该文件不提交到 Git。

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r dist\ble-hr-viewer-1.0.0-SNAPSHOT-debug.apk
```

Linux / macOS：

```sh
./gradlew :app:assembleDebug
adb install -r dist/ble-hr-viewer-1.0.0-SNAPSHOT-debug.apk
```

构建完成后会自动将带产品名和版本号的 APK 导出到 `dist/`，版本号取自构建配置。

若 Gradle 下载遇到证书错误，请检查本机 JDK 的证书与网络配置，或使用已安装的 Gradle 8.13。

## 源码结构与维护

源码位于 `app/src/main/java/com/example/blehr/`，测试目录按相同包结构组织。

```text
MainActivity.kt       系统窗口、setContent
BleHrApp.kt           本地化资源、主题、应用根容器和回调组装
navigation/          类型安全路由、NavHost、250ms 进出动画
feature/home/        主页面布局、Route、ViewModel、权限引导
  components/        标题栏、BPM、趋势、设备、悬浮窗入口
feature/settings/    设置页布局、Route、ViewModel
  components/        语言、主题、悬浮窗外观、蓝牙超时、诊断、帮助
data/heartrate/      进程级 BLE 状态与心率数据流
data/settings/       主题与外观存储、语言适配、设置模型
ble/                 蓝牙扫描、GATT、心率协议解析与模型
overlay/             系统悬浮窗 View 和前台 Service
core/text/           延迟本地化文本与诊断映射
ui/                  跨页面组件与主题
```

| 想调整的内容 | 修改位置 |
| --- | --- |
| 设置页面区块、间距、返回栏 | `feature/settings/SettingsScreen.kt` |
| 语言三段控件 | `feature/settings/components/LanguageSelector.kt` |
| 主题三段控件、浅色和深色配色 | `feature/settings/components/ThemeSelector.kt`、`ui/theme/Theme.kt` |
| 三段控件的动画和尺寸 | `ui/components/AnimatedSegmentedSelector.kt` |
| 设备圆角读取、计算和各组件圆角层级 | `ui/theme/Shapes.kt` |
| 外观预览、颜色选项、透明度控件 | `feature/settings/components/OverlayAppearanceCard.kt` |
| 设置默认值、保存方式 | `data/settings/` |
| 主页面布局、各卡片 | `feature/home/HomeScreen.kt`、`feature/home/components/` |
| 权限检查、打开系统页面、开启悬浮窗 | `feature/home/HomeRoute.kt` |
| 页面跳转与动画 | `navigation/BleHrNavHost.kt` |
| 悬浮窗尺寸、三位数字布局与拖动手势 | `overlay/HeartRateOverlayView.kt` |

`Route` 收集状态并执行系统操作，`Screen` 和组件只接收状态与回调。主页面 ViewModel 属于 Activity，心率 Repository 属于应用进程；导航或语言重建不会主动断开 BLE。

## 检查与发布

Windows 使用以下命令；Linux / macOS 将 `.\gradlew.bat` 替换为 `./gradlew`：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

真机连接后执行设置、主题与导航测试（不启动模拟器）：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

- 发布前核对 `app/build.gradle.kts` 中的 `versionCode`、`versionName` 和本文件的版本信息。
- 发布前对照[发布检查清单](docs/compliance/RELEASE_CHECKLIST.md)逐项确认。
- Debug APK 用于本地测试，导出为 `ble-hr-viewer-1.0.0-SNAPSHOT-debug.apk`。
- 安装包及其 SHA-256 校验值作为发布附件提供，构建产物不提交到 Git。
- 在真机上检查首次启动隐私提示、同意与撤回流程、设备扫描、心率订阅、断开连接，以及悬浮窗的授权、后台显示、拖动和长按关闭。

### 配置 Release 签名

将根目录的 [`keystore.properties.example`](keystore.properties.example) 复制为 `keystore.properties`，以 UTF-8 保存。填入 `storeFile`（密钥库路径）、`storePassword`（密钥库密码）、`keyAlias`（密钥别名）和 `keyPassword`（密钥密码，省略时使用 `storePassword`）。相对路径以项目根目录为准。Windows 路径可写成 `D:/Keys/ble-hr-viewer-release.jks`，文件内容中的字面反斜杠要写成 `\\`。

配置完成后执行 `.\gradlew.bat :app:assembleRelease`，签名 APK 会导出为 `dist/ble-hr-viewer-1.0.0-SNAPSHOT.apk`。如果没有 `keystore.properties`，该命令仍可构建，导出的文件带有 `-unsigned.apk` 后缀。使用 Android SDK Build Tools 中的 `apksigner` 验证签名并查看证书指纹：

```powershell
apksigner verify --print-certs dist/ble-hr-viewer-1.0.0-SNAPSHOT.apk
```

`keystore.properties` 和密钥库只保存在本机。Git 已忽略该配置文件以及 `.jks`、`.keystore` 等常用密钥库格式。

## 合规与发布

- 隐私政策见 [PRIVACY.md](PRIVACY.md)，用户协议见 [AGREEMENT.md](AGREEMENT.md)。构建会从这两个文件的「中文」和「English」章节生成随 APK 打包的离线正文。修改条款时请直接编辑这两个文件。
- 隐私政策公网地址（用于分发页或后续商店）：<https://github.com/frailty9/ble-hr-viewer/blob/main/PRIVACY.md>
- 应用首次启动展示隐私提示，同意后才请求权限；设置页可查看法律文本并撤回同意。
- 隐私政策有重大变更时，在 `PRIVACY.md` 源码顶部的 `consent-version` 注释中递增整数版本。新版会重新征求旧版用户的同意。校对和排版调整沿用原整数版本。
- 在中国境内分发前，按 [APP 备案指引](docs/compliance/APP_FILING.md)办理备案。取得备案号后写入 `gradle.properties`：

  ```properties
  app.filingNumber=京ICP备XXXXXXXX号-XXA
  ```

  重新构建后，备案号会显示在“设置 → 关于与合规”。
- 发布流程、签名校验、权限自查与发布说明模板见[发布检查清单](docs/compliance/RELEASE_CHECKLIST.md)。
- 本应用不是医疗器械，心率数据仅供参考，不得用于诊断或治疗。

## 权限与数据

| 权限 | 用途 |
| --- | --- |
| 附近的设备（Android 12+） | 扫描、连接 BLE 设备 |
| 定位（Android 8–11） | 系统对 BLE 扫描的要求 |
| 显示在其他应用上层 | 显示心率悬浮窗 |
| 前台服务、通知 | 开启悬浮窗时请求；保持设备连接并提供关闭入口 |

应用未声明网络权限，也未集成广告、统计或第三方数据收集 SDK。心率数据在进程内处理，主题、外观设置及最近成功连接的蓝牙设备信息保存在应用的 SharedPreferences 中；系统云备份与设备迁移已关闭。Release 版本不输出设备地址和心率原始数据日志；Debug 日志可能包含这些信息，分享前请删除个人设备信息。首次启动需同意[隐私政策](PRIVACY.md)与[用户协议](AGREEMENT.md)，可在“设置 → 关于与合规”查看或撤回。

## 已知限制

- 仅接收心率，不向其他设备广播心率；设备需实现标准 `0x180D` / `0x2A37` 服务与特征。
- 应用冷启动时，如果蓝牙已开启且权限已授予，会尝试连接最近一次成功订阅心率的设备，最多等待 10 秒。点击扫描或断开可取消本次尝试，运行中掉线后可手动重连。
- 系统确认断开时，主界面和悬浮窗立即清除实时心率。若连接仍在但收不到有效心率，默认 5 秒后清除读数并直连原设备，最多尝试 3 次。「设置 → 蓝牙连接」可按整秒调节超时（3–30 秒）。
- 某些设备报告的接触状态可能不准确。收到 `0 BPM` 时，应用显示“无脉搏信号”。
- 部分系统的省电策略会限制后台服务和悬浮窗。
- 关闭悬浮窗不会主动断开 BLE；需要断开时请在应用内操作。

## 许可证

项目原创代码采用 [MIT License](LICENSE)，Copyright (c) 2026 花殇。

第三方组件保留各自的许可证，见 [第三方声明](THIRD_PARTY_NOTICES.md)。

隐私与使用条款见[隐私政策](PRIVACY.md)和[用户协议](AGREEMENT.md)。
