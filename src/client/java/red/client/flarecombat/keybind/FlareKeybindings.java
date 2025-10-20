package red.client.flarecombat.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import red.client.flarecombat.config.FlareConfig;
import red.client.flarecombat.feature.FlareMacroFeature;

public class FlareKeybindings {
    private static KeyBinding toggleMacro;
    private static boolean wasPressed = false;

    public static void register() {
        // Create keybinding - will use config value if set
        int keyCode = FlareConfig.getKeybindCode();
        if (keyCode == -1) {
            keyCode = GLFW.GLFW_KEY_UNKNOWN; // No default keybind
        }

        toggleMacro = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.flarecombat.toggle",
                InputUtil.Type.KEYSYM,
                keyCode,
                "category.flarecombat"
        ));

        // Register tick event for keybind handling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleMacro.isPressed()) {
                if (!wasPressed) {
                    wasPressed = true;
                    FlareMacroFeature.toggle();
                }
            } else {
                wasPressed = false;
            }
        });
    }

    public static void updateKeybind(int keyCode) {
        if (toggleMacro != null) {
            toggleMacro.setBoundKey(InputUtil.fromKeyCode(keyCode, 0));
            KeyBinding.updateKeysByCode();
        }
    }

    public static KeyBinding getToggleMacro() {
        return toggleMacro;
    }
}
