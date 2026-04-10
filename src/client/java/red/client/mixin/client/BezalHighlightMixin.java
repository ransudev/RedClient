package red.client.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.client.fishing.config.BezalFarmerConfig;
import red.client.fishing.feature.BezalFarmer;

/**
 * Mixin to enable entity outline for tracked Bezal
 * Integrates with Minecraft's entity outline rendering system
 */
@Mixin(MinecraftClient.class)
public class BezalHighlightMixin {
    
    /**
     * Inject into hasOutline to enable outline for tracked Bezal
     */
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void enableBezalOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        // Check if this is the tracked Bezal and highlighting is enabled
        if (BezalFarmerConfig.isEnabled() && 
            BezalFarmerConfig.isHighlightEnabled() && 
            entity != null &&
            entity.equals(BezalFarmer.getTrackedBezal())) {
            cir.setReturnValue(true);
        }
    }
}
