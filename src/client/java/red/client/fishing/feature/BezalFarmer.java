package red.client.fishing.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import red.client.fishing.config.BezalFarmerConfig;
import red.client.flarecombat.mixin.MouseMixin;
import red.client.flarecombat.util.MouseSimulator;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Bezal Farmer - Auto-attacks Bezal entities when within range
 * 
 * Automatically tracks the closest Bezal and attacks it with 3 left-clicks
 * when the player is within 3 blocks. Includes auto-aim functionality.
 * 
 * Features:
 * - Armor stand detection for Bezal name tags
 * - One-target-at-a-time approach (no multi-targeting)
 * - Auto-aim at Bezal before attacking
 * - 3-click attack pattern with configurable delay
 * - Visual highlighting (green in range, yellow too far)
 * 
 * Based on SpikeHelper architecture
 */
public class BezalFarmer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    // Blackhole phase enum
    private enum BlackholePhase {
        IDLE,
        SWAP_TO_BLACKHOLE,
        WAIT_SWAP,
        RIGHT_CLICK,
        WAIT_CLICK,
        SWAP_TO_WEAPON,
        COMPLETE
    }
    
    // Bezal detection pattern - matches armor stand names containing "Bezal" with health format
    // Accepts forms like "300k/300k", "300/300", "1.2m/1.2m", and numbers with commas
    private static final Pattern BEZAL_PATTERN = Pattern.compile("(?i).*bezal.*[\\d,]+(?:\\.\\d+)?[kKmM]?/[\\d,]+(?:\\.\\d+)?[kKmM]?.*");
    private static final double DETECTION_RANGE = 50.0; // Max range to scan for Bezals
    
    // Tracked Bezal entity
    private static Entity trackedBezal = null;
    private static ArmorStandEntity trackedArmorStand = null;
    private static final double LOW_HP_THRESHOLD = 10000.0; // Switch targets when HP falls below 10k
    
    // Attack state
    private static boolean isAttacking = false;
    private static long lastAttackTime = 0;
    private static final long ATTACK_COOLDOWN_MS = 1000; // 1 second cooldown between attack sequences
    
    // Aim Assist state
    private static boolean isAiming = false;
    
    // Weapon state tracking
    private static int lastHeldSlot = -1;
    private static boolean justSwappedToBlackhole = false;
    
    // Blackhole sequence state management (phase-based like FlareCombat)
    private static BlackholePhase blackholePhase = BlackholePhase.IDLE;
    private static int phaseTimer = 0;
    private static int blackholeSlot = -1;
    private static int weaponSlot = -1;
    private static long lastAttackCompletionTime = -1; // Track when last attack finished
    private static final long HP_CHECK_DELAY_MS = 200; // Wait 200ms after attack before checking HP
    private static final int PHASE_WAIT_TICKS = 3; // ~150ms wait (3 ticks at 20ms per tick)
    
    /**
     * Tick update - called every game tick
     * Manages Bezal tracking, auto-aim, and auto-attack
     * Also handles Blackhole phase machine
     */
    public static void tick() {
        if (!BezalFarmerConfig.isEnabled() || client.player == null || client.world == null) {
            stopAimAssist();
            return;
        }
        
        // Process Blackhole phase if active
        if (blackholePhase != BlackholePhase.IDLE) {
            processBlackholePhase();
            // Don't do other actions while executing Blackhole sequence
            return;
        }
        
        // Auto-swap to weapon if not equipped
        if (!isCorrectWeaponEquipped()) {
            swapToWeapon(BezalFarmerConfig.getWeaponName());
            return; // Skip this tick, will continue next tick when weapon is ready
        }
        
        // Validate current tracked Bezal
        if (trackedBezal != null) {
            if (isBezalInvalid(trackedBezal)) {
                clearTrackedBezal();
                stopAimAssist();
            }
        }
        
        // If no tracked Bezal, scan for a new one
        if (trackedBezal == null) {
            scanForClosestBezal();
            stopAimAssist();
        } else {
            // Check if in attack range
            if (isInAttackRange()) {
                // Update Aim Assist if enabled
                if (BezalFarmerConfig.isAutoAimEnabled()) {
                    updateAimAssist();
                }
                
                // Perform attack if not on cooldown
                if (!isAttacking && canAttack()) {
                    performAttack();
                }
            } else {
                stopAimAssist();
            }
        }
        
        // Post-attack HP check for Blackhole sequence (runs with debounce delay)
        if (lastAttackCompletionTime >= 0) {
            checkPostAttackHP();
        }
    }
    
    /**
     * Check if the tracked Bezal is invalid (dead, removed, or out of range)
     */
    private static boolean isBezalInvalid(Entity bezal) {
        if (bezal == null || bezal.isRemoved()) {
            return true;
        }
        
        // Check if it's a living entity and is dead
        if (bezal instanceof LivingEntity && !((LivingEntity) bezal).isAlive()) {
            return true;
        }
        
        // Check if armor stand still exists
        if (trackedArmorStand != null && trackedArmorStand.isRemoved()) {
            return true;
        }
        
        // Check if player is too far away
        if (client.player.distanceTo(bezal) > DETECTION_RANGE) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Clear currently tracked Bezal
     */
    private static void clearTrackedBezal() {
        trackedBezal = null;
        trackedArmorStand = null;
    }
    
    /**
     * Parse the current HP from a Bezal name tag
     * Format: "[Lv80] ♨ Bezal 300k/300k❤" or "Bezal 100/300" etc.
     * Extracts the first number before the "/" character
     * 
     * @return the parsed HP value, or -1 if parsing fails
     */
    private static double parseCurrentHP(String nameTags) {
        if (nameTags == null || nameTags.isEmpty()) {
            return -1;
        }
        
        // Remove formatting codes
        String cleanName = Formatting.strip(nameTags);
        
        // Find the "/" that separates current/max HP
        int slashIndex = cleanName.indexOf('/');
        if (slashIndex <= 0) {
            return -1; // No "/" found or it's at the start
        }
        
        // Extract the substring before the "/"
        String currentHPPart = cleanName.substring(0, slashIndex).trim();
        
        // Find the last number sequence before the "/"
        // This handles cases like "Bezal 300k/300k" -> we get "300k"
        StringBuilder hpStr = new StringBuilder();
        for (int i = currentHPPart.length() - 1; i >= 0; i--) {
            char c = currentHPPart.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == 'k' || c == 'K' || c == 'm' || c == 'M') {
                hpStr.insert(0, c);
            } else if (!hpStr.isEmpty()) {
                // Stop when we hit a non-digit character after already collecting digits
                break;
            }
        }
        
        if (hpStr.isEmpty()) {
            return -1;
        }
        
        String hpValue = hpStr.toString();
        return convertHPStringToNumber(hpValue);
    }
    
    /**
     * Convert HP string like "300k" or "1.2m" to actual number
     * 
     * @param hpStr the HP string (e.g., "300k", "1.2m", "300")
     * @return the numeric HP value
     */
    private static double convertHPStringToNumber(String hpStr) {
        if (hpStr == null || hpStr.isEmpty()) {
            return -1;
        }
        
        hpStr = hpStr.toLowerCase().trim();
        
        try {
            if (hpStr.endsWith("k")) {
                // Parse "k" suffix (thousands)
                double value = Double.parseDouble(hpStr.substring(0, hpStr.length() - 1));
                return value * 1000.0;
            } else if (hpStr.endsWith("m")) {
                // Parse "m" suffix (millions)
                double value = Double.parseDouble(hpStr.substring(0, hpStr.length() - 1));
                return value * 1000000.0;
            } else {
                // Parse plain number
                return Double.parseDouble(hpStr);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Scan for the closest Bezal entity in range
     * Optimized for speed: direct iteration without unnecessary allocations
     */
    private static void scanForClosestBezal() {
        if (client.world == null || client.player == null) {
            return;
        }
        
        ArmorStandEntity closestArmorStand = null;
        double closestDistance = DETECTION_RANGE;
        
        // Direct iteration through world entities with minimal overhead
        for (Entity entity : client.world.getEntities()) {
            // Fast type check before instanceof to avoid expensive casting
            if (entity == null || entity.isRemoved() || !(entity instanceof ArmorStandEntity)) {
                continue;
            }
            
            ArmorStandEntity armorStand = (ArmorStandEntity) entity;
            
            // Skip if no name tag
            if (!armorStand.hasCustomName()) {
                continue;
            }
            
            // Fast Bezal check
            if (!isBezalArmorStand(armorStand)) {
                continue;
            }
            
            // Calculate distance only once
            double distance = client.player.distanceTo(armorStand);
            
            // Update closest if this one is closer and in range
            if (distance < closestDistance) {
                closestArmorStand = armorStand;
                closestDistance = distance;
            }
        }
        
        // If no Bezal found, exit early
        if (closestArmorStand == null) {
            return;
        }
        
        // Find the actual entity below the armor stand
        Entity actualBezal = getEntityBelowArmorStand(closestArmorStand);
        
        if (actualBezal != null) {
            trackedBezal = actualBezal;
            trackedArmorStand = closestArmorStand;
        }
    }
    
    /**
     * Check if an armor stand is a Bezal entity name tag
     */
    private static boolean isBezalArmorStand(ArmorStandEntity armorStand) {
        if (armorStand == null || !armorStand.hasCustomName()) {
            return false;
        }
        
    String rawName = armorStand.getCustomName().getString();
    String name = Formatting.strip(rawName); // Remove formatting/control codes

    // Normalize player name and armor stand name for comparison
    String playerName = client.player != null ? Formatting.strip(client.player.getName().getString()) : "";

    boolean matches = BEZAL_PATTERN.matcher(name).find();
    boolean notPlayer = !name.toLowerCase().contains(playerName.toLowerCase());
        
        return matches && notPlayer;
    }
    
    /**
     * Get the actual entity that an armor stand represents
     * Armor stands float above entities showing their health
     */
    private static Entity getEntityBelowArmorStand(ArmorStandEntity armorStand) {
        if (client.world == null) {
            return null;
        }
        
        // Expand bounding box to find entities below the armor stand
        Box searchBox = armorStand.getBoundingBox().expand(0.5, 3.0, 0.5);
        
        // Search for any living entity (not armor stand, not player)
        List<Entity> nearbyEntities = client.world.getOtherEntities(armorStand, searchBox, entity -> {
            boolean isAlive = !entity.isRemoved() && !entity.equals(client.player);
            boolean isNotArmorStand = !(entity instanceof ArmorStandEntity);
            boolean isLivingEntity = entity instanceof LivingEntity;
            return isAlive && isNotArmorStand && isLivingEntity;
        });
        
        if (nearbyEntities.isEmpty()) {
            return null;
        }
        
        // Return closest entity to the armor stand
        return nearbyEntities.stream()
            .min(Comparator.comparingDouble(entity -> armorStand.distanceTo(entity)))
            .orElse(null);
    }
    
    /**
     * Get the currently tracked Bezal entity (for rendering)
     */
    public static Entity getTrackedBezal() {
        return trackedBezal;
    }
    
    /**
     * Get the distance from player to tracked Bezal
     */
    public static double getDistanceToBezal() {
        if (trackedBezal == null || client.player == null) {
            return -1.0;
        }
        return client.player.distanceTo(trackedBezal);
    }
    
    /**
     * Check if player is in attack range (within 3 blocks)
     */
    public static boolean isInAttackRange() {
        double distance = getDistanceToBezal();
        if (distance < 0) {
            return false;
        }
        
        double attackDistance = BezalFarmerConfig.getAttackDistance();
        return distance <= attackDistance;
    }
    
    /**
     * Get the highlight color for tracked Bezal
     * Always returns green - distance doesn't affect highlighting
     */
    public static int getHighlightColor() {
        return BezalFarmerConfig.getColorInRange(); // Always green regardless of distance
    }
    
    /**
     * Check if can attack (cooldown check)
     */
    private static boolean canAttack() {
        long currentTime = System.currentTimeMillis();
        return currentTime - lastAttackTime >= ATTACK_COOLDOWN_MS;
    }
    
    /**
     * Check HP after attack and trigger Blackhole if below 10k
     */
    private static void checkPostAttackHP() {
        if (!BezalFarmerConfig.isBlackholeEnabled()) {
            return;
        }
        
        if (trackedArmorStand == null || lastAttackCompletionTime < 0) {
            return;
        }
        
        long timeSinceAttack = System.currentTimeMillis() - lastAttackCompletionTime;
        if (timeSinceAttack < HP_CHECK_DELAY_MS) {
            return;
        }
        
        if (!trackedArmorStand.hasCustomName()) {
            return;
        }
        
        String rawName = trackedArmorStand.getCustomName().getString();
        double currentHP = parseCurrentHP(rawName);
        
        if (currentHP < LOW_HP_THRESHOLD && currentHP > 0) {
            // HP is low - start Blackhole sequence (phase-based, not threaded)
            initiateBlackholeSequence();
            lastAttackCompletionTime = -1;
        } else {
            lastAttackCompletionTime = -1;
        }
    }
    
    /**
     * Start the Blackhole sequence - phase-based like FlareCombat
     */
    private static void initiateBlackholeSequence() {
        if (client.player == null || client.player.getInventory() == null) {
            return;
        }
        
        blackholeSlot = findBlackholeInInventory();
        if (blackholeSlot == -1) {
            return;
        }
        
        weaponSlot = client.player.getInventory().getSelectedSlot();
        blackholePhase = BlackholePhase.SWAP_TO_BLACKHOLE;
        phaseTimer = 0;
    }
    
    /**
     * Process Blackhole phase machine - called every tick when executing sequence
     */
    private static void processBlackholePhase() {
        // Handle phase timer countdown
        if (phaseTimer > 0) {
            phaseTimer--;
            return;
        }
        
        // Execute current phase
        switch (blackholePhase) {
            case SWAP_TO_BLACKHOLE:
                // Swap to Blackhole slot
                client.player.getInventory().setSelectedSlot(blackholeSlot);
                blackholePhase = BlackholePhase.WAIT_SWAP;
                phaseTimer = PHASE_WAIT_TICKS;
                break;
            
            case WAIT_SWAP:
                // Wait complete, now right-click
                blackholePhase = BlackholePhase.RIGHT_CLICK;
                break;
            
            case RIGHT_CLICK:
                // Right-click using MouseSimulator (same as FlareCombat)
                MouseSimulator.simulateRightClick(client);
                blackholePhase = BlackholePhase.WAIT_CLICK;
                phaseTimer = PHASE_WAIT_TICKS;
                break;
            
            case WAIT_CLICK:
                // Click complete, swap back to weapon
                blackholePhase = BlackholePhase.SWAP_TO_WEAPON;
                break;
            
            case SWAP_TO_WEAPON:
                // Swap back to weapon
                client.player.getInventory().setSelectedSlot(weaponSlot);
                blackholePhase = BlackholePhase.COMPLETE;
                phaseTimer = 1; // One more tick to complete
                break;
            
            case COMPLETE:
                // Sequence complete
                blackholePhase = BlackholePhase.IDLE;
                blackholeSlot = -1;
                weaponSlot = -1;
                break;
            
            case IDLE:
                // Should not happen during sequence
                break;
        }
    }
    
    /**
     * Perform 3-click attack on the tracked Bezal
     * Aims at the Bezal first, then executes 3 left-clicks with delay
     * Marks attack completion time for post-attack HP checking
     */
    private static void performAttack() {
        if (trackedBezal == null || client.player == null || client.getWindow() == null) {
            return;
        }
        
        isAttacking = true;
        
        // Aim at the Bezal if auto-aim is enabled
        if (BezalFarmerConfig.isAutoAimEnabled()) {
            aimAtBezal();
        }
        
        // Execute clicks in a separate thread to avoid blocking
        new Thread(() -> {
            try {
                int clickCount = BezalFarmerConfig.getClickCount();
                long clickDelay = BezalFarmerConfig.getClickDelayMs();
                
                for (int i = 0; i < clickCount; i++) {
                    simulateLeftClick();
                    
                    // Wait between clicks (except after the last click)
                    if (i < clickCount - 1) {
                        Thread.sleep(clickDelay);
                    }
                }
                
                lastAttackTime = System.currentTimeMillis();
                lastAttackCompletionTime = System.currentTimeMillis(); // Mark attack completion
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                isAttacking = false;
            }
        }).start();
    }
    
    /**
     * Simulate a left click using MouseMixin (FishMaster's approach)
     */
    private static void simulateLeftClick() {
        if (client == null || client.getWindow() == null) {
            return;
        }
        
        try {
            long windowHandle = client.getWindow().getHandle();
            MouseMixin mouseMixin = (MouseMixin) client.mouse;
            
            // Press
            mouseMixin.invokeOnMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0);
            
            // Human-like delay
            try {
                Thread.sleep(10 + new Random().nextInt(20)); // 10-30ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Release
            mouseMixin.invokeOnMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
            
        } catch (Exception e) {
            // Silent failure
        }
    }
    
    /**
     * Update aim assist functionality - aims at tracked Bezal smoothly
     */
    private static void updateAimAssist() {
        if (trackedBezal == null || client.player == null) {
            stopAimAssist();
            return;
        }
        
        if (!isAiming) {
            isAiming = true;
        }
        
        aimAtBezal();
    }
    
    /**
     * Stop aim assist
     */
    private static void stopAimAssist() {
        isAiming = false;
    }
    
    /**
     * Aim the player's camera at the tracked Bezal with smooth interpolation
     */
    private static void aimAtBezal() {
        if (trackedBezal == null || client.player == null) {
            return;
        }
        
        // Get Bezal position (center of entity at mid-height)
        Vec3d bezalPos = trackedBezal.getPos().add(0, trackedBezal.getHeight() * 0.5, 0);
        
        // Get player eye position
        Vec3d eyePos = client.player.getEyePos();
        
        // Calculate direction vector
        Vec3d direction = bezalPos.subtract(eyePos);
        
        // Calculate horizontal distance
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        
        if (horizontalDistance < 0.1) {
            return;
        }
        
        // Calculate target yaw and pitch
        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontalDistance));
        
        // Clamp pitch
        targetPitch = Math.max(-90.0f, Math.min(90.0f, targetPitch));
        
        // Get current rotation
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();
        
        // Calculate angle differences with proper wrapping
        float yawDiff = wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        
        // Smooth interpolation (0.3f for faster snap when attacking)
        float smoothingFactor = 0.3f;
        
        // Apply smooth rotation
        float newYaw = currentYaw + (yawDiff * smoothingFactor);
        float newPitch = currentPitch + (pitchDiff * smoothingFactor);
        
        // Apply the rotation
        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
    }
    
    /**
     * Wrap angle to [-180, 180] range
     */
    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped > 180.0f) {
            wrapped -= 360.0f;
        } else if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }
    
    /**
     * Toggle enabled state
     */
    public static boolean toggle() {
        boolean newState = BezalFarmerConfig.toggle();
        
        if (!newState) {
            clearTrackedBezal();
            sendMessage("Bezal Farmer disabled", Formatting.RED);
        } else {
            sendMessage("Bezal Farmer enabled - Looking for Bezals...", Formatting.GREEN);
        }
        
        return newState;
    }
    
    /**
     * Get enabled state
     */
    public static boolean isEnabled() {
        return BezalFarmerConfig.isEnabled();
    }
    
    /**
     * Get status text for GUI display
     */
    public static String getStatusText() {
        if (!BezalFarmerConfig.isEnabled()) {
            return "Disabled";
        }
        
        if (trackedBezal == null) {
            return "Enabled - Searching...";
        }
        
        double distance = getDistanceToBezal();
        String distanceStr = String.format("%.1f", distance);
        double attackDistance = BezalFarmerConfig.getAttackDistance();
        
        if (isInAttackRange()) {
            return String.format("Tracking - %s blocks ✓ (≤%.0f) ATTACKING", distanceStr, attackDistance);
        } else {
            return String.format("Tracking - %s blocks (need ≤%.0f)", distanceStr, attackDistance);
        }
    }
    
    /**
     * Send a message to the player
     */
    private static void sendMessage(String message, Formatting formatting) {
        if (client.player != null) {
            client.player.sendMessage(
                Text.literal("[BezalFarmer] " + message).formatted(formatting),
                false
            );
        }
    }
    
    /**
     * Reset the farmer (clear tracked Bezal)
     */
    public static void reset() {
        clearTrackedBezal();
        stopAimAssist();
        isAttacking = false;
    }
    
    /**
     * Check if the correct weapon (Prime Huntaxe) is currently equipped
     */
    private static boolean isCorrectWeaponEquipped() {
        if (client.player == null) {
            return false;
        }
        
        String heldItemName = client.player.getMainHandStack().getName().getString();
        String weaponName = BezalFarmerConfig.getWeaponName();
        
        return heldItemName.contains(weaponName);
    }
    
    /**
     * Swap to the specified weapon by searching the hotbar
     */
    private static void swapToWeapon(String weaponName) {
        if (client.player == null || client.player.getInventory() == null) {
            return;
        }
        
        // Search hotbar slots (0-8) for the weapon
        for (int i = 0; i < 9; i++) {
            String slotItemName = client.player.getInventory().getStack(i).getName().getString();
            if (slotItemName.contains(weaponName)) {
                client.player.getInventory().setSelectedSlot(i);
                lastHeldSlot = i;
                return;
            }
        }
        
        sendMessage("Warning: Could not find " + weaponName + " in hotbar", Formatting.RED);
    }
    
    /**
     * Search inventory for Blackhole item by name
     * Searches all slots (0-35) including hotbar and main inventory
     * 
     * @return slot index if found, -1 if not found
     */
    private static int findBlackholeInInventory() {
        if (client.player == null || client.player.getInventory() == null) {
            return -1;
        }
        
        String blackholeItemName = BezalFarmerConfig.getBlackholeItemName();
        
        // Search all inventory slots (0-8 hotbar, 9-35 main inventory)
        for (int i = 0; i < 36; i++) {
            String itemName = client.player.getInventory().getStack(i).getName().getString();
            if (itemName.contains(blackholeItemName)) {
                return i;
            }
        }
        
        return -1; // Not found
    }
    
}
