package com.yourorg.entra;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.INIConfiguration;

import java.io.File;

public final class IniConfig {
    private final INIConfiguration ini;

    public IniConfig(String iniPath) throws ConfigurationException {
        this.ini = new INIConfiguration(new File(iniPath));
    }

    public String getRequired(String section, String key) {
        String fullKey = section + "." + key;
        String val = ini.getString(fullKey);
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required INI value: " + fullKey);
        }
        return val.trim();
    }

    public int getInt(String section, String key, int defaultValue) {
        String fullKey = section + "." + key;
        return ini.getInt(fullKey, defaultValue);
    }
}
