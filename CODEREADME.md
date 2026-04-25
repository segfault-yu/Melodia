# LinMusic 开发日志

> 本文件记录每次代码变更的详细日志，方便中继开发时快速了解项目进展和架构决策。

---

## 项目概览

- **包名**: `com.lin0721.linmusic`
- **最低 SDK**: 26 (Android 8.0)
- **目标 SDK**: 36
- **语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material3
- **网络层**: OkHttp + Retrofit + kotlinx.serialization
- **依赖注入**: Koin 4.0
- **加密**: 自实现网易云 WeApi / LinuxApi / EApi

---

## 当前项目结构

```text
app/src/main/java/com/lin0721/linmusic/
├── LinMusicApplication.kt          # 自定义 Application，初始化 Koin
├── MainActivity.kt                 # 主 Activity，全屏播放器容器
├── data/
│   ├── remote/
│   │   ├── api/                    # NeteaseApiService.kt
│   │   ├── crypto/                 # NeteaseCrypto.kt
│   │   └── network/                # 拦截器 (Crypto, Header, EmptyBody)
│   └── repository/                 # MusicRepository.kt
├── di/                             # Koin 模块定义
├── player/
│   ├── LinMusicPlaybackService.kt    # Media3 播放服务
│   └── PlayerManager.kt              # 播放控制器封装
└── ui/
    ├── home/                       # 首页模块
    ├── player/                     # PlayerComponents.kt (Mini & Full)
    └── theme/                      # Spotify 暗黑主题
```


---

## 变更日志

### 2026-04-22 Session 1 — 网易云加密工具
#### `[NEW] data/remote/crypto/NeteaseCrypto.kt`
- 参考 SPlayer 安卓版本实现网易云音乐加密逻辑
- **WeApi 加密** (`weapi()`): AES-CBC 两轮加密 + RSA 加密 secretKey → 输出 `params` + `encSecKey`
- **LinuxApi 加密** (`linuxapi()`): AES-ECB 加密 → 输出 `eparams` (大写 Hex)
- **EApi 加密** (`eapi()`): MD5 签名 + AES-ECB 加密 → 输出 `params` (大写 Hex)

---

### 2026-04-22 Session 2 — 网络层基础架构
#### `[NEW] data/remote/network/CryptoInterceptor.kt`
- OkHttp `Interceptor` 实现，拦截所有带 body 的 POST 请求
- 根据 URL 特征(`/eapi/`, `/linux/api/`, `/weapi/` 等) 进行路由派发和自动加密组装原始 JSON。

#### `[NEW] data/remote/api/NeteaseApiService.kt`
- Retrofit 接口定义:
  - 手机/邮箱登录 (`POST /weapi/login...`)
  - 每日推荐 (`POST /weapi/v1/discovery/recommend/resource`)
- 基于 Kotlinx.serialization 的数据解析类(`@Serializable`)，支持 `EmptyBody` 数据结构下发。

#### `[NEW] di/NetworkModule.kt`
- Koin `module` 单例注入链路：`Json → CryptoInterceptor → OkHttpClient → Retrofit → NeteaseApiService`
- `OkHttpClient` 配置超时为 15s 并引入 `HttpLoggingInterceptor`。

#### `[NEW] LinMusicApplication.kt` & `[MODIFY] 配置项`
- 创建 `Application` 类并初始化 Koin。
- `AndroidManifest.xml` 中注册网络权限并配置流量通信。
- 自动化 Gradle 配置: 导入 Retrofit、OkHttp、Kotlinx Serialization 以及 Koin 对应拓展，并启用 `kotlin-serialization` 编译插件。

---

### 2026-04-22 Session 3 — 本地单元测试与漏洞修复
#### `[MODIFY] NeteaseCrypto.kt` & `CryptoInterceptor.kt`
- **BUG 修复**: 移除了针对任意 Map 的隐式拆解，由于 `kotlinx.serialization` 的反射机制缺陷，旧解法会在 `Any?` 下报 SerializationException 崩溃。
- 将架构精简为直接透传解析好的原生 JSON `String` 给加密接口，免去了重复解包序列化的沉冗开销。

#### `[NEW] app/src/test/java/.../NeteaseApiTest.kt`
- 添加了脱机本地网络单元测试。绕开 App 引擎单独调用网易后台，验证了网易云服务在收到我们的加密包后没有抛出拦截 400 错误，而是正常返回了 `200 OK`，证实了算法闭环。

---

### 2026-04-22 Session 4 — 数据仓库层 (Repository)
#### `[NEW] data/repository/MusicRepository.kt` & `MusicRepositoryImpl.kt`
- 创建了 Repository 接口规范及其实现类，构造注入 `NeteaseApiService`。
- 将业务接口返回封装为 Kotlin 响应式并发冷流 `Flow<Result<RecommendPlaylistData>>`。
- 利用 `flow {}` 和 `catch {}` 块妥善处理网关拦截和 JSON 突发性反序列化 EOF 异常，为上游 ViewModel 提供极度安全的调用载体。

#### `[NEW] di/RepositoryModule.kt` & `[MODIFY] LinMusicApplication.kt`
- 使用 Koin 的 `singleOf` + `bind<MusicRepository>()` 注入。
- 在 `LinMusicApplication.kt` 的 `modules()` 初始化阵列中追加装载了 `repositoryModule`。

---

### 2026-04-23 Session 5 — ViewModel 层 (Phase 3)
#### `[NEW] ui/home/HomeUiState.kt`
- 定义 `HomeUiState` 密封接口，包含三种状态：
  - `Loading`：加载中占位
  - `Success(val data: RecommendPlaylistData)`：数据加载成功
  - `Error(val message: String)`：请求失败，携带错误消息

#### `[NEW] ui/home/HomeViewModel.kt`
- 构造函数注入 `MusicRepository`，继承 `ViewModel()`。
- 通过 `MutableStateFlow<HomeUiState>` 暴露只读 `StateFlow` 给 UI 层。
- `init {}` 块调用 `loadDailyRecommend()`，在 `viewModelScope` 中收集 Repository 的 `Flow<Result>` 并映射为对应 `HomeUiState`。
- `loadDailyRecommend()` 也可在 UI 层手动调用以实现下拉刷新等场景。

#### `[NEW] di/ViewModelModule.kt`
- 使用 Koin 的 `viewModelOf(::HomeViewModel)` 注册 ViewModel，自动解析构造参数。

#### `[MODIFY] LinMusicApplication.kt`
- 在 `modules()` 阵列中追加 `viewModelModule`。
- 整理 import，将 `repositoryModule` 由全限定名改为顶层 import。

#### `[MODIFY] gradle/libs.versions.toml` & `app/build.gradle.kts`
- 新增 `androidx-lifecycle-viewmodel-compose` 依赖（复用 `lifecycleRuntimeKtx` 版本），为 ViewModel + `viewModelScope` 提供编译支持。

### 2026-04-23 Session 6 — UI 层首页 (Phase 3 续)
#### `[NEW] ui/home/HomeScreen.kt`
- `HomeScreen()` Composable，通过 `koinViewModel()` 获取 `HomeViewModel`，使用 `collectAsStateWithLifecycle()` 收集状态。
- 根据 `HomeUiState` 分支渲染：
  - **Loading**：居中 `CircularProgressIndicator` + 提示文案
  - **Error**：Emoji + 错误文案 + 「重试」按钮（调用 `viewModel.loadDailyRecommend()`）
  - **Success**：`LazyVerticalGrid(2列)` 展示歌单卡片，空列表时显示「暂无推荐歌单」
- 独立抽取 `PlaylistItemCard()` 组件：
  - Coil `AsyncImage` 加载封面图 + `RoundedCornerShape` 圆角裁剪
  - 歌单名称（最多两行 ellipsis）+ 格式化播放次数 + 曲目数
  - `formatPlayCount()` 工具函数自动转换为「x.x万/亿次播放」

#### `[MODIFY] MainActivity.kt`
- 移除原有 `Greeting` 占位，直接调用 `HomeScreen()` 作为入口页面。

#### `[MODIFY] gradle/libs.versions.toml` & `app/build.gradle.kts`
- 新增 **Coil 2.7.0** (`coil-compose`) 图片加载库。
- 新增 `androidx-lifecycle-runtime-compose` 依赖，提供 `collectAsStateWithLifecycle()` 支持。

---

### 2026-04-23 Session 7 — 首页数据源切换为免登录公开接口
#### 背景
真机调试时 `getDailyRecommendPlaylists` 因未登录导致网易云返回空 body，`kotlinx.serialization` 抛出 EOF 异常。为测试 UI 将首页数据源切换为免登录的 `/weapi/personalized` 公开接口。

#### `[MODIFY] data/remote/api/NeteaseApiService.kt`
- 新增 `getPersonalizedPlaylists()` 接口 (`@POST("/weapi/personalized")`)，返回 `PersonalizedResponse`。
- 新增数据类 `PersonalizedResponse`（`code` + `result` 列表）、`PersonalizedData`（`playlists` 列表）、`PersonalizedPlaylist`（`id`, `name`, `picUrl`）。
- 保留原有 `getDailyRecommendPlaylists` 接口供后续登录功能使用。

#### `[MODIFY] data/repository/MusicRepository.kt` & `MusicRepositoryImpl.kt`
- 接口方法由 `getDailyRecommendPlaylists()` 改为 `getPersonalizedPlaylists()`，返回 `Flow<Result<PersonalizedData>>`。
- 实现层调用 `apiService.getPersonalizedPlaylists()` 并将 `response.result` 映射到 `PersonalizedData`。

#### `[MODIFY] ui/home/HomeUiState.kt`
- `Success` 状态泛型由 `RecommendPlaylistData` 改为 `PersonalizedData`。

#### `[MODIFY] ui/home/HomeViewModel.kt`
- 方法名由 `loadDailyRecommend()` 改为 `loadPersonalizedPlaylists()`，调用新的 Repository 方法。

#### `[MODIFY] ui/home/HomeScreen.kt`
- 数据类型由 `RecommendPlaylist` 改为 `PersonalizedPlaylist`。
- `SuccessContent` 读取 `state.data.playlists` 而非 `state.data.recommend`。
- `AsyncImage` 加载图片追加 `?param=300y300` 网易云 CDN 图片压缩后缀，优化内存占用。
- 移除 `playcount` / `trackCount` 展示（新数据类不含这些字段），TopAppBar 标题改为"推荐歌单"。

---

### 2026-04-23 Session 8 — 增强网络层容错能力（处理空响应体）
#### 背景
当服务端返回 200 OK 但响应体为空时，直接传给 `kotlinx.serialization` 会导致反序列化崩溃（抛出 `EOFException`）。为增强 App 鲁棒性，在网络层加入统一拦截，避免 Crash。

#### `[NEW] data/remote/network/ApiException.kt`
- 自定义异常 `ApiException` 继承自 `IOException`。OkHttp 拦截器抛出 `IOException` 子类时可以被 Retrofit 安全捕获并向外传递。

#### `[NEW] data/remote/network/EmptyBodyInterceptor.kt`
- 新增 `EmptyBodyInterceptor` 拦截器。
- 利用 `response.peekBody(Long.MAX_VALUE).string().isBlank()` 不消费数据流的前提下安全探测响应体内容。
- 遇空直接抛出 `ApiException("API body is empty, possibly auth failed")`。

#### `[MODIFY] di/NetworkModule.kt`
- 将 `EmptyBodyInterceptor` 注册到 Koin。
- 在构建 `OkHttpClient` 时通过 `.addInterceptor()` 引入该拦截器，使其阻断所有网络层的空包反序列化异常。

由于之前 `MusicRepositoryImpl` 已经使用 `catch` 捕获异常，此改进可以顺着原有的异常流（`Result.failure` -> `HomeUiState.Error`）直达 UI 层，并触发标准的失败/重试状态展示。

---

### 2026-04-24 Session 9 — 初始化播放服务 (Phase 4 — Media3 Integration)

#### `[MODIFY] gradle/libs.versions.toml` & `app/build.gradle.kts`
- 引入 **AndroidX Media3** (1.3.1) 核心组件：`media3-exoplayer` 和 `media3-session`。

#### `[NEW] player/LinMusicPlaybackService.kt`
- 核心播放服务实现，继承自 `MediaSessionService`，保障后台持续播放能力。
- 在 `onCreate` 中初始化 `ExoPlayer` 实例，并与 `MediaSession` 绑定。
- 实现了 `onGetSession` 钩子，支持外部 `MediaController` 连接。
- 实现了 `onDestroy` 钩子，严格释放 Player 和 Session 资源，防止内存泄漏。

#### `[MODIFY] AndroidManifest.xml`
- **权限申请**：新增 `FOREGROUND_SERVICE` 与 `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (适配 Android 14+ 前台服务新规)。
- **服务注册**：注册 `LinMusicPlaybackService`，设置 `foregroundServiceType="mediaPlayback"` 声明前台服务类型。
- **Intent Filter**：添加 `androidx.media3.session.MediaSessionService` 动作响应。

---

### 2026-04-24 Session 10 — 全局播放控制器 (PlayerManager)

#### `[NEW] player/PlayerManager.kt`
- 创建 UI 与 `LinMusicPlaybackService` 通信的桥梁类。
- 利用 `suspendCancellableCoroutine` 将原生 Guava `ListenableFuture` 的回调转换为协程原生的挂起函数 (`initController()`)，优雅地构建 `MediaController` 连接。
- 通过内部实现 `Player.Listener` 监听回调，将播放器的当前轨道 (`currentTrack`) 和播放/暂停状态 (`isPlaying`) 封装为 `StateFlow`，便于 Compose UI 层的单向数据流监听绑定。
- 提供了面向业务的基本方法封装：`playAudio`（含自动根据元数据构建 `MediaItem`）、`pause`、`resume`、`seekTo` 等。

#### `[NEW] di/PlayerModule.kt` & `[MODIFY] LinMusicApplication.kt`
- 配置 Koin 依赖注入：`single { PlayerManager(androidContext()) }`。
- 将 `playerModule` 挂载到应用启动时的 Koin 运行配置中。

---

### 2026-04-24 Session 11 — 歌曲播放链接 API (Song URL)

#### `[MODIFY] data/remote/api/NeteaseApiService.kt`
- 新增 `@POST("/weapi/song/enhance/player/url/v1")` 接口，用于获取网易云真实歌曲播放链接。
- 定义 `SongUrlRequest` 请求体（固定 `level="standard"`, `encodeType="flac"` 等必要参数）。
- 定义独立的 `SongUrlResponse`、`SongUrlItem` 响应体包装（专门为应对 API 在此路由下不同的 Json 结构嵌套，而非盲目复用泛型包装导致解析丢失）。
- 增加了针对收费歌曲与版权受限时的标识字段 `freeTrialInfo` 保留项，以供后续权限扩展。

#### `[MODIFY] data/repository/MusicRepository.kt` & `MusicRepositoryImpl.kt`
- 仓储层新增方法 `getSongUrl(songId: Long): Flow<Result<String>>`。
- 封装内部的流控制：提取数组的首项，遇到空 URL 和 VIP 收费拦截等无效播放情况时，主动向外抛出 `Result.failure` 异常阻断，确保能够触发 UI 层的错误表现。

---

### 2026-04-24 Session 12 — UI 层播放控制与悬浮播放条 (MiniPlayer)

#### `[MODIFY] ui/home/HomeViewModel.kt`
- 成功注入 `PlayerManager` 控制单例。
- 自动化服务绑定：在 `init` 中通过 `playerManager.initController()` 连接后台播放服务。
- 暴露了基于协程 `SharedFlow` 的 `toastEvent` 用于 UI 层的无副作用的一次性弹窗分发。
- 实现业务方法 `playSong()`：拉取真实音频流地址后，无缝移交给 `playerManager.playAudio` 控制器调度。
- 暴露 `togglePlayPause()` 以供 Compose 层交互触发动态暂停与重播。

#### `[MODIFY] ui/home/HomeScreen.kt`
- **悬浮组件引入 (超前实现)**：设计封装了全局悬浮组件 `MiniPlayer`，被悬浮固定在底部。支持显示带圆角的动态封面、歌名歌手、和状态同步的切换按钮。
- **状态流监听**：利用 `collectAsStateWithLifecycle` 将 `PlayerManager` 暴露出来的底层播放状态 `currentTrack` 及 `isPlaying` 激活为 Compose 动态响应内容。
- **异常捕获**：引入 `LaunchedEffect` 来收集后端抛出的收费歌曲或坏链报错，并调用 `Toast` 展现。
- 修复并补全了 `PlaylistItemCard` 卡片的点击监听连线。

---

### 2026-04-24 Session 13 — 绕过网易云反风控策略与免登录协议修补

#### 核心问题识别
在真机或国内特定网络环境下，直接请求免登录的个性化推荐接口可能引发 `api body is empty, possibly auth failed` 的错误。这本质来源于网易云极强的防逆向/反爬虫鉴权：
1. **设备指纹**：不允许移动端设备无 Cookie 直接访问宽泛的推荐接口。
2. **地域风控**：无中国大陆 IP 参数的请求易被空包拦截。
3. **加密容错**：`WeApi` 的早期实现直接拦截 `{}` 或空包。

#### `[NEW] data/remote/network/HeaderInterceptor.kt`
- 专门解决设备反爬风控的头拦截器。
- 伪装 `User-Agent` 与 `Referer` 为官方 PC 浏览器。
- 注入了 `X-Real-IP` 以及 `X-Forwarded-For` 将请求归属地回流中国国内。
- 解析了原有 Cookie 层，缺失设备标识时注入保底参数 `os=pc; osver=Microsoft...`。

#### `[MODIFY] di/NetworkModule.kt`
- 将 `HeaderInterceptor` 注册成了单例，并将其添加进入 `OkHttpClient` 的构建队列中。
- **注意顺序**：必须放置在 `CryptoInterceptor` 和 `EmptyBodyInterceptor` 的中间运行，以规避参数签名破坏。

#### `[MODIFY] data/remote/api/NeteaseApiService.kt`
- 新增具体的请求体：`@Serializable data class PersonalizedRequest(val limit: Int = 30)`。
- 修改 `@POST("weapi/personalized")` 接口使用此实体类替代原来的 `EmptyBody`。借助 Kotlinx Serialization 的解析，最终将会生成合乎规范的 `{"limit":30}`，使得网络深层的 `CryptoInterceptor.readString()` 将不再接收空对象，从而完美逃逸后端的空包查杀。

### 2026-04-24 Session 14 — 致命 WeApi 签名与 Padding 错误修复
根据实际抓包比对，我们完全重构了 `NeteaseCrypto.kt` 中 `weapi` 的核心实现，解决了 Android 平台特有的致命坑点：
1. **Base64 换行符消除**：`aesEncrypt` 由 `java.util.Base64` 整体迁移至 `android.util.Base64`，并在 `encodeToString` 时强制指定 `Base64.NO_WRAP` 免除换行符问题，防止网易反解析异常。
2. **Padding 防护**：对 `rsaEncrypt` 启用了大数（`BigInteger`）计算模式，对齐结果并利用 `padStart(256, '0')` 左侧强制填充安全位数，以绕开 Android 原生 Cipher 对于非标准模数执行 Padding 的异常。
3. **二次加密规整**：纠正了两次 AES 之间的过渡流传：第二次 AES 必须专门对其上一次输出的最终 Base64 明文字符串再次进行独立化加密计算；并修复了官方通用的 `PRESET_KEY` (`0CoJUm6Qyw8W8jud`) 以及 `PUBLIC_KEY`、`MODULUS` 的硬编码。
4. **IP 轮替动态化**：配合防刷机制，将 `HeaderInterceptor` 中的保底访问 IP 修改为了基于 `Math.random` 构造的中国沿海高亮区动态 IP (`116.25.x.x`) 以逃避定点 IP 风控。

### 2026-04-24 Session 15 — 清理路由劫持，全面回归纯净 WeApi 
之前为了绕过 IPv6 风控，我们曾在 `CryptoInterceptor` 内部实施了硬性的 `Linux Forward` 转发劫持机制，但导致了需要繁体嵌套的 URL (如获取音乐播放链接的 API) 受到牵连持续爆出 `400 Bad Request`。随着 Session 14 中 `NeteaseCrypto` 的底层缺陷彻底修正，经过严格排查，我们决定：
- **完全摘除 Linux 移花接木算法**：在 `CryptoInterceptor.kt` 删除了所有针对 `/weapi/` 执行重定向至 `https://interface.music.163.com/api/linux/forward` 的逻辑。
- 所有的 `/weapi/` 路由流量现在都能最纯粹地通过原版 `CryptoType.WEAPI` 进行算法洗礼，并精确保留它们最初请求的 URL 路径出海。

---

## 待办事项 / 下一步

- [x] Repository 层封装 (数据仓库模式)
- [x] Gradle Sync 验证编译通过并完成单元测试
- [x] ViewModel 层 (架构搭建与 UI State 数据收发)
- [x] UI 层集成 (使用 Jetpack Compose 显示数据状态)
- [x] 首页切换为免登录公开接口 (personalized)
- [x] 初始化 Media3 播放引擎 (Service + Session)
- [x] 全局播放控制器封装 (PlayerManager + StateFlow)
- [x] 歌曲播放链接 API 对接
- [x] UI 层集成播放控制组件 (全局悬浮播放栏 MiniPlayer)
- [x] 全局 UI 视觉升级 (Spotify 暗黑风格)
- [x] 全屏播放器页面实现与动效集成
- [ ] 更多 API 接口补充 (搜索、歌曲详情等)
- [ ] Cookie / Token 管理 (持久化登录态)
- [ ] 歌词解析与显示
- [ ] 错误处理统一封装 (集中分发业务码映射弹窗和过滤)

---

### 2026-04-25 Session 16 — UI 视觉大升级与播放器重构

#### `[MODIFY] ui/theme/Color.kt` & `Theme.kt`
- **配色体系**: 引入 Spotify 经典配色：`SpotifyGreen`, `BackgroundDark`, `SurfaceDark`, `SurfaceLight`, `TextGray`。
- **强制深色模式**: 彻底精简 `Theme.kt`，移除动态颜色（Dynamic Color）和亮色模式切换逻辑，强制 App 锁定在深色材质主题下。

#### `[MODIFY] gradle/libs.versions.toml` & `app/build.gradle.kts`
- **依赖扩充**: 新增 `androidx-compose-material-icons-extended` 库，以支持更丰富的图标集（如 `Icons.Default.List` 等）并修复相应的类型推断报错。

#### `[NEW] ui/player/PlayerComponents.kt`
- **组件抽取**: 将播放器相关 UI 独立化。
- **MiniPlayer**: 高级圆角悬浮样式，支持同步 `currentTrack` 封面、标题及 `isPlaying` 状态，并暴露点击回调用于打开全屏。
- **FullPlayerScreen**: 全屏播放页，采用沉浸式渐变背景，大图封面展示，并与 `MediaItem` 真实元数据及 `PlayerManager` 的 `togglePlayPause` 逻辑深度绑定。

#### `[MODIFY] ui/home/HomeScreen.kt`
- **布局重构**: 采用 `Scaffold` + `LazyColumn` 结构，背景应用绿色到黑色的垂直微渐变。
- **真实数据流**: `AlbumCarousel` 现已接入 `HomeUiState.Success` 中的 `PersonalizedPlaylist` 真实数据，点击卡片直接调用 `viewModel.playSong`。
- **播放器接入**: 移除了本地临时 MiniPlayer 实现，接入统一的 `ui.player.MiniPlayer`。

#### `[MODIFY] MainActivity.kt`
- **全屏动效集成**: 在 Activity 层级通过 `AnimatedVisibility` 容器接管播放器页面的显示状态。
- **交互逻辑**: 通过注入 `HomeViewModel` 监听播放状态，实现点击 MiniPlayer 底部丝滑滑出 `FullPlayerScreen` 的转场效果。

---

### 2026-04-25 Session 17 — EApi / WeApi 通信协议架构深化与修复

#### `[MODIFY] data/remote/crypto/NeteaseCrypto.kt`
- **EApi 算法校准**: 
    - 修正 `EAPI_KEY` 为 `e82ckenh8dichen8`。
    - 修正拼缝符（Salt）为 `-36cd479b6b5-`。
    - 修正散列摘要尾随词为 `md5forencrypt`。

#### `[MODIFY] data/remote/network/CryptoInterceptor.kt`
- **降维算力策略**: 在执行 EApi 加密前，动态将 Payload 中的路径 `/eapi/` 替换为 `/api/`，以对齐网易云后端的验签算法逻辑。
- **指纹负载注入**: 拦截器现在会自动往 EApi 的 JSON Body 中注入包含 `os`, `appver`, `deviceId`, `requestId` 等 9 项核心设备特征的 `header` 对象，从而通过基于载荷内容的 Anti-Cheat 服务器检测。
- **WeApi 健壮性**: 为所有 WeApi 请求强制补全 `csrf_token` 字段，避免因字段缺失导致服务器返回 0 字节空包。

#### `[MODIFY] data/remote/network/HeaderInterceptor.kt`
- **动态域名重定向**: 识别 `/eapi/` 路由流量并将其 Host 无缝切换至原生 APP 专用域名 **`interface.music.163.com`**，成功避开了 Web 域名 `music.163.com` 对非网页加密流的屏蔽。
- **双轨 UA 路由**: 为 EApi 精准下发 `iPhone; iOS 16.2` 移动端 User-Agent 与对应 Cookie，同时维持 WeApi 的 PC 端伪装，消除了指纹冲突导致的 0 字节风控。

#### `[MODIFY] data/remote/api/NeteaseApiService.kt`
- **路由同步**: 将「个性化推荐歌单」接口由不稳定的 `/weapi/personalized` 迁移至修复后的专线 `/eapi/personalized/playlist`。

#### `[MODIFY] di/NetworkModule.kt`
- **连接调优**: 将全局 `connectTimeout`、`readTimeout` 和 `writeTimeout` 统一提升至 **30s**，以应对复杂加密载荷在不佳网络环境下的响应抖动。

