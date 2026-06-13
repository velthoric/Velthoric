/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.body.shape;

import com.github.stephengold.joltjni.ShapeSettings;

/**
 * A collision shape that wraps raw Jolt-JNI {@link ShapeSettings}.
 * <p>
 * WARNING: Passing raw settings directly can lead to undefined behavior or crashes
 * if the settings are invalid or incompatible with the body type. Use at your own risk.
 *
 * @author xI-Mx-Ix
 */
public class VxRawSettingsShape extends VxCollisionShape {

    private final ShapeSettings rawSettings;

    /**
     * Creates a collision shape wrapping raw Jolt-JNI settings.
     *
     * @param rawSettings The raw Jolt-JNI ShapeSettings to wrap.
     */
    public VxRawSettingsShape(ShapeSettings rawSettings) {
        this.rawSettings = rawSettings;
        compile();
    }

    @Override
    protected ShapeSettings createSettings() {
        return ShapeSettings.cloneShapeSettings(rawSettings);
    }

    /**
     * @return The wrapped raw settings.
     */
    public ShapeSettings getRawSettings() {
        return rawSettings;
    }
}