package red.client.flarecombat.util;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import red.client.flarecombat.mixin.MouseMixin;

import java.util.Random;

public class MouseSimulator {
    private static final Random random = new Random();

    /**
     * Simulate a right click using MouseMixin for authentic mouse simulation
     */
    public static void simulateRightClick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }

        try {
            long windowHandle = client.getWindow().getHandle();

            // Cast the Mouse instance to MouseMixin interface (applied via Mixin transformation)
            MouseMixin mouseMixin = (MouseMixin) client.mouse;

            // Simulate right mouse button press
            mouseMixin.invokeOnMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS, 0);

            // Small delay to simulate human-like click duration
            try {
                Thread.sleep(10 + random.nextInt(20)); // 10-30ms delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Simulate right mouse button release
            mouseMixin.invokeOnMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_RELEASE, 0);

        } catch (Exception e) {
            System.err.println("[FlareCombat] Failed to simulate right click: " + e.getMessage());
        }
    }
}
