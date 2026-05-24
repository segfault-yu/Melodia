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
- **图片加载**: Coil 2.7.0

---

## 项目结构
```text
app/src/main/java/com/lin0721/linmusic/
├── LinMusicApplication.kt          # Koin 初始化 + Coil 预热
├── MainActivity.kt                 # 入口容器
├── data/
│   ├── remote/
│   │   ├── api/                    # API 定义与 DTO
│   │   ├── crypto/                 # 加密逻辑 (WeApi/EApi/LinuxApi)
│   │   └── network/                # 拦截器 (加密、Header、空包处理)
│   └── repository/                 # 数据层 (MusicRepository + 领域模型)
├── di/                             # 依赖注入模块
├── player/
│   ├── LinMusicPlaybackService.kt    # Media3 服务
│   └── PlayerManager.kt              # 播放控制器
└── ui/
    ├── home/                       # 首页模块
    ├── library/                    # 音乐库模块
    ├── create/                     # 创建模块 (BottomSheet + ViewModel)
    ├── player/                     # 播放器组件
    ├── playlist/                   # 歌单详情页
    ├── search/                     # 搜索/发现页
    ├── components/                 # 通用组件 (登录、侧边栏等)
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
- **侧边栏动画**: 弃用覆盖式 Drawer，改为 Row 布局，侧边栏展开时主界面自动向右平移"挤压"。

### 2026-05-04 — 真实登录与手势系统
- **网页登录全集成**: 移除模拟登录，通过 WebView 完成真实授权并同步 Cookie。
- **AnchoredDraggable**: 升级手势引擎，实现侧边栏与主界面的"推拉"跟手位移。
- **API 现代化**: 修复构造函数废弃警告，迁移至 flingBehavior 驱动模式。
- **视觉优化**: 增加主页面平移时的动态圆角过渡（0dp -> 32dp）与边缘阴影。

### 2026-05-10 — 首页历史日推
- **历史日推接口**: 新增 `/weapi/discovery/recommend/songs/history/recent` 与 `/weapi/discovery/recommend/songs/history/detail`，通过 WeApi 路由访问（黑胶 VIP 专属）。
- **HistoryRecommendSheet**: 日推卡片新增「历史」入口，弹出 BottomSheet 展示可用日期与对应歌单。
- **HomeFeedData 扩展**: 新增 `dailySongs`、`recentPlaylists` 字段，首页加载并发数提升至 4 路 async。

### 2026-05-15 — 首页排行榜
- **ToplistInfo 领域模型**: 定义于 `MusicRepository.kt`，包含 id、name、coverUrl、updateDesc、topSongs（前三首）。
- **EApi 路由**: `/eapi/toplist/detail` 替代 WeApi 版本（后者对 PC 端特征返回 0 字节），成功绕过服务端风控。
- **MusicRepositoryImpl**: 在 `getToplistDetail()` 中过滤 coverImgUrl/name 为空的占位榜单。
- **ToplistCarousel + ToplistCard**: 首页新增 Spotify 风格深色榜单卡片，含封面渐变蒙版与前三名歌曲列表。
- **首页布局顺序**: 个性化歌单 → 私人雷达 → 最近播放 → 今日推荐 → 排行榜 → 你最爱的艺人。

### 2026-05-15 — 性能优化 (Coil 预热 + 解码并行度 + 列表懒加载)
- **LinMusicApplication.kt**: 即时构建 `ImageLoader` 并赋值（非 lazy），将 `DiskLruCache.initialize()` 移至后台线程，消除启动时 762ms 锁竞争。
- **解码并行度控制**: 添加 `decoderDispatcher(Dispatchers.IO.limitedParallelism(4))` 与 `fetcherDispatcher(Dispatchers.IO.limitedParallelism(8))`，根治 HWUI "Image decoding logging dropped" 警告。
- **MemoryCache/DiskCache 上限**: 分别设置为堆内存 15% 和磁盘空间 2%。
- **DailyRecommendCard**: 从 `Column+verticalScroll+forEachIndexed` 重构为 `LazyColumn+itemsIndexed`，消除日推列表的全量渲染。
- **稳定 key**: 所有 `LazyRow` 均添加 `key = { it.id }`（歌单、排行榜、歌手、最近播放），避免不必要的 recomposition。
- **图片尺寸规范**:
  - `RecommendationCarousel` / `RecentPlaylistCarousel` / `ArtistCircleCard`: `?param=200y200`。
  - `ToplistCard`: `ImageRequest.size(360, 360)`，ImageRequest 使用 `remember(item.coverUrl)` 缓存。

### 2026-05-16 — 你最爱的艺人
- **ArtistInfo 领域模型**: 定义于 `MusicRepository.kt`，包含 id、name、avatarUrl。
- **API 策略**:
  - 优先调用 `/eapi/artist/sublist`（已登录用户的关注歌手列表）。
  - 实际响应结构为 `{"data":[...],"code":200}`，`data` 为直接数组（非嵌套对象），已相应修正 `ArtistSublistResponse` DTO。
  - 若未登录或列表为空，自动降级至 `/eapi/artist/top` 热门歌手兜底。
- **ArtistSublistRequest**: 附带 `limit=25, offset=0, total=true`，避免 EApi 参数校验失败返回 400。
- **FavoriteArtistsSection + ArtistCircleCard**: 圆形头像（`Modifier.clip(CircleShape)`）横向滚动列表，置于首页最末。
- **头像字段优先级**: 使用 `img1v1Url` 作为方形头像主字段，`picUrl` 备用（部分歌手 picUrl 为宽幅图）。

### 2026-05-16 — 歌单界面重构 + 品牌统一
- **沉浸式顶部结构**: 引入 `WindowInsets.statusBars` 完美适配 Edge-to-Edge，修复顶栏内容与系统状态栏的重叠问题。
- **隐藏式搜索栏**: 采用 `initialFirstVisibleItemIndex = 1` 首视区控制，配合 `Modifier.height` 和动态透明度，实现下拉滑出搜索框交互。
- **动态色彩提取 (Color Extraction)**: 配置 Coil `allowHardware(false)` 解锁软渲染模式。使用手写 `10x10` 像素采样与 HSV 色彩自适应修正算法抓取封面主色调，统一作用于背景渐变、搜索栏底色与吸附 TopBar。
- **折叠动效 (Binary Snap)**: 基于 `firstVisibleItemScrollOffset` 构建折叠进度 (`0f..1f`)。歌单名称、播放按钮等元素在 `progress >= 0.8f` 时瞬间切换（`isCollapsed`），不使用渐隐动画，头部信息与 TopBar 标题/FAB 同步显隐。
- **品牌颜色统一**: 全面替换 `SpotifyGreen` 为 `NeteaseRed`，按钮文字从 `Color.Black` 改为 `Color.White`，与主页配色保持一致。

### 2026-05-16 — 全局浮动播放器
- **BottomFloatingIsland 提升**: 从 `HomeScreen` 移至 `LinMusicApp`（`MainActivity.kt`），使太空舱式播放器悬浮于所有界面之上。
- **hazeState 全局化**: `HazeState` 在 `LinMusicApp` 层创建，通过 `.haze(hazeState)` 应用于内容包装 Box，保持毛玻璃效果。
- **播放器状态集中收集**: `currentTrack`、`isPlaying`、`currentPosition`、`duration` 统一在 `LinMusicApp` 通过 `collectAsStateWithLifecycle` 获取。
- **PlaylistScreen 清理**: 移除旧 `MiniPlayer` 组件及相关 import，底部 padding 增至 160dp 避免被浮动岛遮挡。
- **首页精简**: 移除"播客"、"有声书"、"直播"筛选按钮，仅保留"全部"和"音乐"。

### 2026-05-17 — 发现/搜索模块 与 氛围化 UI
- **发现页路由**: 集成 `/eapi/homepage/block/page` 获取动态网格布局，`/eapi/search/defaultkeyword/get` 获取动态搜索占位符。
- **SearchScreen.kt**: 
  - **沉浸式适配**: 使用 `statusBarsPadding()` 避开状态栏，设置 `160.dp` 底部内边距避让全局播放器。
  - **高对比度蒙版**: 为发现卡片封面增加 `Black 40% -> Transparent -> Black 60%` 的垂直渐变，确保标题文字在任何背景下清晰可见。
  - **分类导航**: 横向滚动分类入口（排行榜、歌手、曲风等），图标统一使用 `NeteaseRed` 透明底色方案。
- **氛围背景光 (Static Ambient Light)**: 在主页（HomeScreen）背景底层实现红色静态环境光晕。
  - **绘制技术**: 使用 `Canvas` 与 `RadialGradient` 在上半区域绘制 `NeteaseRed` (25% Alpha) 光晕。
  - **边缘消隐**: 配合 `Brush.verticalGradient` 遮罩，使光晕在屏幕中部平滑淡入主背景色（BackgroundDark）。
- **交互优化**: 移除主页多余的铃铛按钮，简化搜索页顶栏（移除返回键），通过 `SearchViewModel` 统一管理发现区块与搜索关键词加载。

### 2026-05-17 — 搜索全链路实现 + 热搜 + 精品歌单标签
- **云搜索**: 接入 `/eapi/cloudsearch/pc`，支持 400ms 防抖、分页加载与当前播放曲目高亮。
  - `SearchSongsResult` 领域模型封装 songs/totalCount/hasMore。
  - `SearchResultsList` 使用 `derivedStateOf` 实现距底 5 条自动触发 `loadMore()`。
- **热搜榜**: 接入 `/eapi/hotsearchlist/get`，展示前 10 条热搜关键词。
  - `HotSearchRow`: 排名 1-3 使用 `NeteaseRed` + `FontWeight.Bold`，4+ 使用 `TextGray`。
  - 支持 `iconUrl` 展示"热/新"徽标，右侧显示热度分数。
  - 点击热搜词直接触发 `searchWithKeyword()`，自动激活搜索并填入关键词。
- **精品歌单标签**: 并行调用 `/eapi/playlist/highquality/tags` + `/eapi/playlist/highquality/list`。
  - `MusicRepositoryImpl.getPlaylistTags()`: 在 `coroutineScope` 内并发获取标签名与歌单列表，构建 tagName→coverImgUrl 映射。
  - `PlaylistTagCard`: 有封面时展示封面图 + 暗色渐变蒙版 + 标签名；无封面时使用 10 色轮转色板作为背景。
  - 2 列网格布局，卡片高度 100dp，圆角 8dp。
- **搜索入口差异化**: `SearchScreen` 新增 `autoFocus` 参数。
  - 从主页搜索栏进入 → `autoFocus=true`，自动弹出键盘。
  - 从悬浮胶囊导航栏进入 → `autoFocus=false`，展示发现内容。
- **API 路径修正**:
  - `/eapi/search/defaultkeyword` → `/eapi/search/defaultkeyword/get`（末尾需 `/get`）。
  - `/eapi/search/hot/detail` → `/eapi/hotsearchlist/get`（底层真实路径）。
  - 精品歌单从 `/weapi/` 改为 `/eapi/`（weapi 被火山 CDN 风控拦截返回 0 字节）。

### 2026-05-17 — 界面切换动画优化
- **AnimatedContent 替代 Crossfade**: 屏幕转场从纯交叉淡入升级为方向感知的 fade + slide 组合动画。
  - 前进导航（Home→Search/Playlist）: 新页面淡入 + 上滑 40px，旧页面淡出 + 上移 40px。
  - 后退导航: 方向反转，新页面从上方滑下。
  - 退出动画 200ms 先完成，进入动画延迟 100ms 后以 300ms 展开，消除双屏半透明重叠导致的白色闪屏。
  - 统一使用 `FastOutSlowInEasing` 缓动曲线。
- **全屏播放器过渡**: 在原有 `slideInVertically`/`slideOutVertically` 基础上叠加 `fadeIn`/`fadeOut`，使用 `FastOutSlowInEasing` 替代线性 tween，滑入 350ms / 滑出 300ms。
- **白屏闪烁修复**: 根 Box 添加 `.background(BackgroundDark)` 确保深色底板始终可见；`SizeTransform(clip=false)` 防止裁切伪影。

### 2026-05-17 — 音乐库 (Music Library)
- **多接口并行聚合**: 并发拉取 `/eapi/user/playlist` (用户歌单)、`/eapi/album/sublist` (收藏专辑) 以及已有的 `/eapi/artist/sublist` (关注歌手)，在 `coroutineScope` 内使用 `async/await` 提升并行度，秒级加载全量资产。
- **数据归一化设计**: 将不同领域模型的 DTO 数据提取并转换映射为统一的 `LibraryItem`，支持类型区分 (`PLAYLIST`, `ARTIST`, `ALBUM`)。
- **精细化 UI & 特殊处理**:
  - 对“已点赞的歌曲”做定制化处理：渲染为蓝到红的高级流光渐变背景 (`Color(0xFF6366F1)` → `Color(0xFFA855F7)` → `Color(0xFFEC4899)`) 并显示白色心形 icon。
  - 关注歌手头像圆图处理 (`CircleShape`)，歌单与专辑采用方图 (`RoundedCornerShape(8.dp)`)。
  - 支持横向滚动过滤药丸（全部/歌单/专辑/歌手），带选中色调切换微动效。
- **本地置顶 (Pinned) & 排序**:
  - 本地 SharedPreferences 记录 Pinned 状态，置顶项目在副标题行旁显示翡翠绿图钉（Emerald Pin，`Color(0xFF10B981)`），并始终浮动置于列表顶部。
  - 排序规则支持“最近播放”（基于 updateTime）、“创建时间”与“字母排序”，支持 DropdownMenu 随时切换。
- **本地检索 (Local Search) & 创建歌单**:
  - 点击顶栏搜索图标触发 `AnimatedContent`，极速展开展开式搜索框，进行前端关键词过滤。
  - 新建歌单调取 `/eapi/playlist/create` 异步创建，内置暗色高水准 `AlertDialog` 输入框，完美处理未登录防错重定向。

### 2026-05-17 — 全局侧边栏重构 (Global Sidebar Refactoring)
- **顶级手势容器与状态提升**: 将侧边栏状态、手势拖拽（`anchoredDraggable`）和弹出蒙版从主页和音乐库剥离，统一提升至 `MainActivity.kt` 顶层容器，实现全局手势滑动唤起。
- **3D 浮雕平移圆角过渡**: 侧边栏滑出时，主内容层（包括导航胶囊、转场层等）整体平移，动态附加 `0.dp -> 32.dp` 圆角裁剪及 `0f -> 30f` 外层投影，呈现震撼的浮雕式空间折叠交互动效。
- **架构解耦与精简**: `HomeScreen.kt` 与 `LibraryScreen.kt` 移除了全部局部的侧边栏手势及界面嵌套代码，精炼为向顶层回传 `onOpenSidebar` 统一接口，极大精简了代码复杂度。
- **搜索页无缝头像弹出**: 将 `SearchScreen.kt` 顶部头像重构为 clickable，点击即可全局唤醒侧滑抽屉，为核心版块铺平了一致性的多端手势操作。

### 2026-05-17 — 搜索页顶栏重构 + 视图切换
- **搜索页标题栏分离**: 将搜索页顶部重构为独立标题栏 + 搜索栏两层结构。
  - 标题栏包含：登录用户头像（`AsyncImage` 加载 `userProfile.avatarUrl`，未登录显示 `AccountCircle` 图标）、"搜索" 粗体标题（24sp）、听歌识曲入口图标。
  - 搜索栏保持原有设计不变。
- **SearchViewModel 扩展**: 新增 `UserPreferences` 构造函数参数，通过 `stateIn()` 暴露 `userProfile: StateFlow<UserProfile?>` 驱动头像显示。
- **热搜双排布局**: 热搜榜从单列卡片改为双列紧凑网格（`chunked(2)` + `HotSearchCompactItem`）。每项包含排名序号（前 3 名 `NeteaseRed` 加粗）、关键词、可选热度徽标图。
- **最近播放视图切换 (HomeScreen)**: "最近播放" section 新增列表/网格切换按钮。
  - `recentViewIsGrid` remember 状态控制视图模式。
  - 自定义 header：`History` 图标 + "最近播放" 标题 + `IconButton`（`GridView`/`List` 图标切换）。
  - `RecentPlaylistGrid`: 3×3 大封面网格（`chunked(3)` + `Column/Row`），封面 `aspectRatio(1f)` + `RoundedCornerShape(12.dp)`。
  - 条件渲染：`recentViewIsGrid` → `RecentPlaylistGrid`，否则 → `RecentPlaylistCarousel`。
- **音乐库视图切换 (LibraryScreen)**: 排序栏右侧图标升级为功能性 `IconButton`。
  - `viewIsGrid` remember 状态，点击切换 `GridView`/`List` 图标与视图模式。
  - `LibraryGridItem`: 3 列网格项，支持"已点赞歌曲"渐变背景 + 心形图标、歌手圆图、歌单/专辑方图。
  - 网格模式使用 `LazyColumn` + `chunked(3)` 行布局，不足 3 项时 `Spacer` 占位保持对齐。

### 2026-05-18 — 创建界面 (Create BottomSheet + Playlist Creation)
- **全局创建入口**: 底部导航栏"创建"按钮从空操作升级为弹出 `ModalBottomSheet` 菜单。
  - `BottomFloatingIsland` 新增 `onCreateClick` 回调参数，`MainActivity` 统一管理 `showCreateSheet` 状态。
- **CreateViewModel**: 专用 ViewModel，注入 `MusicRepository`、`PlayerManager`、`UserPreferences`。
  - `createNewPlaylist(name, isPrivate)`: 调用 `repository.createPlaylist(name, privacy)`，支持 `isCreating` 防重复提交。
  - 暴露 `toastEvent` 通知创建结果，`userProfile` 检查登录状态。
- **CreateBottomSheet 菜单**: 四项创建入口（新建歌单 / 导入外部歌单 / 上传本地音乐 / 发起一起听）。
  - 每项为 `CreateMenuItem` 组件：圆角图标容器（44dp, `SurfaceLight`）+ 标题/副标题 + 右箭头。
  - 正在播放上下文卡片：检测 `playerManager.currentTrack`，显示当前播放歌曲封面、标题和歌手。
- **新建歌单对话框**: `AlertDialog` 包含名称输入框（`BasicTextField`，占位符"我的新歌单"）和隐私歌单 `Switch` 开关（`NeteaseRed` 激活色）。
  - 确认按钮在创建过程中显示 `CircularProgressIndicator` 替代文字，禁用重复点击。
  - 未登录时 Toast 提示并触发 `onLoginRequest` 回调。
- **Koin 注册**: `ViewModelModule` 新增 `viewModelOf(::CreateViewModel)`。

### 2026-05-19 — 播客分类与播放页视觉重构 (Podcast Filter & FullPlayer Visual Refactoring)
- **播客胶囊**: 主页的 `FilterPills` 过滤栏中重新接入 `"播客"` 胶囊，恢复为三胶囊分类布局。
- **嵌入式播放页顶栏 (Embedded Player TopBar)**:
  - 移除了非滚动状态下固定的顶栏容器。原有的返回箭头、播放来源文本（如“正在播放：搜索”）和更多选项按键被直接嵌入至 `CoverArt` 封面图顶部。
  - 这些元素现在与封面绑定为同一 LazyColumn item，并随滑动做平滑上移、缩放与渐变。
- **氛围化滚动顶栏 (Scrolled TopBar UI)**:
  - 滚动隐藏封面时，顶栏背景统一为歌曲主色调 (`animatedDominant`)，消除与状态栏的色差。
  - 歌曲信息（标题/艺人）更改为**靠左对齐**垂直布局，右侧新增**收藏爱心图标**以及**播放/暂停控制状态按钮**。
- **歌词卡片渐变与卡片重排 (Lyrics HSV & Section Reorder)**:
  - 实现 `Color.toOpaqueHsv` 进行 HSV 色度修正（增强饱和度与亮度调节），重新构建歌词渐变蒙版背景。
  - 调整卡片展示顺序为：歌曲信息 → 播放控制 → 歌词 → 关于艺人 → 相似艺人 → 艺人专辑 → 制作人（底栏置底）。

---

## 网络路由速查

| 端点 | 加密路由 | 说明 |
|------|----------|------|
| `/eapi/*` | EApi (MD5+AES-ECB) → `interface.music.163.com` | iOS UA，绕过 PC 风控 |
| `/weapi/*` | WeApi (AES-CBC+RSA) → `music.163.com` | PC UA，部分私有接口会被拦截返回 0 字节 |
| `/eapi/toplist/detail` | EApi | 排行榜，weapi 版本被风控 |
| `/eapi/artist/sublist` | EApi | 已关注歌手，weapi 版本返回 0 字节 |
| `/eapi/search/defaultkeyword/get` | EApi | 搜索框默认占位文字 |
| `/eapi/hotsearchlist/get` | EApi | 热搜榜详情（关键词+热度+徽标） |
| `/eapi/cloudsearch/pc` | EApi | 云搜索（综合搜索歌曲） |
| `/eapi/playlist/highquality/tags` | EApi | 精品歌单标签列表 |
| `/eapi/playlist/highquality/list` | EApi | 精品歌单列表（含封面图） |
| `/eapi/homepage/block/page` | EApi | 发现/首页动态区块布局 |
| `/weapi/discovery/recommend/songs/history/*` | WeApi | 历史日推，仅 weapi 有此路径 |
| `/eapi/user/playlist` | EApi | 获取当前登录用户的歌单列表 |
| `/eapi/album/sublist` | EApi | 获取收藏的专辑列表 |
| `/eapi/user/subcount` | EApi | 获取用户收藏/关注数量统计（用于初始化计数） |
| `/eapi/playlist/create` | EApi | 新建歌单（支持公开/私密模式） |
| `/weapi/song/play/about/block/page` | WeApi | 音乐百科简要信息（获取曲风、语种、BPM、影综） |
| `/weapi/song/creators` | WeApi | 获取制作团队（作词、作曲、编曲等）成员 |
| `/weapi/artist/follow/count/get` | WeApi | 获取歌手粉丝数量/关注数（避开 EApi 0 字节风控） |
| `/eapi/song/like` | EApi | 喜欢/红心歌曲操作 |
| `/eapi/song/like/get` | EApi | 获取当前用户已红心的歌曲 ID 列表 |
| `/eapi/v1/resource/comments/{threadId}` | EApi | 获取歌曲的评论列表（含热门评论与普通评论） |

### 2026-05-20 — 全屏播放器视觉精修 Phase 5 (Glassmorphism + Lyrics + Micro-interactions)
- **毛玻璃 TopBar (Haze Glassmorphism)**: 滚动吸附顶栏从扁平半透明升级为 Haze 实时模糊效果。`HazeState` 挂载于 LazyColumn，`hazeChild` 在 TopBar 滚动展开时激活（`blurRadius = 24.dp`，`tint = BackgroundDark 50% alpha`）。
- **歌词动画高亮 (Animated Lyrics Highlight)**:
  - 当前行: 22sp `ExtraBold`，使用 `lerp(dominant, White, 0.85f)` 得到的高光色。
  - 非当前行: 16sp `Medium`，`TextGray` alpha 按与当前行距离递减。
  - `animateFloatAsState(tween(400))` 控制 alpha，`animateFloatAsState(tween(350))` 控制 fontSize。
  - 纯音乐（`lyrics.isEmpty()`）自动隐藏歌词卡片。
- **鲜艳度优先色彩提取 (Vibrant-Score Extraction)**: `ColorExtraction.kt` 从面积均值算法重构为 `score = S × V` 逐像素评分，确保提取专辑封面中最鲜艳的颜色而非面积最大的颜色。上半区取 dominant，下半区取 secondary。
- **纯不透明 HSV 渐变 (Opaque HSV Gradient)**: 歌词卡片背景使用 `Brush.linearGradient`，两端颜色通过 `Color.toOpaqueHsv()` 进行纯 HSV 重构（强制 S≥0.75/V∈0.6..0.85 和 S=1.0/V=0.3），彻底消除 alpha compositeOver 导致的灰度污染。
- **封面滚动缩放 (Cover Scroll Scale)**: 基于 `derivedStateOf` + `LazyListState.layoutInfo` 计算封面滚出比例，通过 `graphicsLayer { scaleX/scaleY }` 实现 1.0→0.85 的渐进缩放。
- **播放按钮弹性动画**: `Animatable` + `spring(dampingRatio=0.4f, stiffness=400f)` 实现播放/暂停切换时的回弹缩放效果（0.85→1.0）。
- **非歌词卡片统一底色**: 关于艺人、相似艺人、艺人专辑、制作人等卡片统一使用 `SurfaceDark` 固定底色，仅歌词卡片保留动态渐变。
- **TopBar 图标统一**: 滚动顶栏收藏图标从翡翠绿勾选 (`CheckCircle + #10B981`) 替换为白色实心爱心 (`Favorite + White`)，统一 TopBar 配色。

### 2026-05-22 — 播放队列 UI 优化与纯音乐歌词
- **播放队列重叠与背景优化 (PlayQueueSheet)**:
  - 将歌曲行改为完全不透明的暗色背景（当前项 `SurfaceDark`，拖拽项 `SurfaceLight`），隐藏下方 `SwipeToDismiss` 的红色垃圾桶删除背景，解决底色穿透和垃圾桶与三条杠拖动手柄重叠的问题。
  - 保留三条杠拖动手柄，移除尾部的垃圾桶图标，改由左滑歌曲卡片触发删除手势。
- **纯音乐小歌词适配**:
  - `MusicRepositoryImpl` 在获取歌词时，检测到无歌词或存在"纯音乐"关键字时返回标示为纯音乐的歌词行。
  - 全屏播放器检测到纯音乐时，隐藏大型歌词卡片，同时在进度条上方的小歌词（`MiniLyricLine`）处显示 `纯音乐`。
- **进度条 Slider 游标居中对齐**:
  - 在 `ProgressSection` 内使用 20.dp 大小 Box 容器包裹自定义大小白球并设置 `contentAlignment = Alignment.Center`，解决游标在 M3 Slider 限制下对齐偏上的问题。

### 2026-05-23 — 歌曲详情卡片、红心收藏与精选评论
- **移除制作人卡片**: 移除原有的主要艺人展示卡片（`CreditsCard`），由数据更丰富、排版更精美的 `SongDetailCard` 取而代之。
- **并发防御请求 (MusicRepositoryImpl)**:
  - 使用 `coroutineScope` 并发请求歌曲详情、百科摘要（`/song/wiki/summary`）和创作者列表（`/song/creators`）三个核心接口。
  - 为所有属性提供极强的兜底防御：当接口报错或无数据时，卡片仍能正常展示专辑名与发行日期等基础数据，避免报错导致播放器崩溃。
- **SongDetailCard UI 实现**:
  - 每行属性（曲风、专辑、语种、发行时间、BPM、制作、影综）标签左对齐；“制作”行展示词/曲/编主创且右侧带 `>` 向右指示箭头。
- **艺人粉丝数据接入 (getArtistFollowCount)**:
  - 实现 `/weapi/artist/follow/count/get` 接口，解决 EApi 原生歌手详情字段在特定 Cookie 下被风控导致每月听众返回 0 的缺陷。
  - `PlayerViewModel` 异步加载 `artistFansCount`，在关于歌手卡片中渲染真实的“每月听众：X万”指标。
- **歌曲红心与喜欢同步 (toggleLike)**:
  - 对接 `/eapi/song/like` 与 `/eapi/song/like/get`，初始化登录用户的红心歌单 ID，全屏播放器顶栏收藏图标点击时触发微动画与状态反转，失败时自动做数据回滚。
- **精选评论卡片 (CommentsCard)**:
  - 增加歌曲下方的评论区域，调用 `/eapi/v1/resource/comments/{threadId}` 接口聚合精选热门评论与普通最新评论。
  - 格式化渲染用户头像、发布时间、评论内容及点赞数（如 1.2w+），支持加载中/加载失败重试等交互反馈。

### 2026-05-24 — 歌词与进度条对齐优化、双行播放来源及点播时长兜底
- **歌词卡片对齐与缩放修正**:
  - 移除 `LyricsPreview` 中单行歌词包装的居中 `Box`，将主歌词和翻译文本重置为 `TextAlign.Start` 靠左对齐。
  - 将 `graphicsLayer` 的 `transformOrigin` 重新设定为左侧边缘 `TransformOrigin(0f, 0.5f)`。使高亮行在 1.15 倍缩放时，行首锁定，完美与未激活行首对齐，避免左右错位与偏移。
  - 保留单行 Column `fillMaxWidth(0.85f)`，确保即使横向放大其右侧仍有足够留白以规避被卡片圆角裁剪的问题。
- **歌词卡片纵横比微调**:
  - `LyricsCard` 弃用 `aspectRatio(1f)`（正方形），改为高度比宽度稍短的长方形，计算公式为：`cardHeight = cardWidth * 0.88f`。
- **播放来源栏双行排版与微调**:
  - `CoverArt` 内部的双行标题栏改用居中对齐的 Column，首行显示来源类型（如 "播放自歌单" 等，`11.sp` 粗体与白 60% 透明度），第二行显示具体歌单/推荐名称，加双引号 `“”` 包裹（`14.sp` 纯白粗体），并缩窄两行间距。
  - 微调 `CoverArt` 顶栏 Row 底部间距从 `56.dp` 到 `51.dp`，使整体控制面板精准下移 `15.dp`。
- **进度条完美对齐与时长 DTO 兜底**:
  - `ProgressSection` 中显示时间的 Row 增加 `.padding(horizontal = 6.dp)`，使时间文本与 `Slider` 滑轨两端完美对齐。
  - `NeteaseApiService.kt` 中为 `Track` DTO 增加 `val dt: Long = 0` 时长字段；`PlayerManager.kt` 对 `ExoPlayer` 返回的 duration 强限制非负。
  - 在全屏播放器初始化或切歌缓冲期间，如果播放器时长为 0，自动降级采用 `songDetail.dt` 歌曲详情时长进行兜底，彻底解决点开音乐长度显示 `0:00` 的问题。

---

## 待办事项
- [x] 初始化播放引擎 (Media3)
- [x] 全局播放控制器 (PlayerManager)
- [x] 歌曲播放链接 API 对接
- [x] 全屏播放器页面
- [x] 侧边栏推拉手势
- [x] 真实网页登录集成
- [x] 首页今日推荐 + 历史日推
- [x] 首页排行榜
- [x] 首页你最爱的艺人
- [x] Coil 预热优化 (消除启动掉帧)
- [x] 歌单详情页 (沉浸式折叠 + 动态色彩)
- [x] 全局浮动播放器 (跨页面太空舱)
- [x] 品牌颜色统一 (NeteaseRed)
- [x] 发现/搜索页面 (网格布局 + 氛围背景光)
- [x] 搜索全链路 (云搜索 + 防抖 + 分页 + 播放)
- [x] 热搜榜 + 精品歌单标签卡片
- [x] 界面切换动画 (方向感知 fade+slide)
- [x] 音乐库页面 (多维聚合 + 过滤搜索 + 排序置顶 + 新建歌单)
- [x] 搜索页顶栏重构 (用户头像 + 双排热搜 + 听歌识曲入口)
- [x] 视图切换 (最近播放/音乐库支持列表与3×3网格切换)
- [x] 创建界面 (BottomSheet菜单 + 新建歌单 + 隐私开关 + 播放上下文)
- [x] 全屏播放器精修 (毛玻璃TopBar + 歌词动画 + 封面缩放 + 弹性按钮)
- [x] 鲜艳度色彩提取 (S×V评分算法 + 纯HSV渐变)
- [x] 歌曲详情接口 (曲风/专辑/语种/发行时间/BPM/制作/影综/红心/评论/粉丝数聚合)
- [ ] 歌词解析与同步显示
- [ ] 统一错误处理分发

