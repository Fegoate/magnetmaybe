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
