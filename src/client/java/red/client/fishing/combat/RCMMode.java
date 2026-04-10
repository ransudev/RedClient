package red.client.fishing.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import red.client.fishing.config.FishConfig;
import red.client.fishing.util.FishMouseSimulator;
import red.client.fishing.util.WeaponDetector;

/**
 * RCM (Right Click Mage) Mode
 * Prioritizes Hyperion, falls back to Fire Veil Wand
 * Attacks by right-clicking (uses mage weapon abilities)
 * 
 * For Hyperion: Optionally looks down at player's feet to trigger ground explosion
 * For Fire Veil Wand: Uses normal orientation
 */
public class RCMMode implements CombatMode {
    
    private static final long ATTACK_COOLDOWN = 350; // ~2.8 attacks per second
    private long lastAttackTime = 0;
    private int originalSlot = -1;
    
    // Rotation storage for Hyperion attacks
    private static class SavedRotation {
        float yaw;
        float pitch;
        
        SavedRotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
    
    @Override
    public boolean performAttack(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }
        
        if (!canAttack()) {
            return false;
        }
        
        // Ensure we have the right weapon equipped
        if (!switchToRCMWeapon()) {
            client.player.sendMessage(
                Text.literal("[SCK] ")
                    .formatted(Formatting.RED)
                    .append(Text.literal("No RCM weapon found!")
                    .formatted(Formatting.YELLOW)),
                false
            );
            return false;
        }
        
        ItemStack currentWeapon = client.player.getMainHandStack();
        boolean isHyperion = WeaponDetector.isHyperion(currentWeapon);
        
        // Save original rotation
        SavedRotation originalRotation = new SavedRotation(
            client.player.getYaw(),
            client.player.getPitch()
        );
        
        // For Hyperion: Optionally look straight down at player's feet to trigger ground explosion
        // This is configurable via /fish hypedown true/false
        if (isHyperion && FishConfig.isHyperionLookDownEnabled()) {
            client.player.setPitch(90.0f); // 90° = straight down
        }
        
        // Perform right-click attack
        FishMouseSimulator.simulateRightClick(client);
        lastAttackTime = System.currentTimeMillis();
        
        // Restore original rotation instantly (only if we changed it)
        if (isHyperion && FishConfig.isHyperionLookDownEnabled()) {
            client.player.setYaw(originalRotation.yaw);
            client.player.setPitch(originalRotation.pitch);
        }
        
        return true;
    }
    
    @Override
    public String getModeName() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return "RCM";
        
        // Show which weapon is being used
        ItemStack mainHand = client.player.getMainHandStack();
        if (WeaponDetector.isHyperion(mainHand)) {
            return "RCM (Hyperion)";
        } else if (WeaponDetector.isFireVeilWand(mainHand)) {
            return "RCM (Fire Veil Wand)";
        }
        
        return "RCM";
    }
    
    @Override
    public boolean canAttack() {
        return System.currentTimeMillis() - lastAttackTime >= ATTACK_COOLDOWN;
    }
    
    @Override
    public void enterCombat(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Store original slot for restoration
        originalSlot = client.player.getInventory().getSelectedSlot();
        
        // Switch to RCM weapon
        switchToRCMWeapon();
    }
    
    @Override
    public void exitCombat() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Restore original slot if it had a fishing rod
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
        
        // Check hotbar for Hyperion or Fire Veil Wand
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (WeaponDetector.isHyperion(stack) || WeaponDetector.isFireVeilWand(stack)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Switch to RCM weapon (prioritize Hyperion, fallback to Fire Veil Wand)
     * @return true if weapon was found and equipped
     */
    private boolean switchToRCMWeapon() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        ItemStack currentItem = client.player.getMainHandStack();
        
        // Already holding correct weapon
        if (WeaponDetector.isHyperion(currentItem) || WeaponDetector.isFireVeilWand(currentItem)) {
            return true;
        }
        
        // Priority 1: Find Hyperion
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (WeaponDetector.isHyperion(stack)) {
                client.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        
        // Priority 2: Find Fire Veil Wand
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (WeaponDetector.isFireVeilWand(stack)) {
                client.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        
        return false;
    }
}
