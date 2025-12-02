package red.client.fishing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Configuration for XYZ Lasso Macro
 * Manages settings for auto-catching Zeus, Wai, and Exe mobs
 */
public class XYZConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
        FabricLoader.getInstance().getConfigDir().toFile(),
        "xyz_macro.json"
    );

    // Configuration fields
    private static boolean enabled = false;
    private static double searchRange = 30.0; // Range to search for mobs
    private static int maxAttempts = 3; // Max attempts per mob before blacklisting
    private static long blacklistDuration = 60000; // Blacklist duration in milliseconds (60 seconds)
    private static boolean autoReel = true; // Automatically reel when REEL indicator appears
    private static int reelDelay = 100; // Delay before reeling in milliseconds

    /**
     * Load configuration from file
     */
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // Create default config
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            XYZConfig config = GSON.fromJson(reader, XYZConfig.class);
            if (config != null) {
                enabled = config.enabled;
                searchRange = config.searchRange;
                maxAttempts = config.maxAttempts;
                blacklistDuration = config.blacklistDuration;
                autoReel = config.autoReel;
                reelDelay = config.reelDelay;
            }
        } catch (IOException e) {
            System.err.println("[XYZMacro] Failed to load config: " + e.getMessage());
        }
    }

    /**
     * Save configuration to file
     */
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            XYZConfig config = new XYZConfig();
            config.enabled = enabled;
            config.searchRange = searchRange;
            config.maxAttempts = maxAttempts;
            config.blacklistDuration = blacklistDuration;
            config.autoReel = autoReel;
            config.reelDelay = reelDelay;
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[XYZMacro] Failed to save config: " + e.getMessage());
        }
    }

    // ===== GETTERS AND SETTERS =====

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }

    public static double getSearchRange() {
        return searchRange;
    }

    public static void setSearchRange(double value) {
        searchRange = Math.max(5.0, Math.min(100.0, value));
        save();
    }

    public static int getMaxAttempts() {
        return maxAttempts;
    }

    public static void setMaxAttempts(int value) {
        maxAttempts = Math.max(1, Math.min(10, value));
        save();
    }

    public static long getBlacklistDuration() {
        return blacklistDuration;
    }

    public static void setBlacklistDuration(long value) {
        blacklistDuration = Math.max(10000, Math.min(300000, value));
        save();
    }

    public static boolean isAutoReel() {
        return autoReel;
    }

    public static void setAutoReel(boolean value) {
        autoReel = value;
        save();
    }

    public static int getReelDelay() {
        return reelDelay;
    }

    public static void setReelDelay(int value) {
        reelDelay = Math.max(0, Math.min(1000, value));
        save();
    }
}
