# Melodia 开发日志

> 记录项目架构决策与技术细节。

---
# 注释规范
1. 保持代码精简，禁止 KDoc 风格多行注释。
2. 仅使用单行注释 (//)，仅在复杂逻辑处添加简短说明。

## 项目概览
- **包名**: `com.lin0721.linmusic`
- **SDK**: Min 26 / Target 36
- **核心**: Kotlin + Jetpack Compose + Material3
- **网络**: Retrofit + OkHttp + kotlinx.serialization
- **DI**: Koin 4.0
- **播放器**: Media3 ExoPlayer

---

## 项目结构
```text
app/src/main/java/com/lin0721/linmusic/
├── LinMusicApplication.kt          # Koin 初始化
├── MainActivity.kt                 # 入口容器
├── data/
│   ├── remote/
│   │   ├── api/                    # API 定义
│   │   ├── crypto/                 # 加密逻辑 (WeApi/EApi/LinuxApi)
│   │   └── network/                # 拦截器 (加密、Header、空包处理)
│   └── repository/                 # 数据层
├── di/                             # 依赖注入模块
├── player/
│   ├── LinMusicPlaybackService.kt    # Media3 服务
│   └── PlayerManager.kt              # 播放控制器
└── ui/
    ├── home/                       # 首页模块
    ├── player/                     # 播放器组件
    └── theme/                      # 主题配置
```

---

## 变更日志

### 2026-04-22 — 网易云加密与网络层基础
- **NeteaseCrypto.kt**: 实现 WeApi (AES-CBC + RSA), LinuxApi (AES-ECB), EApi (MD5 + AES-ECB) 加密逻辑。
- **CryptoInterceptor.kt**: OkHttp 拦截器，自动根据请求路径执行对应的加密路由。
- **NeteaseApiService.kt**: 定义 Retrofit 接口，包含登录、每日推荐等核心业务。
- **NetworkModule.kt**: 配置 Koin 网络单例注入链，设置 15s 超时。

### 2026-04-23 — 仓储层与首页加载
- **MusicRepository.kt**: 封装 Flow 数据流，处理网络拦截与序列化异常。
- **HomeViewModel.kt**: 采用 StateFlow 驱动 UI，实现首页数据自动拉取与错误重试。
- **EmptyBodyInterceptor.kt**: 探测空响应体并抛出 ApiException，增强系统鲁棒性。
- **HomeScreen.kt**: 基于 LazyVerticalGrid 实现推荐歌单展示，接入 Coil 图片加载。

### 2026-04-24 — 播放引擎集成 (Media3)
- **LinMusicPlaybackService.kt**: 实现后台持续播放服务，绑定 MediaSession。
- **PlayerManager.kt**: 封装 MediaController 通信，暴露当前轨道与播放状态流。
- **Song URL API**: 接入歌曲播放地址接口，处理版权限制及 VIP 歌曲拦截。
- **HeaderInterceptor.kt**: 注入动态 IP、UA 与 Cookie，解决地域风控及空响应体问题。
- **NeteaseCrypto 修复**: 修正 Android 平台 Base64 换行符及 RSA Padding 填充缺陷。

### 2026-04-25 — 视觉升级与协议深化
- **主题重构**: 锁定深色模式，使用 Spotify 风格配色。
- **MiniPlayer & FullPlayer**: 独立化播放器组件，实现悬浮条与全屏页面的转场动效。
- **协议调优**: 修正 EApi 参数校验逻辑，自动注入 9 项设备特征指纹负载。

### 2026-04-26 — 首页聚合与毛玻璃动效
- **聚合数据源**: 使用 async/await 并发加载推荐歌单与热门歌手，合并为单一 Success 状态。
- **Haze (Glassmorphism)**: 引入 Haze 库实现底部导航栏的真实背景模糊效果。
- **UI 润色**: 实现分时段动态问候语，优化 LazyColumn 底部内边距避免内容遮挡。

### 2026-04-27 — 持久化与进度恢复
- **PlaybackPreferences.kt**: 基于 DataStore 持久化歌曲元数据与播放进度。
- **状态恢复**: 应用启动时自动恢复上一次播放状态，支持断点续传。

### 2026-05-01 — 登录系统与侧边栏
- **UserPreferences.kt**: 存储用户信息及 Cookie。
- **LoginBottomSheet.kt**: 提供网易红风格的登录入口，包含网页及二维码登录选项。
- **ProfileSidebar.kt**: 重构个人中心侧边栏，包含菜单导航与退出登录逻辑。
- **ModalNavigationDrawer**: 集成侧边栏手势开关。

### 2026-05-02 — 2D 挤压布局重构
- **侧边栏动画**: 弃用覆盖式 Drawer，改为 Row 布局，侧边栏展开时主界面自动向右平移“挤压”。

### 2026-05-04 — 真实登录与手势系统
- **网页登录全集成**: 移除模拟登录，通过 WebView 完成真实授权并同步 Cookie。
- **AnchoredDraggable**: 升级手势引擎，实现侧边栏与主界面的“推拉”跟手位移。
- **API 现代化**: 修复构造函数废弃警告，迁移至 flingBehavior 驱动模式。
- **视觉优化**: 增加主页面平移时的动态圆角过渡（0dp -> 32dp）与边缘阴影。

---

## 待办事项
- [x] 初始化播放引擎 (Media3)
- [x] 全局播放控制器 (PlayerManager)
- [x] 歌曲播放链接 API 对接
- [x] 全屏播放器页面
- [x] 侧边栏推拉手势
- [x] 真实网页登录集成
- [ ] 搜索、歌曲详情接口
- [ ] 歌词解析与同步显示
- [ ] 统一错误处理分发
