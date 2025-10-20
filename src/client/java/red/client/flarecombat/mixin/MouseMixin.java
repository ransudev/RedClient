package red.client.flarecombat.mixin;

/**
 * Mixin interface to access Mouse's onMouseButton method
 */
public interface MouseMixin {
    void invokeOnMouseButton(long window, int button, int action, int mods);
}
