package com.jmelgar1.craftfunds;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "craftfunds.conf";
    private static final String CONFIG_DIR = "config";
    
    private static ConfigManager instance;
    private Properties config;
    private Path configPath;
    
    private ConfigManager() {
        this.config = new Properties();
        this.configPath = Paths.get(CONFIG_DIR, CONFIG_FILE_NAME);
        loadConfig();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                createDefaultConfig();
            }
            
            try (InputStream input = Files.newInputStream(configPath)) {
                config.load(input);
                CraftFunds.LOGGER.info("Configuration loaded from {}", configPath);
            }
            
        } catch (IOException e) {
            CraftFunds.LOGGER.error("Failed to load configuration file", e);
            loadDefaultValues();
        }
    }
    
    private void createDefaultConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            
            Properties defaultConfig = new Properties();
            setDefaultValues(defaultConfig);
            
            try (OutputStream output = Files.newOutputStream(configPath)) {
                defaultConfig.store(output, "CraftFunds Configuration File");
            }
            
            this.config = defaultConfig;
            CraftFunds.LOGGER.info("Created default configuration file at {}", configPath);
            
        } catch (IOException e) {
            CraftFunds.LOGGER.error("Failed to create default configuration file", e);
            loadDefaultValues();
        }
    }
    
    private void setDefaultValues(Properties props) {
        props.setProperty("database.url", "jdbc:mysql://localhost:3306/craftfunds");
        props.setProperty("database.username", "your_username_here");
        props.setProperty("database.password", "your_password_here");
        props.setProperty("database.timeout.seconds", "10");
    }
    
    private void loadDefaultValues() {
        config.clear();
        setDefaultValues(config);
        CraftFunds.LOGGER.warn("Loaded default configuration values due to file read error");
    }
    
    public String getDatabaseUrl() {
        return config.getProperty("database.url", "jdbc:mysql://localhost:3306/craftfunds");
    }
    
    public String getDatabaseUsername() {
        return config.getProperty("database.username", "your_username_here");
    }
    
    public String getDatabasePassword() {
        return config.getProperty("database.password", "your_password_here");
    }
    
    public int getDatabaseTimeoutSeconds() {
        try {
            return Integer.parseInt(config.getProperty("database.timeout.seconds", "10"));
        } catch (NumberFormatException e) {
            CraftFunds.LOGGER.warn("Invalid database timeout value in config, using default of 10 seconds");
            return 10;
        }
    }
    
    public boolean hasValidDatabaseCredentials() {
        String username = getDatabaseUsername();
        String password = getDatabasePassword();
        String url = getDatabaseUrl();
        
        return !username.equals("your_username_here") &&
               !password.equals("your_password_here") &&
               !username.trim().isEmpty() &&
               !password.trim().isEmpty() &&
               !url.trim().isEmpty();
    }
    
    public void reloadConfig() {
        CraftFunds.LOGGER.info("Reloading configuration...");
        loadConfig();
    }
}