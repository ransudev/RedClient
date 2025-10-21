package red.client.fishing.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Manages smooth player rotation for melee combat targeting
 * Provides continuous tracking and interpolation
 */
public class RotationManager {
    
    private boolean isTracking = false;
    private Vec3d targetPosition = null;
    private float trackingSpeed = 0.3f;
    
    private float currentYaw = 0.0f;
    private float currentPitch = 0.0f;
    
    /**
     * Start tracking a target position with smooth rotation
     * @param target The position to look at
     * @param speed Rotation speed (0.0 to 1.0, lower is smoother)
     */
    public void trackTarget(Vec3d target, float speed) {
        this.targetPosition = target;
        this.trackingSpeed = MathHelper.clamp(speed, 0.1f, 1.0f);
        this.isTracking = true;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            this.currentYaw = client.player.getYaw();
            this.currentPitch = client.player.getPitch();
        }
    }
    
    /**
     * Stop tracking and rotation updates
     */
    public void stopTracking() {
        this.isTracking = false;
        this.targetPosition = null;
    }
    
    /**
     * Update rotation (should be called every tick)
     */
    public void tick() {
        if (!isTracking || targetPosition == null) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        
        // Calculate required rotation to look at target
        Vec3d playerPos = client.player.getEyePos();
        Vec3d direction = targetPosition.subtract(playerPos).normalize();
        
        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.asin(direction.y));
        
        // Normalize angles
        targetYaw = normalizeYaw(targetYaw);
        targetPitch = clampPitch(targetPitch);
        
        // Smooth interpolation
        float yawDiff = getShortestRotationPath(currentYaw, targetYaw);
        float pitchDiff = targetPitch - currentPitch;
        
        currentYaw += yawDiff * trackingSpeed;
        currentPitch += pitchDiff * trackingSpeed;
        
        currentYaw = normalizeYaw(currentYaw);
        currentPitch = clampPitch(currentPitch);
        
        // Apply rotation to player
        client.player.setYaw(currentYaw);
        client.player.setPitch(currentPitch);
    }
    
    /**
     * Apply instant rotation (no interpolation)
     */
    public void applyInstantRotation(float yaw, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        this.currentYaw = normalizeYaw(yaw);
        this.currentPitch = clampPitch(pitch);
        
        client.player.setYaw(currentYaw);
        client.player.setPitch(currentPitch);
    }
    
    /**
     * Check if currently tracking a target
     */
    public boolean isTracking() {
        return isTracking;
    }
    
    /**
     * Get current target position
     */
    public Vec3d getTargetPosition() {
        return targetPosition;
    }
    
    // ===== Angle Utility Methods =====
    
    /**
     * Normalize yaw to [-180, 180] range
     */
    private float normalizeYaw(float yaw) {
        yaw = yaw % 360.0f;
        if (yaw >= 180.0f) {
            yaw -= 360.0f;
        }
        if (yaw < -180.0f) {
            yaw += 360.0f;
        }
        return yaw;
    }
    
    /**
     * Clamp pitch to [-90, 90] range
     */
    private float clampPitch(float pitch) {
        return MathHelper.clamp(pitch, -90.0f, 90.0f);
    }
    
    /**
     * Get shortest rotation path between two yaw angles
     */
    private float getShortestRotationPath(float from, float to) {
        float diff = to - from;
        
        // Normalize to [-180, 180]
        while (diff > 180.0f) diff -= 360.0f;
        while (diff < -180.0f) diff += 360.0f;
        
        return diff;
    }
}
