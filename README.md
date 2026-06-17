# Melodia 

Melodia 是一款基于 Ko[app](app)tlin + Jetpack Compose 构建的轻量级第三方网易云音乐客户端。
---

## 项目架构

本项目采用现代 Android 开发的最佳实践与分层架构设计（MVVM + Repository）：

```t[]()ext
app/src/main/java/com/lin0721/linmusic/
├── MelodiaApplication.kt           # 应用入口，初始化 Koin 依赖注入及 Coil 图片加载预热
├── MainActivity.kt                 # 单 Activity 容器，管理全局手势、侧边栏及全局浮动播放器
│
├── data/                           # 数据层
│   ├── remote/
│   │   ├── api/                    # Retrofit 接口定义及数据传输对象 (DTO)
│   │   ├── crypto/                 # 核心加密算法 (WeApi, EApi, LinuxApi 的 Kotlin 原生实现)
│   │   └── network/                # OkHttp 拦截器 (用于处理请求加密、设备特征指纹注入及异常处理)
│   └── repository/                 # Repository 实现，将网络 DTO 转换为 UI 消费的领域模型，并提供 Flow 数据流
│
├── di/                             # 依赖注入模块 (使用 Koin 进行轻量级依赖管理)
│
├── player/                         # 播放引擎层
│   ├── MelodiaPlaybackService.kt   # 基于 AndroidX Media3 (ExoPlayer) 的后台音频播放服务
│   └── PlayerManager.kt            # 播放控制器封装，向 UI 层暴露当前的播放状态、轨道信息及进度控制
│
└── ui/                             # 视图层 (100% Jetpack Compose)
    ├── home/                       # 首页模块 (今日推荐、历史日推、排行榜等聚合)
    ├── search/                     # 搜索与发现模块 (云搜索、热搜榜、精品标签)
    ├── library/                    # 音乐库模块 (用户歌单、收藏歌手/专辑的聚合与检索)
    ├── create/                     # 新建/导入歌单等快捷操作面板
    ├── playlist/                   # 歌单详情页 (支持沉浸式折叠及动态色彩提取背景)
    ├── player/                     # 全屏播放器页面 (含毛玻璃顶栏、歌词动效及鲜艳度优先色彩提取)
    ├── components/                 # 全局通用 UI 组件 (原生登录 WebView、侧边栏、底部导航等)
    └── theme/                      # Material 3 主题及配色方案
```

---

## 技术栈与核心选型

- **UI 框架**：Jetpack Compose (Material 3)
- **媒体引擎**：AndroidX Media3 (ExoPlayer + MediaSession)
- **依赖注入**：Koin
- **网络层**：Retrofit2 + OkHttp3 + kotlinx.serialization
- **异步与流式编程**：Kotlin Coroutines + Flow / StateFlow
- **图片加载**：Coil (配置低延迟图片解码与多级缓存)
- **持久化**：Jetpack DataStore & SharedPreferences

---

##  加密与安全路由

所有网络请求的加密与风控规避逻辑均使用 Kotlin 原生复刻，不依赖外部 Node.js 服务：
- **EApi 路由**：使用 MD5 + AES-ECB 加密，用于获取排行榜、收藏列表、用户歌单及搜索等接口，规避 PC 端风控。
- **WeApi 路由**：使用 AES-CBC + RSA 加密，用于获取历史日推及创作者信息等特定接口。
- **设备特征指纹**：自动在拦截器中注入设备特征指纹，保障接口调用的稳定性。

---

## ⚠️ 免责声明

1. 本项目为个人技术研究与 Android 原生架构练手项目。
2. 本项目仅供技术交流与学习使用，不提供任何形式的商业变现。
3. 软件内相关数据接口均来自互联网公开抓包分析，请在下载后 24 小时内删除，严禁用于任何商业用途。
