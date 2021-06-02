# 小马哥JAVA实战营第13周作业


## 作业内容

> 基于文件系统为 Spring Cloud 提供 PropertySourceLocator 实现
> 配置文件命名规则(META-INF/config/default.properties 或者 META-INF/config/default.yaml)


## 解答

> 参考1：org.springframework.cloud.config.client.ConfigServicePropertySourceLocator
> 参考2：《小马哥讲Spring核心编程思想》DynamicResourceMessageSource中监控文件变化的实现

### 整体思路

根据现有Environment中约定的配置，从配置项"${config.file}"中获取文件的路径，即作业中指定的”META-INF/config/default.properties“，然后读取文件的内容，在方法org.springframework.cloud.bootstrap.config.PropertySourceLocator#locate的实现中创建一个新的PropertySource

me.tt.locator.MyPropertySourceLocator#locate的代码为：

```java
@Override
public PropertySource<?> locate(Environment environment) {
    String configFile = environment.resolvePlaceholders("${config.file}");
    messagePropertiesResource = getMessagePropertiesResource(configFile);
    this.messageProperties = loadMessageProperties();
    MapPropertySource propertySource = new MapPropertySource("file-based-property-source", (Map)messageProperties);
    return propertySource;
}
```

其中，配置项"${config.file}"配置在现有的Environment中，其源头在application.properties中

```properties
config.file=/META-INF/config/default.properties
```

### 监控配置文件变化

在发生变化时更新PropertySource对应的MapPropertySource的内容

```java
private void onMessagePropertiesChanged() {
    if (this.messagePropertiesResource.isFile()) { // 判断是否为文件
        // 获取对应文件系统中的文件
        try {
            File messagePropertiesFile = this.messagePropertiesResource.getFile();
            Path messagePropertiesFilePath = messagePropertiesFile.toPath();
            // 获取当前 OS 文件系统
            FileSystem fileSystem = FileSystems.getDefault();
            // 新建 WatchService
            WatchService watchService = fileSystem.newWatchService();
            // 获取资源文件所在的目录
            Path dirPath = messagePropertiesFilePath.getParent();
            // 注册 WatchService 到 dirPath，并且关心修改事件
            dirPath.register(watchService, ENTRY_MODIFY);
            // 处理资源文件变化（异步）
            processMessagePropertiesChanged(watchService);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

/**
    * 处理资源文件变化（异步）
    *
    * @param watchService
    */
private void processMessagePropertiesChanged(WatchService watchService) {
    executorService.submit(() -> {
        while (true) {
            WatchKey watchKey = watchService.take(); // take 发生阻塞
            // watchKey 是否有效
            try {
                if (watchKey.isValid()) {
                    for (WatchEvent event : watchKey.pollEvents()) {
                        Watchable watchable = watchKey.watchable();
                        // 目录路径（监听的注册目录）
                        Path dirPath = (Path) watchable;
                        // 事件所关联的对象即注册目录的子文件（或子目录）
                        // 事件发生源是相对路径
                        Path fileRelativePath = (Path) event.context();
                        File messagePropertiesFile = this.messagePropertiesResource.getFile();
                        Path messagePropertiesFilePath = messagePropertiesFile.toPath();
                        String resourceFileName = messagePropertiesFilePath.getFileName().toString();
                        if (resourceFileName.equals(fileRelativePath.getFileName().toString())) {
                            // 处理为绝对路径
                            Path filePath = dirPath.resolve(fileRelativePath);
                            File file = filePath.toFile();
                            Properties properties = loadMessageProperties(new FileReader(file));
                            synchronized (messageProperties) {
                                messageProperties.clear();
                                messageProperties.putAll(properties);
                            }
                        }
                    }
                }
            } finally {
                if (watchKey != null) {
                    watchKey.reset(); // 重置 WatchKey
                }
            }

        }
    });
}
```

### 定义 PropertySourceLocator @Bean

参照课堂讲解，使用API的方式实现，即声明一个Bean

```java
@Bean
MyPropertySourceLocator myPropertySourceLocator(){
    return new MyPropertySourceLocator();
}
```