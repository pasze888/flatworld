# 环境 / 构建坑

> 按工作区 `AGENTS.md` §7 落点表：环境、构建、运行、工具链坑归本文；
> 已验证的 API 与领域事实见 [docs/reference/dimension-registration.md](reference/dimension-registration.md)。

## 构建环境

- **Java 版本**：系统默认 `JAVA_HOME` 指向 zulu17-jdk（Java 17），但 NeoForge 1.21.1 + Gradle 9.2 需 **Java 21**。
  本机可用 `C:\Users\lzp\scoop\apps\dragonwell21-jdk\current`（21.0.10）。构建前需 `$env:JAVA_HOME` 指向 Java 21。
- **Gradle wrapper 锁**：`~/.gradle/wrapper/dists/gradle-9.2.1-bin/.../gradle-9.2.1-bin.zip.lck`
  在沙箱下会因 workspace 外写权限被拒，报 `FileNotFoundException (... 拒绝访问)`；需用更宽沙箱权限跑
  `./gradlew runData` / `./gradlew build`（`runData` 会启动完整开发版游戏，首次较慢）。

## 长时间构建：超出命令超时上限时怎么跑

`./gradlew build` 首次（AT 或依赖变动会触发 NeoForm 重新 transform）需 1 分钟以上，`runData` 会启动
完整开发版游戏——两者都超过约 30 秒的单条命令上限，前台执行必然被中途杀掉（表现为
`Command timed out`）。改为**后台启动 + 轮询日志**：

```powershell
$env:JAVA_HOME='C:\Users\lzp\scoop\apps\dragonwell21-jdk\current'
$wd='<项目根绝对路径>'
$p = Start-Process -FilePath "$wd\gradlew.bat" -ArgumentList 'build','--console=plain' `
     -WorkingDirectory $wd -RedirectStandardOutput "$wd\build.log" `
     -RedirectStandardError "$wd\build-err.log" -WindowStyle Hidden -PassThru

# 之后每条命令控制在超时内，轮询进度（< 30 秒）：
Start-Sleep -Seconds 27; Get-Content "$wd\build.log" -Tail 20
```

- **`-WorkingDirectory` 不可省**：漏掉之后改用 `cmd /c gradlew.bat ...` 会报
  `'gradlew.bat' is not recognized as an internal or external command`——
  `Start-Process` 不继承调用方的工作目录，而 `gradlew.bat` 不在 `PATH` 里。
- 用 `-RedirectStandardOutput` / `-RedirectStandardError`，别用
  `cmd /c "... 1> out.log 2> err.log"`：后者的重定向符在参数传递过程中容易被拆散。
- `-WindowStyle Hidden` + `-PassThru` 可拿到 `pid`，用 `Get-Process -Id <pid>` 判断是否还在跑；
  日志末尾出现 `BUILD SUCCESSFUL` / `BUILD FAILED` 即终态。后台进程不会随命令结束而终止。
- 日志**写在项目根会出现在 `git status` 里**，收尾务必删除。

## 中文提交消息

Windows 上把中文直接作为参数传给 `git commit -m "<中文>"` 会被转码成乱码。改为写 UTF-8 文件后用
`-F` 读取：

```powershell
git commit -F commit-msg.txt      # commit-msg.txt 为 UTF-8（无 BOM）
```

提交消息文件**不要 `git add`**，提交完立即删除。
