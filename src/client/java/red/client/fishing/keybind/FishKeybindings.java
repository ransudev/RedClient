package red.client.fishing.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import red.client.fishing.config.FishConfig;
import red.client.fishing.feature.AutoFishingFeature;
import red.client.gui.RedClientScreen;

public class FishKeybindings {
    private static KeyBinding toggleFishing;
    private static KeyBinding openGui;
    private static boolean wasPressed = false;
    private static boolean guiWasPressed = false;

    public static void register() {
        int keyCode = FishConfig.getKeybindCode();
        if (keyCode == -1) {
            keyCode = GLFW.GLFW_KEY_UNKNOWN;
        }

        toggleFishing = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autofish.toggle",
                InputUtil.Type.KEYSYM,
                keyCode,
                "category.autofish"
        ));

        // Register GUI keybinding (default: R key)
        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.redclient.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.autofish"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Toggle fishing
            if (toggleFishing.isPressed()) {
                if (!wasPressed) {
                    wasPressed = true;
                    AutoFishingFeature.toggle();
                }
            } else {
                wasPressed = false;
            }

            // Open GUI
            if (openGui.isPressed()) {
                if (!guiWasPressed) {
                    guiWasPressed = true;
                    client.execute(() -> {
                        client.setScreen(new RedClientScreen());
                    });
                }
            } else {
                guiWasPressed = false;
            }
        });
    }

    public static void updateKeybind(int keyCode) {
        if (toggleFishing != null) {
            toggleFishing.setBoundKey(InputUtil.fromKeyCode(keyCode, 0));
            KeyBinding.updateKeysByCode();
        }
    }

    public static KeyBinding getToggleFishing() {
        return toggleFishing;
    }
}
