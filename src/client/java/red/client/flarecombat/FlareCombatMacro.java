package red.client.flarecombat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import red.client.flarecombat.command.FlareCommand;
import red.client.flarecombat.config.FlareConfig;
import red.client.flarecombat.feature.FlareMacroFeature;
import red.client.flarecombat.keybind.FlareKeybindings;

public class FlareCombatMacro implements ClientModInitializer {

    private static boolean initialized = false;

    @Override
    public void onInitializeClient() {
        if (initialized) {
            return; // Prevent double initialization
        }

        try {
            // Load config
            FlareConfig.load();

            // Initialize keybindings
            FlareKeybindings.register();

            // Register commands
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                FlareCommand.register(dispatcher);
            });

            // Register the main tick event for macro
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.player != null && client.world != null) {
                    FlareMacroFeature.tick();
                }
            });

            // Add shutdown hook for proper cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[FlareCombat] Shutting down, saving config...");
                FlareConfig.save();
            }));

            initialized = true;
            System.out.println("[FlareCombat] Macro initialized successfully!");

        } catch (Exception e) {
            System.err.println("[FlareCombat] Failed to initialize macro: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
