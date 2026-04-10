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

    @ModifyVariable(
        method = "setColor(I)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private int modifyBezalColor(int color) {
        if (BezalFarmerConfig.isEnabled() && BezalFarmerConfig.isHighlightEnabled()) {
            Entity tracked = BezalFarmer.getTrackedBezal();
            if (tracked != null) {
                return BezalFarmer.getHighlightColor();
            }
        }
        return color;
    }
}
