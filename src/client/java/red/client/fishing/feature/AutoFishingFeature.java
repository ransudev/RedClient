package red.client.fishing.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import red.client.fishing.config.FishConfig;
import red.client.fishing.util.FishMouseSimulator;

import java.util.Random;

public class AutoFishingFeature {
    private static boolean enabled = false;
    private static int delayTimer = 0;
    private static int castCooldownTimer = 0;
    private static int reelingDelayTimer = 0;
    private static long lastDetectionTime = 0;
    private static boolean mouseWasGrabbed = false;
    private static final Random random = new Random();
    private static final long DETECTION_COOLDOWN = 50;

    private enum FishingState {
        IDLE,
        CASTING,
        FISHING
    }

    private static FishingState currentState = FishingState.IDLE;

    public static void toggle() {
        enabled = !enabled;

        if (!enabled) {
            stop();
            if (FishConfig.isUngrabMouseEnabled()) {
                restoreMouseGrab();
            }
        } else {
            if (!performPreStartChecks()) {
                enabled = false;
                return;
            }
            switchToFishingRod();
            if (FishConfig.isUngrabMouseEnabled()) {
                ungrabMouse();
            }
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text prefix = Text.literal("[AutoFish] ").formatted(Formatting.AQUA, Formatting.BOLD);
            if (enabled) {
                Text message = Text.literal("Auto fishing enabled").formatted(Formatting.GREEN);
                client.player.sendMessage(prefix.copy().append(message), false);
            } else {
                Text message = Text.literal("Auto fishing disabled").formatted(Formatting.RED);
                client.player.sendMessage(prefix.copy().append(message), false);
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void tick() {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            stop();
            return;
        }

        ItemStack mainHand = client.player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = client.player.getStackInHand(Hand.OFF_HAND);

        boolean hasFishingRod = mainHand.getItem() instanceof FishingRodItem ||
                               offHand.getItem() instanceof FishingRodItem;

        if (!hasFishingRod) {
            // Only show error and disable if Sea Creature Killer is not active (to avoid spam during weapon swapping)
            if (!SeaCreatureKiller.isEnabled()) {
                enabled = false;
                resetFishingState();
                sendMessage("Auto fishing stopped: No fishing rod in hands", Formatting.RED);
            } else {
                // Don't disable the macro when SCK is active, just pause fishing logic
                resetFishingState();
            }
            return;
        }

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        if (castCooldownTimer > 0) {
            castCooldownTimer--;
        }

        if (reelingDelayTimer > 0) {
            reelingDelayTimer--;
            if (reelingDelayTimer == 0) {
                FishMouseSimulator.simulateRightClick(client);
                resetFishingState();
                delayTimer = Math.max(30, getRandomizedRecastDelay());
            }
            return;
        }

        switch (currentState) {
            case IDLE:
                if (client.player.fishHook == null) {
                    startCasting(client);
                } else {
                    if (hasBobberInWater(client.player)) {
                        currentState = FishingState.FISHING;
                    }
                }
                break;

            case CASTING:
                if (client.player.fishHook != null && hasBobberInWater(client.player)) {
                    currentState = FishingState.FISHING;
                } else if (client.player.fishHook != null) {
                    // Waiting for bobber to land
                } else {
                    FishMouseSimulator.simulateRightClick(client);
                    delayTimer = 10;
                }
                break;

            case FISHING:
                if (client.player.fishHook == null) {
                    resetFishingState();
                    delayTimer = getRandomizedRecastDelay();
                } else if (hasBobberInWater(client.player)) {
                    if (reelingDelayTimer == 0 && detectArmorStandFishBite(client)) {
                        long currentTimeMillis = System.currentTimeMillis();
                        if (currentTimeMillis - lastDetectionTime >= DETECTION_COOLDOWN) {
                            lastDetectionTime = currentTimeMillis;
                            reelingDelayTimer = getRandomizedReelingDelay();
                        }
                    }
                }
                break;
        }
    }

    private static boolean performPreStartChecks() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null) {
            sendMessage("Cannot start: Player or world is null", Formatting.RED);
            return false;
        }

        if (!hasValidFishingRod()) {
            sendMessage("Cannot start: No fishing rod found in inventory", Formatting.RED);
            return false;
        }

        return true;
    }

    private static boolean hasValidFishingRod() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;

        ItemStack mainHand = client.player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = client.player.getStackInHand(Hand.OFF_HAND);

        if ((mainHand.getItem() instanceof FishingRodItem && !mainHand.isEmpty()) ||
            (offHand.getItem() instanceof FishingRodItem && !offHand.isEmpty())) {
            return true;
        }

        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() instanceof FishingRodItem && !stack.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static void ungrabMouse() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.mouse != null) {
            mouseWasGrabbed = client.mouse.isCursorLocked();
            client.mouse.unlockCursor();
        }
    }

    private static void restoreMouseGrab() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.mouse != null && mouseWasGrabbed && !enabled) {
            client.mouse.lockCursor();
        }
    }

    private static void startCasting(MinecraftClient client) {
        if (client.player == null) return;

        FishMouseSimulator.simulateRightClick(client);
        currentState = FishingState.CASTING;
        delayTimer = 10;
    }

    private static void resetFishingState() {
        currentState = FishingState.IDLE;
        castCooldownTimer = 0;
        reelingDelayTimer = 0;
    }

    private static void stop() {
        resetFishingState();
        delayTimer = 0;
        reelingDelayTimer = 0;
        lastDetectionTime = 0;
    }

    private static boolean switchToFishingRod() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) {
            return false;
        }

        ItemStack mainHand = client.player.getStackInHand(Hand.MAIN_HAND);
        if (mainHand.getItem() instanceof FishingRodItem) {
            return true;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() instanceof FishingRodItem) {
                client.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }

        for (int i = 9; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() instanceof FishingRodItem) {
                int currentSlot = client.player.getInventory().getSelectedSlot();
                client.interactionManager.clickSlot(
                    client.player.playerScreenHandler.syncId,
                    i,
                    currentSlot,
                    net.minecraft.screen.slot.SlotActionType.SWAP,
                    client.player
                );
                return true;
            }
        }

        return false;
    }

    private static boolean hasBobberInWater(PlayerEntity player) {
        FishingBobberEntity bobber = player.fishHook;
        if (bobber == null) return false;
        return bobber.isTouchingWater() || bobber.isInLava();
    }

    private static boolean detectArmorStandFishBite(MinecraftClient client) {
        if (client.player == null || client.world == null || !hasBobberInWater(client.player)) {
            return false;
        }

        var entities = client.world.getEntities();
        if (entities == null) return false;

        for (Entity entity : entities) {
            if (entity instanceof ArmorStandEntity armorStand) {
                if (armorStand.hasCustomName()) {
                    Text customName = armorStand.getCustomName();
                    if (customName != null) {
                        String nameString = customName.getString();
                        if ("!!!".equals(nameString)) {
                            double distance = armorStand.squaredDistanceTo(client.player);
                            if (distance <= 50 * 50) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static int getRandomizedRecastDelay() {
        float baseDelay = FishConfig.getRecastDelay();
        float variance = baseDelay * 0.2f;
        float randomOffset = (random.nextFloat() * 2f - 1f) * variance;
        int randomizedDelay = Math.round(baseDelay + randomOffset);
        return Math.max(2, Math.min(50, randomizedDelay));
    }

    private static int getRandomizedReelingDelay() {
        float baseDelay = FishConfig.getReelingDelay();
        float variance = baseDelay * 0.15f;
        float randomOffset = (random.nextFloat() * 2f - 1f) * variance;
        int randomizedDelay = Math.round(baseDelay + randomOffset);
        return Math.max(2, Math.min(15, randomizedDelay));
    }

    private static void sendMessage(String message, Formatting formatting) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("[AutoFish] ").formatted(Formatting.AQUA, Formatting.BOLD)
                            .append(Text.literal(message).formatted(formatting)),
                    false
            );
        }
    }
}
