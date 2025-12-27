# Java 示例运行说明

出现 `RcsApp.java` 找不到类（如 `RcsLineChartPanel`、`RcsCalculator` 等）的报错，通常是因为在 `rcs` 源码目录内直接执行 `javac RcsApp.java`，导致编译器找不到同一包下的其他源码文件。

请在 `java` 目录下用项目根作为 `sourcepath`/`classpath` 一次性编译全部源码：

```bash
# 编译到 out 目录
javac -d out $(find src/main/java -name "*.java")

# 运行 Swing 演示
java -cp out rcs.RcsApp
```

这样编译器可以发现 `src/main/java/rcs` 下的所有类，错误即可消失。

## WSL / Git Bash 运行脚本提示 "invalid option namepipefail"？

`run.sh` 需要在 Bash 下运行（因为用了 `pipefail` 等 Bash 选项）。如果你习惯执行 `sh run.sh`，或在 WSL/Git Bash 中默认调用的是 `dash`/`sh`，会看到类似 `invalid option namepipefail` 的报错。

解决方案：

```bash
# 确认用 Bash 启动
bash run.sh
```

脚本会在非 Bash 环境下自动重新进入 Bash；若系统代理设置导致 WSL 提示 “localhost 代理未镜像”，可以暂时取消 `http_proxy`/`https_proxy` 或设置 `NO_PROXY=localhost,127.0.0.1` 后再运行。
