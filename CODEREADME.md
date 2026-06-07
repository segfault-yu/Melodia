# Melodia 开发日志

> 记录项目架构决策与技术细节。

---

## 项目概览

- **包名**: `com.lin0721.linmusic`
- **SDK**: Min 26 / Target 36
- **核心**: Kotlin + Jetpack Compose + Material3
- **网络**: Retrofit + OkHttp + kotlinx.serialization
- **DI**: Koin 4.0
- **播放器**: Media3 ExoPlayer
- **图片加载**: Coil 2.7.0

---

## 注释规范

1. 保持代码精简，禁止 KDoc 风格多行注释。
2. 仅使用单行注释 (`//`)，仅在复杂逻辑处添加简短说明。

---

## 项目结构

```text
app/src/main/java/com/lin0721/linmusic/
├── MelodiaApplication.kt           # Koin 初始化 + Coil 预热
├── MainActivity.kt                 # 入口容器，全局手势、侧边栏、浮动播放器
├── data/
│   ├── remote/
│   │   ├── api/                    # API 定义与 DTO
│   │   ├── crypto/                 # 加密逻辑 (WeApi/EApi/LinuxApi)
│   │   └── network/                # 拦截器 (加密、Header、空包处理)
│   └── repository/                 # 数据层 (MusicRepository + 领域模型)
├── di/                             # 依赖注入模块
├── player/
│   ├── MelodiaPlaybackService.kt   # Media3 后台播放服务
│   └── PlayerManager.kt            # 播放控制器封装
└── ui/
    ├── home/                       # 首页模块
    ├── library/                    # 音乐库模块
    ├── create/                     # 创建模块 (BottomSheet + ViewModel)
    ├── player/                     # 全屏播放器组件
    ├── playlist/                   # 歌单详情页
    ├── search/                     # 搜索/发现页
    ├── components/                 # 通用组件 (登录、侧边栏、底部导航栏等)
    └── theme/                      # 主题配置
```

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
| `/eapi/user/subcount` | EApi | 获取用户收藏/关注数量统计 |
| `/eapi/playlist/create` | EApi | 新建歌单（支持公开/私密模式） |
| `/eapi/playlist/manipulate/tracks` | EApi | 歌单歌曲批量添加/移除 |
| `/weapi/song/play/about/block/page` | WeApi | 音乐百科简要信息（曲风、语种、BPM、影综） |
| `/weapi/song/creators` | WeApi | 获取制作团队成员（作词、作曲、编曲等） |
| `/weapi/artist/follow/count/get` | WeApi | 获取歌手粉丝数量（避开 EApi 0 字节风控） |
| `/eapi/song/like` | EApi | 喜欢/红心歌曲操作 |
| `/eapi/song/like/get` | EApi | 获取当前用户已红心的歌曲 ID 列表 |
| `/eapi/v1/resource/comments/{threadId}` | EApi | 获取歌曲评论列表（含热门评论与普通评论） |

---

## 变更日志

### 2026-04-22 — 网络层与加密基础

- **NeteaseCrypto.kt**: 实现 WeApi (AES-CBC + RSA)、LinuxApi (AES-ECB)、EApi (MD5 + AES-ECB) 三路加密逻辑。
- **CryptoInterceptor.kt**: OkHttp 拦截器，自动根据请求路径执行对应的加密路由。
- **NeteaseApiService.kt**: 定义 Retrofit 接口，包含登录、每日推荐等核心业务。
- **NetworkModule.kt**: 配置 Koin 网络单例注入链，设置 15s 超时。

### 2026-04-23 — 仓储层与首页加载

- **MusicRepository.kt**: 封装 Flow 数据流，处理网络拦截与序列化异常。
- **HomeViewModel.kt**: 采用 StateFlow 驱动 UI，实现首页数据自动拉取与错误重试。
- **EmptyBodyInterceptor.kt**: 探测空响应体并抛出 ApiException，增强系统鲁棒性。
- **HomeScreen.kt**: 基于 LazyVerticalGrid 实现推荐歌单展示，接入 Coil 图片加载。

### 2026-04-24 — 播放引擎集成 (Media3)

- **MelodiaPlaybackService.kt**: 实现后台持续播放服务，绑定 MediaSession。
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
- **视觉优化**: 增加主页面平移时的动态圆角过渡（0dp → 32dp）与边缘阴影。

### 2026-05-10 — 首页历史日推

- **历史日推接口**: 新增 `/weapi/discovery/recommend/songs/history/recent` 与 `.../history/detail`，通过 WeApi 路由访问（黑胶 VIP 专属）。
- **HistoryRecommendSheet**: 日推卡片新增「历史」入口，弹出 BottomSheet 展示可用日期与对应歌单。
- **HomeFeedData 扩展**: 新增 `dailySongs`、`recentPlaylists` 字段，首页加载并发数提升至 4 路 async。

### 2026-05-15 — 首页排行榜与性能优化

- **ToplistInfo 领域模型**: 定义于 `MusicRepository.kt`，包含 id、name、coverUrl、updateDesc、topSongs（前三首）。
- **EApi 路由**: `/eapi/toplist/detail` 替代 WeApi 版本（后者对 PC 端特征返回 0 字节），成功绕过服务端风控。
- **ToplistCarousel + ToplistCard**: 首页新增 Spotify 风格深色榜单卡片，含封面渐变蒙版与前三名歌曲列表。
- **首页布局顺序**: 个性化歌单 → 私人雷达 → 最近播放 → 今日推荐 → 排行榜 → 你最爱的艺人。
- **Coil 性能优化**:
  - `MelodiaApplication.kt`: 即时构建 `ImageLoader` 并赋值（非 lazy），将 `DiskLruCache.initialize()` 移至后台线程，消除启动时 762ms 锁竞争。
  - 添加 `decoderDispatcher(Dispatchers.IO.limitedParallelism(4))` 与 `fetcherDispatcher(...(8))`，根治 HWUI 解码日志丢失警告。
  - MemoryCache/DiskCache 上限分别设置为堆内存 15% 和磁盘空间 2%。
- **列表懒加载**: `DailyRecommendCard` 从 `Column+verticalScroll` 重构为 `LazyColumn+itemsIndexed`；所有 `LazyRow` 均添加 `key = { it.id }` 避免不必要的 recomposition。

### 2026-05-16 — 你最爱的艺人、歌单界面重构与全局浮动播放器

- **ArtistInfo 领域模型**: 定义于 `MusicRepository.kt`，包含 id、name、avatarUrl。
- **API 策略**: 优先调用 `/eapi/artist/sublist` 获取已关注歌手，未登录时自动降级至 `/eapi/artist/top` 热门歌手兜底。
- **歌单界面**:
  - 引入 `WindowInsets.statusBars` 适配 Edge-to-Edge，修复顶栏与系统状态栏重叠。
  - 隐藏式搜索栏：`initialFirstVisibleItemIndex = 1` 控制首视区，实现下拉滑出搜索框交互。
  - 动态色彩提取：配置 Coil `allowHardware(false)`，使用手写 10×10 像素采样与 HSV 自适应算法抓取封面主色调，统一作用于背景渐变和 TopBar。
  - 折叠动效 (Binary Snap)：`progress >= 0.8f` 时瞬间切换，头部信息与 TopBar 标题同步显隐。
  - 品牌颜色统一：全面替换 `SpotifyGreen` 为 `NeteaseRed`。
- **BottomFloatingIsland 提升**: 从 `HomeScreen` 移至 `MainActivity`，使播放器悬浮于所有界面之上。
- **hazeState 全局化**: `HazeState` 在 `MainActivity` 层创建，通过 `.haze(hazeState)` 统一应用毛玻璃效果。

### 2026-05-17 — 发现/搜索全链路、音乐库、侧边栏与动画

- **SearchScreen.kt**:
  - 集成 `/eapi/homepage/block/page` 动态网格布局与 `/eapi/search/defaultkeyword/get` 动态占位符。
  - 沉浸式适配：`statusBarsPadding()` 避开状态栏，`160.dp` 底部内边距避让全局播放器。
  - 分类导航：横向滚动分类入口，图标统一使用 `NeteaseRed` 透明底色方案。
- **氛围背景光**: 主页使用 `Canvas + RadialGradient` 绘制 `NeteaseRed (25% Alpha)` 光晕，配合 `Brush.verticalGradient` 遮罩使其在屏幕中部平滑淡入背景色。
- **云搜索**: 接入 `/eapi/cloudsearch/pc`，支持 400ms 防抖、分页加载与当前播放曲目高亮；`SearchResultsList` 使用 `derivedStateOf` 实现距底 5 条自动触发 `loadMore()`。
- **热搜榜**: 接入 `/eapi/hotsearchlist/get`，排名 1-3 使用 `NeteaseRed + FontWeight.Bold`；支持点击直接填入并触发搜索。
- **精品歌单标签**: 并行调用标签与歌单接口，`PlaylistTagCard` 2 列网格布局展示封面渐变蒙版 + 标签名。
- **搜索入口差异化**: `autoFocus` 参数区分"从主页进入"（自动弹键盘）与"从导航栏进入"（展示发现内容）。
- **音乐库 (LibraryScreen)**:
  - 并发拉取用户歌单、收藏专辑、关注歌手，归一化为统一 `LibraryItem`。
  - 过滤药丸、本地置顶（`SharedPreferences`）、排序（最近播放/创建时间/字母）。
  - 展开式搜索框前端过滤，新建歌单调取 `/eapi/playlist/create`。
  - "已点赞的歌曲"渲染蓝红流光渐变背景 + 白色心形图标。
- **全局侧边栏重构**: 手势容器与状态提升至 `MainActivity`，侧边栏滑出时主内容层整体平移并附加 `0.dp → 32.dp` 圆角裁剪与 `0f → 30f` 外层投影，呈现浮雕式折叠动效。
- **AnimatedContent 转场升级**: 前进导航（新页面淡入 + 上滑 40px，旧页面淡出 + 上移 40px），退出动画 200ms 先完成，进入动画延迟 100ms 后以 300ms 展开，消除双屏半透明白色闪屏。
- **搜索页顶栏重构**: 标题栏包含用户头像（`AsyncImage`）、"搜索"粗体标题（24sp）、听歌识曲入口图标；`SearchViewModel` 新增 `userProfile: StateFlow<UserProfile?>` 驱动头像显示。
- **视图切换**: "最近播放"与音乐库均支持列表/网格切换（3×3 大封面网格），状态通过 `remember` 保存。

### 2026-05-18 — 创建界面

- **CreateBottomSheet**: 底部导航栏"创建"按钮弹出 `ModalBottomSheet`，包含四项创建入口（新建歌单/导入外部歌单/上传本地音乐/发起一起听）。
- **CreateViewModel**: 注入 `MusicRepository`、`PlayerManager`、`UserPreferences`，暴露 `toastEvent` 通知创建结果。
- **新建歌单对话框**: `AlertDialog` 包含名称输入框和隐私歌单 `Switch` 开关；创建过程中显示 `CircularProgressIndicator`，防止重复提交。
- **Koin 注册**: `ViewModelModule` 新增 `viewModelOf(::CreateViewModel)`。

### 2026-05-19 — 全屏播放器视觉重构

- **嵌入式播放页顶栏**: 返回箭头、播放来源文本和更多选项直接嵌入 `CoverArt` 封面顶部，随滑动做平滑上移、缩放与渐变。
- **氛围化滚动顶栏**: 滚动隐藏封面时，顶栏背景统一为歌曲主色调，歌曲信息靠左对齐，右侧新增收藏爱心与播放控制按钮。
- **卡片展示顺序**: 歌曲信息 → 播放控制 → 歌词 → 关于艺人 → 相似艺人 → 艺人专辑 → 制作人。
- **歌词 HSV 渐变**: 实现 `Color.toOpaqueHsv` 进行 HSV 色度修正（增强饱和度与亮度调节），重构歌词渐变蒙版背景。
- 重新接入播客胶囊，恢复三胶囊分类布局。

### 2026-05-20 — 全屏播放器视觉精修 Phase 5

- **毛玻璃 TopBar (Haze)**: `HazeState` 挂载于 LazyColumn，`hazeChild` 滚动展开时激活（`blurRadius = 24.dp`，`tint = BackgroundDark 50%`）。
- **歌词动画高亮**: 当前行 22sp `ExtraBold` 高光色，非当前行 16sp `Medium` alpha 随距离递减；`animateFloatAsState` 控制 alpha/fontSize，纯音乐自动隐藏歌词卡片。
- **鲜艳度优先色彩提取**: `ColorExtraction.kt` 重构为 `score = S × V` 逐像素评分，确保提取最鲜艳颜色而非面积最大颜色。
- **封面滚动缩放**: `derivedStateOf + LazyListState.layoutInfo` 计算封面滚出比例，通过 `graphicsLayer` 实现 1.0 → 0.85 渐进缩放。
- **播放按钮弹性动画**: `Animatable + spring(dampingRatio=0.4f, stiffness=400f)` 实现播放/暂停切换时的回弹效果（0.85 → 1.0）。
- **非歌词卡片底色统一**: 关于艺人、相似艺人、艺人专辑等卡片使用 `SurfaceDark` 固定底色。

### 2026-05-22 — 播放队列 UI 优化与纯音乐歌词

- **PlayQueueSheet**: 歌曲行改为完全不透明暗色背景，隐藏 `SwipeToDismiss` 红色底层；保留三条杠拖动手柄，移除尾部垃圾桶图标，改由左滑触发删除。
- **纯音乐适配**: `MusicRepositoryImpl` 检测到无歌词或"纯音乐"关键字时返回标示行；播放器检测到时隐藏大型歌词卡片，小歌词位置显示"纯音乐"。
- **进度条游标居中**: 20.dp Box 容器包裹白球并设置 `contentAlignment = Alignment.Center`，解决 M3 Slider 游标对齐偏上问题。

### 2026-05-23 — 歌曲详情卡片、红心收藏与精选评论

- **SongDetailCard**: 并发请求歌曲详情、百科摘要 (`/song/wiki/summary`) 和创作者列表 (`/song/creators`)，所有属性提供兜底防御避免崩溃。展示曲风、专辑、语种、发行时间、BPM、制作人、影综等属性。
- **artistFansCount**: 实现 `/weapi/artist/follow/count/get`，解决 EApi 在特定 Cookie 下粉丝数返回 0 的风控缺陷。
- **歌曲红心同步**: 对接 `/eapi/song/like` 与 `/eapi/song/like/get`，顶栏收藏图标点击时触发微动画与状态反转，失败时自动回滚。
- **CommentsCard**: 调用评论接口聚合精选热门评论与最新评论，渲染用户头像、发布时间、评论内容及点赞数。

### 2026-05-24 — 歌词对齐、进度条精修与歌曲百科

- **歌词卡片对齐修正**: 移除居中 Box，主歌词与翻译文本重置为 `TextAlign.Start`；`graphicsLayer` 的 `transformOrigin` 设为左侧边缘 `TransformOrigin(0f, 0.5f)`，高亮行缩放时行首锁定。
- **歌词卡片纵横比**: 弃用 `aspectRatio(1f)`，改为 `cardHeight = cardWidth * 0.88f`。
- **播放来源栏双行排版**: 首行显示来源类型（11sp 白 60% 透明度），第二行显示具体名称（14sp 纯白加粗），加双引号包裹。
- **进度条对齐与时长兜底**: 水平边距收窄为 `24.dp`；`Track` DTO 增加 `val dt: Long = 0`；缓冲期间如播放器时长为 0，自动降级采用 `songDetail.dt` 兜底。
- **切歌进度闪烁修复**: `fetchUrlAndPlay` 加载新歌时立即重置 `currentPosition/duration`；`onMediaItemTransition` 时强制设定进度为 0；轮询器增加 `Player.STATE_READY` 状态过滤。
- **歌曲百科扩展**: 从 `SONG_PLAY_ABOUT_WIKI` 模块抽取 `background`（背景故事）与 `awards`（所获奖项）字段并展示。
- **制作团队名单扩展**: 抓取 API 返回的所有幕后角色（制作人、混音师、吉他手等）并排版展示。
- **AboutArtistCard 调优**: 顶部艺人图片高度从 180dp 调大至 260dp。

### 2026-05-27 — 悬浮舱显隐控制、WebView 登录闪烁修复与歌词空状态动效

- **WebView 登录**: 移除花哨的 JS 注入和自动点击流，回归纯净原生网页登录；外层 Compose 容器与 WebView 背景色同步设置为 `#F5F5F7`，彻底解决软键盘弹出时的白屏/暗色闪烁问题。
- **悬浮舱显隐**: `WebViewLoginScreen` 暴露 `onLoginScreenVisibilityChanged` 回调，`MainActivity` 通过 `AnimatedVisibility` 在登录界面展示时平滑淡出 `BottomFloatingIsland`。
- **歌词空状态动效**: 小歌词为空时展示三个大原点呼吸/波浪加载动效（`LoadingDotsAnimation`），固定高度 20dp 防止布局垂直跳变。

### 2026-05-29 — 侧边栏设置与隐私功能

- **SettingsPreferences.kt**: 新建本地设置持久化存储层，Wi-Fi 音质（`wifi_quality`）与移动网络音质（`mobile_quality`）独立存储。
- **MusicRepositoryImpl**: 注入 `Context` 并增加 `isWifiConnected` 动态监测，加载播放链接时自适应拼装对应音质规格。
- **SettingsViewModel**: 实现昵称可用性检查的 500ms 协程防抖；"清理缓存"同时清理 Coil 缓存与 ExoPlayer/Media3 媒体缓存（递归删除 `cacheDir` + `externalCacheDir`）。
- **SettingsScreen**: 沉浸式深色极简设置页，Wi-Fi 与移动网络音质独立配置；根据 `vipType` 渲染黑胶黑金 / 蓝色音乐包 / 灰色未激活三种 VIP 状态卡片。
- **LibraryScreen**: 接入 `SharedPreferences` 记忆"列表/网格"视图切换状态；迁移废弃的 `Icons.Rounded.List` 等图标至 `AutoMirrored` 规范。
- **PlaylistScreen**: 对接 `/eapi/playlist/manipulate/tracks` 支持批量添加/移除歌曲；引入 VIP 红色微标；"爱心按钮"点击未登录时拉出 WebView 登录面板，已登录时弹出收藏至歌单弹窗（Checkbox 批量操作）。

### 2026-05-30 — 代码清理与 Android MD3 底栏重构

- **废物代码清理**: 移除 `CreateBottomSheet.kt` 未使用导入；迁移 `HomeScreen.kt` 废弃的 `Icons.Rounded.List` 至 `AutoMirrored`；移除 `ArtistScreen.kt` 中基于 `java.util.Random` 的虚假播放量逻辑。
- **全局防遮挡审查**: 所有核心滚动页面 `LazyColumn` 底边距统一规范为 `180.dp`，防止悬浮播放舱遮挡底部内容。
- **歌手页全部专辑**: 新增"显示全部"按钮，`ModalBottomSheet + LazyColumn` 展示完整专辑网格，首次拉取上限提升至 50 张。
- **MelodiaBottomBar.kt 新建**:
  - `MelodiaNavigationBar`: 紧凑型底栏（高度 62dp），内置 `383A4A` 药丸状选中指示器，"创建"按钮强制为未选中状态。
  - `MiniPlayerCard`: 采用 `Color(0xFF23232C)` 纯色背景 + 半透明白边框；支持实时色彩提取（关闭硬件位图 `allowHardware(false)` 解锁 GPU 取色）；封面 URL 移除尺寸参数加载高清大图，与全屏播放器配色完全同步。
- **主容器整合**: `MainActivity` 底部 Column 集成 `CreatePopupMenu`、`MiniPlayerCard` 和 `MelodiaNavigationBar`；清理 `HomeScreen.kt` 废弃的浮岛与导航项代码，彻底移除 `WelcomeBanner`。
- **首页精简**: 缩减 `TopGreetingBar` 与 `FilterPills` 间距至 8dp；移除 `ToplistCard` 下方"前三首"列表，实现高密度紧凑排版。

### 2026-06-04 — 播放器手势系统调优与 UI 优化

- **层级拦截修复**: 重构 `FullPlayerScreen` 手势下拉关闭逻辑。废弃播放器内部的位移动画，改为将拉拽 `offset` 与 `velocity` 实时回调给外层 `MainActivity`，由主容器接管后续弹簧动画，解决底层手势被不可见容器遮盖拦截的问题。
- **视觉圆角反馈**: 在主界面为全屏播放器容器增加基于 `playerOffsetY` 的动态 `clip` 剪裁。确保播放页在被向上拉起展开、或向下拖拽收起时，顶部始终呈现完美的 `24.dp` 圆角过渡。
- **MiniPlayerCard 固定**: 移除了悬浮播放卡片自身的位移变换，使其在手势拖拽触发全屏播放器展开的过程中，始终固定在底部导航栏上方不动。
- **移除透明度渐变**: 移除了拖拽全屏播放器时背景的 `alpha` 透明度渐变效果，确保拖动过程始终保持不透明。

### 2026-06-06 — 逐字歌词解析、IPC 恢复、对比度重构与更多选项弹窗

- **IPC 状态恢复修复**: 解决应用进程重启/恢复时，Media3 接口 Binder 通信导致 `MediaItem.mediaMetadata.extras` 被清空进而使 `PlayerViewModel` 在 `observeTrackChanges()` 中读到 `-1L` 无法触发歌词与歌曲详情请求的问题。改为使用 IPC 稳定传输的 `mediaId.toLongOrNull()` 解析 ID。
- **YRC 绝对时间戳对齐**: 修正 `parseYrc()` 中单字偏移量计算。原格式中 `(absoluteTime, duration, 0)` 第一个参数是绝对时间戳，现改为减去行首时间 `lineStartTime` 换算为相对偏移量，从而使 UI 层 `calculateProgressFraction()` 能够正确计算出扫色进度。
- **逐字染色视觉重构**: 在 `FullScreenLyricsView` 中将当前歌词行的未唱字颜色 `inactiveColor` 从极高亮度的 85% 亮白（`highlightColor`）重构为 Spotify-like 的 `Color.White.copy(alpha = 0.35f)` 半透明白色。极大地增强了未唱部分（35% 透明白）与已唱部分（100% 纯白）的视觉对比度，使卡拉 OK 逐字扫色效果格外醒目清晰。
- **逐字歌词性能与几何对齐优化**: 重构了 `KaraokeLyricRow`，使用底层未激活 `Text` 与顶层高亮 `Text` + `Modifier.graphicsLayer` 在 Draw 阶段执行动态矩形裁剪，彻底消除了每帧刷新导致的 Recomposition、Measure 与 Layout 耗时；通过 `TextLayoutResult` 的 `getBoundingBox` 获取字的物理像素边界进行进度计算，解决了中英文混排时字数比例与物理几何宽度不匹配导致的扫色跳跃问题。
- **播放器更多选项底部弹窗**: 新增 `SongMoreOptionsSheet.kt`。顶部展示封面、歌名与歌手名，中部网格包含收藏（联动真实红心状态）、下载、分享（Android 原生分享通道）与一起听；底部列表项支持专辑名展示、歌手信息及关注/已关注同步控制、查看歌曲百科（自动收起并平滑滚动到百科卡片位置）及其他音质音效等精修选项，对未开发功能在真实 UI 中做合理不可用或 Toast 提示。
### 2026-06-07 — 定时播放优化、播放器重定向修复与音质即时切换

- **定时关闭界面展开与滚动优化 (`FullPlayerScreen.kt`)**：
  - 声明了 `skipPartiallyExpanded = true` 的 `sheetState`，使得定时关闭弹窗默认完全展开，避免下部卡片被隐藏。
  - 给定时关闭主布局添加了 `.verticalScroll` 及 `navigationBarsPadding()`，彻底修复了在矮屏幕设备或极窄屏幕区域下，小白条遮挡/截断 Slider 与确定按钮的问题。
- **ExoPlayer 重定向修复 (`MelodiaPlaybackService.kt`)**：
  - 构建了启用 `setAllowCrossProtocolRedirects(true)` 的 `DefaultHttpDataSource.Factory` 并包装成 `DefaultDataSource.Factory`，注册入 `DefaultMediaSourceFactory` 并注入给 `ExoPlayer`。
  - 完美解决了由于网易云 CDN 链接从 HTTPS 重定向到 HTTP 协议导致的 ExoPlayer 抛出 `Response code: 302` 的播放中断崩溃问题。
- **音质选择、切换与即时重载 (`PlayerManager.kt`、`PlayerViewModel.kt` 和 `SongMoreOptionsSheet.kt`)**：
  - `PlayerManager.kt` 新增公有方法 `reloadCurrentTrack()`，支持在保存当前播放进度的前提下，取消原请求并以新音质重载播放歌曲。
  - `PlayerViewModel.kt` 引入 `SettingsPreferences` 和 `Context`，动态感应 WiFi/移动网络状态，产出融合后的 `activeQuality` 状态并支持持久化和触发即时重载。
  - `SongMoreOptionsSheet.kt` 完善了“音质”选项交互。点击呼出的音质选择弹窗（支持标准、极高、无损、Hi-Res、超清母带单选）重构为与定时播放完全一致的 `ModalBottomSheet`，其文本和 VIP 红色微标可依据活动网络下的音质状态进行动态更新。

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
- [x] 视图切换 (最近播放/音乐库支持列表与 3×3 网格切换)
- [x] 创建界面 (BottomSheet 菜单 + 新建歌单 + 隐私开关 + 播放上下文)
- [x] 全屏播放器精修 (毛玻璃TopBar + 歌词动画 + 封面缩放 + 弹性按钮)
- [x] 鲜艳度色彩提取 (S×V 评分算法 + 纯 HSV 渐变)
- [x] 歌曲详情接口 (曲风/专辑/语种/发行时间/BPM/制作/影综/红心/评论/粉丝数聚合)
- [x] 设置页面 (音质配置 + VIP 状态 + 缓存清理 + 昵称修改 + 头像上传)
- [x] 歌单批量操作 (添加/移除歌曲 + 收藏弹窗)
- [x] 歌词解析与同步显示
- [x] 统一错误处理分发

