/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.body.shape;

import com.github.stephengold.joltjni.ShapeRefC;
import com.github.stephengold.joltjni.ShapeResult;
import com.github.stephengold.joltjni.ShapeSettings;
import com.github.stephengold.joltjni.readonly.ConstShape;

/**
 * Abstract base class for all collision shape wrappers.
 * <p>
 * This class provides a clean abstraction over Jolt's shape creation pipeline,
 * encapsulating the {@link ShapeSettings} → {@link ShapeResult} → {@link ShapeRefC}
 * lifecycle. Subclasses only need to implement {@link #createSettings()} to define
 * their specific shape configuration.
 * <p>
 * All shapes created through this wrapper are guaranteed to produce valid {@link ShapeRefC}
 * references, with proper error handling during the creation process.
 *
 * @author xI-Mx-Ix
 */
public abstract class VxCollisionShape {

    private ShapeRefC cachedShapeRef;

    /**
     * Wraps raw Jolt-JNI {@link ShapeSettings} into a collision shape.
     *
     * @param rawSettings The raw Jolt-JNI settings.
     * @return A new VxCollisionShape wrapping the settings.
     */
    public static VxCollisionShape of(ShapeSettings rawSettings) {
        return new VxRawSettingsShape(rawSettings);
    }

    /**
     * Creates the Jolt-specific shape settings for this collision shape.
     * <p>
     * Implementations should construct and return the appropriate {@link ShapeSettings}
     * subclass with all parameters configured. The caller is responsible for closing
     * the returned settings object.
     *
     * @return A new {@link ShapeSettings} instance describing this shape.
     */
    protected abstract ShapeSettings createSettings();

    /**
     * Returns the cached native shape reference, initializing it if necessary.
     * Lock-free for maximum performance.
     *
     * @return The cached ShapeRefC.
     */
    public ShapeRefC getShapeRef() {
        if (cachedShapeRef == null) {
            try (ShapeSettings settings = createSettings();
                 ShapeResult result = settings.create()) {
                if (result.hasError()) {
                    throw new IllegalStateException("Shape creation failed: " + result.getError());
                }
                cachedShapeRef = result.get();
            }
        }
        return cachedShapeRef;
    }

    /**
     * Eagerly compiles and caches the native Jolt shape.
     */
    protected void compile() {
        getShapeRef();
    }

    /**
     * Creates a reference-counted shape from this collision shape's settings.
     * <p>
     * The returned {@link ShapeRefC} must be closed by the caller when no longer needed.
     *
     * @return A valid {@link ShapeRefC} for use in body creation.
     * @throws IllegalStateException If the shape creation fails (e.g. invalid parameters).
     */
    public ShapeRefC createShapeRef() {
        return getShapeRef().toRefC();
    }

    /**
     * Creates/retrieves the underlying {@link ConstShape} instance from this collision shape's settings.
     * <p>
     * The returned shape is valid as long as this VxCollisionShape instance is alive.
     *
     * @return The {@link ConstShape} instance.
     * @throws IllegalStateException If the shape creation fails.
     */
    public ConstShape createShape() {
        return getShapeRef().getPtr();
    }

    /**
     * Resets the cached shape reference, closing the native reference if it exists.
     */
    protected void resetCachedShape() {
        if (cachedShapeRef != null) {
            cachedShapeRef.close();
            cachedShapeRef = null;
        }
    }
}