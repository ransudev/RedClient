package red.client.fishing.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import red.client.fishing.config.SpikeHelperConfig;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spike Helper - Assists with catching Spike sea creatures
 * 
 * Spikes can only be caught when the player is exactly 9 blocks away.
 * This feature tracks the closest Spike and highlights it:
 * - Red: Too close (< 9 blocks)
 * - Green: Correct distance (~9 blocks within tolerance)
 * 
 * Performance: Lightweight single-entity tracking with automatic switching
 */
public class SpikeHelper {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    // Spike detection pattern - matches "[Lv5] ⸙⚓☮ Spike 150/150❤" format
    private static final Pattern SPIKE_PATTERN = Pattern.compile("(?i).*spike.*\\d+/\\d+.*");
    private static final double DETECTION_RANGE = 50.0; // Max range to scan for spikes
    
    // Tracked spike entity
    private static Entity trackedSpike = null;
    private static ArmorStandEntity trackedArmorStand = null;
    private static long lastScanTime = 0;
    private static final long SCAN_INTERVAL_MS = 1000; // Rescan every second if no spike is tracked
    
    // Aim Assist state
    private static boolean isAiming = false;
    
    /**
     * Tick update - called every game tick
     * Manages spike tracking and validation
     */
    public static void tick() {
        if (!SpikeHelperConfig.isEnabled() || client.player == null || client.world == null) {
            stopAimAssist(); // Stop aim assist if feature is disabled
            return;
        }
        
        // Validate current tracked spike
        if (trackedSpike != null) {
            if (isSpikeInvalid(trackedSpike)) {
                clearTrackedSpike();
                stopAimAssist();
            }
        }
        
        // If no tracked spike, scan for a new one (with rate limiting)
        if (trackedSpike == null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScanTime >= SCAN_INTERVAL_MS) {
                scanForClosestSpike();
                lastScanTime = currentTime;
            }
            stopAimAssist(); // No spike to aim at
        } else {
            // Update Aim Assist if enabled
            if (SpikeHelperConfig.isAimAssistEnabled() && isAtCorrectDistance()) {
                updateAimAssist();
            } else {
                stopAimAssist();
            }
        }
    }
    
    /**
     * Check if the tracked spike is invalid (dead, removed, or out of range)
     */
    private static boolean isSpikeInvalid(Entity spike) {
        if (spike == null || spike.isRemoved()) {
            return true;
        }
        
        // Check if armor stand still exists
        if (trackedArmorStand != null && trackedArmorStand.isRemoved()) {
            return true;
        }
        
        // Check if player is too far away
        if (client.player.distanceTo(spike) > DETECTION_RANGE) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Clear currently tracked spike
     */
    private static void clearTrackedSpike() {
        if (trackedSpike != null) {
            sendMessage("Lost track of Spike (removed or out of range)", Formatting.YELLOW);
        }
        trackedSpike = null;
        trackedArmorStand = null;
    }
    
    /**
     * Scan for the closest Spike entity in range
     * Uses the same scanning method as FlareMacroFeature
     */
    private static void scanForClosestSpike() {
        if (client.world == null || client.player == null) {
            return;
        }
        
        ArmorStandEntity closestArmorStand = null;
        double closestDistance = DETECTION_RANGE;
        int spikesFound = 0;
        int armorStandsChecked = 0;
        
        // Iterate through all entities in the world (same as FlareMacroFeature)
        for (Entity entity : client.world.getEntities()) {
            if (entity != null && entity instanceof ArmorStandEntity && entity.hasCustomName()) {
                ArmorStandEntity armorStand = (ArmorStandEntity) entity;
                armorStandsChecked++;
                
                String name = armorStand.getCustomName().getString();
                
                // Debug: Show first few armor stand names being checked
                if (armorStandsChecked <= 3) {
                    sendMessage(String.format("Checking armor stand: '%s'", name), Formatting.GRAY);
                }
                
                if (isSpikeArmorStand(armorStand)) {
                    spikesFound++;
                    double distance = client.player.distanceTo(armorStand);
                    
                    // Debug message for each Spike found
                    sendMessage(String.format("Found Spike armor stand #%d at %.1f blocks: '%s'", 
                        spikesFound, distance, name), Formatting.YELLOW);
                    
                    if (distance <= DETECTION_RANGE && distance < closestDistance) {
                        closestArmorStand = armorStand;
                        closestDistance = distance;
                    }
                }
            }
        }
        
        sendMessage(String.format("Scan complete: checked %d armor stands, found %d Spikes", 
            armorStandsChecked, spikesFound), Formatting.GRAY);
        
        if (closestArmorStand == null) {
            if (spikesFound > 0) {
                sendMessage(String.format("Found %d Spike(s) but all outside detection range (%.0f blocks)", 
                    spikesFound, DETECTION_RANGE), Formatting.RED);
            }
            return;
        }
        
        // Find the actual entity below the armor stand
        sendMessage(String.format("Closest Spike armor stand at %.1f blocks - searching for entity below...", 
            closestDistance), Formatting.AQUA);
        
        Entity actualSpike = getEntityBelowArmorStand(closestArmorStand);
        
        if (actualSpike != null) {
            trackedSpike = actualSpike;
            trackedArmorStand = closestArmorStand;
            sendMessage(String.format("✓ Now tracking Spike entity (Type: %s) - Stand exactly 9 blocks away!", 
                actualSpike.getType().toString()), Formatting.GREEN);
        } else {
            sendMessage("✗ Could not find entity below armor stand - highlighting may not work!", Formatting.RED);
        }
    }
    
    /**
     * Check if an armor stand is a Spike entity name tag
     */
    private static boolean isSpikeArmorStand(ArmorStandEntity armorStand) {
        if (armorStand == null || !armorStand.hasCustomName()) {
            return false;
        }
        
        String name = armorStand.getCustomName().getString();
        Matcher matcher = SPIKE_PATTERN.matcher(name);
        boolean matches = matcher.find();
        boolean notPlayer = !name.toLowerCase().contains(client.player.getName().getString().toLowerCase());
        
        // Debug first match attempt
        if (matches && notPlayer) {
            sendMessage(String.format("✓ Pattern matched: '%s'", name), Formatting.GREEN);
        }
        
        // Must contain "Spike" and health format
        return matches && notPlayer;
    }
    
    /**
     * Get the actual entity that an armor stand represents
     * Armor stands float above sea creatures showing their health
     * Spikes are pufferfish entities in Minecraft
     */
    private static Entity getEntityBelowArmorStand(ArmorStandEntity armorStand) {
        if (client.world == null) {
            return null;
        }
        
        // Expand bounding box to find entities below the armor stand
        Box searchBox = armorStand.getBoundingBox().expand(0.5, 3.0, 0.5);
        
        // First try to find pufferfish specifically (Spikes are pufferfish)
        List<Entity> nearbyPufferfish = client.world.getOtherEntities(armorStand, searchBox, entity -> {
            return entity instanceof PufferfishEntity && !entity.isRemoved();
        });
        
        if (!nearbyPufferfish.isEmpty()) {
            sendMessage("Found pufferfish entity below armor stand!", Formatting.AQUA);
            // Return closest pufferfish to the armor stand
            return nearbyPufferfish.stream()
                .min(Comparator.comparingDouble(entity -> armorStand.distanceTo(entity)))
                .orElse(null);
        }
        
        // Fallback: search for any entity (for debugging)
        List<Entity> nearbyEntities = client.world.getOtherEntities(armorStand, searchBox, entity -> {
            boolean isAlive = !entity.isRemoved() && !entity.equals(client.player);
            boolean isNotArmorStand = !(entity instanceof ArmorStandEntity);
            return isAlive && isNotArmorStand;
        });
        
        if (!nearbyEntities.isEmpty()) {
            Entity found = nearbyEntities.stream()
                .min(Comparator.comparingDouble(entity -> armorStand.distanceTo(entity)))
                .orElse(null);
            if (found != null) {
                sendMessage("Found non-pufferfish entity: " + found.getType().toString(), Formatting.YELLOW);
            }
            return found;
        }
        
        return null;
    }
    
    /**
     * Get the currently tracked spike entity (for rendering)
     */
    public static Entity getTrackedSpike() {
        return trackedSpike;
    }
    
    /**
     * Get the distance from player to tracked spike
     */
    public static double getDistanceToSpike() {
        if (trackedSpike == null || client.player == null) {
            return -1.0;
        }
        return client.player.distanceTo(trackedSpike);
    }
    
    /**
     * Check if player is at the correct distance (for green highlight)
     * Spikes can be caught when player is 9+ blocks away (up to detection range)
     */
    public static boolean isAtCorrectDistance() {
        double distance = getDistanceToSpike();
        if (distance < 0) {
            return false;
        }
        
        double minDistance = SpikeHelperConfig.getTargetDistance(); // 9.0 blocks minimum
        
        // Green when distance is >= 9 blocks (and within detection range)
        return distance >= minDistance && distance <= DETECTION_RANGE;
    }
    
    /**
     * Check if player is too close (for red highlight)
     * Red when player is less than 9 blocks away
     */
    public static boolean isTooClose() {
        double distance = getDistanceToSpike();
        if (distance < 0) {
            return false;
        }
        
        double minDistance = SpikeHelperConfig.getTargetDistance(); // 9.0 blocks minimum
        
        // Red when distance is < 9 blocks
        return distance < minDistance;
    }
    
    /**
     * Get the appropriate highlight color based on distance
     */
    public static int getHighlightColor() {
        if (isAtCorrectDistance()) {
            return SpikeHelperConfig.getColorCorrect(); // Green
        } else {
            return SpikeHelperConfig.getColorTooClose(); // Red (too close or too far)
        }
    }
    
    /**
     * Toggle enabled state
     */
    public static boolean toggle() {
        boolean newState = SpikeHelperConfig.toggle();
        
        if (!newState) {
            clearTrackedSpike();
            sendMessage("Spike Helper disabled", Formatting.RED);
        } else {
            sendMessage("Spike Helper enabled - Looking for Spikes...", Formatting.GREEN);
        }
        
        return newState;
    }
    
    /**
     * Get enabled state
     */
    public static boolean isEnabled() {
        return SpikeHelperConfig.isEnabled();
    }
    
    /**
     * Get status text for GUI display
     */
    public static String getStatusText() {
        if (!SpikeHelperConfig.isEnabled()) {
            return "Disabled";
        }
        
        if (trackedSpike == null) {
            return "Enabled - Searching...";
        }
        
        double distance = getDistanceToSpike();
        String distanceStr = String.format("%.1f", distance);
        double minDistance = SpikeHelperConfig.getTargetDistance();
        
        if (isAtCorrectDistance()) {
            return String.format("Tracking - %s blocks ✓ (≥%.0f blocks)", distanceStr, minDistance);
        } else {
            return String.format("Tracking - %s blocks (TOO CLOSE, need ≥%.0f)", distanceStr, minDistance);
        }
    }
    
    /**
     * Send a message to the player
     */
    private static void sendMessage(String message, Formatting formatting) {
        if (client.player != null) {
            client.player.sendMessage(
                Text.literal("[SpikeHelper] " + message).formatted(formatting),
                false
            );
        }
    }
    
    /**
     * Reset the helper (clear tracked spike)
     */
    public static void reset() {
        clearTrackedSpike();
    }
    
    /**
     * Update aim assist functionality - aims at tracked spike smoothly
     */
    private static void updateAimAssist() {
        if (trackedSpike == null || client.player == null) {
            stopAimAssist();
            return;
        }
        
        // Mark as actively aiming
        if (!isAiming) {
            isAiming = true;
        }
        
        // Aim at the spike
        aimAtSpike();
    }
    
    /**
     * Stop aim assist and restore player state
     */
    private static void stopAimAssist() {
        if (!isAiming) {
            return;
        }
        
        // Reset state
        isAiming = false;
    }
    
    /**
     * Aim the player's camera at the tracked spike with smooth interpolation
     */
    private static void aimAtSpike() {
        if (trackedSpike == null || client.player == null) {
            return;
        }
        
        // Get spike position (center of entity at mid-height for better aiming)
        Vec3d spikePos = trackedSpike.getPos().add(0, trackedSpike.getHeight() * 0.5, 0);
        
        // Get player eye position
        Vec3d eyePos = client.player.getEyePos();
        
        // Calculate direction vector
        Vec3d direction = spikePos.subtract(eyePos);
        
        // Calculate horizontal distance for proper pitch calculation
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        
        // Prevent aiming issues when too close
        if (horizontalDistance < 1.0) {
            return;
        }
        
        // Calculate target yaw and pitch
        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontalDistance));
        
        // Clamp pitch to reasonable range (prevent extreme angles)
        targetPitch = Math.max(-60.0f, Math.min(45.0f, targetPitch));
        
        // Get current rotation
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();
        
        // Calculate angle differences with proper wrapping for yaw
        float yawDiff = wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        
        // Smooth interpolation factor (lower = smoother, higher = faster)
        // Using 0.15f for very smooth, natural movement similar to FishMaster's RENDER_SMOOTHING
        float smoothingFactor = 0.15f;
        
        // Apply smooth rotation with interpolation
        float newYaw = currentYaw + (yawDiff * smoothingFactor);
        float newPitch = currentPitch + (pitchDiff * smoothingFactor);
        
        // Apply the rotation
        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
    }
    
    /**
     * Wrap angle to [-180, 180] range to prevent 360° jumps
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
}
