package red.client.gui.components;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import red.client.flarecombat.config.FlareConfig;
import red.client.flarecombat.feature.FlareMacroFeature;

/**
 * Hunting section for Flare Combat Macro controls
 */
public class HuntingSection extends FlowLayout {
    private final MinecraftClient client = MinecraftClient.getInstance();
    
    // UI Components
    private LabelComponent statusLabel;
    private ButtonComponent toggleButton;
    private LabelComponent keybindLabel;
    private LabelComponent combatModeLabel;
    private LabelComponent clickCountLabel;
    private ButtonComponent modeButton;
    private ButtonComponent clickButton;

    public HuntingSection() {
        super(Sizing.fill(100), Sizing.content(), Algorithm.VERTICAL);
        
        // Section styling
        this.surface(Surface.flat(0x44661100).and(Surface.outline(0xFF884400))); // Orange theme
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
                Components.label(Text.literal("⚔"))
                        .color(Color.ofRgb(0xFF8844))
                        .shadow(true)
        );

        headerRow.child(
                Components.label(Text.literal("Hunting - Flare Combat Macro"))
                        .color(Color.ofRgb(0xFF8844))
                        .shadow(true)
        );

        this.child(headerRow);

        // Description
        this.child(
                Components.label(Text.literal("Automatically detects and attacks flare mobs"))
                        .color(Color.ofRgb(0xCCCCCC))
                        .shadow(false)
        );

        // Status section
        FlowLayout statusSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x22000000))
                .padding(Insets.of(8));
        statusSection.gap(4);

        this.statusLabel = (LabelComponent) Components.label(Text.literal("Status: Disabled"))
                .color(Color.ofRgb(0xFF5252))
                .shadow(false);
        statusSection.child(this.statusLabel);

        this.keybindLabel = (LabelComponent) Components.label(Text.literal("Keybind: Not Set"))
                .color(Color.ofRgb(0x888888))
                .shadow(false);
        statusSection.child(this.keybindLabel);
        
        this.combatModeLabel = (LabelComponent) Components.label(Text.literal("Mode: Hyperion"))
                .color(Color.ofRgb(0xFFAA00))
                .shadow(false);
        statusSection.child(this.combatModeLabel);
        
        this.clickCountLabel = (LabelComponent) Components.label(Text.literal("Click Count: 1"))
                .color(Color.ofRgb(0xFFAA00))
                .shadow(false);
        statusSection.child(this.clickCountLabel);

        this.child(statusSection);

        // Control buttons row 1
        FlowLayout buttonRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        this.toggleButton = (ButtonComponent) Components.button(
                Text.literal("Start Flare Combat"), 
                button -> toggleFlareCombat()
        ).horizontalSizing(Sizing.fixed(150));
        
        buttonRow.child(this.toggleButton);

        buttonRow.child(
                Components.button(Text.literal("Info"), button -> showInfo())
                        .horizontalSizing(Sizing.fixed(70))
        );

        this.child(buttonRow);
        
        // Control buttons row 2 - Mode and Click Count
        FlowLayout buttonRow2 = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        this.modeButton = (ButtonComponent) Components.button(
                Text.literal("Switch Mode"), 
                button -> cycleCombatMode()
        ).horizontalSizing(Sizing.fixed(120));
        buttonRow2.child(this.modeButton);

        this.clickButton = (ButtonComponent) Components.button(
                Text.literal("Click Count"), 
                button -> cycleClickCount()
        ).horizontalSizing(Sizing.fixed(110));
        buttonRow2.child(this.clickButton);

        this.child(buttonRow2);

        // Info section
        FlowLayout infoSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x22222200))
                .padding(Insets.of(8));
        infoSection.gap(2);

        infoSection.child(createInfoLabel("• Hyperion: Right-click attack mode"));
        infoSection.child(createInfoLabel("• Fire Veil Wand: Single-click attack"));
        infoSection.child(createInfoLabel("• Click Count: 1-10 (Hyperion only)"));
        infoSection.child(createInfoLabel("• Toggle with keybind (default: V)"));

        this.child(infoSection);
    }

    private LabelComponent createInfoLabel(String text) {
        return (LabelComponent) Components.label(Text.literal(text))
                .color(Color.ofRgb(0xAAAAAA))
                .shadow(false)
                .sizing(Sizing.content(), Sizing.content());
    }

    /**
     * Toggle the Flare Combat Macro on/off
     */
    private void toggleFlareCombat() {
        if (FlareMacroFeature.isEnabled()) {
            FlareMacroFeature.stop();
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("Flare Combat disabled").formatted(Formatting.RED), 
                        false
                );
            }
        } else {
            FlareMacroFeature.start();
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("Flare Combat enabled").formatted(Formatting.GREEN), 
                        false
                );
            }
        }
        refresh();
    }
    
    /**
     * Cycle through combat modes (Hyperion <-> Fire Veil Wand)
     */
    private void cycleCombatMode() {
        int currentMode = FlareConfig.getCombatMode();
        int newMode = currentMode == 1 ? 2 : 1;
        FlareConfig.setCombatMode(newMode);
        
        String modeName = FlareConfig.getCombatModeName();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("Combat mode: ").formatted(Formatting.GRAY)
                            .append(Text.literal(modeName).formatted(Formatting.GOLD)), 
                    false
            );
        }
        refresh();
    }
    
    /**
     * Cycle through click counts (1-10, only affects Hyperion mode)
     */
    private void cycleClickCount() {
        int currentCount = FlareConfig.getClickCount();
        int newCount = currentCount >= 10 ? 1 : currentCount + 1;
        FlareConfig.setClickCount(newCount);
        
        if (client.player != null) {
            String note = FlareConfig.getCombatMode() == 2 ? " (not used in Fire Veil Wand mode)" : "";
            client.player.sendMessage(
                    Text.literal("Click count: ").formatted(Formatting.GRAY)
                            .append(Text.literal(String.valueOf(newCount)).formatted(Formatting.GOLD))
                            .append(Text.literal(note).formatted(Formatting.YELLOW)), 
                    false
            );
        }
        refresh();
    }

    /**
     * Show information about the Flare Combat Macro
     */
    private void showInfo() {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(""), false);
            client.player.sendMessage(
                    Text.literal("=== Flare Combat Macro ===").formatted(Formatting.GOLD), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Status: ").formatted(Formatting.GRAY)
                            .append(Text.literal(FlareMacroFeature.isEnabled() ? "ENABLED" : "DISABLED")
                                    .formatted(FlareMacroFeature.isEnabled() ? Formatting.GREEN : Formatting.RED)), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Combat Mode: ").formatted(Formatting.GRAY)
                            .append(Text.literal(FlareConfig.getCombatModeName()).formatted(Formatting.YELLOW)), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Click Count: ").formatted(Formatting.GRAY)
                            .append(Text.literal(String.valueOf(FlareConfig.getClickCount())).formatted(Formatting.YELLOW))
                            .append(Text.literal(FlareConfig.getCombatMode() == 2 ? " (not used in Fire Veil Wand mode)" : "")
                                    .formatted(Formatting.DARK_GRAY)), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Commands: /flare start|stop|setmode|setclick").formatted(Formatting.AQUA), 
                    false
            );
            client.player.sendMessage(Text.literal(""), false);
        }
    }

    /**
     * Refresh the section to update status and buttons
     */
    public void refresh() {
        boolean isEnabled = FlareMacroFeature.isEnabled();

        // Update status label
        if (isEnabled) {
            this.statusLabel.text(Text.literal("Status: Enabled ●"));
            this.statusLabel.color(Color.ofRgb(0x4CAF50)); // Green
        } else {
            this.statusLabel.text(Text.literal("Status: Disabled ○"));
            this.statusLabel.color(Color.ofRgb(0xFF5252)); // Red
        }

        // Update toggle button
        if (isEnabled) {
            this.toggleButton.setMessage(Text.literal("Stop Flare Combat"));
        } else {
            this.toggleButton.setMessage(Text.literal("Start Flare Combat"));
        }

        // Update keybind label
        this.keybindLabel.text(Text.literal("Keybind: V (Toggle)"));
        this.keybindLabel.color(Color.ofRgb(0x888888));
        
        // Update combat mode label
        String modeName = FlareConfig.getCombatModeName();
        this.combatModeLabel.text(Text.literal("Mode: " + modeName));
        this.combatModeLabel.color(Color.ofRgb(0xFFAA00));
        
        // Update click count label
        int clickCount = FlareConfig.getClickCount();
        String clickNote = FlareConfig.getCombatMode() == 2 ? " (unused)" : "";
        this.clickCountLabel.text(Text.literal("Click Count: " + clickCount + clickNote));
        this.clickCountLabel.color(Color.ofRgb(0xFFAA00));
    }
}
