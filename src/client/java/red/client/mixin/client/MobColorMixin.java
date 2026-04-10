package red.client.mixin.client;

import net.minecraft.client.render.OutlineVertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import red.client.fishing.config.MobHighlightConfig;

/**
 * Mixin to modify the outline color for tracked mobs in MobHighlight
 * Uses the configured highlight color for all tracked mobs
 */
@Mixin(OutlineVertexConsumerProvider.class)
public class MobColorMixin {

    @ModifyVariable(
        method = "setColor(I)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private int modifyMobColor(int color) {
        if (MobHighlightConfig.isEnabled()) {
            return 0xFF000000 | (MobHighlightConfig.getHighlightColor() & 0x00FFFFFF);
        }
        return color;
    }
}
