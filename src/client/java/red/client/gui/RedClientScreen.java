package red.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import red.client.gui.components.FishingSection;
import red.client.gui.components.HuntingSection;
import red.client.gui.components.SchedulerSection;
import red.client.scheduler.MacroScheduler;

/**
 * Main GUI screen for RedClient with tabbed navigation
 * Features three main tabs:
 * - Scheduler: Macro run time and break management
 * - Hunting: Flare Combat Macro controls
 * - Fishing: Auto fishing and Sea Creature Killer controls
 */
public class RedClientScreen extends BaseOwoScreen<FlowLayout> {
    // Tab enum
    private enum Module {
        SCHEDULER("📅 Scheduler", 0x64B5F6),
        HUNTING("🏹 Hunting", 0xFF9800),
        FISHING("🎣 Fishing", 0x4488FF);
        
        final String label;
        @SuppressWarnings("unused")
        final int color;
        
        Module(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }
    
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private Module currentModule = Module.SCHEDULER;
    
    // Tab buttons
    private ButtonComponent schedulerTabButton;
    private ButtonComponent huntingTabButton;
    private ButtonComponent fishingTabButton;
    
    // Content container
    private FlowLayout contentContainer;
    
    // Section components
    private SchedulerSection schedulerSection;
    private HuntingSection huntingSection;
    private FishingSection fishingSection;

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT);
        rootComponent.padding(Insets.of(10));

        // Build title section
        buildTitleSection(rootComponent);

        // Build tab navigation bar
        buildTabBar(rootComponent);

        // Create main content area with scroll
        this.contentContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .gap(12);

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(90), 
                Sizing.fill(75), 
                this.contentContainer
        );
        rootComponent.child(scrollContainer);

        // Initialize with first tab
        switchModule(Module.SCHEDULER);

        // Build bottom button row
        buildBottomButtons(rootComponent);
    }

    private void buildTitleSection(FlowLayout rootComponent) {
        FlowLayout titleSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.bottom(10));

        // Main title
        titleSection.child(
                Components.label(Text.of("RedClient GUI"))
                        .horizontalTextAlignment(HorizontalAlignment.CENTER)
                        .color(Color.ofRgb(0xFF5252)) // Red color
                        .shadow(true)
                        .margins(Insets.bottom(3))
        );

        // Subtitle
        titleSection.child(
                Components.label(Text.of("Hunting & Fishing Features"))
                        .horizontalTextAlignment(HorizontalAlignment.CENTER)
                        .color(Color.ofRgb(0xAAFFAA)) // Light green
                        .shadow(false)
                        .margins(Insets.bottom(2))
        );

        // Help text
        titleSection.child(
                Components.label(Text.of("Configure your macros and features"))
                        .horizontalTextAlignment(HorizontalAlignment.CENTER)
                        .color(Color.ofRgb(0x888888)) // Gray
                        .shadow(false)
        );

        rootComponent.child(titleSection);
    }

    private void buildTabBar(FlowLayout rootComponent) {
        FlowLayout tabBar = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(4)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .surface(Surface.flat(0x33000000))
                .padding(Insets.of(8));

        this.schedulerTabButton = (ButtonComponent) Components.button(
                Text.literal(Module.SCHEDULER.label),
                button -> switchModule(Module.SCHEDULER)
        ).horizontalSizing(Sizing.fixed(130));
        
        this.huntingTabButton = (ButtonComponent) Components.button(
                Text.literal(Module.HUNTING.label),
                button -> switchModule(Module.HUNTING)
        ).horizontalSizing(Sizing.fixed(130));
        
        this.fishingTabButton = (ButtonComponent) Components.button(
                Text.literal(Module.FISHING.label),
                button -> switchModule(Module.FISHING)
        ).horizontalSizing(Sizing.fixed(130));

        tabBar.child(this.schedulerTabButton);
        tabBar.child(this.huntingTabButton);
        tabBar.child(this.fishingTabButton);

        rootComponent.child(tabBar);
    }

    private void switchModule(Module module) {
        this.currentModule = module;
        
        // Update tab button appearances
        updateTabButtonAppearance(this.schedulerTabButton, module == Module.SCHEDULER);
        updateTabButtonAppearance(this.huntingTabButton, module == Module.HUNTING);
        updateTabButtonAppearance(this.fishingTabButton, module == Module.FISHING);
        
        // Clear and rebuild content
        this.contentContainer.clearChildren();
        
        switch (module) {
            case SCHEDULER:
                if (this.schedulerSection == null) {
                    this.schedulerSection = new SchedulerSection();
                }
                this.contentContainer.child(this.schedulerSection);
                break;
            case HUNTING:
                if (this.huntingSection == null) {
                    this.huntingSection = new HuntingSection();
                }
                this.contentContainer.child(this.huntingSection);
                break;
            case FISHING:
                if (this.fishingSection == null) {
                    this.fishingSection = new FishingSection();
                }
                this.contentContainer.child(this.fishingSection);
                break;
        }
        
        refreshSections();
    }

    private void updateTabButtonAppearance(ButtonComponent button, boolean isActive) {
        // Active tabs get disabled appearance
        if (isActive) {
            button.active = false;
        } else {
            button.active = true;
        }
    }

    private void buildBottomButtons(FlowLayout rootComponent) {
        FlowLayout buttonRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(90), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.of(10));

        // Refresh button
        buttonRow.child(
                Components.button(Text.literal("Refresh"), button -> {
                    refreshSections();
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.literal("GUI refreshed!").formatted(Formatting.GREEN), 
                                false
                        );
                    }
                }).horizontalSizing(Sizing.fixed(100))
        );

        // Done button
        buttonRow.child(
                Components.button(Text.literal("Done"), button -> this.close())
                        .horizontalSizing(Sizing.fixed(100))
        );

        rootComponent.child(buttonRow);
    }

    /**
     * Refresh all sections
     */
    private void refreshSections() {
        if (this.schedulerSection != null) {
            this.schedulerSection.refresh();
        }
        if (this.huntingSection != null) {
            this.huntingSection.refresh();
        }
        if (this.fishingSection != null) {
            this.fishingSection.refresh();
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Update scheduler on every tick
        MacroScheduler.tick();
        // Refresh sections to update timer displays
        refreshSections();
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause game when GUI is open
    }
}
