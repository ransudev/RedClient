package red.client.mixin.client;

import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import red.client.fishing.config.SpikeHelperConfig;
import red.client.fishing.feature.SpikeHelper;

/**
 * Mixin to modify the outline color for tracked Spike
 * Changes color based on distance: Red (too close/far), Green (correct distance)
 */
@Mixin(OutlineVertexConsumerProvider.class)
public class SpikeColorMixin {
    
    /**
     * Modify the outline color for tracked Spike based on distance
     */
    @ModifyVariable(
        method = "setColor(IIII)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private int modifySpikeColor(int red) {
        // This will be called with the default outline color components
        // We'll return the red component of our custom color
        if (SpikeHelperConfig.isEnabled() && SpikeHelperConfig.isHighlightEnabled()) {
            Entity tracked = SpikeHelper.getTrackedSpike();
            if (tracked != null) {
                int color = SpikeHelper.getHighlightColor();
                // Extract red component (bits 16-23 of ARGB)
                return (color >> 16) & 0xFF;
            }
        }
        return red;
    }
    
    @ModifyVariable(
        method = "setColor(IIII)V",
        at = @At("HEAD"),
        ordinal = 1,
        argsOnly = true
    )
    private int modifySpikeColorGreen(int green) {
        if (SpikeHelperConfig.isEnabled() && SpikeHelperConfig.isHighlightEnabled()) {
            Entity tracked = SpikeHelper.getTrackedSpike();
            if (tracked != null) {
                int color = SpikeHelper.getHighlightColor();
                // Extract green component (bits 8-15 of ARGB)
                return (color >> 8) & 0xFF;
            }
        }
        return green;
    }
    
    @ModifyVariable(
        method = "setColor(IIII)V",
        at = @At("HEAD"),
        ordinal = 2,
        argsOnly = true
    )
    private int modifySpikeColorBlue(int blue) {
        if (SpikeHelperConfig.isEnabled() && SpikeHelperConfig.isHighlightEnabled()) {
            Entity tracked = SpikeHelper.getTrackedSpike();
            if (tracked != null) {
                int color = SpikeHelper.getHighlightColor();
                // Extract blue component (bits 0-7 of ARGB)
                return color & 0xFF;
            }
        }
        return blue;
    }
}
