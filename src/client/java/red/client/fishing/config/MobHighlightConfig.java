package red.client.fishing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Configuration for MobHighlight feature
 * Allows customizable mob highlighting by name
 */
public class MobHighlightConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/mobhighlight.json");
    
    // Configuration fields
    private static boolean enabled = false;
    private static String targetMobName = "";
    private static int highlightColor = 0x00FF00; // Default: Green
    private static double detectionRange = 50.0; // Max range to scan for mobs
    
    /**
     * Load configuration from file
     */
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // Create default config
            return;
        }
        
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            MobHighlightConfig config = GSON.fromJson(reader, MobHighlightConfig.class);
            if (config != null) {
                enabled = config.enabled;
                targetMobName = config.targetMobName;
                highlightColor = config.highlightColor;
                detectionRange = config.detectionRange;
            }
        } catch (Exception e) {
            System.err.println("[MobHighlight] Failed to load config: " + e.getMessage());
        }
    }
    
    /**
     * Save configuration to file
     */
    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                MobHighlightConfig config = new MobHighlightConfig();
                config.enabled = enabled;
                config.targetMobName = targetMobName;
                config.highlightColor = highlightColor;
                config.detectionRange = detectionRange;
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            System.err.println("[MobHighlight] Failed to save config: " + e.getMessage());
        }
    }
    
    // Getters
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static String getTargetMobName() {
        return targetMobName;
    }
    
    public static int getHighlightColor() {
        return highlightColor;
    }
    
    public static double getDetectionRange() {
        return detectionRange;
    }
    
    // Setters
    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }
    
    public static void setTargetMobName(String name) {
        targetMobName = name;
        save();
    }
    
    public static void setHighlightColor(int color) {
        highlightColor = color;
        save();
    }
    
    public static void setDetectionRange(double range) {
        detectionRange = range;
        save();
    }
    
    /**
     * Toggle enabled state
     */
    public static boolean toggle() {
        enabled = !enabled;
        save();
        return enabled;
    }
}
