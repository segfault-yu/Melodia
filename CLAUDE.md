# Melodia (LinMusic)

NetEase Cloud Music 第三方 Android 客户端，Kotlin + Jetpack Compose。

## 构建

```bash
# 编译检查
JAVA_HOME="F:/Android Studio/jbr" ./gradlew compileDebugKotlin

# 完整构建
JAVA_HOME="F:/Android Studio/jbr" ./gradlew assembleDebug
```

- compileSdk 36, minSdk 26, targetSdk 36
- Java 11 兼容

## 架构

- **单 Activity**: `MainActivity.kt` 管理 `Screen` 枚举导航 (Home/Playlist/Search/Library)，无 Jetpack Navigation
- **MVVM + Repository**: ViewModel 暴露 `StateFlow`，Repository 返回 `Flow<Result<T>>`
- **DI**: Koin 4.0，5 个模块 (network/repository/viewModel/player/local)
- **网络**: Retrofit + OkHttp，请求自动通过 `CryptoInterceptor` 按路径加密 (EApi/WeApi/LinuxApi)
- **播放**: Media3 ExoPlayer + MediaSession，`PlayerManager` 封装控制器
- **持久化**: DataStore (用户信息/播放状态)，SharedPreferences (置顶项)

## 代码规范

- 禁止 KDoc 风格多行注释，仅使用单行 `//` 在复杂逻辑处添加简短说明
- 默认不写注释，仅在 WHY 不明显时添加
- 深色主题锁定，主色 `NeteaseRed (#C20C0C)`
- 所有 LazyList 必须提供稳定 key
- 图片加载统一 `?param=NNNyNNN` 尺寸裁剪

## 目录结构

```
app/src/main/java/com/lin0721/linmusic/
├── data/remote/api/          # Retrofit 接口 + 所有 DTO (co-located)
├── data/remote/crypto/       # NetEase 加密 (AES/RSA/MD5)
├── data/remote/network/      # OkHttp 拦截器链
├── data/repository/          # Repository 接口/实现 + 领域模型
├── data/local/               # DataStore 持久化
├── di/                       # Koin 模块
├── player/                   # Media3 服务 + PlayerManager
└── ui/{home,search,library,create,playlist,player,components,theme}/
```

## 关键约定

- DTO 和领域模型均使用 `@Serializable`，序列化库为 kotlinx.serialization
- API 请求路径前缀决定加密方式: `/eapi/` → EApi, `/weapi/` → WeApi
- ViewModel 构造函数参数由 Koin `viewModelOf()` 自动注入
- UI State 使用密封类 (Loading/Success/Error) 或扁平 data class
- 全局播放器浮岛 `BottomFloatingIsland` 在 MainActivity 层，各 Screen 底部需 160dp padding 避让
- 侧边栏由 MainActivity 统一管理，各 Screen 通过 `onOpenSidebar` 回调触发

## 详细开发日志

见 [CODEREADME.md](CODEREADME.md)
