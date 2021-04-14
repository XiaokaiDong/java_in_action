package org.geektimes.cache.redis;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.geektimes.cache.AbstractCache;
import org.geektimes.cache.ExpirableEntry;
import org.geektimes.cache.serializer.MyGeneric2StringConverter;
import org.geektimes.cache.serializer.MyGenericJackson2JsonSerializer;
import org.geektimes.cache.serializer.MyGenericToByteSerializer;
import org.geektimes.cache.serializer.MySerializer;

import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LettuceCache<K extends Serializable, V extends Serializable> extends AbstractCache<K, V> {
    private final Logger logger = Logger.getLogger(LettuceCache.class.getName());

    private final StatefulRedisConnection connection;

    private final RedisCommands<String, String> redisCommands;

    private final MyGeneric2StringConverter<K> keySerializer;

    private final MySerializer<Object> valueSerializer = new MyGenericJackson2JsonSerializer();

    protected LettuceCache(CacheManager cacheManager, String cacheName,
                           Configuration<K, V> configuration, StatefulRedisConnection connection) {
        super(cacheManager, cacheName, configuration);
        this.connection = connection;
        this.redisCommands = connection.sync();
        this.keySerializer = new MyGeneric2StringConverter<>(configuration.getKeyType());
    }

    @Override
    protected boolean containsEntry(K key) throws CacheException, ClassCastException {
        try {
            return redisCommands.exists(keySerializer.of(key)) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected ExpirableEntry<K, V> getEntry(K key) throws CacheException, ClassCastException {
        try {
            return getEntry(keySerializer.of(key));
        } catch (Exception e) {
            return null;
        }
    }

    protected ExpirableEntry<K, V> getEntry(String keyString) throws CacheException, ClassCastException {
        String stringValue = redisCommands.get(keyString);

        try {
            return ExpirableEntry.of(keySerializer.from(keyString),
                    (V)valueSerializer.deserialize(stringValue.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void putEntry(ExpirableEntry<K, V> entry) throws CacheException, ClassCastException {
        try {
            redisCommands.set(keySerializer.of(entry.getKey()),
                    new String(valueSerializer.serialize(entry.getValue()), StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.log(Level.WARNING, "序列化错误" + e.getMessage());
        }

    }

    @Override
    protected ExpirableEntry<K, V> removeEntry(K key) throws CacheException, ClassCastException {
        ExpirableEntry<K, V> oldEntry = getEntry(key);
        try {
            redisCommands.del(keySerializer.of(key));
        } catch (Exception e) {
            logger.log(Level.WARNING, "删除键错误" + e.getMessage());
        }

        return oldEntry;
    }

    @Override
    protected void clearEntries() throws CacheException {
        redisCommands.flushall();
    }

    @Override
    protected Set<K> keySet() {
        return null;
    }
}
