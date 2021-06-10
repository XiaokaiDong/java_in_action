package me.tt.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.Consumer;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.cloud.bus.BusBridge;
import org.springframework.cloud.bus.event.EnvironmentChangeRemoteApplicationEvent;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisBusBridge implements BusBridge {
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> syncCommands;

    public static final String REMOTE_EVENT_TOPIC = "topic:remote_event";
    private final String REMOTE_APPLICATION_EVENT_KEY = "application_event";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void send(RemoteApplicationEvent event) {


        Map<String, String> messageBody = new HashMap<>();
        try {
            messageBody.put(REMOTE_APPLICATION_EVENT_KEY, objectMapper.writeValueAsString(event));
            String messageId = syncCommands.xadd(REMOTE_EVENT_TOPIC, messageBody);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    public RedisBusBridge(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
        this.syncCommands = connection.sync();
    }

    public void readRemoteApplicationEvent(){

        try {
            // WARNING: Streams must exist before creating the group
            //          This will not be necessary in Lettuce 5.2, see https://github.com/lettuce-io/lettuce-core/issues/898
            syncCommands.xgroupCreate( XReadArgs.StreamOffset.from(REMOTE_EVENT_TOPIC, "0-0"), "application_1"  );
        }
            catch (RedisBusyException redisBusyException) {
            System.out.println( String.format("\t Group '%s already' exists","application_1"));
        }


        System.out.println("Waiting for new messages");

        List<StreamMessage<String, String>> messages = syncCommands.xreadgroup(
                Consumer.from("application_1", "consumer_1"),
                XReadArgs.StreamOffset.lastConsumed(REMOTE_EVENT_TOPIC)
        );

        if (!messages.isEmpty()) {
            for (StreamMessage<String, String> message : messages) {
                System.out.println(message);
                Map<String, String> body = message.getBody();
                for(Map.Entry<String, String> entry : body.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    System.out.printf("the key is %s\n", key);
                    System.out.printf("the value is %s\n", value);
                    try {
                        EnvironmentChangeRemoteApplicationEvent remoteApplicationEvent = objectMapper.readValue(value, EnvironmentChangeRemoteApplicationEvent.class);
                        System.out.printf("the RemoteApplicationEvent received is %s\n", remoteApplicationEvent);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }

                // Confirm that the message has been processed using XACK
                syncCommands.xack(REMOTE_EVENT_TOPIC, "application_1",  message.getId());
            }
        }


    }

}
