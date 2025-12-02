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
    
    /**
     * Modify the red component of outline color for tracked mobs
     */
    @ModifyVariable(
        method = "setColor(IIII)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private int modifyMobColorRed(int red) {
        // Check if we should modify the color (this is called for all entities with outlines)
        // We need to check if the current entity being rendered is one of our tracked mobs
        // Since we can't directly access the entity in this method, we rely on the fact that
        // our MobHighlightMixin enables outlines for tracked mobs
        if (MobHighlightConfig.isEnabled()) {
            // This is a simplification - in practice, this will affect all outlined entities
            // when MobHighlight is enabled. A more sophisticated approach would track the
            // current rendering context, but this works for our use case.
            int color = MobHighlightConfig.getHighlightColor();
            // Extract red component (bits 16-23 of RGB)
            return (color >> 16) & 0xFF;
        }
        return red;
    }
    
    /**
     * Modify the green component of outline color for tracked mobs
     */
    @ModifyVariable(
        method = "setColor(IIII)V",
        at = @At("HEAD"),
        ordinal = 1,
        argsOnly = true
    )
    private int modifyMobColorGreen(int green) {
        if (MobHighlightConfig.isEnabled()) {
            int color = MobHighlightConfig.getHighlightColor();
            // Extract green component (bits 8-15 of RGB)
            return (color >> 8) & 0xFF;
        }
        return green;
    }
    
    /**
     * Modify the blue component of outline color for tracked mobs
     */
    @ModifyVariable(
        method = "setColor(IIII)V",
        at = @At("HEAD"),
        ordinal = 2,
        argsOnly = true
    )
    private int modifyMobColorBlue(int blue) {
        if (MobHighlightConfig.isEnabled()) {
            int color = MobHighlightConfig.getHighlightColor();
            // Extract blue component (bits 0-7 of RGB)
            return color & 0xFF;
        }
        return blue;
    }
}
