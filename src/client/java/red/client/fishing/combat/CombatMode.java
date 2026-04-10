package red.client.fishing.combat;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

/**
 * Base interface for Sea Creature Killer combat modes
 * Each mode implements its own attack strategy and weapon management
 */
public interface CombatMode {
    
    /**
     * Perform an attack on the target entity
     * @param target The entity to attack
     * @return true if attack was successful, false otherwise
     */
    boolean performAttack(Entity target);
    
    /**
     * Get the display name for this combat mode
     * @return Mode name for UI display
     */
    String getModeName();
    
    /**
     * Check if this mode can currently attack
     * Handles cooldowns and other attack restrictions
     * @return true if ready to attack
     */
    boolean canAttack();
    
    /**
     * Called when entering combat mode
     * Used for setup, weapon switching, rotation initialization
     * @param target The initial target entity
     */
    void enterCombat(Entity target);
    
    /**
     * Called when exiting combat mode
     * Used for cleanup, weapon restoration
     */
    void exitCombat();
    
    /**
     * Check if the player has the required weapon for this mode
     * @return true if weapon is available
     */
    boolean hasRequiredWeapon();
    
    /**
     * Get entity display name helper
     */
    default String getEntityDisplayName(Entity entity) {
        if (entity == null) return "";

        if (entity.hasCustomName() && entity.getCustomName() != null) {
            return entity.getCustomName().getString();
        }

        String typeName = entity.getType().getTranslationKey();
        if (typeName.startsWith("entity.minecraft.")) {
            typeName = typeName.substring("entity.minecraft.".length());
        }

        typeName = typeName.replace("_", " ");
        String[] words = typeName.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(" ");
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        return result.toString();
    }
    
    /**
     * Check if an item is a fishing rod
     */
    default boolean isFishingRod(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        String itemName = stack.getItem().toString().toLowerCase();
        String displayName = stack.getName().getString().toLowerCase();

        return itemName.contains("fishing_rod") ||
               displayName.contains("fishing rod") ||
               displayName.contains("rod");
    }
}
