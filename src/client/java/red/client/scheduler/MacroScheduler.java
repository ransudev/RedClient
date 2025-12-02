package red.client.scheduler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import red.client.fishing.feature.AutoFishingFeature;
import red.client.flarecombat.feature.FlareMacroFeature;

import java.util.Random;

/**
 * Macro Scheduler - Manages run time and break periods for macros
 * 
 * States:
 * - STOPPED: Not running
 * - RUNNING: Macro is active
 * - ON_BREAK: Waiting during break period
 */
public class MacroScheduler {
    private static SchedulerState state = SchedulerState.STOPPED;
    private static long runStartTime = 0;
    private static long breakStartTime = 0;
    private static int currentBreakDuration = 0;
    private static final Random random = new Random();
    
    public enum SchedulerState {
        STOPPED,
        RUNNING,
        ON_BREAK
    }

    /**
     * Start the scheduler - begins macro run
     */
    public static void start() {
        if (state != SchedulerState.STOPPED) {
            return; // Already running or on break
        }
        
        state = SchedulerState.RUNNING;
        runStartTime = System.currentTimeMillis();
        
        // Start the active macro (AutoFish or Flare)
        if (!AutoFishingFeature.isEnabled() && !FlareMacroFeature.isEnabled()) {
            // Start AutoFish by default if nothing is running
            AutoFishingFeature.toggle();
        }
        
        sendMessage("Scheduler started - will run for " + MacroSchedulerConfig.getRunTime() + " minutes", Formatting.GREEN);
    }

    /**
     * Stop the scheduler - stops macro and resets
     */
    public static void stop() {
        if (state == SchedulerState.STOPPED) {
            return;
        }
        
        // Stop any running macro
        if (AutoFishingFeature.isEnabled()) {
            AutoFishingFeature.toggle();
        }
        if (FlareMacroFeature.isEnabled()) {
            FlareMacroFeature.stop();
        }
        
        state = SchedulerState.STOPPED;
        runStartTime = 0;
        breakStartTime = 0;
        currentBreakDuration = 0;
        
        sendMessage("Scheduler stopped", Formatting.RED);
    }

    /**
     * Tick method - call every client tick to update scheduler
     */
    public static void tick() {
        if (state == SchedulerState.STOPPED) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (state == SchedulerState.RUNNING) {
            // Check if run time has elapsed
            long elapsedMillis = currentTime - runStartTime;
            long runTimeMins = MacroSchedulerConfig.getRunTime();
            long runTimeMillis = runTimeMins * 60 * 1000;
            
            if (elapsedMillis >= runTimeMillis) {
                // Run time complete
                handleRunComplete();
            }
        } 
        else if (state == SchedulerState.ON_BREAK) {
            // Check if break time has elapsed
            long elapsedMillis = currentTime - breakStartTime;
            long breakTimeMillis = currentBreakDuration * 60 * 1000;
            
            if (elapsedMillis >= breakTimeMillis) {
                // Break complete - resume macro
                resumeFromBreak();
            }
        }
    }

    /**
     * Handle completion of run time
     */
    private static void handleRunComplete() {
        // Stop any running macro
        if (AutoFishingFeature.isEnabled()) {
            AutoFishingFeature.toggle();
        }
        if (FlareMacroFeature.isEnabled()) {
            FlareMacroFeature.stop();
        }
        
        if (MacroSchedulerConfig.isBreakEnabled()) {
            // Start break
            startBreak();
        } else {
            // Just stop
            state = SchedulerState.STOPPED;
            runStartTime = 0;
            sendMessage("Run time complete - scheduler stopped", Formatting.YELLOW);
        }
    }

    /**
     * Start a break period with random duration
     */
    private static void startBreak() {
        state = SchedulerState.ON_BREAK;
        breakStartTime = System.currentTimeMillis();
        
        // Calculate random break duration
        int minBreak = MacroSchedulerConfig.getBreakMinTime();
        int maxBreak = MacroSchedulerConfig.getBreakMaxTime();
        
        // Ensure min <= max
        if (minBreak > maxBreak) {
            int temp = minBreak;
            minBreak = maxBreak;
            maxBreak = temp;
        }
        
        currentBreakDuration = minBreak + random.nextInt(maxBreak - minBreak + 1);
        
        sendMessage("Taking a break for " + currentBreakDuration + " minutes", Formatting.GOLD);
    }

    /**
     * Resume from break and start new run cycle
     */
    private static void resumeFromBreak() {
        state = SchedulerState.RUNNING;
        runStartTime = System.currentTimeMillis();
        breakStartTime = 0;
        
        // Start the macro again
        if (!AutoFishingFeature.isEnabled() && !FlareMacroFeature.isEnabled()) {
            AutoFishingFeature.toggle();
        }
        
        sendMessage("Break complete - resuming for " + MacroSchedulerConfig.getRunTime() + " minutes", Formatting.GREEN);
    }

    /**
     * Get current state
     */
    public static SchedulerState getState() {
        return state;
    }

    /**
     * Get time remaining in current state (in seconds)
     */
    public static int getTimeRemaining() {
        if (state == SchedulerState.STOPPED) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (state == SchedulerState.RUNNING) {
            long elapsedMillis = currentTime - runStartTime;
            long runTimeMillis = MacroSchedulerConfig.getRunTime() * 60 * 1000;
            long remainingMillis = runTimeMillis - elapsedMillis;
            return (int) Math.max(0, remainingMillis / 1000);
        } 
        else if (state == SchedulerState.ON_BREAK) {
            long elapsedMillis = currentTime - breakStartTime;
            long breakTimeMillis = currentBreakDuration * 60 * 1000;
            long remainingMillis = breakTimeMillis - elapsedMillis;
            return (int) Math.max(0, remainingMillis / 1000);
        }
        
        return 0;
    }

    /**
     * Format time remaining as MM:SS
     */
    public static String getTimeRemainingFormatted() {
        int totalSeconds = getTimeRemaining();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Get status text
     */
    public static String getStatusText() {
        switch (state) {
            case RUNNING:
                return "Running (" + getTimeRemainingFormatted() + " remaining)";
            case ON_BREAK:
                return "On Break (" + getTimeRemainingFormatted() + " remaining)";
            case STOPPED:
            default:
                return "Stopped";
        }
    }

    /**
     * Send message to player
     */
    private static void sendMessage(String message, Formatting formatting) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("[Scheduler] ").formatted(Formatting.AQUA, Formatting.BOLD)
                            .append(Text.literal(message).formatted(formatting)),
                    false
            );
        }
    }
}
