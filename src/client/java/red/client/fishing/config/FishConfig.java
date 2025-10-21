package red.client.fishing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public class FishConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("autofish.json").toFile();

    private static ConfigData data = new ConfigData();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                data = GSON.fromJson(reader, ConfigData.class);
                if (data == null) {
                    data = new ConfigData();
                }
                System.out.println("[AutoFish] Config loaded successfully");
            } catch (Exception e) {
                System.err.println("[AutoFish] Failed to load config: " + e.getMessage());
                data = new ConfigData();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            CONFIG_DIR.toFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
            System.out.println("[AutoFish] Config saved successfully");
        } catch (Exception e) {
            System.err.println("[AutoFish] Failed to save config: " + e.getMessage());
        }
    }

    public static int getKeybindCode() {
        return data.keybindCode;
    }

    public static void setKeybindCode(int code) {
        data.keybindCode = code;
        save();
    }

    public static boolean isUngrabMouseEnabled() {
        return data.ungrabMouse;
    }

    public static void setUngrabMouseEnabled(boolean enabled) {
        data.ungrabMouse = enabled;
        save();
    }

    public static int getRecastDelay() {
        return data.recastDelay;
    }

    public static void setRecastDelay(int delay) {
        data.recastDelay = delay;
        save();
    }

    public static int getReelingDelay() {
        return data.reelingDelay;
    }

    public static void setReelingDelay(int delay) {
        data.reelingDelay = delay;
        save();
    }

    public static boolean isSeaCreatureKillerEnabled() {
        return data.seaCreatureKillerEnabled;
    }

    public static void setSeaCreatureKillerEnabled(boolean enabled) {
        data.seaCreatureKillerEnabled = enabled;
        save();
    }

    public static String getCombatMode() {
        return data.combatMode;
    }

    public static void setCombatMode(String mode) {
        data.combatMode = mode;
        save();
    }

    public static int getSeaCreatureKillThreshold() {
        return data.seaCreatureKillThreshold;
    }

    public static void setSeaCreatureKillThreshold(int threshold) {
        data.seaCreatureKillThreshold = Math.max(1, Math.min(30, threshold)); // Clamp between 1-30
        save();
    }

    public static boolean isHyperionLookDownEnabled() {
        return data.hyperionLookDown;
    }

    public static void setHyperionLookDownEnabled(boolean enabled) {
        data.hyperionLookDown = enabled;
        save();
    }

    private static class ConfigData {
        int keybindCode = -1; // -1 means no keybind set
        boolean ungrabMouse = false; // Default ungrab mouse disabled
        int recastDelay = 10; // Default 10 ticks (500ms) recast delay
        int reelingDelay = 6; // Default 6 ticks (300ms) reeling delay
        boolean seaCreatureKillerEnabled = false; // Default SCK disabled
        String combatMode = "RCM"; // Default combat mode is RCM
        int seaCreatureKillThreshold = 1; // Minimum creatures before SCK engages (1-30)
        boolean hyperionLookDown = true; // Default Hyperion looks down for explosion (true = enabled)
    }
}
