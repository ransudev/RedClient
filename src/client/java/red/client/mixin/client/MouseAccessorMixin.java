package red.client.mixin.client;

import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import red.client.flarecombat.mixin.MouseMixin;

@Mixin(Mouse.class)
public abstract class MouseAccessorMixin implements MouseMixin {
    @Invoker("onMouseButton")
    protected abstract void redclient$invokeOnMouseButton(long window, MouseInput input, int action);

    @Override
    public void invokeOnMouseButton(long window, int button, int action, int mods) {
        redclient$invokeOnMouseButton(window, new MouseInput(button, mods), action);
    }
}
