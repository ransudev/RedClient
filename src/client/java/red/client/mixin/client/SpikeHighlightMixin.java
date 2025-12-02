package red.client.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.client.fishing.config.SpikeHelperConfig;
import red.client.fishing.feature.SpikeHelper;

/**
 * Mixin to enable entity outline for tracked Spike
 * Integrates with Minecraft's entity outline rendering system
 */
@Mixin(MinecraftClient.class)
public class SpikeHighlightMixin {
    
    /**
     * Inject into hasOutline to enable outline for tracked Spike
     */
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void enableSpikeOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        // Check if this is the tracked spike and highlighting is enabled
        if (SpikeHelperConfig.isEnabled() && 
            SpikeHelperConfig.isHighlightEnabled() && 
            entity != null &&
            entity.equals(SpikeHelper.getTrackedSpike())) {
            cir.setReturnValue(true);
        }
    }
}
