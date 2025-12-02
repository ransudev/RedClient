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
import red.client.scheduler.MacroScheduler;
import red.client.scheduler.MacroSchedulerConfig;

/**
 * Scheduler section for macro run time and break management
 */
public class SchedulerSection extends FlowLayout {
    private final MinecraftClient client = MinecraftClient.getInstance();
    
    // UI Components
    private LabelComponent statusLabel;
    private LabelComponent timerLabel;
    private ButtonComponent startButton;
    private ButtonComponent stopButton;
    private ButtonComponent breakToggleButton;
    private LabelComponent runTimeLabel;
    private LabelComponent breakMinLabel;
    private LabelComponent breakMaxLabel;
    private SlimSliderComponent runTimeSlider;
    private SlimSliderComponent breakMinSlider;
    private SlimSliderComponent breakMaxSlider;

    public SchedulerSection() {
        super(Sizing.fill(100), Sizing.content(), Algorithm.VERTICAL);
        
        // Section styling - Purple theme
        this.surface(Surface.flat(0x44330066).and(Surface.outline(0xFF8844FF)));
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
                Components.label(Text.literal("⏱"))
                        .color(Color.ofRgb(0xAA88FF))
                        .shadow(true)
        );

        headerRow.child(
                Components.label(Text.literal("Macro Scheduler"))
                        .color(Color.ofRgb(0xAA88FF))
                        .shadow(true)
        );

        this.child(headerRow);

        // Description
        this.child(
                Components.label(Text.literal("Automatically manage macro run time and breaks"))
                        .color(Color.ofRgb(0xCCCCCC))
                        .shadow(false)
        );

        // Status section
        FlowLayout statusSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x22000000))
                .padding(Insets.of(8));
        statusSection.gap(4);

        this.statusLabel = (LabelComponent) Components.label(Text.literal("Status: Stopped"))
                .color(Color.ofRgb(0x888888))
                .shadow(false);
        statusSection.child(this.statusLabel);

        this.timerLabel = (LabelComponent) Components.label(Text.literal("Time Remaining: --:--"))
                .color(Color.ofRgb(0x888888))
                .shadow(false);
        statusSection.child(this.timerLabel);

        this.child(statusSection);

        // Control buttons
        FlowLayout buttonRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        this.startButton = (ButtonComponent) Components.button(
                Text.literal("Start Scheduler"), 
                button -> startScheduler()
        ).horizontalSizing(Sizing.fixed(140));
        buttonRow.child(this.startButton);

        this.stopButton = (ButtonComponent) Components.button(
                Text.literal("Stop Scheduler"), 
                button -> stopScheduler()
        ).horizontalSizing(Sizing.fixed(130));
        buttonRow.child(this.stopButton);

        this.child(buttonRow);

        // Break toggle button
        this.breakToggleButton = (ButtonComponent) Components.button(
                Text.literal("Breaks: OFF"), 
                button -> toggleBreaks()
        ).horizontalSizing(Sizing.fixed(150));
        this.child(this.breakToggleButton);

        // Configuration sliders
        FlowLayout configSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x22000000))
                .padding(Insets.of(10));
        configSection.gap(8);

        configSection.child(
                Components.label(Text.literal("Configuration"))
                        .color(Color.ofRgb(0xAA88FF))
                        .shadow(false)
        );

        // Run Time Slider (1-180 minutes = 1-3 hours)
        configSection.child(createSliderSection(
                "Run Time",
                1, 180, 5,
                MacroSchedulerConfig.getRunTime(),
                value -> {
                    int intValue = (int) Math.round(value);
                    MacroSchedulerConfig.setRunTime(intValue);
                    int hours = intValue / 60;
                    int minutes = intValue % 60;
                    String timeStr = hours > 0 ? 
                            String.format("%dh %dm", hours, minutes) : 
                            String.format("%d min", minutes);
                    this.runTimeLabel.text(Text.literal(timeStr));
                },
                label -> this.runTimeLabel = label,
                slider -> this.runTimeSlider = slider
        ));

        // Break Min Time Slider (1-60 minutes)
        configSection.child(createSliderSection(
                "Break Min Time",
                1, 60, 1,
                MacroSchedulerConfig.getBreakMinTime(),
                value -> {
                    int intValue = (int) Math.round(value);
                    MacroSchedulerConfig.setBreakMinTime(intValue);
                    this.breakMinLabel.text(Text.literal(String.format("%d min", intValue)));
                },
                label -> this.breakMinLabel = label,
                slider -> this.breakMinSlider = slider
        ));

        // Break Max Time Slider (1-60 minutes)
        configSection.child(createSliderSection(
                "Break Max Time",
                1, 60, 1,
                MacroSchedulerConfig.getBreakMaxTime(),
                value -> {
                    int intValue = (int) Math.round(value);
                    MacroSchedulerConfig.setBreakMaxTime(intValue);
                    this.breakMaxLabel.text(Text.literal(String.format("%d min", intValue)));
                },
                label -> this.breakMaxLabel = label,
                slider -> this.breakMaxSlider = slider
        ));

        this.child(configSection);
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
                        .color(Color.ofRgb(0xCCCCCC))
                        .shadow(false)
        );

        // Value label (will be populated by consumer)
        int hours = initialValue / 60;
        int minutes = initialValue % 60;
        String initialText = labelText.contains("Run Time") && hours > 0 ?
                String.format("%dh %dm", hours, minutes) :
                String.format("%d min", initialValue);
        LabelComponent valueLabel = (LabelComponent) Components.label(Text.literal(initialText))
                .color(Color.ofRgb(0xFFFFFF))
                .shadow(false);
        labelRow.child(valueLabel);

        sliderSection.child(labelRow);

        // Slider
        SlimSliderComponent slider = Components.slimSlider(SlimSliderComponent.Axis.HORIZONTAL);
        slider.sizing(Sizing.fill(100), Sizing.fixed(8));
        slider.min(min);
        slider.max(max);
        slider.value(initialValue);
        
        // Set up the callback
        slider.onChanged().subscribe(value -> {
            onChange.accept(value);
        });
        
        sliderSection.child(slider);

        // Pass references to consumers
        labelConsumer.accept(valueLabel);
        sliderConsumer.accept(slider);

        return sliderSection;
    }

    /**
     * Start the scheduler
     */
    private void startScheduler() {
        MacroScheduler.start();
        refresh();
    }

    /**
     * Stop the scheduler
     */
    private void stopScheduler() {
        MacroScheduler.stop();
        refresh();
    }

    /**
     * Toggle break enable/disable
     */
    private void toggleBreaks() {
        boolean currentState = MacroSchedulerConfig.isBreakEnabled();
        MacroSchedulerConfig.setBreakEnabled(!currentState);
        
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("Breaks: ").formatted(Formatting.GRAY)
                            .append(Text.literal(!currentState ? "ENABLED" : "DISABLED")
                                    .formatted(!currentState ? Formatting.GREEN : Formatting.RED)), 
                    false
            );
        }
        refresh();
    }

    /**
     * Refresh the section to update status and buttons
     */
    public void refresh() {
        MacroScheduler.SchedulerState state = MacroScheduler.getState();

        // Update status label
        switch (state) {
            case RUNNING:
                this.statusLabel.text(Text.literal("Status: Running ●"));
                this.statusLabel.color(Color.ofRgb(0x4CAF50)); // Green
                break;
            case ON_BREAK:
                this.statusLabel.text(Text.literal("Status: On Break ⏸"));
                this.statusLabel.color(Color.ofRgb(0xFFAA00)); // Gold
                break;
            case STOPPED:
            default:
                this.statusLabel.text(Text.literal("Status: Stopped ○"));
                this.statusLabel.color(Color.ofRgb(0x888888)); // Gray
                break;
        }

        // Update timer label
        if (state != MacroScheduler.SchedulerState.STOPPED) {
            String timeRemaining = MacroScheduler.getTimeRemainingFormatted();
            this.timerLabel.text(Text.literal("Time Remaining: " + timeRemaining));
            this.timerLabel.color(Color.ofRgb(0xFFFFFF));
        } else {
            this.timerLabel.text(Text.literal("Time Remaining: --:--"));
            this.timerLabel.color(Color.ofRgb(0x888888));
        }

        // Update break toggle button
        boolean breaksEnabled = MacroSchedulerConfig.isBreakEnabled();
        this.breakToggleButton.setMessage(Text.literal("Breaks: " + (breaksEnabled ? "ON" : "OFF")));

        // Update button states
        boolean isRunning = state != MacroScheduler.SchedulerState.STOPPED;
        this.startButton.active = !isRunning;
        this.stopButton.active = isRunning;
    }
}
