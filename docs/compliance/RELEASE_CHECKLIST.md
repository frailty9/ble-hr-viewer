# 发布检查清单（直发 APK）

适用于 1.0.0 起的正式版本。每次对外提供安装包前逐项确认；不适用的项目注明原因。

## 1. 版本与标识

- [ ] `app/build.gradle.kts` 的 `versionCode` 单调递增，`versionName` 与本次发布一致。
- [ ] `applicationId` 为 `io.github.frailty9.blehr`，未在发布后变更。
- [ ] `README.md` 的版本表与构建命令中的 APK 文件名已同步。
- [ ] 发布分支为 `1.0.0`，正式版本提交后打 tag `v1.0.0`。
- [ ] 确认从 0.4.x 切换的说明。1.0.0 会作为独立应用安装，可与旧版共存，旧版的本地设置和权限不会迁移。

## 2. 签名与构建

- [ ] 发布机存在 `keystore.properties`，密钥库已离线备份，且密码保存在密码管理器中。
- [ ] 确认 `keystore.properties`、`*.jks`、`*.keystore` 未被 Git 跟踪。
- [ ] 构建：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

- [ ] 验证签名与证书指纹：

```powershell
apksigner verify --print-certs dist\ble-hr-viewer-1.0.0.apk
```

- [ ] 记录证书 SHA-256 指纹；后续版本必须使用同一签名证书。
- [ ] 计算安装包 SHA-256 并随发布说明提供：

```powershell
Get-FileHash -Algorithm SHA256 dist\ble-hr-viewer-1.0.0.apk
```

- [ ] 核对 APK 标识与权限：

```powershell
aapt dump badging dist\ble-hr-viewer-1.0.0.apk
aapt dump permissions dist\ble-hr-viewer-1.0.0.apk
```

预期：包名 `io.github.frailty9.blehr`；不包含 `android.permission.INTERNET`。

## 3. 首次启动与隐私合规

- [ ] 全新安装（清除应用数据）后，启动只显示隐私提示，不弹系统权限框、不扫描蓝牙、不显示悬浮窗。
- [ ] 隐私提示中「隐私政策」「用户协议」可打开，中英文内容可读。
- [ ] 点击「不同意并退出」后应用关闭；再次启动仍显示提示。
- [ ] 点击「同意并继续」后才进入主界面。
- [ ] 主界面在授予蓝牙权限前显示权限说明；只有点击「授予权限」才出现系统权限框。
- [ ] 开启悬浮窗时才请求通知权限（Android 13+）；拒绝通知不影响主界面。
- [ ] 「设置 → 关于与合规」显示版本号、联系邮箱、隐私政策、用户协议、开源许可；备案号配置后显示。
- [ ] 「撤回同意并退出」会清除最近设备信息、关闭悬浮窗服务并退出；再次启动重新显示隐私提示。
- [ ] Release 包运行时 logcat 不输出蓝牙地址、设备名称或心率原始数据。
- [ ] 锁屏/后台/断连场景下，应用不申请或使用与隐私政策不一致的权限。

## 4. 权限与数据一致性

- [ ] 清单权限与隐私政策表格一致：附近的设备、定位（Android 8–11）、显示在其他应用上层、前台服务、通知。
- [ ] 应用无网络权限，无广告、统计或第三方数据收集 SDK。
- [ ] `allowBackup=false`、`dataExtractionRules` 排除本地数据（可用 `aapt dump xmltree` 检查）。
- [ ] 心率数据仅在本机处理；最近设备信息仅存于应用私有目录。

## 5. 分发材料

- [ ] APK：`dist/ble-hr-viewer-1.0.0.apk`
- [ ] SHA-256 校验文件或发布说明中的校验值
- [ ] [PRIVACY.md](../../PRIVACY.md)、[AGREEMENT.md](../../AGREEMENT.md)、[THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md)
- [ ] GitHub Release 说明包含：版本号、变更摘要、已知限制、非医疗器械声明、安装与签名校验方法
- [ ] 如已取得 APP 备案号，发布说明中同时展示

## 6. 发布说明模板

```markdown
## BLE HR Viewer 1.0.0

首个正式版本。

- 包名：io.github.frailty9.blehr（与 0.4.x 分别安装，旧版本地设置不会迁移）
- 版本：1.0.0（versionCode 21）
- 安装包：ble-hr-viewer-1.0.0.apk
- SHA-256：<填写>
- 签名证书 SHA-256：<填写>

隐私政策：https://github.com/frailty9/ble-hr-viewer/blob/main/PRIVACY.md
用户协议：https://github.com/frailty9/ble-hr-viewer/blob/main/AGREEMENT.md

本应用不是医疗器械，心率数据仅供参考，不得用于诊断或治疗。
```

## 7. 后续事项

- [ ] 在中国境内提供分发前，按 [APP 备案指引](APP_FILING.md)完成备案。
- [ ] 若上架应用商店，补充商店要求的隐私政策 URL、备案号、软著、截图与隐私合规检测。
- [ ] 每次修改权限、数据用途或第三方组件时，同步更新隐私政策，并在必要时提升同意版本号重新征得同意。
