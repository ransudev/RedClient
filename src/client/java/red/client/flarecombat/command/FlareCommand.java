package red.client.flarecombat.command;

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
import red.client.flarecombat.config.FlareConfig;
import red.client.flarecombat.feature.FlareMacroFeature;
import red.client.flarecombat.keybind.FlareKeybindings;

public class FlareCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("flare")
                .then(ClientCommandManager.literal("start")
                        .executes(FlareCommand::startMacro))
                .then(ClientCommandManager.literal("stop")
                        .executes(FlareCommand::stopMacro))
                .then(ClientCommandManager.literal("setclick")
                        .then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, 10))
                                .executes(FlareCommand::setClickCount)))
                .then(ClientCommandManager.literal("keybind")
                        .then(ClientCommandManager.argument("key", IntegerArgumentType.integer())
                                .executes(FlareCommand::setKeybind)))
                .then(ClientCommandManager.literal("ungrab")
                        .then(ClientCommandManager.argument("enabled", StringArgumentType.word())
                                .executes(FlareCommand::setUngrabMouse)))
                .executes(FlareCommand::showStatus));
    }

    private static int startMacro(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        if (!FlareMacroFeature.isEnabled()) {
            FlareMacroFeature.start();
        } else {
            sendMessage("Macro is already running!", Formatting.YELLOW);
        }

        return 1;
    }

    private static int stopMacro(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        if (FlareMacroFeature.isEnabled()) {
            FlareMacroFeature.stop();
        } else {
            sendMessage("Macro is not running!", Formatting.YELLOW);
        }

        return 1;
    }

    private static int setClickCount(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        int count = IntegerArgumentType.getInteger(context, "count");
        FlareConfig.setClickCount(count);
        sendMessage("Click count set to: " + count, Formatting.GREEN);

        return 1;
    }

    private static int setKeybind(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        int keyCode = IntegerArgumentType.getInteger(context, "key");
        FlareConfig.setKeybindCode(keyCode);
        FlareKeybindings.updateKeybind(keyCode);
        
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

        FlareConfig.setUngrabMouseEnabled(enabled);
        sendMessage("Ungrab mouse: " + (enabled ? "ENABLED" : "DISABLED"), enabled ? Formatting.GREEN : Formatting.YELLOW);

        return 1;
    }

    private static int showStatus(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        sendMessage("=== FlareCombat Status ===", Formatting.AQUA);
        sendMessage("Macro: " + (FlareMacroFeature.isEnabled() ? "RUNNING" : "STOPPED"),
                FlareMacroFeature.isEnabled() ? Formatting.GREEN : Formatting.RED);
        sendMessage("Click Count: " + FlareConfig.getClickCount(), Formatting.YELLOW);
        
        int keyCode = FlareConfig.getKeybindCode();
        String keyName = keyCode == -1 ? "Not set" : (GLFW.glfwGetKeyName(keyCode, 0) != null ? 
                GLFW.glfwGetKeyName(keyCode, 0) : "Key " + keyCode);
        sendMessage("Keybind: " + keyName, Formatting.YELLOW);
        sendMessage("Ungrab Mouse: " + (FlareConfig.isUngrabbMouseEnabled() ? "ENABLED" : "DISABLED"),
                FlareConfig.isUngrabbMouseEnabled() ? Formatting.GREEN : Formatting.RED);

        return 1;
    }

    private static void sendMessage(String message, Formatting formatting) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("[FlareCombat] ").formatted(Formatting.AQUA, Formatting.BOLD)
                            .append(Text.literal(message).formatted(formatting)),
                    false
            );
        }
    }
}
