package red.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
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

/**
 * Main GUI screen for RedClient
 * Features two main sections:
 * - Hunting: Flare Combat Macro controls
 * - Fishing: Auto fishing and Sea Creature Killer controls
 */
public class RedClientScreen extends BaseOwoScreen<FlowLayout> {
    private FlowLayout mainContent;
    private ScrollContainer<FlowLayout> scrollContainer;
    
    // Section components
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

        // Create main content area with scroll
        this.mainContent = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .padding(Insets.of(5));
        this.mainContent.gap(12);

        this.scrollContainer = Containers.verticalScroll(
                Sizing.fill(90), 
                Sizing.fill(75), 
                this.mainContent
        );
        rootComponent.child(this.scrollContainer);

        // Build sections
        buildSections();

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

    private void buildSections() {
        // Hunting Section (Flare Combat Macro)
        this.huntingSection = new HuntingSection();
        this.mainContent.child(this.huntingSection);

        // Fishing Section (Auto Fishing + Sea Creature Killer)
        this.fishingSection = new FishingSection();
        this.mainContent.child(this.fishingSection);
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
     * Refresh all sections to update their state
     */
    private void refreshSections() {
        if (this.huntingSection != null) {
            this.huntingSection.refresh();
        }
        if (this.fishingSection != null) {
            this.fishingSection.refresh();
        }
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause the game when GUI is open
    }
}
