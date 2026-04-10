package red.client.fishing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CinderbatHighlightConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "cinderbat_highlight.json"
    );

    private static boolean enabled = true;
    private static boolean highlightEnabled = true;
    private static boolean debugEnabled = false;
    private static double detectionRange = 220.0;
    private static int highlightColor = 0xFF00FF00;

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            CinderbatHighlightConfig config = GSON.fromJson(reader, CinderbatHighlightConfig.class);
            if (config != null) {
                enabled = config.enabled;
                highlightEnabled = config.highlightEnabled;
                debugEnabled = config.debugEnabled;
                detectionRange = Math.max(20.0, Math.min(500.0, config.detectionRange));
                if (Math.abs(detectionRange - 100.0) < 0.001) {
                    detectionRange = 220.0;
                }
                highlightColor = config.highlightColor;
            }
        } catch (IOException e) {
            System.err.println("[CinderbatHighlight] Failed to load config: " + e.getMessage());
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            CinderbatHighlightConfig config = new CinderbatHighlightConfig();
            config.enabled = enabled;
            config.highlightEnabled = highlightEnabled;
            config.debugEnabled = debugEnabled;
            config.detectionRange = detectionRange;
            config.highlightColor = highlightColor;
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[CinderbatHighlight] Failed to save config: " + e.getMessage());
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isHighlightEnabled() {
        return highlightEnabled;
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static double getDetectionRange() {
        return detectionRange;
    }

    public static int getHighlightColor() {
        return highlightColor;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }

    public static void setHighlightEnabled(boolean value) {
        highlightEnabled = value;
        save();
    }

    public static void setDebugEnabled(boolean value) {
        debugEnabled = value;
        save();
    }

    public static void setDetectionRange(double value) {
        detectionRange = Math.max(20.0, Math.min(500.0, value));
        save();
    }

    public static void setHighlightColor(int value) {
        highlightColor = value;
        save();
    }

    public static boolean toggle() {
        enabled = !enabled;
        save();
        return enabled;
    }
}
