package org.geektimes.config.source;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileConfigSource implements ConfigSource {

    private final Logger logger = Logger.getLogger(FileConfigSource.class.getName());

    private final Map<String, String> properties;

    private final static String FILE_CHARSET = "GBK";

    public FileConfigSource() {
        this("MyConfig.properties");
    }

    public FileConfigSource(String proFileName) {
        Map<String, String> properties1;
        Properties fileProperties = new Properties();
        //InputStream input = Object.class.getResourceAsStream(proFileName);
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(proFileName);
        InputStreamReader inputStreamReader = null;

        properties1 = null;

        if (input != null) {
            try {
                inputStreamReader = new InputStreamReader(input, FILE_CHARSET);
                fileProperties.load(inputStreamReader);
                properties1 = new HashMap<String, String>((Map)fileProperties);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "加载配置文件" + proFileName + "失败");
            }
        }else {
            properties1 = new HashMap<>();
        }
        this.properties = properties1;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return "Config File Properties";
    }
}
