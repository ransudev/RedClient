package red.client.fishing.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import red.client.flarecombat.mixin.MouseMixin;
import red.client.fishing.util.RotationManager;
import red.client.fishing.util.WeaponDetector;

import java.util.*;

/**
 * Melee Combat Mode
 * Complex implementation with armor stand detection, entity tracking, and rotation management
 * Uses left-click attacks with proper targeting and click rate limiting
 */
public class MeleeMode implements CombatMode {
    
    private static final long MIN_ATTACK_INTERVAL = 334; // ~3 attacks per second (1000ms / 3)
    private static final double ENTITY_SEARCH_RANGE = 15.0;
    private static final Random random = new Random();
    
    private long lastAttackTime = 0;
    private int originalSlot = -1;
    private Entity actualTargetEntity = null;
    
    private final RotationManager rotationManager;
    
    public MeleeMode() {
        this.rotationManager = new RotationManager();
    }
    
    @Override
    public boolean performAttack(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }
        
        // Check click rate limiting
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime < MIN_ATTACK_INTERVAL) {
            return false; // Skip to maintain rate limit
        }
        
        if (!canAttack()) {
            return false;
        }
        
        // Switch to melee weapon
        if (!switchToMeleeWeapon()) {
            client.player.sendMessage(
                Text.literal("[SCK] ")
                    .formatted(Formatting.RED)
                    .append(Text.literal("No melee weapon found!")
                    .formatted(Formatting.YELLOW)),
                false
            );
            return false;
        }
        
        // Find actual entity to attack (resolve armor stand to real entity)
        Entity actualTarget = findActualSeaCreature(target);
        if (actualTarget == null) {
            actualTarget = target;
        }
        
        actualTargetEntity = actualTarget;
        
        // Update rotation to look at target
        updateRotationTarget(actualTarget);
        
        // Perform left-click attack
        simulateLeftClick(client);
        lastAttackTime = currentTime;
        
        return true;
    }
    
    @Override
    public String getModeName() {
        return "Melee";
    }
    
    @Override
    public boolean canAttack() {
        return System.currentTimeMillis() - lastAttackTime >= MIN_ATTACK_INTERVAL;
    }
    
    @Override
    public void enterCombat(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Store original slot
        originalSlot = client.player.getInventory().getSelectedSlot();
        
        // Switch to melee weapon
        switchToMeleeWeapon();
        
        // Find actual target entity
        actualTargetEntity = findActualSeaCreature(target);
        if (actualTargetEntity == null) {
            actualTargetEntity = target;
        }
    }
    
    @Override
    public void exitCombat() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Stop rotation tracking
        rotationManager.stopTracking();
        actualTargetEntity = null;
        
        // Restore original slot
        if (originalSlot != -1) {
            ItemStack originalItem = client.player.getInventory().getStack(originalSlot);
            if (isFishingRod(originalItem)) {
                client.player.getInventory().setSelectedSlot(originalSlot);
            } else {
                // Find any fishing rod
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = client.player.getInventory().getStack(i);
                    if (isFishingRod(stack)) {
                        client.player.getInventory().setSelectedSlot(i);
                        break;
                    }
                }
            }
            originalSlot = -1;
        }
    }
    
    @Override
    public boolean hasRequiredWeapon() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        // Check hotbar for melee weapons
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (WeaponDetector.isMeleeWeapon(stack)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Update rotation to smoothly track the target entity
     */
    private void updateRotationTarget(Entity target) {
        if (target == null) {
            rotationManager.stopTracking();
            return;
        }
        
        Vec3d targetPos = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        
        // Use smooth continuous tracking
        rotationManager.trackTarget(targetPos, 0.3f);
    }
    
    /**
     * Switch to melee weapon in hotbar
     */
    private boolean switchToMeleeWeapon() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        ItemStack currentItem = client.player.getMainHandStack();
        if (WeaponDetector.isMeleeWeapon(currentItem)) {
            return true;
        }
        
        // Search hotbar for melee weapon
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (WeaponDetector.isMeleeWeapon(stack)) {
                client.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Simulate left-click attack using MouseMixin
     */
    private void simulateLeftClick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        
        try {
            long windowHandle = client.getWindow().getHandle();
            MouseMixin mouseMixin = (MouseMixin) client.mouse;
            
            // Simulate left mouse button press
            mouseMixin.invokeOnMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0);
            
            // Human-like click duration
            try {
                Thread.sleep(10 + random.nextInt(20)); // 10-30ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Simulate left mouse button release
            mouseMixin.invokeOnMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
            
        } catch (Exception e) {
            System.err.println("[MeleeMode] Failed to simulate left click: " + e.getMessage());
        }
    }
    
    /**
     * Find actual sea creature entity using armor stand detection
     * Based on FishMaster's /track command logic
     */
    private Entity findActualSeaCreature(Entity initialTarget) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return null;
        
        List<Entity> possibleEntities = new ArrayList<>();
        
        // Get target name for searching
        String targetName = getEntityDisplayName(initialTarget);
        if (targetName == null || targetName.isEmpty()) return null;
        
        // Search for armor stands with matching names
        Box searchBox = new Box(
            client.player.getPos().add(-ENTITY_SEARCH_RANGE, -ENTITY_SEARCH_RANGE, -ENTITY_SEARCH_RANGE),
            client.player.getPos().add(ENTITY_SEARCH_RANGE, ENTITY_SEARCH_RANGE, ENTITY_SEARCH_RANGE)
        );
        
        List<ArmorStandEntity> matchingArmorStands = client.world.getEntitiesByClass(
            ArmorStandEntity.class,
            searchBox,
            armorStand -> {
                if (armorStand.hasCustomName()) {
                    String nameTag = armorStand.getCustomName().getString().toLowerCase();
                    return nameTag.contains(targetName.toLowerCase()) &&
                           !nameTag.contains(client.player.getName().getString().toLowerCase()) &&
                           !armorStand.isRemoved() &&
                           getHealthFromArmorStandName(nameTag) > 0;
                }
                return false;
            }
        );
        
        // For each matching armor stand, find the actual entity
        for (ArmorStandEntity armorStand : matchingArmorStands) {
            Entity actualEntity = getEntityBelowArmorStand(armorStand);
            if (actualEntity != null && actualEntity instanceof LivingEntity &&
                !actualEntity.equals(client.player) && ((LivingEntity) actualEntity).isAlive()) {
                possibleEntities.add(actualEntity);
            }
        }
        
        if (possibleEntities.isEmpty()) {
            return null;
        }
        
        // Return closest entity
        return possibleEntities.stream()
            .min(Comparator.comparingDouble(entity -> client.player.distanceTo(entity)))
            .orElse(null);
    }
    
    /**
     * Get the actual entity that an armor stand represents
     */
    private Entity getEntityBelowArmorStand(ArmorStandEntity armorStand) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;
        
        // Expand bounding box to find nearby entities
        Box searchBox = armorStand.getBoundingBox().expand(0.3, 2.0, 0.3);
        
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
     * Extract health value from armor stand name
     */
    private int getHealthFromArmorStandName(String name) {
        int health = 0;
        try {
            // Look for patterns like "❤ 100/150" or "100/150"
            String[] parts = name.split(" ");
            for (String part : parts) {
                if (part.contains("/") && part.matches(".*\\d+/\\d+.*")) {
                    String healthPart = part.replaceAll("[^0-9/]", "");
                    String[] healthNumbers = healthPart.split("/");
                    if (healthNumbers.length >= 2) {
                        health = Integer.parseInt(healthNumbers[0].replace(",", ""));
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return health;
    }
    
    /**
     * Get actual target entity (for external access)
     */
    public Entity getActualTarget() {
        return actualTargetEntity;
    }
    
    /**
     * Get rotation manager (for tick updates)
     */
    public RotationManager getMeleeRotationManager() {
        return rotationManager;
    }
}
