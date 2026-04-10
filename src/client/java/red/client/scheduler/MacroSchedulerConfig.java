package red.client.scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

/**
 * Configuration for the Macro Scheduler
 * Manages run time, break time, and break enable status
 */
public class MacroSchedulerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("macro_scheduler.json").toFile();

    private static ConfigData data = new ConfigData();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                data = GSON.fromJson(reader, ConfigData.class);
                if (data == null) {
                    data = new ConfigData();
                }
                System.out.println("[MacroScheduler] Config loaded successfully");
            } catch (Exception e) {
                System.err.println("[MacroScheduler] Failed to load config: " + e.getMessage());
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
            System.out.println("[MacroScheduler] Config saved successfully");
        } catch (Exception e) {
            System.err.println("[MacroScheduler] Failed to save config: " + e.getMessage());
        }
    }

    // Run time in minutes
    public static int getRunTime() {
        return data.runTimeMinutes;
    }

    public static void setRunTime(int minutes) {
        data.runTimeMinutes = Math.max(1, Math.min(180, minutes)); // 1-180 minutes (1-3 hours)
        save();
    }

    // Break minimum time in minutes
    public static int getBreakMinTime() {
        return data.breakMinMinutes;
    }

    public static void setBreakMinTime(int minutes) {
        data.breakMinMinutes = Math.max(1, Math.min(60, minutes)); // 1-60 minutes
        save();
    }

    // Break maximum time in minutes
    public static int getBreakMaxTime() {
        return data.breakMaxMinutes;
    }

    public static void setBreakMaxTime(int minutes) {
        data.breakMaxMinutes = Math.max(1, Math.min(60, minutes)); // 1-60 minutes
        save();
    }

    // Break enabled status
    public static boolean isBreakEnabled() {
        return data.breakEnabled;
    }

    public static void setBreakEnabled(boolean enabled) {
        data.breakEnabled = enabled;
        save();
    }

    private static class ConfigData {
        int runTimeMinutes = 60; // Default 60 minutes (1 hour)
        int breakMinMinutes = 1; // Default minimum 1 minute break
        int breakMaxMinutes = 25; // Default maximum 25 minutes break
        boolean breakEnabled = false; // Default breaks disabled
    }
}
