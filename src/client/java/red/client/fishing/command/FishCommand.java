package red.client.fishing.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import red.client.fishing.config.FishConfig;
import red.client.fishing.feature.AutoFishingFeature;
import red.client.fishing.feature.SeaCreatureKiller;
import red.client.fishing.keybind.FishKeybindings;

public class FishCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("fish")
                .then(ClientCommandManager.literal("start")
                        .executes(FishCommand::startFishing))
                .then(ClientCommandManager.literal("stop")
                        .executes(FishCommand::stopFishing))
                .then(ClientCommandManager.literal("sck")
                        .then(ClientCommandManager.argument("enabled", StringArgumentType.word())
                                .executes(FishCommand::setSeaCreatureKiller)))
                .then(ClientCommandManager.literal("gk")
                        .then(ClientCommandManager.argument("enabled", StringArgumentType.word())
                                .executes(FishCommand::setGroupKilling)))
                .then(ClientCommandManager.literal("gkset")
                        .then(ClientCommandManager.argument("threshold", IntegerArgumentType.integer(1, 30))
                                .executes(FishCommand::setGroupKillingThreshold)))
                .then(ClientCommandManager.literal("hypedown")
                        .then(ClientCommandManager.argument("enabled", StringArgumentType.word())
                                .executes(FishCommand::setHyperionLookDown)))
                .then(ClientCommandManager.literal("keybind")
                        .then(ClientCommandManager.argument("key", IntegerArgumentType.integer())
                                .executes(FishCommand::setKeybind)))
                .then(ClientCommandManager.literal("ungrab")
                        .then(ClientCommandManager.argument("enabled", StringArgumentType.word())
                                .executes(FishCommand::setUngrabMouse)))
                .then(ClientCommandManager.literal("recastdelay")
                        .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(2, 50))
                                .executes(FishCommand::setRecastDelay)))
                .then(ClientCommandManager.literal("reelingdelay")
                        .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(2, 15))
                                .executes(FishCommand::setReelingDelay)))
                .executes(FishCommand::showStatus));
    }

    private static int startFishing(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        if (!AutoFishingFeature.isEnabled()) {
            AutoFishingFeature.toggle();
        } else {
            sendMessage("Auto fishing is already running!", Formatting.YELLOW);
        }

        return 1;
    }

    private static int stopFishing(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        if (AutoFishingFeature.isEnabled()) {
            AutoFishingFeature.toggle();
        } else {
            sendMessage("Auto fishing is not running!", Formatting.YELLOW);
        }

        return 1;
    }

    private static int setKeybind(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        int keyCode = IntegerArgumentType.getInteger(context, "key");
        FishConfig.setKeybindCode(keyCode);
        FishKeybindings.updateKeybind(keyCode);
        
        String keyName = GLFW.glfwGetKeyName(keyCode, 0);
        if (keyName == null) {
            keyName = "Key " + keyCode;
        }
        
        sendMessage("Keybind set to: " + keyName, Formatting.GREEN);

        return 1;
    }

    private static int setUngrabMouse(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        String value = StringArgumentType.getString(context, "enabled").toLowerCase();
        boolean enabled;

        if (value.equals("true") || value.equals("yes") || value.equals("on") || value.equals("1")) {
            enabled = true;
        } else if (value.equals("false") || value.equals("no") || value.equals("off") || value.equals("0")) {
            enabled = false;
        } else {
            sendMessage("Invalid value! Use 'true' or 'false'", Formatting.RED);
            return 0;
        }

        FishConfig.setUngrabMouseEnabled(enabled);
        sendMessage("Ungrab mouse: " + (enabled ? "ENABLED" : "DISABLED"), enabled ? Formatting.GREEN : Formatting.YELLOW);

        return 1;
    }

    private static int setRecastDelay(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        FishConfig.setRecastDelay(ticks);
        sendMessage("Recast delay set to: " + ticks + " ticks (" + (ticks * 50) + "ms)", Formatting.GREEN);

        return 1;
    }

    private static int setReelingDelay(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        FishConfig.setReelingDelay(ticks);
        sendMessage("Reeling delay set to: " + ticks + " ticks (" + (ticks * 50) + "ms)", Formatting.GREEN);

        return 1;
    }

    private static int setSeaCreatureKiller(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        String value = StringArgumentType.getString(context, "enabled").toLowerCase();
        boolean enabled;

        if (value.equals("true") || value.equals("yes") || value.equals("on") || value.equals("1")) {
            enabled = true;
        } else if (value.equals("false") || value.equals("no") || value.equals("off") || value.equals("0")) {
            enabled = false;
        } else {
            sendMessage("Invalid value! Use 'true' or 'false'", Formatting.RED);
            return 0;
        }

        SeaCreatureKiller.setEnabled(enabled);

        return 1;
    }

    private static int setGroupKilling(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        String value = StringArgumentType.getString(context, "enabled").toLowerCase();
        boolean enabled;

        if (value.equals("true") || value.equals("yes") || value.equals("on") || value.equals("1")) {
            enabled = true;
        } else if (value.equals("false") || value.equals("no") || value.equals("off") || value.equals("0")) {
            enabled = false;
        } else {
            sendMessage("Invalid value! Use 'true' or 'false'", Formatting.RED);
            return 0;
        }

        // Group killing is controlled by threshold: 1 = disabled (immediate), >1 = enabled (wait for group)
        if (enabled) {
            // Enable group killing with a default threshold of 5
            int currentThreshold = FishConfig.getSeaCreatureKillThreshold();
            if (currentThreshold <= 1) {
                FishConfig.setSeaCreatureKillThreshold(5);
                sendMessage("Group Killing: ENABLED (threshold set to 5)", Formatting.GREEN);
                sendMessage("Use '/fish gkset <1-30>' to adjust threshold", Formatting.YELLOW);
            } else {
                sendMessage("Group Killing: already enabled (threshold: " + currentThreshold + ")", Formatting.GREEN);
            }
        } else {
            // Disable group killing by setting threshold to 1
            FishConfig.setSeaCreatureKillThreshold(1);
            sendMessage("Group Killing: DISABLED (immediate attack mode)", Formatting.YELLOW);
        }

        return 1;
    }

    private static int setGroupKillingThreshold(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        int threshold = IntegerArgumentType.getInteger(context, "threshold");
        FishConfig.setSeaCreatureKillThreshold(threshold);
        
        if (threshold == 1) {
            sendMessage("Group Killing Threshold: " + threshold + " (DISABLED - immediate attack)", Formatting.YELLOW);
        } else {
            sendMessage("Group Killing Threshold: " + threshold + " creatures", Formatting.GREEN);
            sendMessage("SCK will wait for " + threshold + " creatures before attacking", Formatting.AQUA);
        }

        return 1;
    }

    private static int setHyperionLookDown(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        String value = StringArgumentType.getString(context, "enabled").toLowerCase();
        boolean enabled;

        if (value.equals("true") || value.equals("yes") || value.equals("on") || value.equals("1")) {
            enabled = true;
        } else if (value.equals("false") || value.equals("no") || value.equals("off") || value.equals("0")) {
            enabled = false;
        } else {
            sendMessage("Invalid value! Use 'true' or 'false'", Formatting.RED);
            return 0;
        }

        FishConfig.setHyperionLookDownEnabled(enabled);
        
        if (enabled) {
            sendMessage("Hyperion Look Down: ENABLED", Formatting.GREEN);
            sendMessage("Hyperion will look down for ground explosions", Formatting.AQUA);
        } else {
            sendMessage("Hyperion Look Down: DISABLED", Formatting.YELLOW);
            sendMessage("Hyperion will use normal orientation (may reduce AoE)", Formatting.GOLD);
        }

        return 1;
    }

    private static int showStatus(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        sendMessage("=== AutoFish Status ===", Formatting.AQUA);
        sendMessage("Auto Fishing: " + (AutoFishingFeature.isEnabled() ? "RUNNING" : "STOPPED"),
                AutoFishingFeature.isEnabled() ? Formatting.GREEN : Formatting.RED);
        
        int keyCode = FishConfig.getKeybindCode();
        String keyName = keyCode == -1 ? "Not set" : (GLFW.glfwGetKeyName(keyCode, 0) != null ? 
                GLFW.glfwGetKeyName(keyCode, 0) : "Key " + keyCode);
        sendMessage("Keybind: " + keyName, Formatting.YELLOW);
        sendMessage("Ungrab Mouse: " + (FishConfig.isUngrabMouseEnabled() ? "ENABLED" : "DISABLED"),
                FishConfig.isUngrabMouseEnabled() ? Formatting.GREEN : Formatting.RED);
        sendMessage("Recast Delay: " + FishConfig.getRecastDelay() + " ticks (" + (FishConfig.getRecastDelay() * 50) + "ms)", Formatting.YELLOW);
        sendMessage("Reeling Delay: " + FishConfig.getReelingDelay() + " ticks (" + (FishConfig.getReelingDelay() * 50) + "ms)", Formatting.YELLOW);
        sendMessage("Sea Creature Killer: " + (FishConfig.isSeaCreatureKillerEnabled() ? "ENABLED" : "DISABLED"),
                FishConfig.isSeaCreatureKillerEnabled() ? Formatting.GREEN : Formatting.RED);
        if (SeaCreatureKiller.isEnabled()) {
            sendMessage("SCK Kills: " + SeaCreatureKiller.getKillCount(), Formatting.GOLD);
            int threshold = FishConfig.getSeaCreatureKillThreshold();
            String groupStatus = threshold > 1 ? "ENABLED (" + threshold + " creatures)" : "DISABLED (immediate attack)";
            sendMessage("Group Killing: " + groupStatus, threshold > 1 ? Formatting.GREEN : Formatting.YELLOW);
            sendMessage("Hyperion Look Down: " + (FishConfig.isHyperionLookDownEnabled() ? "ENABLED" : "DISABLED"),
                    FishConfig.isHyperionLookDownEnabled() ? Formatting.GREEN : Formatting.YELLOW);
        }

        return 1;
    }

    private static void sendMessage(String message, Formatting formatting) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("[AutoFish] ").formatted(Formatting.AQUA, Formatting.BOLD)
                            .append(Text.literal(message).formatted(formatting)),
                    false
            );
        }
    }
}
