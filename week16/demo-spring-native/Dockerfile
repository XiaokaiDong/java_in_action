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

