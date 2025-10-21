package red.client.fishing.util;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import red.client.flarecombat.mixin.MouseMixin;

import java.util.Random;

public class FishMouseSimulator {
    private static final Random random = new Random();

    public static void simulateRightClick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }

        try {
            long windowHandle = client.getWindow().getHandle();
            MouseMixin mouseMixin = (MouseMixin) client.mouse;

            mouseMixin.invokeOnMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_PRESS, 0);

            try {
                Thread.sleep(10 + random.nextInt(20));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            mouseMixin.invokeOnMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_RELEASE, 0);

        } catch (Exception e) {
            System.err.println("[AutoFish] Failed to simulate right click: " + e.getMessage());
        }
    }
}
