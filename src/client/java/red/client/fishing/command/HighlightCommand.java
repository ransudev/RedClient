package red.client.fishing.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import red.client.fishing.config.MobHighlightConfig;
import red.client.fishing.feature.MobHighlight;

public class HighlightCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("highlight")
                .then(ClientCommandManager.argument("mobName", StringArgumentType.greedyString())
                        .executes(HighlightCommand::setMobTarget))
                .then(ClientCommandManager.literal("clear")
                        .executes(HighlightCommand::clearTarget))
                .then(ClientCommandManager.literal("status")
                        .executes(HighlightCommand::showStatus))
                .then(ClientCommandManager.literal("color")
                        .then(ClientCommandManager.argument("hexColor", StringArgumentType.word())
                                .executes(HighlightCommand::setColor)))
                .then(ClientCommandManager.literal("range")
                        .then(ClientCommandManager.argument("distance", StringArgumentType.word())
                                .executes(HighlightCommand::setRange)))
                .executes(HighlightCommand::showHelp));
    }

    private static int setMobTarget(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        String mobName = StringArgumentType.getString(context, "mobName");
        
        if (mobName == null || mobName.trim().isEmpty()) {
            sendMessage("Please specify a mob name!", Formatting.RED);
            return 0;
        }

        MobHighlight.setTargetMob(mobName);
        sendMessage(String.format("Highlighting enabled for mobs matching: '%s'", mobName), Formatting.GREEN);
        sendMessage("Use '/highlight clear' to stop highlighting", Formatting.YELLOW);

        return 1;
    }

    private static int clearTarget(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        MobHighlight.clearTarget();
        sendMessage("Mob highlighting disabled", Formatting.YELLOW);

        return 1;
    }

    private static int setColor(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        String hexColor = StringArgumentType.getString(context, "hexColor");
        
        try {
            // Parse hex color (support both #FF0000 and FF0000 formats)
            String colorStr = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
            int color = Integer.parseInt(colorStr, 16);
            
            MobHighlightConfig.setHighlightColor(color);
            sendMessage(String.format("Highlight color set to: #%06X", color), Formatting.GREEN);
        } catch (NumberFormatException e) {
            sendMessage("Invalid hex color! Use format: FF0000 or #FF0000", Formatting.RED);
            return 0;
        }

        return 1;
    }

    private static int setRange(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        String rangeStr = StringArgumentType.getString(context, "distance");
        
        try {
            double range = Double.parseDouble(rangeStr);
            
            if (range < 10.0 || range > 200.0) {
                sendMessage("Range must be between 10 and 200 blocks!", Formatting.RED);
                return 0;
            }
            
            MobHighlightConfig.setDetectionRange(range);
            sendMessage(String.format("Detection range set to: %.0f blocks", range), Formatting.GREEN);
        } catch (NumberFormatException e) {
            sendMessage("Invalid range! Use a number between 10 and 200", Formatting.RED);
            return 0;
        }

        return 1;
    }

    private static int showStatus(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        sendMessage("=== Mob Highlight Status ===", Formatting.AQUA);
        sendMessage("Enabled: " + (MobHighlightConfig.isEnabled() ? "YES" : "NO"),
                MobHighlightConfig.isEnabled() ? Formatting.GREEN : Formatting.RED);
        
        String targetName = MobHighlightConfig.getTargetMobName();
        if (targetName != null && !targetName.trim().isEmpty()) {
            sendMessage("Target Mob: '" + targetName + "'", Formatting.YELLOW);
        } else {
            sendMessage("Target Mob: Not set", Formatting.GRAY);
        }
        
        sendMessage(String.format("Highlight Color: #%06X", MobHighlightConfig.getHighlightColor()), Formatting.YELLOW);
        sendMessage(String.format("Detection Range: %.0f blocks", MobHighlightConfig.getDetectionRange()), Formatting.YELLOW);
        sendMessage("Tracking: " + MobHighlight.getStatusText(), Formatting.AQUA);

        return 1;
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }

        sendMessage("=== Mob Highlight Commands ===", Formatting.AQUA);
        sendMessage("/highlight <mob name> - Highlight mobs with matching name", Formatting.YELLOW);
        sendMessage("  Example: /highlight bezal", Formatting.GRAY);
        sendMessage("/highlight clear - Stop highlighting", Formatting.YELLOW);
        sendMessage("/highlight status - Show current settings", Formatting.YELLOW);
        sendMessage("/highlight color <hex> - Set highlight color", Formatting.YELLOW);
        sendMessage("  Example: /highlight color FF0000 (red)", Formatting.GRAY);
        sendMessage("/highlight range <blocks> - Set detection range (10-200)", Formatting.YELLOW);
        sendMessage("  Example: /highlight range 75", Formatting.GRAY);

        return 1;
    }

    private static void sendMessage(String message, Formatting formatting) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("[MobHighlight] ").formatted(Formatting.GOLD, Formatting.BOLD)
                            .append(Text.literal(message).formatted(formatting)),
                    false
            );
        }
    }
}
