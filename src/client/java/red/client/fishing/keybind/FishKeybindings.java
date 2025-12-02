package red.client.fishing.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import red.client.fishing.config.FishConfig;
import red.client.fishing.feature.AutoFishingFeature;
import red.client.fishing.feature.BezalFarmer;
import red.client.fishing.feature.SpikeHelper;
import red.client.fishing.feature.XYZMacro;
import red.client.gui.RedClientScreen;

public class FishKeybindings {
    private static KeyBinding toggleFishing;
    private static KeyBinding openGui;
    private static KeyBinding toggleSpikeHelper;
    private static KeyBinding toggleBezalFarmer;
    private static KeyBinding toggleXYZMacro;
    private static boolean wasPressed = false;
    private static boolean guiWasPressed = false;
    private static boolean spikeHelperWasPressed = false;
    private static boolean bezalFarmerWasPressed = false;
    private static boolean xyzMacroWasPressed = false;

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

        // Register Spike Helper keybinding (default: H key)
        toggleSpikeHelper = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.spikehelper.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.autofish"
        ));

        // Register Bezal Farmer keybinding (default: B key)
        toggleBezalFarmer = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bezalfarmer.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.autofish"
        ));

        toggleXYZMacro = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xyzmacro.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
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

            // Toggle Spike Helper
            if (toggleSpikeHelper.isPressed()) {
                if (!spikeHelperWasPressed) {
                    spikeHelperWasPressed = true;
                    SpikeHelper.toggle();
                }
            } else {
                spikeHelperWasPressed = false;
            }

            // Toggle Bezal Farmer
            if (toggleBezalFarmer.isPressed()) {
                if (!bezalFarmerWasPressed) {
                    bezalFarmerWasPressed = true;
                    BezalFarmer.toggle();
                }
            } else {
                bezalFarmerWasPressed = false;
            }

            // Toggle XYZ Macro
            if (toggleXYZMacro.isPressed()) {
                if (!xyzMacroWasPressed) {
                    xyzMacroWasPressed = true;
                    XYZMacro.toggle();
                }
            } else {
                xyzMacroWasPressed = false;
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
