package me.tt.locator;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class MyPropertySourceLocator implements PropertySourceLocator, ResourceLoaderAware {

    private static final String ENCODING = "UTF-8";

    private Resource messagePropertiesResource;

    private Properties messageProperties;

    private ExecutorService executorService;

    private ResourceLoader resourceLoader;


    @Override
    public PropertySource<?> locate(Environment environment) {
        String configFile = environment.resolvePlaceholders("${config.file}");
        messagePropertiesResource = getMessagePropertiesResource(configFile);
        this.messageProperties = loadMessageProperties();
        MapPropertySource propertySource = new MapPropertySource("file-based-property-source", (Map)messageProperties);
        return propertySource;
    }

    public MyPropertySourceLocator() {

        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private ResourceLoader getResourceLoader() {
        return this.resourceLoader != null ? this.resourceLoader : new DefaultResourceLoader();
    }

    private Resource getMessagePropertiesResource(String resourcePath) {
        ResourceLoader resourceLoader = getResourceLoader();
        Resource resource = resourceLoader.getResource(resourcePath);
        return resource;
    }

    private Properties loadMessageProperties() {
        EncodedResource encodedResource = new EncodedResource(this.messagePropertiesResource, ENCODING);
        try {
            return loadMessageProperties(encodedResource.getReader());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Properties loadMessageProperties(Reader reader) {
        Properties properties = new Properties();
        try {
            properties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return properties;
    }

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
}
