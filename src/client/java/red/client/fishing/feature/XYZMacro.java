package red.client.fishing.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import red.client.fishing.config.XYZConfig;
import red.client.flarecombat.util.MouseSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * XYZ Macro - Automates catching XYZ mobs with lasso
 * Ported from CobaltFabric mod - FIXED reel timing and rotation tracking
 * 
 * Key fixes:
 * 1. Uses rotation tracking to lock onto mobs during lasso throw
 * 2. REEL state waits for "REEL" indicator in armor stand name
 * 3. Schedules 50ms delay before actually reeling
 * 4. CONFIRM_REEL goes back to REEL state (not REEL_IN) if still seeing indicator
 */
public class XYZMacro {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    // Macro state machine - must match Cobalt's exact flow
    private enum State {
        FIND_MOB,
        ROTATE,
        THROW_LASSO,
        REEL,           // Wait for REEL indicator + keep tracking
        REEL_IN,        // Perform the actual reel after delay
        CONFIRM_REEL,   // Wait for REEL to disappear
        HANDLE_DEATH,
        EASE_BACK
    }
    
    // State tracking
    private static boolean enabled = false;
    private static State state = State.FIND_MOB;
    private static long delayUntil = 0; // Timestamp when delay expires
    private static boolean isRotating = false; // Track if rotation is in progress
    private static boolean isTracking = false; // Track if tracking target
    
    // Smooth rotation tracking
    private static float targetYaw = 0f;
    private static float targetPitch = 0f;
    private static float currentYaw = 0f;
    private static float currentPitch = 0f;
    private static float initialYaw = 0f;      // Store initial angle before tracking
    private static float initialPitch = 0f;    // Store initial angle before tracking
    private static final float ROTATION_SPEED = 0.15f; // Smooth rotation speed (lower = smoother)
    
    // Target tracking
    private static ArmorStandEntity targetMob = null;
    private static Vec3d ogPos = null;
    private static Vec3d returnTarget = null;
    private static long lastThrow = 0;
    private static final List<ArmorStandEntity> blacklisted = new ArrayList<>();
    
    // Detection constants
    private static final double DETECTION_RANGE = 15.0;
    private static final double ESCAPE_DISTANCE = 15.0;
    private static final long THROW_TIMEOUT_MS = 5000; // 5 seconds
    private static final long REEL_DELAY_MS = 50; // Delay before reeling (50ms)
    
    // Target mob names - Cobalt uses "zee" not "zeus"
    private static final String[] MOB_NAMES = {"exe", "wai", "zee"};
    private static final String REEL_KEYWORD = "reel";
    
    /**
     * Toggle the macro on/off
     */
    public static boolean toggle() {
        if (enabled) {
            stop();
        } else {
            start();
        }
        return enabled;
    }
    
    /**
     * Start the macro
     */
    public static void start() {
        if (client.player == null) {
            return;
        }
        
        enabled = true;
        state = State.FIND_MOB;
        targetMob = null;
        blacklisted.clear();
        delayUntil = 0;
        isRotating = false;
        isTracking = false;
        
        // Initialize rotation state
        currentYaw = client.player.getYaw();
        currentPitch = client.player.getPitch();
        targetYaw = currentYaw;
        targetPitch = currentPitch;
    }
    
    /**
     * Stop the macro
     */
    public static void stop() {
        enabled = false;
        state = State.FIND_MOB;
        targetMob = null;
        blacklisted.clear();
        delayUntil = 0;
        isRotating = false;
        isTracking = false;
    }
    
    /**
     * Check if macro is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get current status text
     */
    public static String getStatusText() {
        if (!enabled) {
            return "Disabled";
        }
        return "Active - " + state.toString().replace("_", " ");
    }
    
    /**
     * Main tick update - called every game tick
     * CRITICAL: This must NOT block on delays, delays are handled with timestamps
     */
    public static void tick() {
        if (!enabled || client.player == null || client.world == null) {
            return;
        }
        
        // Don't run if any screen is open
        if (client.currentScreen != null) {
            return;
        }
        
        // Check if we're still in delay - if so, don't process state
        if (System.currentTimeMillis() < delayUntil) {
            return;
        }
        
        // Check if mob escaped
        if (targetMob != null && ogPos != null) {
            double distance = targetMob.getPos().distanceTo(ogPos);
            if (distance > ESCAPE_DISTANCE && state.ordinal() > State.THROW_LASSO.ordinal() && state != State.HANDLE_DEATH) {
                blacklisted.add(targetMob);
                targetMob = null;
                state = State.HANDLE_DEATH;
                return;
            }
        }
        
        // Execute current state
        processState();
    }
    
    /**
     * Process the current state
     */
    private static void processState() {
        switch (state) {
            case FIND_MOB:
                // Search for exe/wai/zee mobs
                targetMob = findMob(MOB_NAMES, DETECTION_RANGE);
                if (targetMob != null) {
                    // Save initial rotation before starting to track
                    if (client.player != null) {
                        initialYaw = client.player.getYaw();
                        initialPitch = client.player.getPitch();
                    }
                    state = State.ROTATE;
                }
                break;
                
            case ROTATE:
                // Rotate to target mob - keep calling lookAtEntity until rotation complete
                if (targetMob != null && client.player != null) {
                    lookAtEntity(targetMob);
                    // Only transition when rotation is complete
                    if (!isRotating) {
                        state = State.THROW_LASSO;
                    }
                }
                break;
                
            case THROW_LASSO:
                // Rotation complete, throw lasso
                if (targetMob != null && client.player != null) {
                    // Save original position
                    returnTarget = client.player.getPos();
                    ogPos = targetMob.getPos();
                    
                    // Right-click to throw lasso
                    MouseSimulator.simulateRightClick(client);
                    
                    // Schedule delay before state change
                    delayUntil = System.currentTimeMillis() + 100;
                    lastThrow = System.currentTimeMillis();
                    
                    // Start tracking the target (keep looking at it)
                    isTracking = true;
                    
                    state = State.REEL;
                }
                break;
                
            case REEL:
                // Wait for "REEL" indicator to appear near current target
                boolean hasReel = hasReelIndicator();
                long timeSinceThrow = System.currentTimeMillis() - lastThrow;
                
                // Timeout if REEL doesn't appear within 5 seconds
                if (timeSinceThrow >= THROW_TIMEOUT_MS && !hasReel) {
                    state = State.FIND_MOB;
                    targetMob = null;
                    isTracking = false;
                    return;
                }
                
                // Re-enable tracking if not already tracking
                if (!isTracking && targetMob != null) {
                    isTracking = true;
                }
                
                // Keep tracking target while waiting
                if (isTracking && targetMob != null) {
                    lookAtEntity(targetMob);
                }
                
                // When REEL appears, schedule 50ms delay and move to REEL_IN
                if (hasReel) {
                    delayUntil = System.currentTimeMillis() + REEL_DELAY_MS;
                    lastThrow = System.currentTimeMillis(); // Reset timer
                    state = State.REEL_IN;
                }
                break;
                
            case REEL_IN:
                // After delay passes, right-click to reel in
                // Stop tracking rotation
                isTracking = false;
                
                // Perform reel
                MouseSimulator.simulateRightClick(client);
                state = State.CONFIRM_REEL;
                break;
                
            case CONFIRM_REEL:
                // Check if reel indicator disappeared near current target
                // Cobalt's logic: if NO reel found, go back to REEL state
                boolean stillReeling = hasReelIndicator();
                if (!stillReeling) {
                    // REEL indicator gone - go back to REEL state (not HANDLE_DEATH!)
                    state = State.REEL;
                } else {
                    // Still seeing REEL indicator - stay in CONFIRM_REEL (implicit by not changing state)
                    // This waits for next tick to check again
                }
                break;
                
            case HANDLE_DEATH:
                // Capture or escape detected via chat - ready for next mob
                // Smoothly return to initial rotation
                returnToInitialRotation();
                state = State.FIND_MOB;
                targetMob = null;
                isTracking = false;
                break;
                
            case EASE_BACK:
                // Return to original position (currently unused)
                state = State.FIND_MOB;
                break;
        }
    }
    
    /**
     * Handle chat messages for macro events
     */
    public static void onChat(String message) {
        if (!enabled) {
            return;
        }
        
        String lowerMsg = message.toLowerCase(Locale.ROOT);
        
        // Check for escape message - Cobalt checks "You didn't" + "escaped"
        if (lowerMsg.contains("you didn't") && lowerMsg.contains("escaped")) {
            state = State.FIND_MOB;
            if (targetMob != null) {
                blacklisted.add(targetMob);
                targetMob = null;
            }
        }
        
        // Check for successful capture - "You caught..." or "You received shard"
        if ((lowerMsg.contains("you caught") || lowerMsg.contains("you received")) && 
            lowerMsg.contains("shard") && state != State.HANDLE_DEATH) {
            if (targetMob != null) {
                blacklisted.add(targetMob);
                targetMob = null;
            }
            state = State.HANDLE_DEATH;
        }
    }
    
    /**
     * Check if REEL indicator exists near the current target mob
     * This prevents switching to different mobs mid-capture
     */
    private static boolean hasReelIndicator() {
        if (targetMob == null || client.world == null) {
            return false;
        }
        
        Vec3d targetPos = targetMob.getPos();
        double searchRadius = 3.0;
        
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity)) {
                continue;
            }
            
            ArmorStandEntity stand = (ArmorStandEntity) entity;
            
            // Check distance from target mob
            if (stand.getPos().distanceTo(targetPos) > searchRadius) {
                continue;
            }
            
            // Get entity name
            String name = null;
            if (stand.getCustomName() != null) {
                name = stand.getCustomName().getString().toLowerCase(Locale.ROOT);
            } else {
                name = stand.getDisplayName().getString().toLowerCase(Locale.ROOT);
            }
            
            // Check for REEL keyword
            if (name.contains(REEL_KEYWORD.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Find a mob matching the given names within range
     * Searches through armor stands for matching custom names
     */
    private static ArmorStandEntity findMob(String[] names, double range) {
        if (client.world == null || client.player == null) {
            return null;
        }
        
        ArmorStandEntity closest = null;
        double closestDistance = range;
        
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity)) {
                continue;
            }
            
            ArmorStandEntity armorStand = (ArmorStandEntity) entity;
            
            // Skip blacklisted mobs
            if (blacklisted.contains(armorStand)) {
                continue;
            }
            
            // Get entity name from custom name or display name
            String entityName = null;
            if (armorStand.getCustomName() != null) {
                entityName = armorStand.getCustomName().getString();
            } else {
                entityName = armorStand.getDisplayName().getString();
            }
            
            // Check if name contains any of the target keywords
            String nameLower = entityName.toLowerCase(Locale.ROOT);
            boolean matches = false;
            for (String name : names) {
                if (nameLower.contains(name.toLowerCase(Locale.ROOT))) {
                    matches = true;
                    break;
                }
            }
            
            if (!matches) {
                continue;
            }
            
            // Check distance
            double distance = client.player.distanceTo(armorStand);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = armorStand;
            }
        }
        
        return closest;
    }
    
    /**
     * Look at an entity - smoothly rotates player to face target
     * Uses interpolation to avoid robotic snapping
     */
    private static void lookAtEntity(Entity entity) {
        if (client.player == null || entity == null) {
            return;
        }
        
        // Target position (slightly above entity center)
        Vec3d targetPos = entity.getPos().add(0, 1.0, 0);
        Vec3d eyePos = client.player.getEyePos();
        Vec3d direction = targetPos.subtract(eyePos);
        
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        
        if (horizontalDistance < 0.1) {
            return;
        }
        
        // Calculate target rotation
        targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
        targetPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontalDistance));
        
        // Clamp target pitch
        targetPitch = Math.max(-90.0f, Math.min(90.0f, targetPitch));
        
        // Get current rotation
        currentYaw = client.player.getYaw();
        currentPitch = client.player.getPitch();
        
        // Calculate rotation difference
        float yawDiff = wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        
        // Check if rotation is complete (within 1 degree)
        if (Math.abs(yawDiff) < 1.0f && Math.abs(pitchDiff) < 1.0f) {
            isRotating = false;
            return;
        }
        
        // Smooth interpolation
        float newYaw = currentYaw + yawDiff * ROTATION_SPEED;
        float newPitch = currentPitch + pitchDiff * ROTATION_SPEED;
        
        // Clamp new pitch
        newPitch = Math.max(-90.0f, Math.min(90.0f, newPitch));
        
        // Apply smooth rotation
        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
        
        isRotating = true;
    }
    
    /**
     * Smoothly return to initial rotation angle before tracking started
     */
    private static void returnToInitialRotation() {
        if (client.player == null) {
            return;
        }
        
        // Set target to initial rotation
        targetYaw = initialYaw;
        targetPitch = initialPitch;
        
        // Get current rotation
        currentYaw = client.player.getYaw();
        currentPitch = client.player.getPitch();
        
        // Calculate rotation difference
        float yawDiff = wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        
        // Check if already at initial rotation (within 1 degree)
        if (Math.abs(yawDiff) < 1.0f && Math.abs(pitchDiff) < 1.0f) {
            return;
        }
        
        // Smooth interpolation back to initial position
        float newYaw = currentYaw + yawDiff * ROTATION_SPEED;
        float newPitch = currentPitch + pitchDiff * ROTATION_SPEED;
        
        // Clamp new pitch
        newPitch = Math.max(-90.0f, Math.min(90.0f, newPitch));
        
        // Apply smooth rotation
        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
    }
    
    /**
     * Wrap degrees to [-180, 180] range for proper rotation calculation
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
