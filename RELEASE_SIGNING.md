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

`.github/workflows/release.yml` 在推送 `v*.*.*` 形式的 tag 时触发正式签名构建
并发布 GitHub Release，需要在仓库 Settings → Secrets and variables → Actions
下配置以下 4 个 Repository secrets：

| Secret 名 | 内容 |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `melodia-release.jks` 的 base64 编码全文 |
| `RELEASE_STORE_PASSWORD` | keystore 密码 |
| `RELEASE_KEY_ALIAS` | 固定值 `melodia` |
| `RELEASE_KEY_PASSWORD` | key 密码 |

`RELEASE_KEYSTORE_BASE64` 在流水线里解码落盘为 `melodia-release.jks` 后，
再以同名环境变量 `RELEASE_STORE_FILE`/`RELEASE_STORE_PASSWORD`/
`RELEASE_KEY_ALIAS`/`RELEASE_KEY_PASSWORD` 注入给 `signingValue()` 读取，
和本地 `local.properties` 走的是同一套读取逻辑。

生成 base64（Windows PowerShell，输出无换行的纯 base64，直接粘贴进 Secret 即可）：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("melodia-release.jks")) | Set-Clipboard
```

（不要用 `certutil -encode`：它会在内容前后加 `-----BEGIN/END CERTIFICATE-----` 分隔行，
不是合法 base64 字符，CI 侧解码会报 `base64: invalid input`。）

发布时只需：

```bash
git tag v1.0.0
git push origin v1.0.0
```

## 注意

- `melodia-release.jks` 一旦丢失，已上架的应用无法再发布更新，务必单独备份。
- 不要把密码提交到仓库、写进 CI 日志，或放入 `gradle.properties`（该文件被跟踪）。
