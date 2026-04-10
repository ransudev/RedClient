package red.client.fishing.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import red.client.fishing.config.MobHighlightConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Mob Highlight - Highlights any mob by name pattern
 * 
 * Users can specify a mob name (or partial name) and all matching mobs will be highlighted.
 * Example: /highlight bezal will highlight all mobs with "bezal" in their name
 * 
 * Performance: Efficient multi-entity tracking with automatic cleanup
 */
public class MobHighlight {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    // Tracked entities
    private static final List<Entity> trackedMobs = new ArrayList<>();
    private static final List<ArmorStandEntity> trackedArmorStands = new ArrayList<>();
    
    private static long lastScanTime = 0;
    private static final long SCAN_INTERVAL_MS = 1000; // Rescan every second
    
    private static Pattern mobNamePattern = null;
    
    /**
     * Tick update - called every game tick
     * Manages mob tracking and validation
     */
    public static void tick() {
        if (!MobHighlightConfig.isEnabled() || client.player == null || client.world == null) {
            return;
        }
        
        // Check if we have a valid target name
        String targetName = MobHighlightConfig.getTargetMobName();
        if (targetName == null || targetName.trim().isEmpty()) {
            clearTrackedMobs();
            return;
        }
        
        // Update pattern if target name changed
        updatePattern(targetName);
        
        // Validate currently tracked mobs
        validateTrackedMobs();
        
        // Scan for new mobs periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScanTime >= SCAN_INTERVAL_MS) {
            scanForMobs();
            lastScanTime = currentTime;
        }
    }
    
    /**
     * Update the pattern for matching mob names
     */
    private static void updatePattern(String targetName) {
        try {
            // Case-insensitive pattern that matches mob names containing the target string
            String patternStr = "(?i).*" + Pattern.quote(targetName) + ".*";
            mobNamePattern = Pattern.compile(patternStr);
        } catch (Exception e) {
            sendMessage("Invalid mob name pattern: " + e.getMessage(), Formatting.RED);
            mobNamePattern = null;
        }
    }
    
    /**
     * Validate currently tracked mobs (remove dead/removed/out of range entities)
     */
    private static void validateTrackedMobs() {
        double range = MobHighlightConfig.getDetectionRange();
        
        // Remove invalid mobs
        trackedMobs.removeIf(entity -> {
            if (entity == null || entity.isRemoved()) {
                return true;
            }
            if (client.player.distanceTo(entity) > range) {
                return true;
            }
            return false;
        });
        
        // Remove invalid armor stands
        trackedArmorStands.removeIf(armorStand -> {
            return armorStand == null || armorStand.isRemoved();
        });
    }
    
    /**
     * Scan for mobs matching the target name pattern
     */
    private static void scanForMobs() {
        if (client.world == null || client.player == null || mobNamePattern == null) {
            return;
        }
        
        double range = MobHighlightConfig.getDetectionRange();
        int mobsFound = 0;
        
        // Clear previous tracking
        trackedMobs.clear();
        trackedArmorStands.clear();
        
        // Iterate through all entities in the world
        for (Entity entity : client.world.getEntities()) {
            if (entity != null && entity instanceof ArmorStandEntity && entity.hasCustomName()) {
                ArmorStandEntity armorStand = (ArmorStandEntity) entity;
                
                String name = armorStand.getCustomName().getString();
                
                // Check if name matches pattern
                if (mobNamePattern.matcher(name).find()) {
                    double distance = client.player.distanceTo(armorStand);
                    
                    if (distance <= range) {
                        mobsFound++;
                        
                        // Find the actual entity below the armor stand
                        Entity actualMob = getEntityBelowArmorStand(armorStand);
                        
                        if (actualMob != null) {
                            trackedMobs.add(actualMob);
                            trackedArmorStands.add(armorStand);
                        }
                    }
                }
            }
        }
        
        // Only send messages if we just started tracking or found new mobs
        if (mobsFound > 0 && trackedMobs.size() != mobsFound) {
            sendMessage(String.format("Tracking %d mob(s) matching '%s'", 
                trackedMobs.size(), MobHighlightConfig.getTargetMobName()), Formatting.GREEN);
        }
    }
    
    /**
     * Get the actual entity that an armor stand represents
     * Armor stands float above entities showing their health/name
     */
    private static Entity getEntityBelowArmorStand(ArmorStandEntity armorStand) {
        if (client.world == null) {
            return null;
        }
        
        // Expand bounding box to find entities below the armor stand
        net.minecraft.util.math.Box searchBox = armorStand.getBoundingBox().expand(0.5, 3.0, 0.5);
        
        // Search for any non-armor-stand entity
        List<Entity> nearbyEntities = client.world.getOtherEntities(armorStand, searchBox, entity -> {
            boolean isAlive = !entity.isRemoved() && !entity.equals(client.player);
            boolean isNotArmorStand = !(entity instanceof ArmorStandEntity);
            return isAlive && isNotArmorStand;
        });
        
        if (!nearbyEntities.isEmpty()) {
            // Return closest entity to the armor stand
            return nearbyEntities.stream()
                .min((e1, e2) -> Double.compare(armorStand.distanceTo(e1), armorStand.distanceTo(e2)))
                .orElse(null);
        }
        
        return null;
    }
    
    /**
     * Clear all tracked mobs
     */
    private static void clearTrackedMobs() {
        trackedMobs.clear();
        trackedArmorStands.clear();
    }
    
    /**
     * Get all tracked mob entities (for rendering)
     */
    public static List<Entity> getTrackedMobs() {
        return new ArrayList<>(trackedMobs); // Return copy for safety
    }
    
    /**
     * Check if an entity is currently being tracked
     */
    public static boolean isTrackedMob(Entity entity) {
        return trackedMobs.contains(entity);
    }
    
    /**
     * Set the target mob name and enable tracking
     */
    public static void setTargetMob(String mobName) {
        MobHighlightConfig.setTargetMobName(mobName);
        MobHighlightConfig.setEnabled(true);
        clearTrackedMobs();
        sendMessage(String.format("Now highlighting mobs matching: '%s'", mobName), Formatting.GREEN);
    }
    
    /**
     * Clear the target and disable tracking
     */
    public static void clearTarget() {
        MobHighlightConfig.setTargetMobName("");
        MobHighlightConfig.setEnabled(false);
        clearTrackedMobs();
        sendMessage("Mob highlighting disabled", Formatting.YELLOW);
    }
    
    /**
     * Get status text for GUI display
     */
    public static String getStatusText() {
        if (!MobHighlightConfig.isEnabled()) {
            return "Disabled";
        }
        
        String targetName = MobHighlightConfig.getTargetMobName();
        if (targetName == null || targetName.trim().isEmpty()) {
            return "Enabled - No target set";
        }
        
        int count = trackedMobs.size();
        if (count == 0) {
            return String.format("Searching for '%s'...", targetName);
        } else {
            return String.format("Tracking %d '%s' mob(s)", count, targetName);
        }
    }
    
    /**
     * Send a message to the player
     */
    private static void sendMessage(String message, Formatting formatting) {
        if (client.player != null) {
            client.player.sendMessage(
                Text.literal("[MobHighlight] " + message).formatted(formatting),
                false
            );
        }
    }
    
    /**
     * Reset the helper (clear tracked mobs)
     */
    public static void reset() {
        clearTrackedMobs();
    }
}
