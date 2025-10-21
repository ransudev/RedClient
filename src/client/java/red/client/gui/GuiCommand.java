package red.client.gui;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Command to open the RedClient GUI
 * Usage: /redgui
 */
public class GuiCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("redgui")
                .executes(context -> {
                    MinecraftClient client = context.getSource().getClient();
                    
                    // Schedule GUI opening for next tick (must be done on main thread)
                    client.execute(() -> {
                        client.setScreen(new RedClientScreen());
                    });
                    
                    context.getSource().sendFeedback(
                            Text.literal("Opening RedClient GUI...").formatted(Formatting.GREEN)
                    );
                    
                    return 1;
                }));
    }
}
