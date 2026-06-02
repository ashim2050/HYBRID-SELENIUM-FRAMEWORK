package com.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton config reader - loads config.properties from the test classpath.
 * Falls back to sensible defaults when a key is absent.
 */
public class ConfigReader {

    private static final Logger logger = LogManager.getLogger(ConfigReader.class);
    private static final String CONFIG_FILE = "config/config.properties";
    private static final Properties properties = new Properties();

    static {
        InputStream in = ConfigReader.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE);
        String loadedFrom = CONFIG_FILE;
        if (in == null) {
            loadedFrom = "config.properties";
            in = ConfigReader.class.getClassLoader().getResourceAsStream(loadedFrom);
        }
        try {
            if (in != null) {
                properties.load(in);
                logger.info("Config loaded from classpath: {}", loadedFrom);
            } else {
                logger.warn("Config.properties not found on classpath, using hard-coded defaults.");
            }
        } catch (IOException e) {
            logger.error("Failed to load config.properties", e);
        }
    }

    private ConfigReader() {}

    /** Returns raw property value or @code null. */
    public static String get(String key) {
        return properties.getProperty(key);
    }

    /** Returns property value or defaultValue when key is missing. */
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    // Convenience accessors
    public static String getMasterFilePath() {
        return get("master_excel.path", "Input/MasterConfig.xlsx");
    }

    public static String getModuleDefaultSheet() {
        return get("module.default.sheet", "Sheet1");
    }

    public static String getJsonOutputPath() {
        return get("json.output.path", "output/testdata/");
    }

    public static String getTestNgOutputPath() {
        return get("testing.output.path", "output/testing_dynamic.xml");
    }

    public static String getTestNGOutputPath() {
        return getTestNgOutputPath();
    }

    public static String getReportsOutputPath() {
        return get("reports.output.path", "output/reports/");
    }

    public static String getBrowser() {
        return get("browser", "chrome");
    }

   public static boolean isHeadless() {
        // Check system property override first (allows -Dheadless=true/false from Jenkins)
        String systemProperty = System.getProperty("headless");
        if (systemProperty != null) {
            boolean headlessValue = Boolean.parseBoolean(systemProperty);
            logger.info("Using headless mode from system property: " + headlessValue);
            return headlessValue;
        }
        
        // Auto-detect Jenkins environment and force headless mode (if no override)
        if (isRunningUnderJenkins()) {
            logger.info("Running under Jenkins - forcing headless mode (set -Dheadless=false to override)");
            return true;
        }
        return Boolean.parseBoolean(get("headless", "false"));
    }

    /**
     * Detects if the code is running under a Jenkins CI/CD environment.
     * Checks for common Jenkins environment variables.
     */
    private static boolean isRunningUnderJenkins() {
        return System.getenv("JENKINS_HOME") != null 
            || System.getenv("BUILD_ID") != null 
            || System.getenv("BUILD_NUMBER") != null
            || System.getenv("JENKINS_URL") != null
            || System.getenv("CI") != null && "jenkins".equalsIgnoreCase(System.getenv("CI_SYSTEM"));
    }

public static String getBaseUrl() {
    return get("base.url", "https://the-internet.herokuapp.com");
}

public static String getApiBaseUrl() {
    return get("api.base.url", "https://jsonplaceholder.typicode.com");
}

public static String getApiProxyHost() {
    return get("api.proxy.host", "");
}

public static int getApiProxyPort() {
    return Integer.parseInt(get("api.proxy.port", "0"));
}

public static int getApiProxyPORT() {
    return getApiProxyPort();
}

public static int getImplicitWait() {
    return Integer.parseInt(get("implicit.wait", "10"));
}

public static int getExplicitWait() {
    return Integer.parseInt(get("explicit.wait", "20"));
}

public static int getPageLoadTimeout() {
    return Integer.parseInt(get("page.load.timeout", "30"));
}

public static String getParallelExecution() {
    return get("parallel.execution", "false");
}

public static int getThreadCount() {
    return Integer.parseInt(get("thread.count", "1"));
}

}
