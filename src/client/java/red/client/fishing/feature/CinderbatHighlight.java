package red.client.fishing.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import red.client.fishing.config.CinderbatHighlightConfig;

import java.util.ArrayList;
import java.util.List;

public class CinderbatHighlight {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final List<Entity> trackedCinderbats = new ArrayList<>();
    private static long lastDebugSummaryTime = 0;
    private static long lastScanTime = 0;
    private static final long EMPTY_SCAN_INTERVAL_MS = 500;
    private static final long TRACKING_SCAN_INTERVAL_MS = 2000;

    public static void tick() {
        if (!CinderbatHighlightConfig.isEnabled() || client.player == null || client.world == null) {
            trackedCinderbats.clear();
            return;
        }

        trackedCinderbats.removeIf(entity -> entity == null || entity.isRemoved() || client.player.distanceTo(entity) > CinderbatHighlightConfig.getDetectionRange());

        long now = System.currentTimeMillis();
        long scanInterval = trackedCinderbats.isEmpty() ? EMPTY_SCAN_INTERVAL_MS : TRACKING_SCAN_INTERVAL_MS;
        if (now - lastScanTime >= scanInterval) {
            scanForCinderbats();
            lastScanTime = now;
        }
    }

    private static void scanForCinderbats() {
        if (client.world == null || client.player == null) {
            return;
        }

        double range = CinderbatHighlightConfig.getDetectionRange();
        int batsChecked = 0;
        int detectedThisTick = 0;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof BatEntity bat)) {
                continue;
            }

            batsChecked++;
            double distance = client.player.distanceTo(entity);

            if (entity.isRemoved() || distance > range) {
                continue;
            }

            if (CinderbatHighlightConfig.isDebugEnabled() && batsChecked <= 5) {
                String batName = bat.hasCustomName() ? bat.getCustomName().getString() : "<no custom name>";
                sendMessage(String.format("Bat #%d: dist=%.1f hp=%.1f max=%.1f name='%s'",
                        batsChecked, distance, bat.getHealth(), bat.getMaxHealth(), batName), Formatting.DARK_GRAY);
            }

            String matchReason = getCinderbatMatchReason(bat);
            if (matchReason != null) {
                if (!trackedCinderbats.contains(entity)) {
                    trackedCinderbats.add(entity);
                    detectedThisTick++;
                    int x = (int) Math.floor(entity.getX());
                    int y = (int) Math.floor(entity.getY());
                    int z = (int) Math.floor(entity.getZ());
                    sendMessage(String.format("Cinderbat detected at %d %d %d", x, y, z), Formatting.GOLD);
                    if (CinderbatHighlightConfig.isDebugEnabled()) {
                        sendMessage(String.format("+ Added cinderbat at %.1f blocks (HP: %.0f, reason=%s)",
                                distance, bat.getHealth(), matchReason), Formatting.YELLOW);
                    }
                }
            }
        }

        if (CinderbatHighlightConfig.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            if (now - lastDebugSummaryTime >= 1000) {
                sendMessage(String.format("Scan: bats=%d range=%.0f tracked=%d newlyAdded=%d",
                        batsChecked, range, trackedCinderbats.size(), detectedThisTick), Formatting.GRAY);
                if (trackedCinderbats.isEmpty()) {
                    sendMessage("Still searching - no matched bats cached yet", Formatting.RED);
                }
                lastDebugSummaryTime = now;
            }
        }
    }

    private static String getCinderbatMatchReason(BatEntity bat) {
        float health = bat.getHealth();
        float maxHealth = bat.getMaxHealth();

        if (health > 4_800_000.0f && maxHealth == 6.0f) {
            return "nofrills-health";
        }

        if (health >= 1000.0f && maxHealth >= 1000.0f) {
            return "high-health-bat";
        }

        return null;
    }

    public static boolean isTracked(Entity entity) {
        return trackedCinderbats.contains(entity);
    }

    public static int getHighlightColor() {
        return 0xFF000000 | (CinderbatHighlightConfig.getHighlightColor() & 0x00FFFFFF);
    }

    public static boolean hasTrackedCinderbats() {
        return !trackedCinderbats.isEmpty();
    }

    public static String getStatusText() {
        if (!CinderbatHighlightConfig.isEnabled()) {
            return "Disabled";
        }
        if (trackedCinderbats.isEmpty()) {
            return "Enabled - Searching... (range " + String.format("%.0f", CinderbatHighlightConfig.getDetectionRange()) + ")";
        }
        return "Tracking " + trackedCinderbats.size() + " cinderbat(s)";
    }

    public static boolean toggle() {
        boolean newState = CinderbatHighlightConfig.toggle();
        if (!newState) {
            trackedCinderbats.clear();
        }
        sendMessage("Cinderbat Highlight " + (newState ? "enabled" : "disabled"), newState ? Formatting.GREEN : Formatting.RED);
        return newState;
    }

    private static void sendMessage(String message, Formatting formatting) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[Cinderbat] " + message).formatted(formatting), false);
        }
    }
}
