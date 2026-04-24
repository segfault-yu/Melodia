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
├── MainActivity.kt                 # 主 Activity (Compose)
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   └── NeteaseApiService.kt  # Retrofit 接口 + 请求/响应数据类
│   │   ├── crypto/
│   │   │   └── NeteaseCrypto.kt      # 网易云加密工具 (WeApi/LinuxApi/EApi)
│   │   └── network/
│   │       └── CryptoInterceptor.kt  # OkHttp 加密拦截器
│   └── repository/
│       ├── MusicRepository.kt        # Repository 接口定义
│       └── MusicRepositoryImpl.kt    # Repository 响应实现 (处理流与错误)
├── di/
│   ├── NetworkModule.kt             # Koin 网络层依赖模块
│   ├── RepositoryModule.kt          # Koin 数据层依赖模块
│   └── ViewModelModule.kt           # Koin ViewModel 层依赖模块
└── ui/
    ├── home/
    │   ├── HomeScreen.kt             # 首页 Compose 页面 (LazyVerticalGrid + PlaylistItemCard)
    │   ├── HomeUiState.kt            # 首页 UI 状态密封接口 (Loading/Success/Error)
    │   └── HomeViewModel.kt          # 首页 ViewModel (注入 MusicRepository)
    └── theme/                        # Compose 主题 (自动生成)
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

## 待办事项 / 下一步

- [x] Repository 层封装 (数据仓库模式)
- [x] Gradle Sync 验证编译通过并完成单元测试
- [x] ViewModel 层 (架构搭建与 UI State 数据收发)
- [x] UI 层集成 (使用 Jetpack Compose 显示数据状态)
- [x] 首页切换为免登录公开接口 (personalized)
- [ ] Cookie / Token 管理 (拦截器抓取登录态响应参数，配合 DataStore/Preferences 进行持久化保存并在请求中复用)
- [ ] 更多 API 接口补充 (搜索、歌曲详情、播放链接等)
- [ ] 错误处理统一封装 (集中分发业务码映射弹窗和过滤)

