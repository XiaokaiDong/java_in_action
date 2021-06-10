package me.tt;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import me.tt.cloud.RedisBusBridge;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.EnvironmentChangeRemoteApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisBusBridgeDemo {
    public static void main(String[] args) {

        // 创建 BeanFactory 容器
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        // 注册 Configuration Class（配置类）
        applicationContext.register(RedisBusBridgeDemo.class);

        // 启动 Spring 应用上下文
        applicationContext.refresh();

        RedisBusBridgeDemo redisBusBridgeDemo = (RedisBusBridgeDemo)applicationContext.getBean("redisBusBridgeDemo");

        RedisClient redisClient = RedisClient.create("redis://password01!@22.16.221.152:6379"); // change to reflect your environment
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        Map<String, String> nullMap = new HashMap<>();
        nullMap.put("key1", "value1");
        nullMap.put("key2", "value2");

        EnvironmentChangeRemoteApplicationEvent environmentChangeRemoteApplicationEvent =
                new EnvironmentChangeRemoteApplicationEvent(redisBusBridgeDemo,
                        "nullService", new Destination() {
                    @Override
                    public String getDestinationAsString() {
                        return RedisBusBridge.REMOTE_EVENT_TOPIC;
                    }
                },nullMap);

        RedisBusBridge redisBusBridge = new RedisBusBridge(connection);
        redisBusBridge.send(environmentChangeRemoteApplicationEvent);

        redisBusBridge.readRemoteApplicationEvent();

        connection.close();
        redisClient.shutdown();

        // 显示地关闭 Spring 应用上下文
        applicationContext.close();

    }


}
