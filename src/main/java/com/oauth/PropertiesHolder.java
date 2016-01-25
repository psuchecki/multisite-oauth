package com.oauth;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class PropertiesHolder {

    private static PropertiesConfiguration properties;

    public static void initialize() throws ConfigurationException {
        properties = new PropertiesConfiguration("config.properties");
    }

    public static String getProperty(String key){
        return (String) properties.getProperty(key);
    }

    public static boolean useEvernoteSandbox() {
        return Boolean.valueOf((String) properties.getProperty("evernote.usesandbox"));
    }
}
