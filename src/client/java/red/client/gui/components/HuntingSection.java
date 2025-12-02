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
import red.client.fishing.config.BezalFarmerConfig;
import red.client.fishing.feature.BezalFarmer;
import red.client.fishing.feature.XYZMacro;

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
    
    // BezalFarmer UI Components
    private LabelComponent bezalStatusLabel;
    private ButtonComponent bezalToggleButton;
    private LabelComponent bezalAutoAimLabel;
    private ButtonComponent bezalAutoAimButton;
    private LabelComponent bezalBlackholeLabel;
    private ButtonComponent bezalBlackholeButton;
    
    // XYZ Macro UI Components
    private LabelComponent xyzStatusLabel;
    private ButtonComponent xyzToggleButton;

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

        // ===== BEZAL FARMER SECTION =====
        // Spacer
        FlowLayout spacer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fixed(16));
        this.child(spacer);
        
        // BezalFarmer header
        FlowLayout bezalHeaderRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .verticalAlignment(VerticalAlignment.CENTER);

        bezalHeaderRow.child(
                Components.label(Text.literal("🌊"))
                        .color(Color.ofRgb(0x00FFFF))
                        .shadow(true)
        );

        bezalHeaderRow.child(
                Components.label(Text.literal("Bezal Farmer"))
                        .color(Color.ofRgb(0x00FFFF))
                        .shadow(true)
        );

        this.child(bezalHeaderRow);

        // BezalFarmer Status section
        FlowLayout bezalStatusSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x22000000))
                .padding(Insets.of(8));
        bezalStatusSection.gap(4);

        this.bezalStatusLabel = (LabelComponent) Components.label(Text.literal("Status: Disabled"))
                .color(Color.ofRgb(0xFF5252))
                .shadow(false);
        bezalStatusSection.child(this.bezalStatusLabel);

        this.child(bezalStatusSection);

        // BezalFarmer Control buttons
        FlowLayout bezalButtonRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        this.bezalToggleButton = (ButtonComponent) Components.button(
                Text.literal("Enable Bezal Farmer"), 
                button -> toggleBezalFarmer()
        ).horizontalSizing(Sizing.fixed(160));
        
        bezalButtonRow.child(this.bezalToggleButton);

        bezalButtonRow.child(
                Components.button(Text.literal("Info"), button -> showBezalInfo())
                        .horizontalSizing(Sizing.fixed(70))
        );

        this.child(bezalButtonRow);
        
        // BezalFarmer Auto-Aim section
        FlowLayout bezalAutoAimRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        this.bezalAutoAimLabel = (LabelComponent) Components.label(Text.literal("Auto-Aim: ON"))
                .color(Color.ofRgb(0x4CAF50))
                .shadow(false);
        bezalAutoAimRow.child(this.bezalAutoAimLabel);

        this.bezalAutoAimButton = (ButtonComponent) Components.button(
                Text.literal("Disable Auto-Aim"), 
                button -> toggleBezalAutoAim()
        ).horizontalSizing(Sizing.fixed(130));
        
        bezalAutoAimRow.child(this.bezalAutoAimButton);

        this.child(bezalAutoAimRow);

        // ===== BLACKHOLE SETTINGS SECTION =====
        FlowLayout bezalBlackholeRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(8)
                .verticalAlignment(VerticalAlignment.CENTER);

        this.bezalBlackholeLabel = (LabelComponent) Components.label(Text.literal("Blackhole: OFF"))
                .color(Color.ofRgb(0xFF5252))
                .shadow(false);
        bezalBlackholeRow.child(this.bezalBlackholeLabel);

        this.bezalBlackholeButton = (ButtonComponent) Components.button(
                Text.literal("Enable Blackhole"), 
                button -> toggleBezalBlackhole()
        ).horizontalSizing(Sizing.fixed(130));
        
        bezalBlackholeRow.child(this.bezalBlackholeButton);

        this.child(bezalBlackholeRow);
        
        // ===== XYZ MACRO SECTION =====
        FlowLayout xyzHeader = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of(0, 10, 0, 0)); // Top margin

        xyzHeader.child(
                Components.label(Text.literal("🎣"))
                        .color(Color.ofRgb(0xAA00FF))
                        .shadow(true)
        );

        xyzHeader.child(
                Components.label(Text.literal("XYZ Lasso Macro"))
                        .color(Color.ofRgb(0xAA00FF))
                        .shadow(true)
        );

        this.child(xyzHeader);
        
        // XYZ Status section
        FlowLayout xyzStatusSection = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .surface(Surface.flat(0x22000000))
                .padding(Insets.of(8));
        xyzStatusSection.gap(4);

        this.xyzStatusLabel = (LabelComponent) Components.label(Text.literal("Status: Disabled"))
                .color(Color.ofRgb(0xFF5252))
                .shadow(false);
        xyzStatusSection.child(this.xyzStatusLabel);

        this.child(xyzStatusSection);
        
        // XYZ Controls
        FlowLayout xyzToggleRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap(6)
                .padding(Insets.of(4));

        this.xyzToggleButton = (ButtonComponent) Components.button(
                Text.literal("Start XYZ Macro"), 
                button -> toggleXYZMacro()
        ).horizontalSizing(Sizing.fixed(200));
        
        xyzToggleRow.child(this.xyzToggleButton);
        this.child(xyzToggleRow);
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
     * Toggle BezalFarmer on/off
     */
    private void toggleBezalFarmer() {
        BezalFarmer.toggle();
        refresh();
    }

    /**
     * Toggle BezalFarmer Auto-Aim
     */
    private void toggleBezalAutoAim() {
        boolean currentState = BezalFarmerConfig.isAutoAimEnabled();
        BezalFarmerConfig.setAutoAimEnabled(!currentState);
        refresh();
    }

    /**
     * Toggle Blackhole usage on low HP
     */
    private void toggleBezalBlackhole() {
        boolean currentState = BezalFarmerConfig.isBlackholeEnabled();
        BezalFarmerConfig.setBlackholeEnabled(!currentState);
        refresh();
    }
    
    /**
     * Toggle XYZ Macro on/off
     */
    private void toggleXYZMacro() {
        XYZMacro.toggle();
        refresh();
    }

    /**
     * Show information about BezalFarmer
     */
    private void showBezalInfo() {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(""), false);
            client.player.sendMessage(
                    Text.literal("=== Bezal Farmer ===").formatted(Formatting.GOLD), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Status: ").formatted(Formatting.GRAY)
                            .append(Text.literal(BezalFarmer.isEnabled() ? "ENABLED" : "DISABLED")
                                    .formatted(BezalFarmer.isEnabled() ? Formatting.GREEN : Formatting.RED)), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Attack Distance: ").formatted(Formatting.GRAY)
                            .append(Text.literal(String.format("%.1f", BezalFarmerConfig.getAttackDistance())).formatted(Formatting.YELLOW))
                            .append(Text.literal(" blocks").formatted(Formatting.GRAY)), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Clicks per Attack: ").formatted(Formatting.GRAY)
                            .append(Text.literal(String.valueOf(BezalFarmerConfig.getClickCount())).formatted(Formatting.YELLOW)), 
                    false
            );
            client.player.sendMessage(
                    Text.literal("Auto-Aim: ").formatted(Formatting.GRAY)
                            .append(Text.literal(BezalFarmerConfig.isAutoAimEnabled() ? "ON" : "OFF")
                                    .formatted(BezalFarmerConfig.isAutoAimEnabled() ? Formatting.GREEN : Formatting.RED)), 
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

        // ===== REFRESH BEZAL FARMER =====
        if (this.bezalStatusLabel != null && this.bezalToggleButton != null && this.bezalAutoAimLabel != null) {
            refreshBezalFarmer();
        }
        
        // ===== REFRESH XYZ MACRO =====
        if (this.xyzStatusLabel != null && this.xyzToggleButton != null) {
            refreshXYZMacro();
        }
    }

    /**
     * Refresh BezalFarmer section status
     */
    private void refreshBezalFarmer() {
        boolean isEnabled = BezalFarmer.isEnabled();
        String statusText = BezalFarmer.getStatusText();

        // Update status label
        if (isEnabled) {
            // Color based on tracking status
            if (BezalFarmer.getTrackedBezal() != null) {
                if (BezalFarmer.isInAttackRange()) {
                    this.bezalStatusLabel.text(Text.literal(statusText + " ●"));
                    this.bezalStatusLabel.color(Color.ofRgb(0x4CAF50)); // Green - attacking
                } else {
                    this.bezalStatusLabel.text(Text.literal(statusText + " ●"));
                    this.bezalStatusLabel.color(Color.ofRgb(0xFFEB3B)); // Yellow - tracking but too far
                }
            } else {
                this.bezalStatusLabel.text(Text.literal(statusText + " ●"));
                this.bezalStatusLabel.color(Color.ofRgb(0xFF9800)); // Orange - searching
            }
        } else {
            this.bezalStatusLabel.text(Text.literal(statusText + " ○"));
            this.bezalStatusLabel.color(Color.ofRgb(0xFF5252)); // Red - disabled
        }

        // Update button
        this.bezalToggleButton.setMessage(Text.literal(isEnabled ? "Disable Bezal Farmer" : "Enable Bezal Farmer"));

        // Update Auto-Aim status
        boolean autoAimEnabled = BezalFarmerConfig.isAutoAimEnabled();
        if (autoAimEnabled) {
            this.bezalAutoAimLabel.text(Text.literal("Auto-Aim: ON"));
            this.bezalAutoAimLabel.color(Color.ofRgb(0x4CAF50)); // Green
        } else {
            this.bezalAutoAimLabel.text(Text.literal("Auto-Aim: OFF"));
            this.bezalAutoAimLabel.color(Color.ofRgb(0xFF5252)); // Red
        }
        this.bezalAutoAimButton.setMessage(Text.literal(autoAimEnabled ? "Disable Auto-Aim" : "Enable Auto-Aim"));

        // Update Blackhole status
        boolean blackholeEnabled = BezalFarmerConfig.isBlackholeEnabled();
        if (this.bezalBlackholeLabel != null && this.bezalBlackholeButton != null) {
            if (blackholeEnabled) {
                this.bezalBlackholeLabel.text(Text.literal("Blackhole: ON"));
                this.bezalBlackholeLabel.color(Color.ofRgb(0x4CAF50)); // Green
            } else {
                this.bezalBlackholeLabel.text(Text.literal("Blackhole: OFF"));
                this.bezalBlackholeLabel.color(Color.ofRgb(0xFF5252)); // Red
            }
            this.bezalBlackholeButton.setMessage(Text.literal(blackholeEnabled ? "Disable Blackhole" : "Enable Blackhole"));
        }
    }
    
    /**
     * Refresh XYZ Macro section status
     */
    private void refreshXYZMacro() {
        boolean isEnabled = XYZMacro.isEnabled();
        String statusText = XYZMacro.getStatusText();

        // Update status label
        if (isEnabled) {
            this.xyzStatusLabel.text(Text.literal(statusText + " ●"));
            // Color based on state
            if (statusText.contains("Throwing") || statusText.contains("Reeling")) {
                this.xyzStatusLabel.color(Color.ofRgb(0x4CAF50)); // Green - active
            } else if (statusText.contains("Rotating")) {
                this.xyzStatusLabel.color(Color.ofRgb(0xFFEB3B)); // Yellow - preparing
            } else {
                this.xyzStatusLabel.color(Color.ofRgb(0xFF9800)); // Orange - searching
            }
        } else {
            this.xyzStatusLabel.text(Text.literal(statusText + " ○"));
            this.xyzStatusLabel.color(Color.ofRgb(0xFF5252)); // Red - disabled
        }

        // Update button
        this.xyzToggleButton.setMessage(Text.literal(isEnabled ? "Stop XYZ Macro" : "Start XYZ Macro"));
    }
}
