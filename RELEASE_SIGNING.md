# Release 签名配置

`app/build.gradle.kts` 从仓库外读取签名材料，**密钥与密码永不进入版本控制**。
四项配置缺任意一项时，`release` 自动退回调试签名，`assembleRelease` 仍会产出
可安装的 `app-release.apk`，便于随时验证混淆后的行为。

## 本地配置

1. 生成 keystore（密码请自行设置，不要写进任何提交的文件）：

```bash
keytool -genkeypair -v -keystore melodia-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias melodia
```

2. 把 `melodia-release.jks` 放在仓库根目录。`.gitignore` 已包含 `*.jks` 与
   `*.keystore`，不会被提交。

3. 在 `local.properties`（已被 gitignore）末尾追加四项：

```properties
RELEASE_STORE_FILE=melodia-release.jks
RELEASE_STORE_PASSWORD=你的密钥库密码
RELEASE_KEY_ALIAS=melodia
RELEASE_KEY_PASSWORD=你的密钥密码
```

4. 打包：

```bash
./gradlew assembleRelease
```

配置生效时产出正式签名包；未生效时产出调试签名包，两者文件名相同，可通过
`keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk`
确认实际签名者。

## CI 配置

构建脚本对 `local.properties` 与环境变量同名读取，CI 侧只需注入这四个环境变量
（GitHub Actions 用 Repository secrets）。keystore 本身建议以 base64 存为
secret，在流水线里解码落盘后再指向它。

## 注意

- `melodia-release.jks` 一旦丢失，已上架的应用无法再发布更新，务必单独备份。
- 不要把密码提交到仓库、写进 CI 日志，或放入 `gradle.properties`（该文件被跟踪）。
