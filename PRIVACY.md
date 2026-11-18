# BLE HR Viewer 隐私政策 / Privacy Policy

<!-- consent-version: 1 -->

应用包名 / Package: `io.github.frailty9.blehr` · 联系邮箱 / Contact: `ydl9263@qq.com`

应用内同一文本位于「设置 → 关于与合规 → 隐私政策」，按应用语言显示中文或英文。

---

## 中文

生效日期：2026 年 11 月 18 日 · 版本：1.0

### 一、适用范围与处理者

本政策适用于 Android 应用 BLE HR Viewer（包名 `io.github.frailty9.blehr`，以下简称“本应用”）。处理者为应用开发者“花殇”，联系方式：`ydl9263@qq.com`。

### 二、我们处理哪些信息

本应用仅在本机处理以下信息：

1. **蓝牙设备信息**：设备名称、蓝牙地址（MAC）及地址类型。用于显示扫描结果、建立连接，以及在下次启动时重连最近一次成功订阅心率数据的设备。
2. **心率数据**：心率数值（BPM）、RR 间期、能量消耗、传感器接触状态、原始通知包及解析诊断信息。用于在界面和悬浮窗中显示实时心率、趋势与诊断信息。
3. **应用设置**：界面语言、主题、悬浮窗颜色与透明度、心率数据超时时间、悬浮窗开关状态。

本应用不要求注册账号，不收集姓名、手机号、位置、通讯录、短信、照片、IMEI、Android ID、广告标识符等信息，也不读取剪贴板或已安装应用列表。

### 三、处理目的与合法性基础

我们基于你的同意处理上述信息，仅用于实现蓝牙心率设备连接、实时显示心率、悬浮窗展示、设备重连和保存界面偏好。心率数据属于敏感个人信息，仅在本机内存中处理，不会上传。对于心率这一敏感个人信息，我们在首次启动时通过单独的勾选项征得你的同意。

### 四、权限及用途

| 权限 | 用途 |
| --- | --- |
| 附近的设备（Android 12+） | 扫描并连接蓝牙心率设备 |
| 定位（Android 8–11） | 仅因系统要求蓝牙扫描所需；不读取实际位置 |
| 显示在其他应用上层 | 显示心率悬浮窗 |
| 前台服务与通知 | 悬浮窗开启时保持蓝牙连接，并提供关闭入口 |

权限均在你使用相应功能时请求。拒绝授权不会影响其他功能，但相关功能将无法使用。

### 五、存储位置与期限

上述信息保存在应用私有目录（SharedPreferences）或内存中，不会上传到服务器。应用会保存最近一次成功订阅心率通知的设备信息，用于下次启动时尝试重连。主动断开连接后仍会保留这条记录。成功订阅其他设备后会替换这条记录，撤回同意或卸载应用时会删除它。心率读数仅用于当前会话展示，不写入持久化存储。应用未启用系统云备份与设备迁移备份。

### 六、共享、转让与公开披露

本应用未声明网络权限，不含广告、统计或第三方数据收集 SDK，不会向任何第三方共享、转让或公开披露你的信息。应用使用的 AndroidX、Kotlin 等开源组件仅在本机运行，不收集数据。

### 七、你的权利

你可以在应用内「设置 → 关于与合规」查看本政策，并通过「撤回同意并退出」撤回同意；撤回后应用会清除保存的蓝牙设备信息并退出。你也可以随时卸载应用，卸载即删除全部本地数据。如需查阅、更正或删除信息，或对本政策有疑问，请联系 `ydl9263@qq.com`。

### 八、未成年人保护

本应用不面向 14 周岁以下儿童单独提供服务。若你是未成年人，请在监护人同意和指导下使用。

### 九、安全措施

数据仅保存在应用私有目录，应用不申请网络权限，Release 版本不输出设备地址和心率原始数据日志。请妥善保管设备，避免他人在已解锁设备上查看你的心率信息。

### 十、医疗免责

本应用仅用于查看心率设备输出的数据，不是医疗器械，不能用于疾病诊断、治疗或健康监测决策。如有健康问题，请咨询专业医疗机构。

### 十一、政策更新

如本政策发生重大变更，我们会在应用内提示并重新征得你的同意。更新后的政策自标注的生效日期起适用。应用备案信息可在「设置 → 关于与合规」中查看。

---

## English

Effective date: November 18, 2026 · Version: 1.0

### 1. Scope and controller

This policy applies to the Android app BLE HR Viewer (package `io.github.frailty9.blehr`, the "App"). The controller is the individual developer "花殇". Contact: `ydl9263@qq.com`.

### 2. Information we process

The App processes the following information only on this device:

1. **Bluetooth device details**: device name, Bluetooth address (MAC), and address type. Used to list scan results, connect, and reconnect to the last device that successfully subscribed to heart rate data.
2. **Heart rate data**: heart rate in BPM, RR intervals, energy expended, sensor contact status, raw notification packets, and parsing diagnostics. Used to show live heart rate, trends, and diagnostics on screen and in the floating overlay.
3. **App settings**: interface language, theme, overlay colors and opacity, heart rate timeout, and overlay enabled state.

The App does not require an account and does not collect your name, phone number, location, contacts, messages, photos, IMEI, Android ID, or advertising identifiers. It does not read the clipboard or the list of installed apps.

### 3. Purpose and legal basis

We process the information above based on your consent, only to connect to Bluetooth heart rate devices, show live heart rate, display the floating overlay, reconnect to devices, and save interface preferences. Heart rate is sensitive personal information and is processed in memory on this device only. It is not uploaded. We obtain your separate consent for heart rate through a dedicated checkbox on first launch.

### 4. Permissions and purposes

| Permission | Purpose |
| --- | --- |
| Nearby devices (Android 12+) | Scan for and connect to Bluetooth heart rate devices |
| Location (Android 8–11) | Required by the system for Bluetooth scanning only; the actual location is never read |
| Display over other apps | Show the heart rate floating overlay |
| Foreground service and notifications | Keep the Bluetooth connection active while the overlay is shown and provide a way to close it |

Permissions are requested only when you use the related feature. Denying a permission does not affect other features, but the related feature will not be available.

### 5. Storage and retention

The information above is stored in the App's private storage (SharedPreferences) or in memory and is never uploaded to a server. The App remembers the last device it successfully subscribed to for heart rate notifications so it can try reconnecting the next time it starts. The record is kept even if you disconnect. A successful subscription to another device replaces the record. Withdrawing consent or uninstalling the App deletes it. Heart rate readings are shown for the current session and are not written to persistent storage. System cloud backup and device-to-device transfer are disabled.

### 6. Sharing, transfer, and disclosure

The App does not declare the network permission and contains no advertising, analytics, or third-party data collection SDKs. It does not share, transfer, or disclose your information to any third party. Open source components such as AndroidX and Kotlin run locally and do not collect data.

### 7. Your rights

You can read this policy in the App under Settings → About and compliance, and withdraw consent with "Withdraw consent and exit". Withdrawal clears the saved Bluetooth device and closes the App. You can also uninstall the App at any time; uninstalling deletes all local data. To access, correct, or delete information, or to ask questions about this policy, contact `ydl9263@qq.com`.

### 8. Children

The App is not offered to children under 14 on their own. If you are a minor, use the App with the consent and guidance of a guardian.

### 9. Security

Data is kept in the App's private storage, the App has no network permission, and release builds do not log device addresses or raw heart rate data. Keep your device secure to prevent others from viewing your heart rate information on an unlocked device.

### 10. Medical disclaimer

The App only displays data reported by heart rate devices. It is not a medical device and must not be used for diagnosis, treatment, or health monitoring decisions. Consult a qualified medical professional for health concerns.

### 11. Changes to this policy

If this policy changes materially, we will notify you in the App and ask for your consent again. The updated policy applies from the effective date shown above. App filing information, when available, is shown under Settings → About and compliance.
