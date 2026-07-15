# ~~用Codex瞎几把写的（）~~

# OpenWrt-MGR

轻量 Android OpenWrt 管理客户端（Jetpack Compose + Material You）。

Author: **by Ryobunoki using Codex**  
Version: **1.6.6** · Package: `com.openwrt.mgr`

## 功能

- LuCI / ubus 登录（默认网关 `192.168.10.1`）
- 系统概览：主机名、型号、版本、uptime、负载、内存
- 设备列表、无线接口、网口与上游网络信息
- 存储占用（挂载点 / 硬盘 / U 盘等）
- 插件扫描与常用操作
- 内置 SSH 终端（JSch slim + 等宽字体）
- Material You 主题、樱花粉默认色、纯色 / 自定义壁纸
- 明暗 / AMOLED 模式
- 多语言：跟随系统、简中、繁中、英语、日语、俄语、韩语
- 可选记住密码

## 环境要求

- JDK **17**
- Android SDK（`compileSdk 36`，`minSdk 26`）
- Windows / macOS / Linux 均可（Gradle Wrapper 已包含）

## 快速构建

1. 克隆或解压本仓库
2. 配置 SDK（任选其一）  
   - 复制 `local.properties.example` 为 `local.properties`，填写 `sdk.dir=...`  
   - 或设置环境变量 `ANDROID_HOME` / `ANDROID_SDK_ROOT`
3. 构建：

```bash
# Windows
gradlew.bat :app:assembleRelease

# macOS / Linux
./gradlew :app:assembleRelease
```

输出 APK：

```
app/build/outputs/apk/release/app-release.apk
```

Windows 也可使用 `do_build.bat`（会把 release APK 复制为项目根目录 `OpenWrt-MGR-release.apk`）。

### 调试构建

```bash
gradlew.bat :app:assembleDebug
```

## 默认连接

| 项 | 默认值 |
| --- | --- |
| Host | `192.168.10.1` |
| User | `root` |
| 协议 | LuCI / ubus JSON-RPC · SSH |

首次打开可在登录页修改主机、端口、账号密码；可勾选是否保存密码。

## 项目结构

```
OpenWrt-MGR/
├── app/
│   ├── build.gradle          # 应用模块、R8、slim JSch
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/openwrt/mgr/   # 业务代码
│       ├── java/com/jcraft/jsch/   # slim JSch 运行时 stub
│       └── res/                    # 资源、字体、多语言
├── gradle/wrapper/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── do_build.bat
├── LICENSE
└── README.md
```

## 技术说明

- **UI**: Jetpack Compose · Material 3
- **网络**: LuCI cookie / ubus session
- **SSH**: [mwiede/jsch](https://github.com/mwiede/jsch) 经构建任务裁剪为 `jsch-slim.jar`，缺失能力由 `com.jcraft.jsch` 包内 stub 补齐（端口转发等未实现功能会 no-op / 抛出明确异常）
- **发布优化**: R8 full mode、资源收缩、abi 仅 `arm64-v8a`
- **终端字体**: 内置 JetBrains Mono VF（`res/font`）

> 仓库中的 release 签名默认指向本机 debug keystore，方便直接编出可安装包。公开发布请替换为自己的签名配置。

## 许可

MIT License · Copyright (c) Ryobunoki

第三方依赖遵循其各自许可证（AndroidX、Kotlin、JSch 等）。

## 免责声明

本软件仅供在你有权管理的设备上使用。请勿用于未授权访问他人网络设备。SSH / 管理操作具有风险，重启、改密等请谨慎。
