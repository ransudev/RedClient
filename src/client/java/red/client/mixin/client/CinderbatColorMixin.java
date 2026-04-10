package red.client.mixin.client;

import net.minecraft.client.render.OutlineVertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import red.client.fishing.config.CinderbatHighlightConfig;
import red.client.fishing.feature.CinderbatHighlight;

@Mixin(OutlineVertexConsumerProvider.class)
public class CinderbatColorMixin {

    @ModifyVariable(
            method = "setColor(I)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private int modifyCinderbatColor(int color) {
        if (CinderbatHighlightConfig.isEnabled()
                && CinderbatHighlightConfig.isHighlightEnabled()
                && CinderbatHighlight.hasTrackedCinderbats()) {
            return CinderbatHighlight.getHighlightColor();
        }
        return color;
    }
}
