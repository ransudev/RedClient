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

    @ModifyVariable(
        method = "setColor(I)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private int modifySpikeColor(int color) {
        if (SpikeHelperConfig.isEnabled() && SpikeHelperConfig.isHighlightEnabled()) {
            Entity tracked = SpikeHelper.getTrackedSpike();
            if (tracked != null) {
                return SpikeHelper.getHighlightColor();
            }
        }
        return color;
    }
}
