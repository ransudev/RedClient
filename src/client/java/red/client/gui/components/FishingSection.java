package red.client.gui.components;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.SlimSliderComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import red.client.fishing.combat.CombatModeManager;
import red.client.fishing.combat.MeleeMode;
import red.client.fishing.config.FishConfig;
import red.client.fishing.config.SpikeHelperConfig;
import red.client.fishing.feature.AutoFishingFeature;
import red.client.fishing.feature.SeaCreatureKiller;
import red.client.fishing.feature.SpikeHelper;

/**
 * Fishing section for Auto Fishing and Sea Creature Killer controls with tabbed interface
 */
public class FishingSection extends FlowLayout {
    private final MinecraftClient client = MinecraftClient.getInstance();
    
    // Tab enum
    private enum Tab {
        AUTO_FISHING("🎣 Auto Fishing", 0x4488FF),
        SEA_CREATURE_KILLER("⚔ Sea Creature Killer", 0xFF5555),
        SPIKE_HELPER("🔱 Spike Helper", 0x55FF55),
        CONFIGURATION("⚙ Configuration", 0xFFAA00);
        
        final String label;
        final int color;
        
        Tab(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }
    
    private Tab currentTab = Tab.AUTO_FISHING;
    
    // Tab buttons
    private ButtonComponent autoFishTabButton;
    private ButtonComponent sckTabButton;
    private ButtonComponent spikeTabButton;
    private ButtonComponent configTabButton;
    
    // Content container
    private FlowLayout contentContainer;
    
    // UI Components - Auto Fishing
    private LabelComponent autoFishStatusLabel;
    private ButtonComponent autoFishToggleButton;
    
    // UI Components - Sea Creature Killer
    private LabelComponent sckStatusLabel;
    private LabelComponent sckModeLabel;
    private LabelComponent sckTargetLabel;
    private LabelComponent groupKillingLabel;
    private LabelComponent hyperionLookDownLabel;
    private ButtonComponent sckToggleButton;
    private ButtonComponent sckModeButton;
    private ButtonComponent groupKillingButton;
    private ButtonComponent hyperionToggleButton;
    
    // UI Components - Spike Helper
    private LabelComponent spikeStatusLabel;
    private ButtonComponent spikeToggleButton;
    private LabelComponent spikeAimAssistLabel;
    private ButtonComponent spikeAimAssistButton;
    
    // UI Components - Sliders
    private SlimSliderComponent recastDelaySlider;
    private LabelComponent recastDelayLabel;
    private SlimSliderComponent reelingDelaySlider;
    private LabelComponent reelingDelayLabel;
    private SlimSliderComponent groupKillThresholdSlider;
    private LabelComponent groupKillThresholdLabel;

    public FishingSection() {
        super(Sizing.fill(100), Sizing.content(), Algorithm.VERTICAL);
        
        // Section styling
        this.surface(Surface.flat(0x44001166).and(Surface.outline(0xFF4488FF))); // Blue theme
        this.padding(Insets.of(12));
        this.gap(10);

        buildSection();
        refresh();
    }

    private void buildSection() {
        // Section header
        FlowLayout headerRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .verticalAlignment(VerticalAlignment.CENTER);

        headerRow.child(
                Components.label(Text.literal("🎣"))
                        .color(Color.ofRgb(0x4488FF))
                        .shadow(true)
        );

        headerRow.child(
                Components.label(Text.literal("Fishing Module"))
                        .color(Color.ofRgb(0x4488FF))
                        .shadow(true)
        );

        this.child(headerRow);

        // Tab navigation bar
        FlowLayout tabBar = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .surface(Surface.flat(0x33000000))
                .padding(Insets.of(6));

        this.autoFishTabButton = (ButtonComponent) Components.button(
                Text.literal(Tab.AUTO_FISHING.label),
                button -> switchTab(Tab.AUTO_FISHING)
        ).horizontalSizing(Sizing.fixed(130));
        
        this.sckTabButton = (ButtonComponent) Components.button(
                Text.literal(Tab.SEA_CREATURE_KILLER.label),
                button -> switchTab(Tab.SEA_CREATURE_KILLER)
        ).horizontalSizing(Sizing.fixed(180));
        
        this.spikeTabButton = (ButtonComponent) Components.button(
                Text.literal(Tab.SPIKE_HELPER.label),
                button -> switchTab(Tab.SPIKE_HELPER)
        ).horizontalSizing(Sizing.fixed(130));
        
        this.configTabButton = (ButtonComponent) Components.button(
                Text.literal(Tab.CONFIGURATION.label),
                button -> switchTab(Tab.CONFIGURATION)
        ).horizontalSizing(Sizing.fixed(130));

        tabBar.child(this.autoFishTabButton);
        tabBar.child(this.sckTabButton);
        tabBar.child(this.spikeTabButton);
        tabBar.child(this.configTabButton);

        this.child(tabBar);

        // Content container (will be populated based on active tab)
        this.contentContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(10);
        
        this.child(this.contentContainer);

        // Initialize with first tab
        switchTab(Tab.AUTO_FISHING);
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        
        // Update tab button appearances
        updateTabButtonAppearance(this.autoFishTabButton, tab == Tab.AUTO_FISHING);
        updateTabButtonAppearance(this.sckTabButton, tab == Tab.SEA_CREATURE_KILLER);
        updateTabButtonAppearance(this.spikeTabButton, tab == Tab.SPIKE_HELPER);
        updateTabButtonAppearance(this.configTabButton, tab == Tab.CONFIGURATION);
        
        // Clear and rebuild content
        this.contentContainer.clearChildren();
        
        switch (tab) {
            case AUTO_FISHING:
                this.contentContainer.child(buildAutoFishingSection());
                break;
            case SEA_CREATURE_KILLER:
                this.contentContainer.child(buildSeaCreatureKillerSection());
                break;
            case SPIKE_HELPER:
                this.contentContainer.child(buildSpikeHelperSection());
                break;
            case CONFIGURATION:
                this.contentContainer.child(buildConfigurationSection());
                break;
        }
        
        refresh();
    }

    private void updateTabButtonAppearance(ButtonComponent button, boolean isActive) {
        // Active tabs get highlighted appearance
        if (isActive) {
            button.active = false; // Disable clicking on active tab
        } else {
            button.active = true;
        }
    }

    private FlowLayout buildAutoFishingSection() {
        FlowLayout section = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x33000000))
                .padding(Insets.of(10));
        section.gap(6);

        // Subsection title
        section.child(
                Components.label(Text.literal("Auto Fishing"))
                        .color(Color.ofRgb(0x64B5F6))
                        .shadow(false)
        );

        // Status
        this.autoFishStatusLabel = (LabelComponent) Components.label(Text.literal("Status: Disabled"))
                .color(Color.ofRgb(0xFF5252))
                .shadow(false);
        section.child(this.autoFishStatusLabel);

        // Control buttons
        FlowLayout buttonRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        this.autoFishToggleButton = (ButtonComponent) Components.button(
                Text.literal("Start Auto Fish"), 
                button -> toggleAutoFish()
        ).horizontalSizing(Sizing.fixed(150));
        
        buttonRow.child(this.autoFishToggleButton);

        buttonRow.child(
                Components.button(Text.literal("Info"), button -> showAutoFishInfo())
                        .horizontalSizing(Sizing.fixed(70))
        );

        section.child(buttonRow);

        return section;
    }

    private FlowLayout buildSeaCreatureKillerSection() {
        FlowLayout section = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x33000000))
                .padding(Insets.of(10));
        section.gap(6);

        // Subsection title
        section.child(
                Components.label(Text.literal("Sea Creature Killer"))
                        .color(Color.ofRgb(0x64B5F6))
                        .shadow(false)
        );

        // Status info
        FlowLayout statusGrid = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(3);

        this.sckStatusLabel = (LabelComponent) Components.label(Text.literal("Status: Disabled"))
                .color(Color.ofRgb(0xFF5252))
                .shadow(false);
        statusGrid.child(this.sckStatusLabel);

        this.sckModeLabel = (LabelComponent) Components.label(Text.literal("Combat Mode: RCM"))
                .color(Color.ofRgb(0xAAAAAA))
                .shadow(false);
        statusGrid.child(this.sckModeLabel);

        this.sckTargetLabel = (LabelComponent) Components.label(Text.literal("Target: None"))
                .color(Color.ofRgb(0xAAAAAA))
                .shadow(false);
        statusGrid.child(this.sckTargetLabel);

        this.groupKillingLabel = (LabelComponent) Components.label(Text.literal("Group Killing: OFF"))
                .color(Color.ofRgb(0xAAAAAA))
                .shadow(false);
        statusGrid.child(this.groupKillingLabel);

        this.hyperionLookDownLabel = (LabelComponent) Components.label(Text.literal("Hyperion Look Down: ON"))
                .color(Color.ofRgb(0xAAAAAA))
                .shadow(false);
        statusGrid.child(this.hyperionLookDownLabel);

        section.child(statusGrid);

        // Control buttons row 1
        FlowLayout buttonRow1 = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        this.sckToggleButton = (ButtonComponent) Components.button(
                Text.literal("Enable SCK"), 
                button -> toggleSCK()
        ).horizontalSizing(Sizing.fixed(120));
        buttonRow1.child(this.sckToggleButton);

        this.sckModeButton = (ButtonComponent) Components.button(
                Text.literal("Mode: RCM"), 
                button -> cycleSCKMode()
        ).horizontalSizing(Sizing.fixed(130));
        buttonRow1.child(this.sckModeButton);

        section.child(buttonRow1);

        // Control buttons row 2
        FlowLayout buttonRow2 = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        this.groupKillingButton = (ButtonComponent) Components.button(
                Text.literal("Group: OFF"), 
                button -> toggleGroupKilling()
        ).horizontalSizing(Sizing.fixed(120));
        buttonRow2.child(this.groupKillingButton);

        this.hyperionToggleButton = (ButtonComponent) Components.button(
                Text.literal("Hyperion ↓: ON"), 
                button -> toggleHyperionLookDown()
        ).horizontalSizing(Sizing.fixed(130));
        buttonRow2.child(this.hyperionToggleButton);

        buttonRow2.child(
                Components.button(Text.literal("Info"), button -> showSCKInfo())
                        .horizontalSizing(Sizing.fixed(70))
        );

        section.child(buttonRow2);

        return section;
    }

    private FlowLayout buildSpikeHelperSection() {
        FlowLayout section = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x33000000))
                .padding(Insets.of(10));
        section.gap(6);

        // Subsection title
        section.child(
                Components.label(Text.literal("Spike Helper"))
                        .color(Color.ofRgb(0x64B5F6))
                        .shadow(false)
        );

        // Description
        section.child(
                Components.label(Text.literal("Tracks closest Spike - catch at 9+ blocks distance"))
                        .color(Color.ofRgb(0xAAAAAA))
                        .shadow(false)
        );

        // Status
        this.spikeStatusLabel = (LabelComponent) Components.label(Text.literal("Status: Disabled"))
                .color(Color.ofRgb(0xFF5252))
                .shadow(false);
        section.child(this.spikeStatusLabel);

        // Control buttons
        FlowLayout buttonRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        this.spikeToggleButton = (ButtonComponent) Components.button(
                Text.literal("Enable Spike Helper"), 
                button -> toggleSpikeHelper()
        ).horizontalSizing(Sizing.fixed(160));
        
        buttonRow.child(this.spikeToggleButton);

        buttonRow.child(
                Components.button(Text.literal("Info"), button -> showSpikeHelperInfo())
                        .horizontalSizing(Sizing.fixed(70))
        );

        section.child(buttonRow);

        // Aim Assist subsection
        FlowLayout aimAssistRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        this.spikeAimAssistLabel = (LabelComponent) Components.label(Text.literal("Aim Assist: OFF"))
                .color(Color.ofRgb(0xFF5252))
                .shadow(false);
        
        aimAssistRow.child(this.spikeAimAssistLabel);

        this.spikeAimAssistButton = (ButtonComponent) Components.button(
                Text.literal("Enable Aim Assist"), 
                button -> toggleAimAssist()
        ).horizontalSizing(Sizing.fixed(130));
        
        aimAssistRow.child(this.spikeAimAssistButton);

        section.child(aimAssistRow);

        // Info section with bullet points
        FlowLayout infoBox = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x22000000))
                .padding(Insets.of(8));
        infoBox.gap(3);

        infoBox.child(Components.label(Text.literal("• Red highlight: Too close (< 9 blocks)"))
                .color(Color.ofRgb(0xFF5252))
                .shadow(false));
        
        infoBox.child(Components.label(Text.literal("• Green highlight: Good distance (≥ 9 blocks)"))
                .color(Color.ofRgb(0x66FF66))
                .shadow(false));
        
        infoBox.child(Components.label(Text.literal("• Auto-switches to closest Spike"))
                .color(Color.ofRgb(0xAAAAAA))
                .shadow(false));

        section.child(infoBox);

        return section;
    }

    private FlowLayout buildConfigurationSection() {
        FlowLayout section = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x33000000))
                .padding(Insets.of(10));
        section.gap(8);

        // Subsection title
        section.child(
                Components.label(Text.literal("Configuration"))
                        .color(Color.ofRgb(0x64B5F6))
                        .shadow(false)
        );

        // Recast Delay Slider (1 tick = 50ms)
        section.child(createSliderSection(
                "Recast Delay",
                0, 500, 5,
                FishConfig.getRecastDelay(),
                value -> {
                    int intValue = (int) Math.round(value);
                    FishConfig.setRecastDelay(intValue);
                    int ms = intValue * 50; // Convert ticks to milliseconds
                    this.recastDelayLabel.text(Text.literal(String.format("%d ms", ms)));
                    FishConfig.save();
                },
                label -> this.recastDelayLabel = label,
                slider -> this.recastDelaySlider = slider
        ));

        // Reeling Delay Slider (1 tick = 50ms)
        section.child(createSliderSection(
                "Reeling Delay",
                0, 100, 5,
                FishConfig.getReelingDelay(),
                value -> {
                    int intValue = (int) Math.round(value);
                    FishConfig.setReelingDelay(intValue);
                    int ms = intValue * 50; // Convert ticks to milliseconds
                    this.reelingDelayLabel.text(Text.literal(String.format("%d ms", ms)));
                    FishConfig.save();
                },
                label -> this.reelingDelayLabel = label,
                slider -> this.reelingDelaySlider = slider
        ));

        // Group Kill Threshold Slider
        section.child(createSliderSection(
                "Group Kill Threshold",
                1, 30, 1,
                FishConfig.getSeaCreatureKillThreshold(),
                value -> {
                    int intValue = (int) Math.round(value);
                    FishConfig.setSeaCreatureKillThreshold(intValue);
                    this.groupKillThresholdLabel.text(Text.literal(String.format("%d mobs", intValue)));
                    FishConfig.save();
                },
                label -> this.groupKillThresholdLabel = label,
                slider -> this.groupKillThresholdSlider = slider
        ));

        return section;
    }

    private FlowLayout createSliderSection(
            String labelText, 
            int min, 
            int max, 
            int step,
            int initialValue,
            java.util.function.Consumer<Double> onChange,
            java.util.function.Consumer<LabelComponent> labelConsumer,
            java.util.function.Consumer<SlimSliderComponent> sliderConsumer
    ) {
        FlowLayout sliderSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(4);

        // Label row with value
        FlowLayout labelRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .verticalAlignment(VerticalAlignment.CENTER);

        labelRow.child(
                Components.label(Text.literal(labelText + ":"))
                        .color(Color.ofRgb(0xFFAB91))
                        .shadow(false)
        );

        LabelComponent valueLabel = (LabelComponent) Components.label(
                Text.literal(labelText.contains("Threshold") 
                        ? String.format("%d mobs", initialValue)
                        : labelText.contains("Delay")
                        ? String.format("%d ms", initialValue * 50)
                        : String.format("%d ticks", initialValue))
        ).color(Color.ofRgb(0x4CAF50)).shadow(false);
        
        labelConsumer.accept(valueLabel);
        labelRow.child(valueLabel);
        sliderSection.child(labelRow);

        // Slider
        SlimSliderComponent slider = Components.slimSlider(SlimSliderComponent.Axis.HORIZONTAL);
        slider.sizing(Sizing.fill(100), Sizing.content());
        slider.min((double) min);
        slider.max((double) max);
        slider.value((double) initialValue);
        slider.stepSize((double) step);
        slider.onChanged().subscribe(value -> {
            onChange.accept(value);
        });
        
        sliderConsumer.accept(slider);
        sliderSection.child(slider);

        return sliderSection;
    }

    // === AUTO FISHING ACTIONS ===

    private void toggleAutoFish() {
        if (AutoFishingFeature.isEnabled()) {
            AutoFishingFeature.toggle(); // This will stop it
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("Auto Fishing disabled").formatted(Formatting.RED), 
                        false
                );
            }
        } else {
            AutoFishingFeature.toggle(); // This will start it
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("Auto Fishing enabled").formatted(Formatting.GREEN), 
                        false
                );
            }
        }
        refresh();
    }

    private void showAutoFishInfo() {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(""), false);
            client.player.sendMessage(
                    Text.literal("=== Auto Fishing ===").formatted(Formatting.GOLD), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Status: ").formatted(Formatting.GRAY)
                            .append(Text.literal(AutoFishingFeature.isEnabled() ? "ENABLED" : "DISABLED")
                                    .formatted(AutoFishingFeature.isEnabled() ? Formatting.GREEN : Formatting.RED)), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Recast Delay: " + (FishConfig.getRecastDelay() * 50) + " ms").formatted(Formatting.YELLOW), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Reeling Delay: " + (FishConfig.getReelingDelay() * 50) + " ms").formatted(Formatting.YELLOW), 
                    false
            );
            client.player.sendMessage(Text.literal(""), false);
        }
    }

    // === SEA CREATURE KILLER ACTIONS ===

    private void toggleSCK() {
        if (SeaCreatureKiller.isEnabled()) {
            SeaCreatureKiller.setEnabled(false);
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("Sea Creature Killer disabled").formatted(Formatting.RED), 
                        false
                );
            }
        } else {
            SeaCreatureKiller.setEnabled(true);
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("Sea Creature Killer enabled").formatted(Formatting.GREEN), 
                        false
                );
            }
        }
        refresh();
    }

    private void cycleSCKMode() {
        String currentMode = FishConfig.getCombatMode();
        String newMode = currentMode.equals("RCM") ? "MELEE" : "RCM";
        FishConfig.setCombatMode(newMode);
        FishConfig.save();
        
        // Switch the active combat mode immediately (not just in config)
        CombatModeManager.getInstance().setMode(newMode);
        
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("Combat mode changed to: " + newMode).formatted(Formatting.GREEN), 
                    false
            );
        }
        refresh();
    }

    private void toggleGroupKilling() {
        boolean current = FishConfig.getSeaCreatureKillThreshold() > 1;
        if (current) {
            FishConfig.setSeaCreatureKillThreshold(1);
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("Group killing disabled").formatted(Formatting.RED), 
                        false
                );
            }
        } else {
            FishConfig.setSeaCreatureKillThreshold(5);
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("Group killing enabled (threshold: 5)").formatted(Formatting.GREEN), 
                        false
                );
            }
        }
        FishConfig.save();
        refresh();
    }

    private void toggleHyperionLookDown() {
        boolean current = FishConfig.isHyperionLookDownEnabled();
        FishConfig.setHyperionLookDownEnabled(!current);
        FishConfig.save();
        
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("Hyperion look down: " + (!current ? "ENABLED" : "DISABLED"))
                            .formatted(!current ? Formatting.GREEN : Formatting.RED), 
                    false
            );
        }
        refresh();
    }

    private void showSCKInfo() {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(""), false);
            client.player.sendMessage(
                    Text.literal("=== Sea Creature Killer ===").formatted(Formatting.GOLD), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Status: ").formatted(Formatting.GRAY)
                            .append(Text.literal(SeaCreatureKiller.isEnabled() ? "ENABLED" : "DISABLED")
                                    .formatted(SeaCreatureKiller.isEnabled() ? Formatting.GREEN : Formatting.RED)), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Combat Mode: " + FishConfig.getCombatMode()).formatted(Formatting.YELLOW), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Group Killing: " + (FishConfig.getSeaCreatureKillThreshold() > 1 ? "ON" : "OFF"))
                            .formatted(Formatting.YELLOW), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Threshold: " + FishConfig.getSeaCreatureKillThreshold()).formatted(Formatting.YELLOW), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Hyperion Look Down: " + (FishConfig.isHyperionLookDownEnabled() ? "ON" : "OFF"))
                            .formatted(Formatting.YELLOW), 
                    false
            );
            client.player.sendMessage(Text.literal(""), false);
        }
    }

    // === SPIKE HELPER ACTIONS ===

    private void toggleSpikeHelper() {
        SpikeHelper.toggle();
        refresh();
    }

    private void toggleAimAssist() {
        boolean currentState = SpikeHelperConfig.isAimAssistEnabled();
        SpikeHelperConfig.setAimAssistEnabled(!currentState);
        refresh();
    }

    private void showSpikeHelperInfo() {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(""), false);
            client.player.sendMessage(
                    Text.literal("=== Spike Helper ===").formatted(Formatting.GOLD), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Status: ").formatted(Formatting.GRAY)
                            .append(Text.literal(SpikeHelper.isEnabled() ? "ENABLED" : "DISABLED")
                                    .formatted(SpikeHelper.isEnabled() ? Formatting.GREEN : Formatting.RED)), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Target Distance: 9.0 blocks (minimum)").formatted(Formatting.YELLOW), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Current Status: " + SpikeHelper.getStatusText()).formatted(Formatting.YELLOW), 
                    false
            );
            client.player.sendMessage(Text.literal(""), false);
            client.player.sendMessage(
                    Text.literal("Spikes can be caught when at least 9 blocks away.").formatted(Formatting.GRAY), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Red highlight = too close, Green = 9+ blocks").formatted(Formatting.GRAY), 
                    false
            );
            client.player.sendMessage(Text.literal(""), false);
        }
    }

    /**
     * Refresh the section to update status and buttons
     */
    public void refresh() {
        // Only refresh components that currently exist (are on active tab)
        if (this.autoFishStatusLabel != null) {
            refreshAutoFish();
        }
        if (this.sckStatusLabel != null) {
            refreshSCK();
        }
        if (this.spikeStatusLabel != null) {
            refreshSpikeHelper();
        }
        if (this.recastDelaySlider != null) {
            refreshSliders();
        }
    }

    private void refreshAutoFish() {
        if (this.autoFishStatusLabel == null || this.autoFishToggleButton == null) {
            return;
        }
        
        boolean isEnabled = AutoFishingFeature.isEnabled();

        // Update status
        if (isEnabled) {
            this.autoFishStatusLabel.text(Text.literal("Status: Enabled ●"));
            this.autoFishStatusLabel.color(Color.ofRgb(0x4CAF50));
        } else {
            this.autoFishStatusLabel.text(Text.literal("Status: Disabled ○"));
            this.autoFishStatusLabel.color(Color.ofRgb(0xFF5252));
        }

        // Update button
        if (isEnabled) {
            this.autoFishToggleButton.setMessage(Text.literal("Stop Auto Fish"));
        } else {
            this.autoFishToggleButton.setMessage(Text.literal("Start Auto Fish"));
        }
    }

    private void refreshSCK() {
        if (this.sckStatusLabel == null || this.sckModeLabel == null || this.sckTargetLabel == null || 
            this.groupKillingLabel == null || this.hyperionLookDownLabel == null || this.sckToggleButton == null) {
            return;
        }
        
        boolean isEnabled = SeaCreatureKiller.isEnabled();
        String mode = FishConfig.getCombatMode();
        int threshold = FishConfig.getSeaCreatureKillThreshold();
        boolean hyperionLookDown = FishConfig.isHyperionLookDownEnabled();

        // Update status
        if (isEnabled) {
            this.sckStatusLabel.text(Text.literal("Status: Enabled ●"));
            this.sckStatusLabel.color(Color.ofRgb(0x4CAF50));
        } else {
            this.sckStatusLabel.text(Text.literal("Status: Disabled ○"));
            this.sckStatusLabel.color(Color.ofRgb(0xFF5252));
        }

        // Update mode label
        this.sckModeLabel.text(Text.literal("Combat Mode: " + mode));
        this.sckModeLabel.color(Color.ofRgb(0xAAAAAA));

        // Update target label (melee mode specific)
        String targetInfo = "Target: None";
        if (mode.equalsIgnoreCase("MELEE")) {
            MeleeMode meleeMode = CombatModeManager.getInstance().getMeleeMode();
            if (meleeMode != null && meleeMode.getActualTarget() != null) {
                targetInfo = "Target: " + meleeMode.getActualTarget().getName().getString();
            }
        }
        this.sckTargetLabel.text(Text.literal(targetInfo));
        this.sckTargetLabel.color(Color.ofRgb(0xAAAAAA));

        // Update group killing label
        boolean groupKillingEnabled = threshold > 1;
        this.groupKillingLabel.text(Text.literal("Group Killing: " + (groupKillingEnabled ? "ON (" + threshold + ")" : "OFF")));
        this.groupKillingLabel.color(groupKillingEnabled ? Color.ofRgb(0x4CAF50) : Color.ofRgb(0xAAAAAA));

        // Update hyperion label
        this.hyperionLookDownLabel.text(Text.literal("Hyperion Look Down: " + (hyperionLookDown ? "ON" : "OFF")));
        this.hyperionLookDownLabel.color(hyperionLookDown ? Color.ofRgb(0x4CAF50) : Color.ofRgb(0xAAAAAA));

        // Update buttons
        this.sckToggleButton.setMessage(Text.literal(isEnabled ? "Disable SCK" : "Enable SCK"));
        this.sckModeButton.setMessage(Text.literal("Mode: " + mode));
        this.groupKillingButton.setMessage(Text.literal("Group: " + (groupKillingEnabled ? "ON" : "OFF")));
        this.hyperionToggleButton.setMessage(Text.literal("Hyperion ↓: " + (hyperionLookDown ? "ON" : "OFF")));
    }

    private void refreshSpikeHelper() {
        if (this.spikeStatusLabel == null || this.spikeToggleButton == null || this.spikeAimAssistLabel == null) {
            return;
        }
        
        boolean isEnabled = SpikeHelper.isEnabled();
        String statusText = SpikeHelper.getStatusText();

        // Update status label
        if (isEnabled) {
            // Color based on tracking status
            if (SpikeHelper.getTrackedSpike() != null) {
                if (SpikeHelper.isAtCorrectDistance()) {
                    this.spikeStatusLabel.text(Text.literal(statusText + " ●"));
                    this.spikeStatusLabel.color(Color.ofRgb(0x4CAF50)); // Green - correct distance
                } else {
                    this.spikeStatusLabel.text(Text.literal(statusText + " ●"));
                    this.spikeStatusLabel.color(Color.ofRgb(0xFF9800)); // Orange - tracking but wrong distance
                }
            } else {
                this.spikeStatusLabel.text(Text.literal(statusText + " ●"));
                this.spikeStatusLabel.color(Color.ofRgb(0xFFEB3B)); // Yellow - searching
            }
        } else {
            this.spikeStatusLabel.text(Text.literal(statusText + " ○"));
            this.spikeStatusLabel.color(Color.ofRgb(0xFF5252)); // Red - disabled
        }

        // Update button
        this.spikeToggleButton.setMessage(Text.literal(isEnabled ? "Disable Spike Helper" : "Enable Spike Helper"));

        // Update Aim Assist status
        boolean aimAssistEnabled = SpikeHelperConfig.isAimAssistEnabled();
        if (aimAssistEnabled) {
            this.spikeAimAssistLabel.text(Text.literal("Aim Assist: ON"));
            this.spikeAimAssistLabel.color(Color.ofRgb(0x4CAF50)); // Green
        } else {
            this.spikeAimAssistLabel.text(Text.literal("Aim Assist: OFF"));
            this.spikeAimAssistLabel.color(Color.ofRgb(0xFF5252)); // Red
        }
        this.spikeAimAssistButton.setMessage(Text.literal(aimAssistEnabled ? "Disable Aim Assist" : "Enable Aim Assist"));
    }

    private void refreshSliders() {
        // Update slider values from config (only if they exist)
        if (this.recastDelaySlider != null && this.recastDelayLabel != null) {
            this.recastDelaySlider.value((double) FishConfig.getRecastDelay());
            this.recastDelayLabel.text(Text.literal(String.format("%d ticks", FishConfig.getRecastDelay())));
        }
        
        if (this.reelingDelaySlider != null && this.reelingDelayLabel != null) {
            this.reelingDelaySlider.value((double) FishConfig.getReelingDelay());
            this.reelingDelayLabel.text(Text.literal(String.format("%d ticks", FishConfig.getReelingDelay())));
        }
        
        if (this.groupKillThresholdSlider != null && this.groupKillThresholdLabel != null) {
            this.groupKillThresholdSlider.value((double) FishConfig.getSeaCreatureKillThreshold());
            this.groupKillThresholdLabel.text(Text.literal(String.format("%d mobs", FishConfig.getSeaCreatureKillThreshold())));
        }
    }
}
