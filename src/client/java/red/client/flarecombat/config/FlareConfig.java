package red.client.flarecombat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public class FlareConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("flarecombat.json").toFile();

    private static ConfigData data = new ConfigData();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                data = GSON.fromJson(reader, ConfigData.class);
                if (data == null) {
                    data = new ConfigData();
                }
                System.out.println("[FlareCombat] Config loaded successfully");
            } catch (Exception e) {
                System.err.println("[FlareCombat] Failed to load config: " + e.getMessage());
                data = new ConfigData();
            }
        } else {
            save(); // Create default config
        }
    }

    public static void save() {
        try {
            CONFIG_DIR.toFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
            System.out.println("[FlareCombat] Config saved successfully");
        } catch (Exception e) {
            System.err.println("[FlareCombat] Failed to save config: " + e.getMessage());
        }
    }

    public static int getClickCount() {
        return data.clickCount;
    }

    public static void setClickCount(int count) {
        data.clickCount = count;
        save();
    }

    public static int getKeybindCode() {
        return data.keybindCode;
    }

    public static void setKeybindCode(int code) {
        data.keybindCode = code;
        save();
    }

    public static boolean isUngrabbMouseEnabled() {
        return data.ungrabMouse;
    }

    public static void setUngrabMouseEnabled(boolean enabled) {
        data.ungrabMouse = enabled;
        save();
    }

    public static int getCombatMode() {
        return data.combatMode;
    }

    public static void setCombatMode(int mode) {
        data.combatMode = mode;
        save();
    }

    public static String getCombatModeName() {
        switch (data.combatMode) {
            case 1:
                return "Hyperion";
            case 2:
                return "Fire Veil Wand";
            default:
                return "Hyperion";
        }
    }

    private static class ConfigData {
        int clickCount = 3; // Default 3 clicks
        int keybindCode = -1; // -1 means no keybind set
        boolean ungrabMouse = false; // Default ungrab mouse disabled
        int combatMode = 1; // Default mode: 1 = Hyperion, 2 = Fire Veil Wand
    }
}
