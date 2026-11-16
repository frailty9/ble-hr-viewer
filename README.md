# BLE Heart Rate Viewer

A simple BLE heart rate client with a real-time floating overlay.

一个简单的 Android 心率查看工具，通过 BLE 接收手环等设备的心率（Heart Rate，简称 HR），并支持在其他应用上方实时显示。仓库名为 `ble-hr-viewer`。

应用作为 GATT Notify 客户端，订阅标准 Heart Rate Service（`0x180D`）下的 Heart Rate Measurement（`0x2A37`）特征。

## 功能

- 扫描、连接 BLE 心率设备，显示实时 BPM、心率趋势和设备接触状态。
- 解析 RR 间期、能量消耗与原始通知包，显示异常数据及错误原因。
- 系统悬浮窗显示心率，可拖动、长按关闭，并可调整颜色和透明度。
- 悬浮窗开启时通过前台服务保持后台连接；主界面支持横竖屏并保持屏幕常亮。

需要 Android 8.0 及以上、支持 BLE 的手机，以及能够提供标准心率服务的设备。

## 使用

1. 在手环或其配套应用中开启心率广播。
2. 启动应用，授予蓝牙相关权限。Android 11 及以下还需要定位权限并开启系统定位服务。
3. 点击“扫描 BLE”，选择设备连接。设备旁的 `HR` 标记表示其广播包含标准心率服务。
4. 需要悬浮窗时，点击“授权并开启”，允许“显示在其他应用上层”后返回应用。
5. 拖动悬浮窗移动位置，长按约 0.6 秒关闭；也可通过通知关闭。单击悬浮窗不执行操作。

悬浮窗的颜色、透明度设置会保存在本机，并在运行时立即生效。

## 构建

| 项目 | 版本 |
| --- | --- |
| JDK | 17 或 21 |
| Gradle Wrapper | 8.13 |
| Android Gradle Plugin | 8.9.0 |
| Kotlin | 2.0.21 |
| Android SDK | compileSdk / targetSdk 35，minSdk 26 |
| 应用版本 | 0.3.4（versionCode 10） |

使用 Android Studio 打开项目并配置 Android SDK，或在本机的 `local.properties` 中设置 `sdk.dir`。该文件不提交到 Git。

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Linux / macOS：

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

若 Gradle 下载遇到证书错误，请检查本机 JDK 的证书与网络配置，或使用已安装的 Gradle 8.13。

## 检查与发布

Windows 使用以下命令；Linux / macOS 将 `.\gradlew.bat` 替换为 `./gradlew`：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

- 发布前核对 `app/build.gradle.kts` 中的 `versionCode`、`versionName` 和本文件的版本信息。
- Debug APK 用于本地测试。当前未配置 release 签名，`assembleRelease` 生成的是未签名 APK；正式分发前需使用维护者自己的签名密钥生成签名 APK。
- 签名密钥、密码和本机配置不入库。安装包及其 SHA-256 校验值作为发布附件提供，构建产物不提交到 Git。
- 在真机上检查设备扫描、心率订阅、断开连接，以及悬浮窗的授权、后台显示、拖动和长按关闭。

## 权限与数据

| 权限 | 用途 |
| --- | --- |
| 附近的设备（Android 12+） | 扫描、连接 BLE 设备 |
| 定位（Android 8–11） | 系统对 BLE 扫描的要求 |
| 显示在其他应用上层 | 显示心率悬浮窗 |
| 前台服务、通知 | 悬浮窗运行时保持设备连接并提供关闭入口 |

应用未声明网络权限。心率数据在进程内处理，外观设置通过 SharedPreferences 保存在本机。调试日志可能包含设备名称、地址和原始心率包，分享日志前请删除个人设备信息。

## 已知限制

- 仅接收心率，不向其他设备广播心率；设备需实现标准 `0x180D` / `0x2A37` 服务与特征。
- 断开连接后需手动重新连接。
- 某些设备的佩戴状态标志不可靠。当前收到 `0 BPM` 时显示“无脉搏信号”，具体行为取决于设备。
- 部分系统的省电策略会限制后台服务和悬浮窗。
- 关闭悬浮窗不会主动断开 BLE；需要断开时请在应用内操作。

## 许可证

项目原创代码采用 [MIT License](LICENSE)，Copyright (c) 2026 花殇。

第三方组件保留各自的许可证，见 [第三方声明](THIRD_PARTY_NOTICES.md)。
