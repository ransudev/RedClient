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
import red.client.fishing.util.RotationManager;
import red.client.fishing.util.WeaponDetector;
import red.client.fishing.feature.SeaCreatureKiller;

import org.lwjgl.glfw.GLFW;
import red.client.flarecombat.mixin.MouseMixin;

import java.util.*;
import java.util.Comparator;
import java.util.Random;

/**
 * Melee Combat Mode - Exact implementation based on FishMaster's MeleeMode
 * 
 * Uses armor stand detection to find actual sea creature entities
 * Implements smooth continuous rotation tracking via RotationManager
 * Enforces click rate limiting (~3 clicks per second)
 * Explicitly rejects RCM weapons (Hyperion, Fire Veil Wand)
 * 
 * Attack Flow:
 * 1. Find or validate target creature (via armor stand name tags)
 * 2. Check click rate limiting (334ms minimum interval)
 * 3. Switch to melee weapon (priority: Figstone > Prime Axe > other)
 * 4. Find actual entity via armor stand detection
 * 5. Start continuous rotation tracking via RotationManager.trackTarget()
 * 6. Simulate left click (click happens while tracking is active)
 * 7. RotationManager.tick() continues updating every frame in SeaCreatureKiller.tick()
 */
public class MeleeMode implements CombatMode {
    
    private static final long MIN_ATTACK_INTERVAL = 334; // ~3 attacks per second
    private static final double ENTITY_SEARCH_RANGE = 15.0;
    
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
        
        // Check click rate limiting first
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime < MIN_ATTACK_INTERVAL) {
            return false;
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
        
        // Find actual entity to attack using armor stand detection (FishMaster approach)
        Entity actualTarget = findActualSeaCreature(target);
        if (actualTarget == null) {
            actualTarget = target;
        }
        
        if (actualTarget == null) {
            return false;
        }
        
        actualTargetEntity = actualTarget;
        
        // Start continuous tracking to the target
        updateRotationTarget(actualTarget);
        
        // Execute the attack immediately (rotation will update continuously via tick())
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
        
        // Start tracking immediately
        updateRotationTarget(actualTargetEntity);
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
            if (WeaponDetector.isMeleeWeapon(stack) && 
                !WeaponDetector.isHyperion(stack) && 
                !WeaponDetector.isFireVeilWand(stack)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Update rotation to smoothly track the target entity
     * Uses RotationManager.trackTarget() which continuously updates until stopTracking() is called
     * This method is based on FishMaster's RotationHandler usage
     */
    private void updateRotationTarget(Entity target) {
        if (target == null) {
            rotationManager.stopTracking();
            return;
        }
        
        Vec3d targetPos = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        
        // Use smooth continuous tracking (0.3f = reasonable smoothing speed, like FishMaster)
        rotationManager.trackTarget(targetPos, 0.3f);
    }
    
    /**
     * Switch to melee weapon in hotbar
     * IMPORTANT: Explicitly rejects RCM weapons (Hyperion, Fire Veil Wand)
     * Prioritizes: Figstone Splitter > Prime Axe > Other melee weapons
     */
    private boolean switchToMeleeWeapon() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        ItemStack currentItem = client.player.getMainHandStack();
        
        // If already holding a melee weapon (but NOT RCM weapons), keep it
        if (WeaponDetector.isMeleeWeapon(currentItem) && 
            !WeaponDetector.isHyperion(currentItem) && 
            !WeaponDetector.isFireVeilWand(currentItem)) {
            return true;
        }
        
        // Priority 1: Find Figstone Splitter
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            String name = stack.getName().getString().toLowerCase();
            if (name.contains("figstone splitter")) {
                client.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        
        // Priority 2: Find Prime Axe
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            String name = stack.getName().getString().toLowerCase();
            if (name.contains("prime axe")) {
                client.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        
        // Priority 3: Find any other melee weapon (but NOT RCM weapons)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (WeaponDetector.isMeleeWeapon(stack) && 
                !WeaponDetector.isHyperion(stack) && 
                !WeaponDetector.isFireVeilWand(stack)) {
                client.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Simulate left-click attack using MouseMixin (FishMaster's approach)
     * This directly invokes GLFW mouse button press/release for authentic simulation
     */
    private void simulateLeftClick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        
        try {
            long windowHandle = client.getWindow().getHandle();
            
            // Cast the Mouse instance to MouseMixin interface (applied via Mixin transformation)
            MouseMixin mouseMixin = (MouseMixin) client.mouse;
            
            // Simulate left mouse button press
            mouseMixin.invokeOnMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0);
            
            // Small delay to simulate human-like click duration
            try {
                Thread.sleep(10 + new Random().nextInt(20)); // 10-30ms delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Simulate left mouse button release
            mouseMixin.invokeOnMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
            
        } catch (Exception e) {
            // Silent failure - not critical for functionality
        }
    }
    
    /**
     * Find actual sea creature entity using armor stand detection
     * This is the core of FishMaster's approach - matches armor stand names to find real entities
     */
    private Entity findActualSeaCreature(Entity initialTarget) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return null;
        
        List<Entity> possibleEntities = new ArrayList<>();
        
        // Search through all sea creature names
        for (String creatureName : SeaCreatureKiller.getTargetCreatures()) {
            // Find armor stands with matching creature names
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
                        return nameTag.contains(creatureName.toLowerCase()) &&
                               !nameTag.contains(client.player.getName().getString().toLowerCase()) &&
                               !armorStand.isRemoved() &&
                               getHealthFromArmorStandName(nameTag) > 0;
                    }
                    return false;
                }
            );
            
            // For each matching armor stand, find the actual entity it represents
            for (ArmorStandEntity armorStand : matchingArmorStands) {
                Entity actualEntity = getEntityBelowArmorStand(armorStand);
                if (actualEntity != null && actualEntity instanceof LivingEntity &&
                    !actualEntity.equals(client.player) && ((LivingEntity) actualEntity).isAlive()) {
                    possibleEntities.add(actualEntity);
                }
            }
        }
        
        if (possibleEntities.isEmpty()) {
            return null;
        }
        
        // Return the closest entity
        return possibleEntities.stream()
            .min(Comparator.comparingDouble(entity -> client.player.distanceTo(entity)))
            .orElse(null);
    }
    
    /**
     * Get the actual entity that an armor stand represents
     * FishMaster's approach: expand box around armor stand and find nearby living entities
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
     * Looks for patterns like "❤ 100/150" or "100/150"
     */
    private int getHealthFromArmorStandName(String name) {
        int health = 0;
        try {
            // Look for patterns like "❤ 100/150" or "100/150" at the end of the name
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
    
    /**
     * Check if item is a fishing rod
     */
    public boolean isFishingRod(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getName().getString().toLowerCase();
        return name.contains("rod") || name.contains("fishing");
    }
}


