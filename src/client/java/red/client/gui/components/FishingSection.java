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
import red.client.fishing.config.FishConfig;
import red.client.fishing.feature.AutoFishingFeature;
import red.client.fishing.feature.SeaCreatureKiller;

/**
 * Fishing section for Auto Fishing and Sea Creature Killer controls
 */
public class FishingSection extends FlowLayout {
    private final MinecraftClient client = MinecraftClient.getInstance();
    
    // UI Components - Auto Fishing
    private LabelComponent autoFishStatusLabel;
    private ButtonComponent autoFishToggleButton;
    
    // UI Components - Sea Creature Killer
    private LabelComponent sckStatusLabel;
    private LabelComponent sckModeLabel;
    private LabelComponent groupKillingLabel;
    private LabelComponent hyperionLookDownLabel;
    private ButtonComponent sckToggleButton;
    private ButtonComponent sckModeButton;
    private ButtonComponent groupKillingButton;
    private ButtonComponent hyperionToggleButton;
    
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
                Components.label(Text.literal("Fishing - Auto Fish & Sea Creature Killer"))
                        .color(Color.ofRgb(0x4488FF))
                        .shadow(true)
        );

        this.child(headerRow);

        // Description
        this.child(
                Components.label(Text.literal("Automated fishing with sea creature combat"))
                        .color(Color.ofRgb(0xCCCCCC))
                        .shadow(false)
        );

        // === AUTO FISHING SECTION ===
        this.child(buildAutoFishingSection());

        // === SEA CREATURE KILLER SECTION ===
        this.child(buildSeaCreatureKillerSection());

        // === CONFIGURATION SECTION ===
        this.child(buildConfigurationSection());
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
                100, 500, 10,
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
                10, 100, 5,
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
                    Text.literal("Recast Delay: " + FishConfig.getRecastDelay() + " ticks").formatted(Formatting.YELLOW), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Reeling Delay: " + FishConfig.getReelingDelay() + " ticks").formatted(Formatting.YELLOW), 
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

    // === REFRESH ===

    /**
     * Refresh the section to update status and buttons
     */
    public void refresh() {
        refreshAutoFish();
        refreshSCK();
        refreshSliders();
    }

    private void refreshAutoFish() {
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

    private void refreshSliders() {
        // Update slider values from config
        if (this.recastDelaySlider != null) {
            this.recastDelaySlider.value((double) FishConfig.getRecastDelay());
            this.recastDelayLabel.text(Text.literal(String.format("%d ticks", FishConfig.getRecastDelay())));
        }
        
        if (this.reelingDelaySlider != null) {
            this.reelingDelaySlider.value((double) FishConfig.getReelingDelay());
            this.reelingDelayLabel.text(Text.literal(String.format("%d ticks", FishConfig.getReelingDelay())));
        }
        
        if (this.groupKillThresholdSlider != null) {
            this.groupKillThresholdSlider.value((double) FishConfig.getSeaCreatureKillThreshold());
            this.groupKillThresholdLabel.text(Text.literal(String.format("%d mobs", FishConfig.getSeaCreatureKillThreshold())));
        }
    }
}
