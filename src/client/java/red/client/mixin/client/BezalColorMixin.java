package red.client.mixin.client;

import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import red.client.fishing.config.BezalFarmerConfig;
import red.client.fishing.feature.BezalFarmer;

/**
 * Mixin to modify the outline color for tracked Bezal
 * Changes color based on distance: Yellow (too far), Green (in attack range)
 */
@Mixin(OutlineVertexConsumerProvider.class)
public class BezalColorMixin {
    
    /**
     * Modify the outline color for tracked Bezal based on distance
     */
    @ModifyVariable(
        method = "setColor(IIII)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private int modifyBezalColor(int red) {
        if (BezalFarmerConfig.isEnabled() && BezalFarmerConfig.isHighlightEnabled()) {
            Entity tracked = BezalFarmer.getTrackedBezal();
            if (tracked != null) {
                int color = BezalFarmer.getHighlightColor();
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
    private int modifyBezalColorGreen(int green) {
        if (BezalFarmerConfig.isEnabled() && BezalFarmerConfig.isHighlightEnabled()) {
            Entity tracked = BezalFarmer.getTrackedBezal();
            if (tracked != null) {
                int color = BezalFarmer.getHighlightColor();
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
    private int modifyBezalColorBlue(int blue) {
        if (BezalFarmerConfig.isEnabled() && BezalFarmerConfig.isHighlightEnabled()) {
            Entity tracked = BezalFarmer.getTrackedBezal();
            if (tracked != null) {
                int color = BezalFarmer.getHighlightColor();
                // Extract blue component (bits 0-7 of ARGB)
                return color & 0xFF;
            }
        }
        return blue;
    }
}
