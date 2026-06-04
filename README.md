# Melodia 🎵

> 一款基于 Kotlin + Jetpack Compose 构建的极简、轻量级第三方音乐客户端。

## 📖 项目简介

Melodia 致力于提供纯粹的音乐聆听体验。剥离了繁杂的社交功能和商业推广，让音乐播放器回归本质。
## ✨ 核心特性（规划中）

- **纯粹极简**：100% Jetpack Compose 构建，抛弃沉重的旧版 UI 系统，界面干净无广。
- **现代内核**：基于 AndroidX Media3 (ExoPlayer) 打造，播放稳定，切歌丝滑。
- **性能优先**：摒弃繁重的跨平台框架或 Node.js 本地服务，网络与加密逻辑全量 Kotlin 原生实现。
- **高扩展性**：清晰的架构分层，未来可轻松扩展本地播放或多音源插件。

## 🛠 技术栈选型

本项目严格遵循现代化 Android 开发最佳实践：

- **开发语言**: Kotlin
- **UI 框架**: Jetpack Compose (Material 3)
- **架构模式**: MVVM + Repository (单 Activity 架构)
- **依赖注入**: Koin (轻量级，极速启动)
- **网络与解析**: Retrofit2 + OkHttp3 + kotlinx.serialization
- **异步编程**: Kotlin Coroutines + Flow / StateFlow
- **媒体引擎**: AndroidX Media3
- **图片加载**: Coil

## 🚀 架构规划与开发进度

- [x] **Phase 0**: 仓库初始化与技术栈定型
- [ ] **Phase 1**: 基础网络层与核心加密算法 (Weapi/Eapi 等) Kotlin 原生复刻
- [ ] **Phase 2**: 数据流层搭建 (Data/Repository)，实现基础登录与接口打通
- [ ] **Phase 3**: Media3 播放引擎封装 (Service & Controller)
- [ ] **Phase 4**: 核心 UI 搭建 (播放页、歌单页、首页瀑布流)
- [ ] **Phase 5**: 性能剖析、内存泄漏排查与开源发布准备

## ⚠️ 免责声明

1. 本项目为个人业余时间利用 AI 辅助开发的 Android 原生技术架构练手项目。
2. 本项目仅供技术交流与学习使用，不提供任何形式的商业变现。
3. 软件内相关数据接口均来自互联网公开抓包分析，请使用者在 24 小时内删除，**严禁用于任何商业用途**，否则由此引发的法律纠纷由使用者自行承担。
