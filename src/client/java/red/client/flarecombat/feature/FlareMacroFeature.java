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
    private static final double DETECTION_RANGE = 11.0;
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
    private static int atonementSlot = -1;

    // Timing constants (in ticks, 1 tick = 50ms)
    // Increased delays with humanization ranges
    private static final int BASE_WAIT = 15; // 750ms base wait (increased from 500ms)
    private static final int WAIT_VARIANCE = 5; // ±250ms variance for humanization
    private static final int OVERFLUX_INTERVAL = 800; // 40 seconds = 800 ticks
    private static final int CLICK_DELAY_BASE = 4; // 200ms between clicks (increased from 100ms)
    private static final int CLICK_DELAY_VARIANCE = 2; // ±100ms variance
    private static final long FLARE_DEATH_TIMEOUT_MS = 30_000L;
    private static final long ATONEMENT_INTERVAL_MS = 20_000L;
    private static long lastOverfluxTime = 0;
    private static long macroStartTime = 0;
    private static long clickOnlyWaitStartTime = 0;
    private static long lastMissingAtonementWarnAt = 0;
    private static long lastAtonementUseTime = 0;
    private static int clickOnlyClickCounter = 0;

    private static ClickOnlyState clickOnlyState = ClickOnlyState.CLICK_SEQUENCE;
    private static HealOverrideState healOverrideState = HealOverrideState.IDLE;
    private static int healTimer = 0;
    private static int healReturnSlot = -1;

    private enum ClickOnlyState {
        CLICK_SEQUENCE,
        WAIT_FOR_FLARE_DEATH
    }

    private enum HealOverrideState {
        IDLE,
        WAIT_AFTER_WAND_SWITCH,
        WAIT_AFTER_HEAL_CLICK
    }

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

        boolean clickOnlyMode = isClickOnlyMode();
        if (!clickOnlyMode) {
            // Scan for items for weapon/overflux modes
            if (!scanHotbarForItems()) {
                sendMessage("Cannot start: Required items not found in hotbar!", Formatting.RED);
                return;
            }
        } else {
            overfluxSlot = -1;
            hyperionSlot = -1;
        }

        enabled = true;
        currentPhase = clickOnlyMode ? MacroPhase.HYPERION_LOOP : MacroPhase.OVERFLUX_WAIT_INITIAL;
        phaseTimer = getRandomizedWait();
        macroStartTime = System.currentTimeMillis();
        lastOverfluxTime = macroStartTime;
        hyperionClickCounter = 0;
        clickOnlyClickCounter = 0;
        clickOnlyState = ClickOnlyState.CLICK_SEQUENCE;
        clickOnlyWaitStartTime = 0;
        healOverrideState = HealOverrideState.IDLE;
        healTimer = 0;
        healReturnSlot = -1;
        lastAtonementUseTime = macroStartTime;

        // Ungrab mouse if enabled in config
        if (FlareConfig.isUngrabbMouseEnabled()) {
            ungrabMouse();
        }

        sendMessage("Macro started! Targeting: " + getEntityDisplayName(flareEntity), Formatting.GREEN);
        if (clickOnlyMode) {
            sendMessage("Mode: " + FlareConfig.getCombatModeName() + " | No item checks required", Formatting.YELLOW);
        } else {
            sendMessage("Mode: " + FlareConfig.getCombatModeName() + " | Overflux slot: " + overfluxSlot + " | Weapon slot: " + hyperionSlot, Formatting.YELLOW);
        }
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
        atonementSlot = -1;
        clickOnlyClickCounter = 0;
        clickOnlyState = ClickOnlyState.CLICK_SEQUENCE;
        clickOnlyWaitStartTime = 0;
        healOverrideState = HealOverrideState.IDLE;
        healTimer = 0;
        healReturnSlot = -1;
        lastAtonementUseTime = 0;

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

        // Priority override: use Wand of Atonement every 20 seconds (except click-only mode)
        if (!isClickOnlyMode() && handleAtonementOverride(client)) {
            return;
        }

        // Verify Flare entity still exists or rescan for new one
        if (flareEntity == null || flareEntity.isRemoved() || 
            client.player.distanceTo(flareEntity) > DETECTION_RANGE) {
            
            // Try to find a new Flare entity
            if (scanForFlare()) {
                sendMessage("Target updated: " + getEntityDisplayName(flareEntity), Formatting.GREEN);
                if (isClickOnlyMode()) {
                    // A previous flare is gone and a new one is now available - restart click sequence
                    clickOnlyState = ClickOnlyState.CLICK_SEQUENCE;
                    clickOnlyClickCounter = 0;
                    clickOnlyWaitStartTime = 0;
                }
                // Continue with current phase - don't reset the macro
            } else {
                // No Flare found - wait and try again next tick
                // Don't stop the macro, just pause until a Flare appears
                if (isClickOnlyMode()) {
                    clickOnlyState = ClickOnlyState.CLICK_SEQUENCE;
                    clickOnlyClickCounter = 0;
                    clickOnlyWaitStartTime = 0;
                }
                return;
            }
        }

        // Check if it's time for next Overflux phase (only in non click-only modes)
        if (!isClickOnlyMode()) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastOverflux = currentTime - lastOverfluxTime;
            if (timeSinceLastOverflux >= (OVERFLUX_INTERVAL * 50) && currentPhase == MacroPhase.HYPERION_LOOP) {
                currentPhase = MacroPhase.OVERFLUX_WAIT_INITIAL;
                phaseTimer = getRandomizedWait();
                lastOverfluxTime = currentTime;
                sendMessage("Starting Overflux phase...", Formatting.YELLOW);
            }
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
                // Overflux phase complete, move to weapon loop
                currentPhase = MacroPhase.HYPERION_LOOP;
                hyperionClickCounter = 0;
                String modeName = FlareConfig.getCombatModeName();
                sendMessage("Overflux activated! Starting " + modeName + " loop...", Formatting.GREEN);
                break;

            case HYPERION_LOOP:
                // Execute weapon attack loop (Hyperion or Fire Veil Wand)
                executeWeaponLoop(client);
                break;
        }
    }

    private static void executeWeaponLoop(MinecraftClient client) {
        int combatMode = FlareConfig.getCombatMode();

        // Mode 3: Flare detect + click only (no overflux/weapon checks)
        if (combatMode == FlareConfig.COMBAT_MODE_FLARE_CLICK_ONLY) {
            executeClickOnlyLoop(client);
            return;
        }

        // Switch to weapon slot (Hyperion or Fire Veil Wand)
        if (hyperionSlot != -1 && client.player.getInventory().getSelectedSlot() != hyperionSlot) {
            client.player.getInventory().setSelectedSlot(hyperionSlot);
            phaseTimer = getRandomizedWait(); // Wait after switch with humanization
            return;
        }

        // Mode 1: Hyperion - Multiple clicks
        if (combatMode == FlareConfig.COMBAT_MODE_HYPERION) {
            if (hyperionClickCounter < FlareConfig.getClickCount()) {
                MouseSimulator.simulateRightClick(client);
                hyperionClickCounter++;
                
                // If we've completed all clicks, set a longer wait before next iteration
                if (hyperionClickCounter >= FlareConfig.getClickCount()) {
                    phaseTimer = getRandomizedWait(); // Longer wait after completing all clicks
                } else {
                    phaseTimer = getRandomizedClickDelay(); // Short delay between clicks
                }
            } else {
                // All clicks done, reset counter and wait before next loop iteration
                hyperionClickCounter = 0;
                phaseTimer = getRandomizedWait();
            }
        }
        // Mode 2: Fire Veil Wand - Single click only
        else if (combatMode == FlareConfig.COMBAT_MODE_FIRE_VEIL) {
            MouseSimulator.simulateRightClick(client);
            phaseTimer = getRandomizedWait(); // Wait before next single click
        }
    }

    private static void executeClickOnlyLoop(MinecraftClient client) {
        if (clickOnlyState == ClickOnlyState.CLICK_SEQUENCE) {
            if (clickOnlyClickCounter < FlareConfig.getClickCount()) {
                MouseSimulator.simulateRightClick(client);
                clickOnlyClickCounter++;
                phaseTimer = getRandomizedClickDelay();
                return;
            }

            clickOnlyState = ClickOnlyState.WAIT_FOR_FLARE_DEATH;
            clickOnlyWaitStartTime = System.currentTimeMillis();
            clickOnlyClickCounter = 0;
            phaseTimer = 1;
            return;
        }

        long waited = System.currentTimeMillis() - clickOnlyWaitStartTime;
        if (waited >= FLARE_DEATH_TIMEOUT_MS) {
            // Flare still alive after timeout - repeat the same click sequence
            clickOnlyState = ClickOnlyState.CLICK_SEQUENCE;
            clickOnlyClickCounter = 0;
            clickOnlyWaitStartTime = 0;
            sendMessage("Flare still alive after 30s. Repeating click sequence...", Formatting.YELLOW);
        }

        phaseTimer = 1;
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

        int combatMode = FlareConfig.getCombatMode();
        if (combatMode == FlareConfig.COMBAT_MODE_FLARE_CLICK_ONLY) {
            return true;
        }

        String weaponToFind = combatMode == FlareConfig.COMBAT_MODE_HYPERION ? "Hyperion" : "Fire Veil Wand";

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

            if (itemName.contains(weaponToFind)) {
                hyperionSlot = i;
            }
        }

        if (overfluxSlot == -1 || hyperionSlot == -1) {
            if (overfluxSlot == -1) {
                sendMessage("Overflux Power Orb not found in hotbar!", Formatting.RED);
            }
            if (hyperionSlot == -1) {
                sendMessage(weaponToFind + " not found in hotbar!", Formatting.RED);
            }
        }

        return overfluxSlot != -1 && hyperionSlot != -1;
    }

    private static boolean handleAtonementOverride(MinecraftClient client) {
        if (healOverrideState == HealOverrideState.WAIT_AFTER_WAND_SWITCH) {
            if (healTimer > 0) {
                healTimer--;
                return true;
            }

            MouseSimulator.simulateRightClick(client);
            if (healReturnSlot != -1 && client.player.getInventory().getSelectedSlot() != healReturnSlot) {
                client.player.getInventory().setSelectedSlot(healReturnSlot);
            }

            lastAtonementUseTime = System.currentTimeMillis();

            healOverrideState = HealOverrideState.WAIT_AFTER_HEAL_CLICK;
            healTimer = getRandomizedClickDelay();
            return true;
        }

        if (healOverrideState == HealOverrideState.WAIT_AFTER_HEAL_CLICK) {
            if (healTimer > 0) {
                healTimer--;
                return true;
            }

            healOverrideState = HealOverrideState.IDLE;
            healReturnSlot = -1;
            return true;
        }

        long now = System.currentTimeMillis();
        if (now - lastAtonementUseTime < ATONEMENT_INTERVAL_MS) {
            return false;
        }

        atonementSlot = findHotbarSlot(client, "Wand of Atonement");
        if (atonementSlot == -1) {
            if (now - lastMissingAtonementWarnAt >= 3000) {
                sendMessage("Wand of Atonement not found in hotbar!", Formatting.RED);
                lastMissingAtonementWarnAt = now;
            }
            return false;
        }

        healReturnSlot = findHotbarSlot(client, "Hyperion");
        if (healReturnSlot == -1) {
            healReturnSlot = hyperionSlot != -1 ? hyperionSlot : client.player.getInventory().getSelectedSlot();
        }

        if (client.player.getInventory().getSelectedSlot() != atonementSlot) {
            client.player.getInventory().setSelectedSlot(atonementSlot);
        }

        healOverrideState = HealOverrideState.WAIT_AFTER_WAND_SWITCH;
        healTimer = getRandomizedClickDelay();
        sendMessage("Using Wand of Atonement...", Formatting.YELLOW);
        return true;
    }

    private static int findHotbarSlot(MinecraftClient client, String itemNamePart) {
        String target = itemNamePart.toLowerCase();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            String stackName = stack.getName().getString().toLowerCase();
            if (stackName.contains(target)) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isClickOnlyMode() {
        return FlareConfig.getCombatMode() == FlareConfig.COMBAT_MODE_FLARE_CLICK_ONLY;
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
