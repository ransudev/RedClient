package red.client.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.client.fishing.config.CinderbatHighlightConfig;
import red.client.fishing.feature.CinderbatHighlight;

@Mixin(MinecraftClient.class)
public class CinderbatHighlightMixin {

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void enableCinderbatOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (CinderbatHighlightConfig.isEnabled()
                && CinderbatHighlightConfig.isHighlightEnabled()
                && entity != null
                && CinderbatHighlight.isTracked(entity)) {
            cir.setReturnValue(true);
        }
    }
}
