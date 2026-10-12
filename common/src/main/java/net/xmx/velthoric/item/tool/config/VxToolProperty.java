/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.item.tool.config;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a single configuration property with a specific type and optional bounds.
 *
 * @param <T> The type of the value (e.g., Integer, Boolean, String).
 * @author xI-Mx-Ix
 */
public class VxToolProperty<T> {
    private final String name;
    private T value;
    private final T defaultValue;
    private final Number min;
    private final Number max;
    private final Class<T> type;

    /**
     * Creates a new property.
     *
     * @param name  The display name or key of the property.
     * @param value The initial/default value.
     * @param min   The minimum allowed value (nullable for non-numeric types).
     * @param max   The maximum allowed value (nullable for non-numeric types).
     * @param type  The class type of the value.
     */
    public VxToolProperty(String name, T value, @Nullable Number min, @Nullable Number max, Class<T> type) {
        this.name = name;
        this.value = clamp(value, min, max, type);
        this.defaultValue = value;
        this.min = min;
        this.max = max;
        this.type = type;
    }

    /**
     * Gets the name of the property.
     *
     * @return The property name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the current value.
     *
     * @return The value.
     */
    public T getValue() {
        return value;
    }

    /**
     * Gets the original default value.
     *
     * @return The default value.
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the value of the property.
     * <p>
     * Unchecked cast is suppressed as the VxToolConfig manager ensures
     * type safety via correct setter usage.
     *
     * @param value The new value to set.
     */
    @SuppressWarnings("unchecked")
    public void setValue(Object value) {
        this.value = clamp((T) value, this.min, this.max, this.type);
    }

    /**
     * Clamps numeric values to their defined bounds.
     *
     * @param val  The value to clamp.
     * @param min  The minimum allowed value.
     * @param max  The maximum allowed value.
     * @param type The type class of the value.
     * @param <T>  The property type.
     * @return The clamped value, or the original value if not numeric or bounded.
     */
    @SuppressWarnings("unchecked")
    private static <T> T clamp(T val, @Nullable Number min, @Nullable Number max, Class<T> type) {
        if (val == null) return null;

        if (type == Integer.class && min != null && max != null) {
            int v = ((Number) val).intValue();
            return (T) Integer.valueOf(Math.max(min.intValue(), Math.min(max.intValue(), v)));
        } else if (type == Long.class && min != null && max != null) {
            long v = ((Number) val).longValue();
            return (T) Long.valueOf(Math.max(min.longValue(), Math.min(max.longValue(), v)));
        } else if (type == Float.class && min != null && max != null) {
            float v = ((Number) val).floatValue();
            if (Float.isNaN(v)) return (T) Float.valueOf(min.floatValue());
            return (T) Float.valueOf(Math.max(min.floatValue(), Math.min(max.floatValue(), v)));
        } else if (type == Double.class && min != null && max != null) {
            double v = ((Number) val).doubleValue();
            if (Double.isNaN(v)) return (T) Double.valueOf(min.doubleValue());
            return (T) Double.valueOf(Math.max(min.doubleValue(), Math.min(max.doubleValue(), v)));
        }

        return val;
    }

    /**
     * Gets the minimum allowed value, if applicable.
     *
     * @return The minimum value as a Number, or null.
     */
    @Nullable
    public Number getMin() {
        return min;
    }

    /**
     * Gets the maximum allowed value, if applicable.
     *
     * @return The maximum value as a Number, or null.
     */
    @Nullable
    public Number getMax() {
        return max;
    }

    /**
     * Gets the class type of the property value.
     *
     * @return The class type.
     */
    public Class<T> getType() {
        return type;
    }
}