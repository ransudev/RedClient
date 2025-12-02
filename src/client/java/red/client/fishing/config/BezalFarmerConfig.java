package red.client.fishing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Configuration for Bezal Farmer feature
 * Manages settings for tracking and auto-attacking Bezal entities
 */
public class BezalFarmerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
        FabricLoader.getInstance().getConfigDir().toFile(),
        "bezal_farmer.json"
    );

    // Configuration fields
    private static boolean enabled = false;
    private static boolean highlightEnabled = true;
    private static boolean autoAimEnabled = true; // Auto-aim at Bezal before attacking
    private static double attackDistance = 3.0; // Distance to trigger attack (3 blocks)
    private static int clickCount = 3; // Number of clicks per attack
    private static long clickDelayMs = 50; // Delay between clicks in milliseconds
    
    // Weapon and Blackhole settings
    private static String weaponName = "Prime Huntaxe"; // Weapon name to auto-swap to
    private static boolean blackholeEnabled = false; // Toggle Blackhole usage after low HP
    private static String blackholeItemName = "Pocket Black Hole"; // Name to search for in inventory

    // Color values (ARGB format for entity glow)
    private static int colorTooFar = 0xFFFFFF00; // Yellow when too far to attack
    private static int colorInRange = 0xFF00FF00; // Green when in attack range

    /**
     * Load configuration from file
     */
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // Create default config
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            BezalFarmerConfig config = GSON.fromJson(reader, BezalFarmerConfig.class);
            if (config != null) {
                enabled = config.enabled;
                highlightEnabled = config.highlightEnabled;
                autoAimEnabled = config.autoAimEnabled;
                attackDistance = config.attackDistance;
                clickCount = config.clickCount;
                clickDelayMs = config.clickDelayMs;
                colorTooFar = config.colorTooFar;
                colorInRange = config.colorInRange;
                weaponName = config.weaponName;
                blackholeEnabled = config.blackholeEnabled;
                blackholeItemName = config.blackholeItemName;
            }
        } catch (IOException e) {
            System.err.println("[BezalFarmer] Failed to load config: " + e.getMessage());
        }
    }

    /**
     * Save configuration to file
     */
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            BezalFarmerConfig config = new BezalFarmerConfig();
            config.enabled = enabled;
            config.highlightEnabled = highlightEnabled;
            config.autoAimEnabled = autoAimEnabled;
            config.attackDistance = attackDistance;
            config.clickCount = clickCount;
            config.clickDelayMs = clickDelayMs;
            config.colorTooFar = colorTooFar;
            config.colorInRange = colorInRange;
            config.weaponName = weaponName;
            config.blackholeEnabled = blackholeEnabled;
            config.blackholeItemName = blackholeItemName;
            
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[BezalFarmer] Failed to save config: " + e.getMessage());
        }
    }

    // Getters
    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isHighlightEnabled() {
        return highlightEnabled;
    }

    public static boolean isAutoAimEnabled() {
        return autoAimEnabled;
    }

    public static double getAttackDistance() {
        return attackDistance;
    }

    public static int getClickCount() {
        return clickCount;
    }

    public static long getClickDelayMs() {
        return clickDelayMs;
    }

    public static int getColorTooFar() {
        return colorTooFar;
    }

    public static int getColorInRange() {
        return colorInRange;
    }

    // Setters
    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }

    public static void setHighlightEnabled(boolean value) {
        highlightEnabled = value;
        save();
    }

    public static void setAutoAimEnabled(boolean value) {
        autoAimEnabled = value;
        save();
    }

    public static void setAttackDistance(double value) {
        attackDistance = Math.max(1.0, Math.min(10.0, value)); // Clamp between 1-10
        save();
    }

    public static void setClickCount(int value) {
        clickCount = Math.max(1, Math.min(10, value)); // Clamp between 1-10
        save();
    }

    public static void setClickDelayMs(long value) {
        clickDelayMs = Math.max(10, Math.min(500, value)); // Clamp between 10-500ms
        save();
    }

    public static void setColorTooFar(int color) {
        colorTooFar = color;
        save();
    }

    public static void setColorInRange(int color) {
        colorInRange = color;
        save();
    }

    public static String getWeaponName() {
        return weaponName;
    }

    public static void setWeaponName(String value) {
        weaponName = value;
        save();
    }

    public static boolean isBlackholeEnabled() {
        return blackholeEnabled;
    }

    public static void setBlackholeEnabled(boolean value) {
        blackholeEnabled = value;
        save();
    }

    public static String getBlackholeItemName() {
        return blackholeItemName;
    }

    public static void setBlackholeItemName(String value) {
        blackholeItemName = value;
        save();
    }

    /**
     * Toggle enabled state and return new value
     */
    public static boolean toggle() {
        enabled = !enabled;
        save();
        return enabled;
    }
}
