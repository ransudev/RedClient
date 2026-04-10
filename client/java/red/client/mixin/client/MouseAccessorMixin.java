package red.client.mixin.client;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import red.client.flarecombat.mixin.MouseMixin;

@Mixin(Mouse.class)
public abstract class MouseAccessorMixin implements MouseMixin {
    
    @Invoker("onMouseButton")
    public abstract void invokeOnMouseButton(long window, int button, int action, int mods);
}
