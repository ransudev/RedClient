package red.client.fishing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import red.client.fishing.command.FishCommand;
import red.client.fishing.command.RedCommand;
import red.client.fishing.config.FishConfig;
import red.client.fishing.feature.AutoFishingFeature;
import red.client.fishing.keybind.FishKeybindings;
import red.client.gui.GuiCommand;

public class FishingMod implements ClientModInitializer {

    private static boolean initialized = false;

    @Override
    public void onInitializeClient() {
        if (initialized) {
            return;
        }

        try {
            // Load config
            FishConfig.load();

            // Initialize keybindings
            FishKeybindings.register();

            // Register commands
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                FishCommand.register(dispatcher);
                RedCommand.register(dispatcher);
                GuiCommand.register(dispatcher, registryAccess);
            });

            // Register tick event for auto fishing and sea creature killer
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.player != null && client.world != null) {
                    AutoFishingFeature.tick();
                    red.client.fishing.feature.SeaCreatureKiller.tick();
                }
            });

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[AutoFish] Shutting down, saving config...");
                FishConfig.save();
            }));

            initialized = true;
            System.out.println("[AutoFish] Module initialized successfully!");

        } catch (Exception e) {
            System.err.println("[AutoFish] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
