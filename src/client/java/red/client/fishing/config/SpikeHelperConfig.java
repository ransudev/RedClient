package red.client.fishing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Configuration for Spike Helper feature
 * Manages settings for tracking and highlighting Spike entities
 */
public class SpikeHelperConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
        FabricLoader.getInstance().getConfigDir().toFile(),
        "spike_helper.json"
    );

    // Configuration fields
    private static boolean enabled = false;
    private static boolean highlightEnabled = true;
    private static boolean aimAssistEnabled = false; // Aim Assist feature
    private static double targetDistance = 9.0; // Exact distance required for catching Spikes
    private static double distanceTolerance = 0.5; // Tolerance for "correct" distance (green highlight)

    // Color values (ARGB format for entity glow)
    private static int colorTooClose = 0xFFFF0000; // Red when too close
    private static int colorCorrect = 0xFF00FF00; // Green when at correct distance

    /**
     * Load configuration from file
     */
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // Create default config
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            SpikeHelperConfig config = GSON.fromJson(reader, SpikeHelperConfig.class);
            if (config != null) {
                enabled = config.enabled;
                highlightEnabled = config.highlightEnabled;
                aimAssistEnabled = config.aimAssistEnabled;
                targetDistance = config.targetDistance;
                distanceTolerance = config.distanceTolerance;
                colorTooClose = config.colorTooClose;
                colorCorrect = config.colorCorrect;
            }
        } catch (IOException e) {
            System.err.println("[SpikeHelper] Failed to load config: " + e.getMessage());
        }
    }

    /**
     * Save configuration to file
     */
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            SpikeHelperConfig config = new SpikeHelperConfig();
            config.enabled = enabled;
            config.highlightEnabled = highlightEnabled;
            config.aimAssistEnabled = aimAssistEnabled;
            config.targetDistance = targetDistance;
            config.distanceTolerance = distanceTolerance;
            config.colorTooClose = colorTooClose;
            config.colorCorrect = colorCorrect;
            
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[SpikeHelper] Failed to save config: " + e.getMessage());
        }
    }

    // Getters
    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isHighlightEnabled() {
        return highlightEnabled;
    }

    public static boolean isAimAssistEnabled() {
        return aimAssistEnabled;
    }

    public static double getTargetDistance() {
        return targetDistance;
    }

    public static double getDistanceTolerance() {
        return distanceTolerance;
    }

    public static int getColorTooClose() {
        return colorTooClose;
    }

    public static int getColorCorrect() {
        return colorCorrect;
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

    public static void setAimAssistEnabled(boolean value) {
        aimAssistEnabled = value;
        save();
    }

    public static void setTargetDistance(double value) {
        targetDistance = Math.max(1.0, Math.min(50.0, value)); // Clamp between 1-50
        save();
    }

    public static void setDistanceTolerance(double value) {
        distanceTolerance = Math.max(0.1, Math.min(2.0, value)); // Clamp between 0.1-2.0
        save();
    }

    public static void setColorTooClose(int color) {
        colorTooClose = color;
        save();
    }

    public static void setColorCorrect(int color) {
        colorCorrect = color;
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
