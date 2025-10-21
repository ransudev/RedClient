package red.client.fishing.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.GlowSquidEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import red.client.fishing.combat.CombatMode;
import red.client.fishing.combat.CombatModeManager;
import red.client.fishing.config.FishConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sea Creature Killer - Main controller for automatic sea creature combat
 * Uses modular combat system with RCM and Melee modes
 */
public class SeaCreatureKiller {
    private static boolean enabled = false;
    private static Entity targetEntity = null;
    private static final double DETECTION_RANGE = 6.0;
    private static int killCount = 0;
    
    private static boolean inCombatMode = false;
    private static boolean needsToSwitchBack = false;
    private static long combatEndTime = 0;
    private static final long COMBAT_TO_FISHING_DELAY = 400;
    private static boolean clusterActive = false; // Track if threshold has been met

    private static final Set<String> TARGET_CREATURES = new HashSet<>();
    private static final CombatModeManager combatManager = CombatModeManager.getInstance();

    static {
        // Initialize target creatures list (Hypixel Skyblock sea creatures)
        
        // Water - Common
        TARGET_CREATURES.add("Squid");
        TARGET_CREATURES.add("Sea Walker");
        TARGET_CREATURES.add("Night Squid");
        TARGET_CREATURES.add("Sea Guardian");
        TARGET_CREATURES.add("Sea Witch");
        TARGET_CREATURES.add("Sea Archer");
        TARGET_CREATURES.add("Sea Leech");
        
        // Water - Rare
        TARGET_CREATURES.add("Rider of the Deep");
        TARGET_CREATURES.add("Catfish");
        TARGET_CREATURES.add("Carrot King");
        TARGET_CREATURES.add("Sea Emperor");
        TARGET_CREATURES.add("Guardian Defender");
        TARGET_CREATURES.add("Deep Sea Protector");
        TARGET_CREATURES.add("Water Hydra");
        TARGET_CREATURES.add("The Sea Emperor");
        TARGET_CREATURES.add("Agarimoo");
        
        // Oasis
        TARGET_CREATURES.add("Oasis Rabbit");
        TARGET_CREATURES.add("Oasis Sheep");
        TARGET_CREATURES.add("Water Worm");
        TARGET_CREATURES.add("Poisoned Water Worm");
        
        // Spooky
        TARGET_CREATURES.add("Scarecrow");
        TARGET_CREATURES.add("Nightmare");
        TARGET_CREATURES.add("Werewolf");
        TARGET_CREATURES.add("Phantom Fisher");
        TARGET_CREATURES.add("Grim Reaper");
        TARGET_CREATURES.add("Abyssal Miner");
        
        // Winter
        TARGET_CREATURES.add("Frozen Steve");
        TARGET_CREATURES.add("Frosty");
        TARGET_CREATURES.add("Grinch");
        TARGET_CREATURES.add("Yeti");
        TARGET_CREATURES.add("Nutcracker");
        TARGET_CREATURES.add("Reindrake");
        
        // Shark
        TARGET_CREATURES.add("Nurse Shark");
        TARGET_CREATURES.add("Blue Shark");
        TARGET_CREATURES.add("Tiger Shark");
        TARGET_CREATURES.add("Great White Shark");
        
        // Swamp
        TARGET_CREATURES.add("Trash Gobbler");
        TARGET_CREATURES.add("Dumpster Diver");
        TARGET_CREATURES.add("Bayou Sludge");
        TARGET_CREATURES.add("Bayou Sludgling");
        TARGET_CREATURES.add("Alligator");
        TARGET_CREATURES.add("Snapping Turtle");
        TARGET_CREATURES.add("Frog Man");
        TARGET_CREATURES.add("Titanoboa");
        TARGET_CREATURES.add("Banshee");
        TARGET_CREATURES.add("Blue Ringed Octopus");
        TARGET_CREATURES.add("Wiki Tiki");
        TARGET_CREATURES.add("Bogged");
        TARGET_CREATURES.add("Tadgang");
        TARGET_CREATURES.add("Wetwing");
        TARGET_CREATURES.add("Ent");
        TARGET_CREATURES.add("Tidetot");
        
        // Lava - Common
        TARGET_CREATURES.add("Pyroclastic Worm");
        TARGET_CREATURES.add("Lava Blaze");
        TARGET_CREATURES.add("Lava Pigman");
        TARGET_CREATURES.add("Flaming Worm");
        
        // Lava - Rare
        TARGET_CREATURES.add("Magma Slug");
        TARGET_CREATURES.add("Moogma");
        TARGET_CREATURES.add("Lava Leech");
        TARGET_CREATURES.add("Lava Flame");
        TARGET_CREATURES.add("Fire Eel");
        TARGET_CREATURES.add("Taurus");
        TARGET_CREATURES.add("Plhlegblast");
        TARGET_CREATURES.add("Thunder");
        TARGET_CREATURES.add("Lord Jawbus");
    }

    public static boolean isEnabled() {
        return enabled && AutoFishingFeature.isEnabled();
    }

    public static void setEnabled(boolean newState) {
        if (enabled != newState) {
            enabled = newState;
            FishConfig.setSeaCreatureKillerEnabled(enabled);

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                String status = enabled ? "ENABLED" : "DISABLED";
                client.player.sendMessage(
                    Text.literal("[AutoFish] Sea Creature Killer: ")
                        .formatted(Formatting.AQUA, Formatting.BOLD)
                        .append(Text.literal(status)
                        .formatted(enabled ? Formatting.GREEN : Formatting.RED)),
                    false
                );
            }

            if (!enabled) {
                exitCombat();
                clusterActive = false; // Reset cluster state
            }
        }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static void tick() {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Check if current target is still valid
        if (inCombatMode && targetEntity != null && 
            (targetEntity.isRemoved() || !isTargetSeaCreature(targetEntity) ||
             client.player.distanceTo(targetEntity) > DETECTION_RANGE)) {
            exitCombat();
            return;
        }

        // Handle post-combat weapon switching
        if (!inCombatMode) {
            if (needsToSwitchBack && System.currentTimeMillis() - combatEndTime > COMBAT_TO_FISHING_DELAY) {
                switchBackToFishingRod();
            }

            // Find new target if we don't have one
            if (targetEntity == null) {
                findNearestTargetCreature();
            }

            // Enter combat mode if we have a target
            if (targetEntity != null) {
                enterCombat(targetEntity);
            }
            return;
        }

        // Combat mode active - delegate attack to current mode
        if (targetEntity != null) {
            CombatMode currentMode = combatManager.getCurrentMode();
            if (currentMode != null && currentMode.canAttack()) {
                currentMode.performAttack(targetEntity);
            }
        }
        
        // Tick combat manager for rotation updates (melee mode)
        combatManager.tick();
    }

    private static void enterCombat(Entity target) {
        targetEntity = target;
        inCombatMode = true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            needsToSwitchBack = false;

            // Enter combat with current mode
            CombatMode currentMode = combatManager.getCurrentMode();
            if (currentMode != null) {
                currentMode.enterCombat(target);
            }

            String entityName = getEntityDisplayName(target);
            String modeName = currentMode != null ? currentMode.getModeName() : "Unknown";
            client.player.sendMessage(
                Text.literal("[SCK] ")
                    .formatted(Formatting.RED)
                    .append(Text.literal("Attacking " + entityName + " [" + modeName + "]")
                    .formatted(Formatting.YELLOW)),
                false
            );
        }
    }

    private static void exitCombat() {
        if (inCombatMode) {
            combatEndTime = System.currentTimeMillis();
            needsToSwitchBack = true;
            killCount++;
            
            // Exit combat with current mode
            CombatMode currentMode = combatManager.getCurrentMode();
            if (currentMode != null) {
                currentMode.exitCombat();
            }
        }

        targetEntity = null;
        inCombatMode = false;
    }

    private static void switchBackToFishingRod() {
        // Weapon switching is now handled by combat modes
        // This method is kept for the post-combat delay logic
        needsToSwitchBack = false;
    }

    private static void findNearestTargetCreature() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Vec3d playerPos = client.player.getPos();
        Box searchBox = new Box(
            playerPos.x - DETECTION_RANGE, playerPos.y - DETECTION_RANGE, playerPos.z - DETECTION_RANGE,
            playerPos.x + DETECTION_RANGE, playerPos.y + DETECTION_RANGE, playerPos.z + DETECTION_RANGE
        );

        List<Entity> entities = client.world.getOtherEntities(
            client.player, 
            searchBox, 
            SeaCreatureKiller::isTargetSeaCreature
        );

        int creatureCount = entities.size();

        // Reset cluster state if no creatures found
        if (creatureCount == 0) {
            clusterActive = false;
            targetEntity = null;
            return;
        }

        // Check if we should wait for more creatures (threshold system)
        if (!clusterActive) {
            int threshold = Math.max(1, FishConfig.getSeaCreatureKillThreshold());
            if (creatureCount < threshold) {
                // Not enough creatures yet - wait for more to spawn
                targetEntity = null;
                return;
            }
            // Threshold reached - activate cluster mode
            clusterActive = true;
        }

        // Find nearest creature
        Entity nearestCreature = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : entities) {
            double distance = client.player.distanceTo(entity);
            if (distance < nearestDistance && distance <= DETECTION_RANGE) {
                nearestDistance = distance;
                nearestCreature = entity;
            }
        }

        targetEntity = nearestCreature;
    }

    public static boolean isTargetSeaCreature(Entity entity) {
        if (entity == null) return false;

        String entityName = getEntityDisplayName(entity);

        // Check against target creatures list
        for (String targetName : TARGET_CREATURES) {
            if (entityName.contains(targetName)) {
                return true;
            }
        }

        // Also target vanilla sea creatures
        return entity instanceof SquidEntity ||
               entity instanceof GlowSquidEntity ||
               entity instanceof GuardianEntity ||
               entity instanceof ElderGuardianEntity;
    }

    private static String getEntityDisplayName(Entity entity) {
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

    public static int getKillCount() {
        return killCount;
    }

    public static Entity getCurrentTarget() {
        return targetEntity;
    }

    public static void reset() {
        enabled = false;
        targetEntity = null;
        killCount = 0;
        exitCombat();
    }
}
