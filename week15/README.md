# 小马哥JAVA实战营第15周作业


## 作业内容

>通过 GraalVM 将一个简单 Spring Boot 工程构建为 Native Image，要求：
> - 代码要自己手写 @Controller @RequestMapping("/helloworld")
> - 相关插件可以参考 Spring Native Samples


## 解答

使用https://start.spring.io创建一个简单的Spring Web工程，实现题目要求的controller，然后对这个工程进行编译。

- 作业平台windows10

- 参考链接： https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/#getting-started-native-build-tools

- 编译过程

这是本次作业问题最多的地方。一开始使用Windows + Docker Desktop的组合，因为那台Windows本身就是虚拟机，而Docker Desktop for windows不论后端是WSL2还是Hyper-v组件，都需要打开虚拟化支持，只能放弃。

又尝试利用linux(Ubuntu18.04)上的docker，构建时仍然报错。

故而采用 GraalVM native build tools，即参考链接中给出的内容。

首先按照https://www.graalvm.org/docs/getting-started/windows的说明，安装GraalVM，因为是Windows平台，还需要安装MSVC 2017，特别的

> The last prerequisite, common for both distributions, is the proper Developer Command Prompt for your version of Visual Studio. On Windows the native-image tool only works when it is executed from the x64 Native Tools Command Prompt.

工程很简单，按照Spring的说明，使用如下命令构建

>$ mvn -Pnative -DskipTests package

但是多次报错。仔细查看mvn的输出，发现构建插件使用的native-image.exe路径在${GRAAL_HOME}目录中并不存在，而是存在于${GRAAL_HOME}/jre/lib中，数次尝试后，将${GRAAL_HOME}/jre/lib中的内容拷贝到${GRAAL_HOME}/lib中，构建成功

参见demo-spring-native.exe，文件体积并没有减少很多，但启动速度确实是飞快。