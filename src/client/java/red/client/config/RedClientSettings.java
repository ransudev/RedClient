package red.client.config;

import red.client.flarecombat.config.FlareConfig;
import red.client.fishing.config.BezalFarmerConfig;
import red.client.fishing.config.CinderbatHighlightConfig;
import red.client.fishing.config.FishConfig;
import red.client.fishing.config.SpikeHelperConfig;
import red.client.fishing.config.XYZConfig;
import red.client.scheduler.MacroSchedulerConfig;

public class RedClientSettings {
    public boolean seaCreatureKillerEnabled;
    public int recastDelayTicks;
    public int reelingDelayTicks;
    public int groupKillThreshold;
    public boolean hyperionLookDown;
    public boolean ungrabMouse;

    public int flareCombatMode;
    public int flareClickCount;

    public boolean bezalEnabled;
    public boolean bezalAutoAim;
    public float bezalAttackDistance;
    public int bezalClickCount;
    public boolean bezalBlackhole;

    public boolean xyzEnabled;
    public float xyzSearchRange;

    public boolean cinderbatEnabled;
    public float cinderbatDetectionRange;
    public boolean cinderbatDebug;

    public boolean spikeEnabled;
    public boolean spikeHighlight;
    public boolean spikeAimAssist;

    public boolean schedulerBreaksEnabled;
    public int schedulerRunMinutes;
    public int schedulerBreakMinMinutes;
    public int schedulerBreakMaxMinutes;

    public static RedClientSettings fromRuntime() {
        RedClientSettings settings = new RedClientSettings();

        settings.seaCreatureKillerEnabled = FishConfig.isSeaCreatureKillerEnabled();
        settings.recastDelayTicks = FishConfig.getRecastDelay();
        settings.reelingDelayTicks = FishConfig.getReelingDelay();
        settings.groupKillThreshold = FishConfig.getSeaCreatureKillThreshold();
        settings.hyperionLookDown = FishConfig.isHyperionLookDownEnabled();
        settings.ungrabMouse = FishConfig.isUngrabMouseEnabled();

        settings.flareCombatMode = FlareConfig.getCombatMode();
        settings.flareClickCount = FlareConfig.getClickCount();

        settings.bezalEnabled = BezalFarmerConfig.isEnabled();
        settings.bezalAutoAim = BezalFarmerConfig.isAutoAimEnabled();
        settings.bezalAttackDistance = (float) BezalFarmerConfig.getAttackDistance();
        settings.bezalClickCount = BezalFarmerConfig.getClickCount();
        settings.bezalBlackhole = BezalFarmerConfig.isBlackholeEnabled();

        settings.xyzEnabled = XYZConfig.isEnabled();
        settings.xyzSearchRange = (float) XYZConfig.getSearchRange();

        settings.cinderbatEnabled = CinderbatHighlightConfig.isEnabled();
        settings.cinderbatDetectionRange = (float) CinderbatHighlightConfig.getDetectionRange();
        settings.cinderbatDebug = CinderbatHighlightConfig.isDebugEnabled();

        settings.spikeEnabled = SpikeHelperConfig.isEnabled();
        settings.spikeHighlight = SpikeHelperConfig.isHighlightEnabled();
        settings.spikeAimAssist = SpikeHelperConfig.isAimAssistEnabled();

        settings.schedulerBreaksEnabled = MacroSchedulerConfig.isBreakEnabled();
        settings.schedulerRunMinutes = MacroSchedulerConfig.getRunTime();
        settings.schedulerBreakMinMinutes = MacroSchedulerConfig.getBreakMinTime();
        settings.schedulerBreakMaxMinutes = MacroSchedulerConfig.getBreakMaxTime();

        return settings;
    }

    public void apply() {
        FishConfig.setSeaCreatureKillerEnabled(this.seaCreatureKillerEnabled);
        FishConfig.setRecastDelay(this.recastDelayTicks);
        FishConfig.setReelingDelay(this.reelingDelayTicks);
        FishConfig.setSeaCreatureKillThreshold(this.groupKillThreshold);
        FishConfig.setHyperionLookDownEnabled(this.hyperionLookDown);
        FishConfig.setUngrabMouseEnabled(this.ungrabMouse);

        FlareConfig.setCombatMode(this.flareCombatMode);
        FlareConfig.setClickCount(this.flareClickCount);

        BezalFarmerConfig.setEnabled(this.bezalEnabled);
        BezalFarmerConfig.setAutoAimEnabled(this.bezalAutoAim);
        BezalFarmerConfig.setAttackDistance(this.bezalAttackDistance);
        BezalFarmerConfig.setClickCount(this.bezalClickCount);
        BezalFarmerConfig.setBlackholeEnabled(this.bezalBlackhole);

        XYZConfig.setEnabled(this.xyzEnabled);
        XYZConfig.setSearchRange(this.xyzSearchRange);

        CinderbatHighlightConfig.setEnabled(this.cinderbatEnabled);
        CinderbatHighlightConfig.setDetectionRange(this.cinderbatDetectionRange);
        CinderbatHighlightConfig.setDebugEnabled(this.cinderbatDebug);

        SpikeHelperConfig.setEnabled(this.spikeEnabled);
        SpikeHelperConfig.setHighlightEnabled(this.spikeHighlight);
        SpikeHelperConfig.setAimAssistEnabled(this.spikeAimAssist);

        MacroSchedulerConfig.setBreakEnabled(this.schedulerBreaksEnabled);
        MacroSchedulerConfig.setRunTime(this.schedulerRunMinutes);
        MacroSchedulerConfig.setBreakMinTime(this.schedulerBreakMinMinutes);
        MacroSchedulerConfig.setBreakMaxTime(this.schedulerBreakMaxMinutes);
    }
}
