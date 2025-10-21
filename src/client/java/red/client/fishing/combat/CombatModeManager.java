package red.client.fishing.combat;

import red.client.fishing.config.FishConfig;

/**
 * Manager for combat modes
 * Handles mode selection, switching, and configuration
 */
public class CombatModeManager {
    
    private static CombatModeManager instance;
    
    private final RCMMode rcmMode;
    private final MeleeMode meleeMode;
    private CombatMode currentMode;
    
    private CombatModeManager() {
        this.rcmMode = new RCMMode();
        this.meleeMode = new MeleeMode();
        
        // Load mode from config
        updateModeFromConfig();
    }
    
    public static CombatModeManager getInstance() {
        if (instance == null) {
            instance = new CombatModeManager();
        }
        return instance;
    }
    
    /**
     * Get current active combat mode
     */
    public CombatMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Get RCM mode instance
     */
    public RCMMode getRCMMode() {
        return rcmMode;
    }
    
    /**
     * Get Melee mode instance
     */
    public MeleeMode getMeleeMode() {
        return meleeMode;
    }
    
    /**
     * Set combat mode by name
     * @param modeName "RCM" or "Melee"
     */
    public void setMode(String modeName) {
        CombatMode newMode = null;
        
        switch (modeName.toLowerCase()) {
            case "rcm":
                newMode = rcmMode;
                break;
            case "melee":
                newMode = meleeMode;
                break;
            default:
                System.err.println("[CombatModeManager] Unknown mode: " + modeName);
                return;
        }
        
        if (newMode != currentMode) {
            // Exit current mode if different
            if (currentMode != null) {
                currentMode.exitCombat();
            }
            
            currentMode = newMode;
            FishConfig.setCombatMode(modeName);
            
            System.out.println("[CombatModeManager] Switched to mode: " + currentMode.getModeName());
        }
    }
    
    /**
     * Update mode from config
     */
    public void updateModeFromConfig() {
        String configMode = FishConfig.getCombatMode();
        
        switch (configMode.toLowerCase()) {
            case "melee":
                currentMode = meleeMode;
                break;
            case "rcm":
            default:
                currentMode = rcmMode;
                break;
        }
    }
    
    /**
     * Check if player has required weapon for current mode
     */
    public boolean hasRequiredWeapon() {
        return currentMode != null && currentMode.hasRequiredWeapon();
    }
    
    /**
     * Get mode name for display
     */
    public String getCurrentModeName() {
        return currentMode != null ? currentMode.getModeName() : "None";
    }
    
    /**
     * Tick melee mode rotation updates
     */
    public void tick() {
        // Only melee mode needs tick updates for rotation
        if (currentMode instanceof MeleeMode) {
            ((MeleeMode) currentMode).getMeleeRotationManager().tick();
        }
    }
}
