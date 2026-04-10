package red.client.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.client.fishing.config.MobHighlightConfig;
import red.client.fishing.feature.MobHighlight;

/**
 * Mixin to enable entity outline for tracked mobs in MobHighlight
 * Integrates with Minecraft's entity outline rendering system
 */
@Mixin(MinecraftClient.class)
public class MobHighlightMixin {
    
    /**
     * Inject into hasOutline to enable outline for tracked mobs
     */
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void enableMobHighlightOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        // Check if this is a tracked mob and highlighting is enabled
        if (MobHighlightConfig.isEnabled() && 
            entity != null &&
            MobHighlight.isTrackedMob(entity)) {
            cir.setReturnValue(true);
        }
    }
}
