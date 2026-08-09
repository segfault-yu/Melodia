# Melodia 业务域优先分层重构规划

> 临时规划文档，用于确认后再动代码。方案落地过程中会持续更新为进度清单；全部域迁移完成、旧结构彻底清理后可以删除或归档进 `CODEREADME.md`。
> 分支：`refactor/feature-domain-layering`（已从 `main` 切出）。本分支的提交不附加 Co-Authored-By 署名。

---

## 1. 现状问题（有数据支撑）

- `domain/model/` 目录事实上是空壳：只有一个 64 行的 `HomeFeedModels.kt`，绝大多数领域模型（`ToplistInfo`、`ArtistInfo`、`PersonalizedData`…）直接定义在 `data/repository/MusicRepository.kt` 里。
- `MusicRepositoryImpl.kt`（1091 行）+ `MusicRepository.kt`（248 行）是纵贯 8 个业务域的"上帝仓库"：51 个方法里，能明确归到单一业务域的不到一半，其余都是 2-4 个域共用。
- `NeteaseApiService.kt`（1537 行）内部虽然有 30 个 `// ===== xxx =====` 分组注释，但分组粒度不统一、有分类错误（`getHomepageBlocks` 被归进"用户信息"组）、有游离方法（`likeComment` 不在任何分组里）。
- DI 层（`di/RepositoryModule.kt`）只做了一件事：`MusicRepositoryImpl` 绑定到唯一的 `MusicRepository` 接口。8 个 ViewModel + `PlayerManager`（播放引擎单例）全部注入同一个巨石接口，没有任何一个消费者只依赖自己实际用到的子集。
- 项目当前是单 Gradle module（只有 `:app`），本次重构**只做目录/包结构调整，不做多 module 拆分**——但目录边界要为将来的 module 化留好界面。

---

## 2. 目标目录结构

```
com.lin0721.linmusic/
├── MelodiaApplication.kt
├── MainActivity.kt
│
├── core/                              # 真正跨域共享的基础设施
│   ├── network/                       # Retrofit/OkHttp 基础设施
│   │   ├── crypto/                    # NeteaseCrypto, CryptoInterceptor
│   │   ├── ApiException.kt
│   │   ├── EmptyBodyInterceptor.kt
│   │   └── HeaderInterceptor.kt
│   ├── api/
│   │   └── NeteaseApiService.kt       # 保留单一 Retrofit 接口文件（见 §6 说明）
│   ├── auth/                          # 登录态 + 当前用户信息（跨域共享，见 §5 待确认①）
│   │   ├── UserPreferences.kt         # 从 data/local 迁入
│   │   └── AuthRepository.kt          # getAccountInfo / logout / cookies
│   ├── contentfilter/                 # blockedArtistIds 过滤能力（见 §5 待确认②）
│   ├── log/                           # AppLogger, CrashHandler
│   ├── ui/
│   │   ├── theme/                     # Color/Theme/Shapes/Spacing/Type/ColorExtraction
│   │   └── components/                # SongRow/DragHandle/FilterChipsRow/ToastManager/…
│   └── di/                            # 仅保留 NetworkModule/LocalModule 中真正公共的部分
│
└── feature/
    ├── home/
    │   ├── data/HomeRepository.kt(+Impl)
    │   ├── domain/                    # PersonalizedData/RecentPlayItem/ToplistInfo/HomeFeedPage…
    │   └── ui/HomeScreen.kt, HomeViewModel.kt, HomeUiState.kt, components/
    ├── search/
    │   ├── data/SearchRepository.kt(+Impl)
    │   ├── domain/                    # SearchSongsResult/HotSearch/PlaylistTag…
    │   └── ui/SearchScreen.kt, SearchViewModel.kt
    ├── library/
    │   ├── data/LibraryRepository.kt(+Impl)
    │   ├── domain/                    # AlbumSubItem/UserSubcountResponse…
    │   └── ui/LibraryScreen.kt, LibraryViewModel.kt
    ├── playlist/                      # 承载歌单详情/专辑详情/每日推荐/听歌排行/历史日推 4 种"列表详情"页
    │   ├── data/PlaylistRepository.kt(+Impl)
    │   ├── domain/                    # PlaylistDetail/UserPlaylist…
    │   └── ui/PlaylistScreen.kt, PlaylistViewModel.kt, PlaylistUiState.kt
    ├── artist/
    │   ├── data/ArtistRepository.kt(+Impl)   # 含 getFavoriteArtists（见 §5 待确认③）
    │   ├── domain/                    # ArtistDetailInfo/ArtistAlbum/ArtistInfo…
    │   └── ui/ArtistScreen.kt, ArtistViewModel.kt
    ├── player/
    │   ├── data/
    │   │   ├── PlayerRepository.kt(+Impl)     # getLyrics/getSongWiki/getSongDetail/getSongUrl/getSimilarSongs/getIntelligenceSongs
    │   │   └── PlaybackPreferences.kt         # 从 data/local 迁入，天然属于 player
    │   ├── domain/                    # LyricLine/SongWikiData/QueueItem/PlayMode…
    │   └── (原 player/ 包内容原地保留：PlayerManager/MelodiaPlaybackService/BluetoothReceiver/FloatingLyricService/AudioCacheManager)
    │       ui/FullPlayerScreen.kt, PlayerViewModel.kt, PlayerComponents.kt, LyricsComponents.kt, CommentsComponents.kt, …
    ├── settings/
    │   ├── data/SettingsRepository.kt(+Impl)  # getUserLevel/getVipInfo/getUserBindings/updateUserProfile/checkNickname/dailySignin/uploadAvatar
    │   └── ui/SettingsScreen.kt, SettingsViewModel.kt, 各二级设置页
    ├── create/
    │   └── ui/CreateBottomSheet.kt, CreateViewModel.kt         # 依赖 playlist 的 CreatePlaylistUseCase（见 §5 待确认④）
    └── comment/
        ├── data/CommentRepository.kt(+Impl)   # getComments/likeComment
        └── domain/                    # CommentsResponse…
```

跨域依赖只允许 `feature.X` 依赖 `core.*` 和其他具体的 `feature.Y.data`（通过接口），禁止 `core` 反向依赖任何 `feature`。

---

## 3. 每个域仓储切片的方法归属（基于真实调用点，非猜测）

| 域 | 归属方法 | 消费者证据 |
|---|---|---|
| home | `getPersonalizedPlaylists`、`getRecentPlaylists`、`getToplistDetail`、`getDailyRecommendSongs`、`getHistoryRecommendDates/Detail` | `HomeViewModel`；后两者也被 `PlaylistViewModel`（id=-1 每日推荐详情页）复用 |
| search | `getDefaultSearchKeyword`、`searchSongs`、`getHotSearches`、`getPlaylistTags` | `SearchViewModel` 独占 |
| library | `getCollectedAlbums`、`getUserSubcount`、`getUserPlaylists`、`getUserRecord` | `LibraryViewModel`；`getUserPlaylists` 也被 `ArtistViewModel`/`PlaylistViewModel`（"加入歌单"选择器）复用 |
| playlist | `getPlaylistDetail`、`getAlbumDetail`、`subscribePlaylist`、`manipulatePlaylistTracks` | `PlaylistViewModel`；后两者也被 `ArtistViewModel` 复用 |
| artist | `getArtistTopSongs`、`getArtistDetail`、`getArtistAlbums`、`getArtistFansCount`、`subscribeArtist`、`checkArtistFollowed`、`getSimilarArtists`、`getFavoriteArtists` | `ArtistViewModel`；除 `getArtistTopSongs`/`getFavoriteArtists` 外，其余全部也被 `PlayerViewModel`（播放页内嵌歌手迷你卡片）复用 |
| player | `getLyrics`、`getSongWiki`、`getSongDetail`、`getIntelligenceSongs`、`getSongUrl`、`getSimilarSongs` | `PlayerViewModel`；`getSongUrl`/`getSimilarSongs` 还被 **`PlayerManager`**（非 UI 单例播放引擎）直接依赖 |
| settings | `getUserLevel`、`getVipInfo`、`getUserBindings`、`updateUserProfile`、`checkNickname`、`dailySignin`、`uploadAvatar` | `SettingsViewModel` 独占 |
| create | `createPlaylist` | 语义主人是 `CreateViewModel`，但目前被 `ArtistViewModel`/`LibraryViewModel`/`PlaylistViewModel` 各自内联复制了一遍"建歌单+加当前歌曲"流程 |
| comment | `getComments`（两个重载）、`likeComment` | `PlaylistViewModel`（歌单详情评论 Tab）+ `PlayerViewModel`（播放页评论 Tab） |
| core/auth | `getAccountInfo`、`logout` | `HomeViewModel`/`ArtistViewModel`/`LibraryViewModel`/`PlaylistViewModel`（头像昵称展示）+ `SettingsViewModel`（登出） |

---

## 4. 迁移阶段与顺序

- **阶段 0（本次立即执行）：骨架 + home 试点**
  1. 建 `core/` 顶层包，先把 `ui/theme/`、`ui/components/`、`core/log/`、`data/remote/network/`、`data/remote/crypto/` 平移进去（纯移动+改 import，不改行为）。
  2. 建 `feature/home/`，把 `HomeRepository`（从 `MusicRepository` 里按 §3 表格拆出 home 的方法）、`HomeViewModel`/`HomeUiState`/`HomeScreen` 迁入，`ui/home/HomeFeedModels.kt` 迁到 `feature/home/domain/`。
  3. 验证：`./gradlew assembleDebug` 编译通过 + 手动跑一遍首页（个性化推荐/最近播放/排行榜/每日推荐/最爱歌手/头像展示）无回归。
  4. 提交一次 commit，作为后续 7 个域的迁移模板参照。
- **阶段 1-7：按 search → library → artist → comment → playlist → player → settings → create 顺序逐域迁移**（顺序理由：先做依赖少的叶子域，`playlist`/`player` 依赖别的域较多，放后面；`create` 依赖 `playlist` 的 UseCase，放最后）。每个域一个独立提交，迁移完立刻编译+手测再进入下一个域。
- **阶段 8：清理**——`MusicRepository.kt`/`MusicRepositoryImpl.kt` 应该被拆空，删除该文件；`di/RepositoryModule.kt` 拆成每域一个 Koin module 或合并进各 `feature` 包内的 `di.kt`；确认没有遗留 import 指向旧路径。

**每个阶段结束后是否合并回 `main`，由你决定**（阶段 0 跑通后我会再和你确认一次节奏，不会自作主张连续做完 8 个域）。

---

## 5. 待拍板点 —— 已确认（2026-08-09，全部按推荐方案执行）

1. **auth → 独立放 `core/auth`。**
2. **blockedArtistIds → 抽到 `core/contentfilter`。**
3. **`getFavoriteArtists` → 归 artist 域**，home/library 依赖 `feature.artist.data.ArtistRepository` 读取。
4. **`createPlaylist` 四处复制 → 本次不收敛。** 只做物理搬迁，各域调用点行为保持完全不变；"抽成公共 UseCase 消灭重复代码"记为后续独立任务，不在本次范围内。
5. **两处死代码（`getHomepageBlocks`/`getDiscoveryBlocks`/`getDailyRecommendPlaylists`）→ 保留，不删除。** 原样迁移到对应域（`getHomepageBlocks`/`domain/model` 里的 `HomeFeedPage` 系列迁入 `feature/home/domain`；`getDiscoveryBlocks` 迁入 `feature/search`；`getDailyRecommendPlaylists` 迁入 `feature/home/data`），标注"当前无调用者"，删除是不可逆操作，留给后续任务处理。
6. **两份重复"播放模式"存储 → 本次不清理，只搬物理位置，行为不变。** 避免牵连仍未修复的 shuffle bug。
7. **`PlayerManager` 精简依赖 → 在 player 域迁移阶段（阶段 6）顺势完成，而非跳过。** 因为 `getSongUrl`/`getSimilarSongs` 本来就要从 `MusicRepository` 移到 `feature.player.data.PlayerRepository`，`PlayerManager` 必然要跟着换依赖类型，这不是"额外收益型"改动而是拆分的必要一步。执行时**只改 DI 接线（构造函数参数类型 + Koin module），不改动 `PlayerManager.kt` 里任何与 shuffle bug 相关的内部逻辑行**（`collectLatest`/`applyMode`/`saveQueueState` 等维持原样，只是方法调用对象换了）。

---

## 6. 本次不做的事（明确排界）

- 不做 Gradle 多 module 拆分（`settings.gradle.kts` 仍只有 `:app`），只做包目录调整。
- `NeteaseApiService.kt` 这个 Retrofit 接口文件本身**不按域物理拆分**成 8 个文件——Retrofit 接口是纯声明，拆分收益远低于拆分 Repository，仍作为 `core/api` 下的单一文件保留（如果你希望连它也按域拆，请告诉我，工作量会显著增加）。
- 不修复 shuffle 播放模式竞态 bug（已按你的决定搁置，还原了调试日志）。
- 不改变任何 UI 交互行为或视觉表现——这是一次纯结构性重构，功能行为在每个阶段都要保持 100% 一致。

---

## 7. 进度 —— 全部完成

- [x] §5 七个待确认点已按推荐方案确认（2026-08-09）
- [x] 阶段 0 骨架：`core/ui/theme`、`core/ui/components`、`core/network`、`core/network/crypto`、`core/api` 迁移完成
- [x] `core/auth`（UserPreferences + AuthRepository）拆出
- [x] `core/contentfilter`（ContentFilter.filterBlockedArtists）拆出，8 处重复过滤逻辑收敛
- [x] `feature/home`（试点域）
- [x] `feature/search`
- [x] `feature/library`
- [x] `feature/artist`
- [x] `feature/comment`
- [x] `feature/playlist`（含红心 getLikedSongIds/likeSong）
- [x] `feature/player`（含 PlayerManager DI 接线，只改类型未动 shuffle bug 相关逻辑）
- [x] `feature/settings`
- [x] `feature/create`（`MusicRepository`/`MusicRepositoryImpl` 已清空并删除）
- [x] `domain/model/`、`data/repository/`、`ui/<domain>/` 旧目录全部清空/删除
- [x] 全项目 grep 确认无残留 `data.repository.*` 或旧 `ui.<domain>` 包引用
- [x] `./gradlew :app:assembleDebug` 完整构建通过（非仅编译，含资源/清单/dex）
- [ ] **运行时验证未完成**：当前环境未连接 Android 设备/模拟器，用户已确认"先不验证，继续往下做"——建议合并前用真机/模拟器跑一遍主流程（登录、首页、搜索、播放、歌单收藏、心动模式、设置签到、新建歌单）

## 8. 后续可选事项（本次范围外，已在 §5 里明确搁置）

- `createPlaylist` 四处重复的"建歌单+加入当前歌曲"流程尚未收敛成公共 UseCase
- 两处死代码（`getHomepageBlocks`/`getDiscoveryBlocks`/`getDailyRecommendPlaylists`）仍保留未删
- 两份重复的"播放模式"存储（`SettingsPreferences.playMode` vs `PlaybackPreferences.playMode`）未清理
- 之前搁置的 shuffle 播放模式并发 bug 尚未修复
- 分支尚未合并回 `main`，由用户决定节奏
