package red.client.fishing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import red.client.fishing.command.FishCommand;
import red.client.fishing.command.HighlightCommand;
import red.client.fishing.command.RedCommand;
import red.client.fishing.config.FishConfig;
import red.client.fishing.config.MobHighlightConfig;
import red.client.fishing.config.SpikeHelperConfig;
import red.client.fishing.feature.AutoFishingFeature;
import red.client.fishing.feature.MobHighlight;
import red.client.fishing.feature.SpikeHelper;
import red.client.fishing.keybind.FishKeybindings;
import red.client.gui.GuiCommand;
import red.client.scheduler.MacroSchedulerConfig;
import red.client.scheduler.MacroScheduler;

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
            MacroSchedulerConfig.load();
            SpikeHelperConfig.load();
            MobHighlightConfig.load();
            red.client.fishing.config.BezalFarmerConfig.load();
            red.client.fishing.config.XYZConfig.load();

            // Initialize keybindings
            FishKeybindings.register();

            // Register commands
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                FishCommand.register(dispatcher);
                RedCommand.register(dispatcher);
                HighlightCommand.register(dispatcher);
                GuiCommand.register(dispatcher, registryAccess);
            });

            // Register tick event for auto fishing, sea creature killer, spike helper, mob highlight, bezal farmer, XYZ macro, and scheduler
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.player != null && client.world != null) {
                    AutoFishingFeature.tick();
                    red.client.fishing.feature.SeaCreatureKiller.tick();
                    SpikeHelper.tick();
                    MobHighlight.tick();
                    red.client.fishing.feature.BezalFarmer.tick();
                    red.client.fishing.feature.XYZMacro.tick();
                    MacroScheduler.tick();
                }
            });
            
            // Register chat event for XYZ Macro
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                String text = message.getString();
                red.client.fishing.feature.XYZMacro.onChat(text);
            });

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[AutoFish] Shutting down, saving config...");
                FishConfig.save();
                MacroSchedulerConfig.save();
                SpikeHelperConfig.save();
                MobHighlightConfig.save();
                red.client.fishing.config.BezalFarmerConfig.save();
                red.client.fishing.config.XYZConfig.save();
            }));

            initialized = true;
            System.out.println("[AutoFish] Module initialized successfully!");

        } catch (Exception e) {
            System.err.println("[AutoFish] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
