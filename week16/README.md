# 小马哥JAVA实战营第16周作业


## 作业内容

>将 Spring Boot 应用打包 Java Native 应用，再将该应用通过 Dockerfile 构建 Docker 镜像，部署到 Docker 容器中，并且成功运行，Spring Boot 应用的实现复杂度不做要求


## 解答

在上周WIN10平台的基础上，又按照

- https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/#getting-started-buildpacks
- https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/#getting-started-native-build-tools

的指引在linux平台（Ubuntu 18.04）上成功完成了编译。第一个方法直接生成并部署一个DOCKER镜像。

>上周LINUX环境编译失败的问题有两个：
>- github不通，下载依赖失败
>- 虚拟机内存不足（主要原因）

### 现在在第二个方法的基础上自行编写Dockerfile来完成作业

第二个方法就是“Getting started with Native Build Tools”，运行如下命令成功后，会生成一个可执行文件

```bash
$ mvn -Pnative -DskipTests package
```

>运行上面命令会依赖两个包，安装命令为
>- apt install g++
>- apt install zlib1g

```bash
root@k8s-node2:/home/appuser/work/demo-spring-native# ls -lrt
总用量 69336
-rw-r--r-- 1 root root     6608 6月  16 02:06 mvnw.cmd
-rw-r--r-- 1 root root    10070 6月  16 02:06 mvnw
-rw-r--r-- 1 root root     2719 6月  16 02:06 HELP.md
-rw-r--r-- 1 root root     4344 6月  16 14:57 pom.xml
drwxr-xr-x 4 root root     4096 6月  16 21:16 src
-rw-r--r-- 1 root root     8039 6月  22 09:17 demo-spring-native.iml
-rwxr-xr-x 1 root root 70934952 6月  22 15:28 demo-spring-native
-rw-r--r-- 1 root root       33 6月  22 15:28 demo-spring-native.build_artifacts.txt
drwxr-xr-x 9 root root     4096 6月  22 15:51 target
-rw-r--r-- 1 root root     1117 6月  22 16:27 Dockerfile
```

使用ldd命令查看可执行文件demo-spring-native的依赖

```bash
root@k8s-node2:/home/appuser/work/demo-spring-native# ldd demo-spring-native
        linux-vdso.so.1 (0x00007ffcde786000)
        libstdc++.so.6 => /usr/lib/x86_64-linux-gnu/libstdc++.so.6 (0x00007f4945fbd000)
        libpthread.so.0 => /lib/x86_64-linux-gnu/libpthread.so.0 (0x00007f4945d9e000)
        libdl.so.2 => /lib/x86_64-linux-gnu/libdl.so.2 (0x00007f4945b9a000)
        libz.so.1 => /lib/x86_64-linux-gnu/libz.so.1 (0x00007f494597d000)
        librt.so.1 => /lib/x86_64-linux-gnu/librt.so.1 (0x00007f4945775000)
        libc.so.6 => /lib/x86_64-linux-gnu/libc.so.6 (0x00007f4945384000)
        libm.so.6 => /lib/x86_64-linux-gnu/libm.so.6 (0x00007f4944fe6000)
        /lib64/ld-linux-x86-64.so.2 (0x00007f494a8cb000)
        libgcc_s.so.1 => /lib/x86_64-linux-gnu/libgcc_s.so.1 (0x00007f4944dce000)
```

可以看出，依赖都是一些动态库，其中有一些是依赖包g++和zlib1g里的，故制作Dockerfile如下

```Dockerfile
# 使用ubuntu:18.04镜像作为基础镜像
FROM ubuntu:18.04

# 将工作目录切换为/app
WORKDIR /app

# 将编译好的demo-spring-native放入/app
ADD demo-spring-native /app

# 安装依赖的包
RUN  apt install g++
RUN  apt install zlib1g

# 允许外界访问容器的8080端口
EXPOSE 8080

# 设置环境变量


# 设置容器进程为demo-spring-native
CMD ["./demo-spring-native"]

```

然后构建镜像

```bash
docker build -t my_spring_native .

#查看镜像
root@k8s-node2:/home/appuser/work/demo-spring-native# docker image ls
REPOSITORY                  TAG                 IMAGE ID            CREATED             SIZE
my_spring_native            latest              cb8b29b637a4        31 minutes ago      134MB

#运行镜像
docker run -p 8080:8080 my_spring_native
....
```

本次作业内的镜像为Ubuntu下的可执行文件，工程和上周的相同

