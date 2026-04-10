package red.client.fishing.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.Set;

/**
 * Utility class for detecting weapon types in player inventory
 * Supports Hypixel Skyblock weapons and vanilla weapons
 */
public class WeaponDetector {
    
    // Hyperion variants
    private static final Set<String> HYPERION_KEYWORDS = Set.of(
        "hyperion",
        "valkyrie",
        "astraea",
        "scylla"
    );
    
    // Fire Veil Wand and variants
    private static final Set<String> FIRE_VEIL_KEYWORDS = Set.of(
        "fire veil wand",
        "fire veil",
        "wand of healing",
        "wand of strength",
        "wand of atonement",
        "wand of mending",
        "wand of restoration"
    );
    
    // Melee weapons (axes used for foraging/combat)
    private static final Set<String> MELEE_KEYWORDS = Set.of(
        "rookie axe",
        "sculptor's axe",
        "promising axe",
        "sweet axe",
        "efficient axe",
        "spruce axe",
        "seriously damaged axe",
        "decent axe",
        "fig hew",
        "figstone splitter",
        "treecapitator",
        "jungle axe",
        "sword",
        "blade",
        "cleaver",
        "katana"
    );
    
    /**
     * Check if item is a Hyperion or variant
     */
    public static boolean isHyperion(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        
        String displayName = getNormalizedName(stack);
        
        for (String keyword : HYPERION_KEYWORDS) {
            if (displayName.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if item is a Fire Veil Wand or variant
     */
    public static boolean isFireVeilWand(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        
        String displayName = getNormalizedName(stack);
        
        for (String keyword : FIRE_VEIL_KEYWORDS) {
            if (displayName.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if item is a melee weapon (axe, sword, etc.)
     */
    public static boolean isMeleeWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        
        String displayName = getNormalizedName(stack);
        String itemName = stack.getItem().toString().toLowerCase();
        
        // Check against keyword list
        for (String keyword : MELEE_KEYWORDS) {
            if (displayName.contains(keyword)) {
                return true;
            }
        }
        
        // Check vanilla weapon types
        if (itemName.contains("sword") || itemName.contains("axe")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if item is any RCM weapon (Hyperion or Fire Veil Wand)
     */
    public static boolean isRCMWeapon(ItemStack stack) {
        return isHyperion(stack) || isFireVeilWand(stack);
    }
    
    /**
     * Get weapon type for display
     */
    public static String getWeaponType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "None";
        
        if (isHyperion(stack)) return "Hyperion";
        if (isFireVeilWand(stack)) return "Fire Veil Wand";
        if (isMeleeWeapon(stack)) return "Melee";
        
        return "Unknown";
    }
    
    /**
     * Get normalized item name (lowercase, formatting stripped)
     */
    private static String getNormalizedName(ItemStack stack) {
        String displayName = stack.getName().getString();
        String stripped = Formatting.strip(displayName);
        return (stripped != null ? stripped : displayName).toLowerCase(Locale.ROOT);
    }
    
    /**
     * Find best RCM weapon in hotbar (prioritize Hyperion)
     * @return slot number or -1 if not found
     */
    public static int findBestRCMWeaponSlot(net.minecraft.client.MinecraftClient client) {
        if (client.player == null) return -1;
        
        // Priority 1: Hyperion
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (isHyperion(stack)) {
                return i;
            }
        }
        
        // Priority 2: Fire Veil Wand
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (isFireVeilWand(stack)) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Find any melee weapon in hotbar
     * @return slot number or -1 if not found
     */
    public static int findMeleeWeaponSlot(net.minecraft.client.MinecraftClient client) {
        if (client.player == null) return -1;
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (isMeleeWeapon(stack)) {
                return i;
            }
        }
        
        return -1;
    }
}
