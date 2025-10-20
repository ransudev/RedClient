package red.client.flarecombat.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import red.client.flarecombat.config.FlareConfig;
import red.client.flarecombat.util.MouseSimulator;

import java.util.Random;

public class FlareMacroFeature {
    private static boolean enabled = false;
    private static Entity flareEntity = null;
    private static final double DETECTION_RANGE = 6.0;
    private static final Random random = new Random();

    // Mouse grab state tracking
    private static boolean mouseWasGrabbed = false;

    // Phase tracking
    private static MacroPhase currentPhase = MacroPhase.IDLE;
    private static int phaseTimer = 0;
    private static int hyperionClickCounter = 0;

    // Slots
    private static int overfluxSlot = -1;
    private static int hyperionSlot = -1;

    // Timing constants (in ticks, 1 tick = 50ms)
    // Increased delays with humanization ranges
    private static final int BASE_WAIT = 15; // 750ms base wait (increased from 500ms)
    private static final int WAIT_VARIANCE = 5; // ±250ms variance for humanization
    private static final int OVERFLUX_INTERVAL = 800; // 40 seconds = 800 ticks
    private static final int CLICK_DELAY_BASE = 4; // 200ms between clicks (increased from 100ms)
    private static final int CLICK_DELAY_VARIANCE = 2; // ±100ms variance
    private static long lastOverfluxTime = 0;
    private static long macroStartTime = 0;

    private enum MacroPhase {
        IDLE,
        OVERFLUX_WAIT_INITIAL,
        OVERFLUX_SWITCH,
        OVERFLUX_WAIT_SWITCH,
        OVERFLUX_CLICK,
        OVERFLUX_WAIT_CLICK,
        HYPERION_LOOP
    }

    public static void toggle() {
        if (enabled) {
            stop();
        } else {
            start();
        }
    }

    public static void start() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        // Scan for Flare entity
        if (!scanForFlare()) {
            sendMessage("Cannot start: Flare entity not found nearby!", Formatting.RED);
            return;
        }

        // Scan for items
        if (!scanHotbarForItems()) {
            sendMessage("Cannot start: Required items not found in hotbar!", Formatting.RED);
            return;
        }

        enabled = true;
        currentPhase = MacroPhase.OVERFLUX_WAIT_INITIAL;
        phaseTimer = getRandomizedWait();
        macroStartTime = System.currentTimeMillis();
        lastOverfluxTime = macroStartTime;
        hyperionClickCounter = 0;

        // Ungrab mouse if enabled in config
        if (FlareConfig.isUngrabbMouseEnabled()) {
            ungrabMouse();
        }

        sendMessage("Macro started! Targeting: " + getEntityDisplayName(flareEntity), Formatting.GREEN);
        sendMessage("Overflux slot: " + overfluxSlot + " | Hyperion slot: " + hyperionSlot, Formatting.YELLOW);
    }

    public static void stop() {
        if (!enabled) {
            return;
        }

        enabled = false;
        currentPhase = MacroPhase.IDLE;
        phaseTimer = 0;
        flareEntity = null;
        overfluxSlot = -1;
        hyperionSlot = -1;

        // Restore mouse grab if it was ungrabbed
        if (FlareConfig.isUngrabbMouseEnabled()) {
            restoreMouseGrab();
        }

        sendMessage("Macro stopped!", Formatting.RED);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void tick() {
        if (!enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            stop();
            return;
        }

        // Verify Flare entity still exists
        if (flareEntity == null || flareEntity.isRemoved() || 
            client.player.distanceTo(flareEntity) > DETECTION_RANGE) {
            sendMessage("Flare entity lost! Stopping macro...", Formatting.RED);
            stop();
            return;
        }

        // Check if it's time for next Overflux phase
        long currentTime = System.currentTimeMillis();
        long timeSinceLastOverflux = currentTime - lastOverfluxTime;
        
        if (timeSinceLastOverflux >= (OVERFLUX_INTERVAL * 50) && currentPhase == MacroPhase.HYPERION_LOOP) {
            // Time to restart Overflux phase
            currentPhase = MacroPhase.OVERFLUX_WAIT_INITIAL;
            phaseTimer = getRandomizedWait();
            lastOverfluxTime = currentTime;
            sendMessage("Starting Overflux phase...", Formatting.YELLOW);
        }

        // Handle phase timer
        if (phaseTimer > 0) {
            phaseTimer--;
            return;
        }

        // Execute current phase
        switch (currentPhase) {
            case IDLE:
                // Should not happen during macro execution
                break;

            case OVERFLUX_WAIT_INITIAL:
                // Initial wait complete, switch to Overflux
                currentPhase = MacroPhase.OVERFLUX_SWITCH;
                break;

            case OVERFLUX_SWITCH:
                // Switch to Overflux slot
                if (overfluxSlot != -1) {
                    client.player.getInventory().setSelectedSlot(overfluxSlot);
                    currentPhase = MacroPhase.OVERFLUX_WAIT_SWITCH;
                    phaseTimer = getRandomizedWait();
                } else {
                    sendMessage("Overflux slot not found! Stopping...", Formatting.RED);
                    stop();
                }
                break;

            case OVERFLUX_WAIT_SWITCH:
                // Wait after switch complete, now click
                currentPhase = MacroPhase.OVERFLUX_CLICK;
                break;

            case OVERFLUX_CLICK:
                // Right-click Overflux
                MouseSimulator.simulateRightClick(client);
                currentPhase = MacroPhase.OVERFLUX_WAIT_CLICK;
                phaseTimer = getRandomizedWait();
                break;

            case OVERFLUX_WAIT_CLICK:
                // Overflux phase complete, move to Hyperion loop
                currentPhase = MacroPhase.HYPERION_LOOP;
                hyperionClickCounter = 0;
                sendMessage("Overflux activated! Starting Hyperion loop...", Formatting.GREEN);
                break;

            case HYPERION_LOOP:
                // Execute Hyperion attack loop
                executeHyperionLoop(client);
                break;
        }
    }

    private static void executeHyperionLoop(MinecraftClient client) {
        // Switch to Hyperion
        if (hyperionSlot != -1 && client.player.getInventory().getSelectedSlot() != hyperionSlot) {
            client.player.getInventory().setSelectedSlot(hyperionSlot);
            phaseTimer = getRandomizedWait(); // Wait after switch with humanization
            return;
        }

        // Perform right-clicks
        if (hyperionClickCounter < FlareConfig.getClickCount()) {
            MouseSimulator.simulateRightClick(client);
            hyperionClickCounter++;
            phaseTimer = getRandomizedClickDelay(); // Humanized delay between clicks
        } else {
            // Reset counter for next loop iteration
            hyperionClickCounter = 0;
            phaseTimer = getRandomizedWait(); // Wait before next loop iteration
        }
    }

    /**
     * Get a randomized wait time with variance for humanization
     * Base: 750ms ± 250ms (range: 500ms to 1000ms)
     */
    private static int getRandomizedWait() {
        int variance = random.nextInt(WAIT_VARIANCE * 2 + 1) - WAIT_VARIANCE;
        return BASE_WAIT + variance;
    }

    /**
     * Get a randomized click delay with variance for humanization
     * Base: 200ms ± 100ms (range: 100ms to 300ms)
     */
    private static int getRandomizedClickDelay() {
        int variance = random.nextInt(CLICK_DELAY_VARIANCE * 2 + 1) - CLICK_DELAY_VARIANCE;
        return CLICK_DELAY_BASE + variance;
    }

    /**
     * Ungrab the mouse cursor for background usage
     */
    private static void ungrabMouse() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.mouse != null) {
            mouseWasGrabbed = client.mouse.isCursorLocked();
            client.mouse.unlockCursor();
            sendMessage("Mouse ungrabbed - allows background macro usage", Formatting.YELLOW);
        }
    }

    /**
     * Restore mouse grab state when macro stops
     */
    private static void restoreMouseGrab() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.mouse != null && mouseWasGrabbed && !enabled) {
            client.mouse.lockCursor();
            sendMessage("Mouse grab restored", Formatting.YELLOW);
        }
    }

    private static boolean scanForFlare() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }

        for (Entity entity : client.world.getEntities()) {
            if (entity != null && entity.hasCustomName()) {
                String name = entity.getCustomName().getString();
                if (name.contains("Flare")) {
                    double distance = client.player.distanceTo(entity);
                    if (distance <= DETECTION_RANGE) {
                        flareEntity = entity;
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean scanHotbarForItems() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        overfluxSlot = -1;
        hyperionSlot = -1;

        // Scan hotbar (slots 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            String itemName = stack.getName().getString();

            if (itemName.contains("Overflux Power Orb")) {
                overfluxSlot = i;
            }

            if (itemName.contains("Hyperion")) {
                hyperionSlot = i;
            }
        }

        return overfluxSlot != -1 && hyperionSlot != -1;
    }

    private static String getEntityDisplayName(Entity entity) {
        if (entity == null) {
            return "Unknown";
        }

        if (entity.hasCustomName() && entity.getCustomName() != null) {
            return entity.getCustomName().getString();
        }

        return entity.getType().getTranslationKey();
    }

    private static void sendMessage(String message, Formatting formatting) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("[FlareCombat] ").formatted(Formatting.AQUA, Formatting.BOLD)
                            .append(Text.literal(message).formatted(formatting)),
                    false
            );
        }
    }
}
