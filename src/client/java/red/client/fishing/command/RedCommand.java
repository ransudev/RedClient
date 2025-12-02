package red.client.fishing.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import red.client.gui.RedClientScreen;

public class RedCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("red")
                .then(ClientCommandManager.literal("help")
                        .executes(RedCommand::showHelp))
                .then(ClientCommandManager.literal("gui")
                        .executes(RedCommand::openGui))
                .executes(RedCommand::showHelp));
    }

    private static int openGui(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        
        // Schedule GUI opening for next tick (must be done on main thread)
        client.execute(() -> {
            client.setScreen(new RedClientScreen());
        });
        
        context.getSource().sendFeedback(
                Text.literal("Opening RedClient GUI...").formatted(Formatting.GREEN)
        );
        
        return 1;
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        sendHeader("RedClient - Command Help");
        sendDivider();
        
        sendCategory("GUI Access");
        sendCommand("/red gui", "Open RedClient GUI (or press R key)");
        sendCommand("/redgui", "Alternative command to open GUI");
        
        sendCategory("Auto Fishing Controls");
        sendCommand("/fish start", "Start auto fishing");
        sendCommand("/fish stop", "Stop auto fishing");
        sendCommand("/fish", "Show current status");
        
        sendCategory("Sea Creature Killer");
        sendCommand("/fish sck <true/false>", "Enable/disable Sea Creature Killer");
        sendCommand("/fish gk <true/false>", "Enable/disable group killing");
        sendCommand("/fish gkset <1-30>", "Set group kill threshold (creatures to wait for)");
        sendCommand("/fish hypedown <true/false>", "Enable/disable Hyperion look-down");
        
        sendCategory("Configuration");
        sendCommand("/fish keybind <keycode>", "Set toggle keybind");
        sendCommand("/fish ungrab <true/false>", "Enable/disable mouse ungrab");
        sendCommand("/fish recastdelay <0-50>", "Set recast delay (ticks)");
        sendCommand("/fish reelingdelay <0-15>", "Set reeling delay (ticks)");
        
        sendCategory("Information");
        sendCommand("/red help", "Show this help message");
        
        sendDivider();
        sendFooter("Tip: Use 'true/yes/on/1' or 'false/no/off/0' for boolean values");

        return 1;
    }

    private static void sendHeader(String title) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("╔══════════════════════════════════════╗")
                            .formatted(Formatting.AQUA, Formatting.BOLD),
                    false
            );
            client.player.sendMessage(
                    Text.literal("║ ").formatted(Formatting.AQUA, Formatting.BOLD)
                            .append(Text.literal(centerText(title, 34)).formatted(Formatting.WHITE, Formatting.BOLD))
                            .append(Text.literal(" ║").formatted(Formatting.AQUA, Formatting.BOLD)),
                    false
            );
        }
    }

    private static void sendDivider() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("╠══════════════════════════════════════╣")
                            .formatted(Formatting.AQUA),
                    false
            );
        }
    }

    private static void sendFooter(String tip) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("║ ").formatted(Formatting.AQUA)
                            .append(Text.literal("💡 " + tip).formatted(Formatting.YELLOW, Formatting.ITALIC)),
                    false
            );
            client.player.sendMessage(
                    Text.literal("╚══════════════════════════════════════╝")
                            .formatted(Formatting.AQUA, Formatting.BOLD),
                    false
            );
        }
    }

    private static void sendCategory(String category) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("║ ").formatted(Formatting.AQUA)
                            .append(Text.literal("▶ " + category).formatted(Formatting.GOLD, Formatting.BOLD)),
                    false
            );
        }
    }

    private static void sendCommand(String command, String description) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("║   ").formatted(Formatting.AQUA)
                            .append(Text.literal(command).formatted(Formatting.GREEN))
                            .append(Text.literal(" - ").formatted(Formatting.GRAY))
                            .append(Text.literal(description).formatted(Formatting.WHITE)),
                    false
            );
        }
    }

    private static String centerText(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int padding = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        sb.append(text);
        while (sb.length() < width) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
