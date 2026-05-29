/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.behavior;

import net.xmx.velthoric.core.behavior.impl.VxKillBehavior;
import net.xmx.velthoric.core.behavior.impl.VxPhysicsSyncBehavior;
import net.xmx.velthoric.core.behavior.impl.VxRigidPhysicsBehavior;
import net.xmx.velthoric.core.behavior.impl.VxSoftPhysicsBehavior;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * An immutable set of configured behavior instances that defines which behaviors a body type has.
 * <p>
 * This class provides a clean API for assembling default behavior configurations.
 * Configurations are stored directly within the behavior instances.
 *
 * @author xI-Mx-Ix
 */
public final class VxBehaviorSet {

    /**
     * The internal map storing the configured behavior instances, keyed by their respective identifiers.
     */
    private final Map<VxBehaviorId, VxBehavior> behaviors;

    /**
     * Constructs a new immutable behavior set from the provided map of behaviors.
     *
     * @param behaviors The map of behavior instances to be stored in this set.
     */
    private VxBehaviorSet(Map<VxBehaviorId, VxBehavior> behaviors) {
        this.behaviors = Collections.unmodifiableMap(new LinkedHashMap<>(behaviors));
    }

    /**
     * Creates and returns a new behavior set containing the default behaviors for a rigid body.
     *
     * @return A preconfigured behavior set for rigid bodies.
     */
    public static VxBehaviorSet rigid() {
        return new Builder()
                .add(new VxRigidPhysicsBehavior())
                .add(new VxPhysicsSyncBehavior())
                .add(new VxKillBehavior())
                .build();
    }

    /**
     * Creates and returns a new behavior set containing the default behaviors for a soft body.
     *
     * @return A preconfigured behavior set for soft bodies.
     */
    public static VxBehaviorSet soft() {
        return new Builder()
                .add(new VxSoftPhysicsBehavior())
                .add(new VxPhysicsSyncBehavior())
                .add(new VxKillBehavior())
                .build();
    }

    /**
     * Creates and returns an empty behavior set.
     *
     * @return A new empty behavior set.
     */
    public static VxBehaviorSet empty() {
        return new Builder().build();
    }

    /**
     * Initializes a new builder for assembling a custom behavior set.
     *
     * @return A new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder populated with the behaviors currently stored in this set.
     *
     * @return A new builder instance initialized with the current behaviors.
     */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.behaviors.putAll(this.behaviors);
        return b;
    }

    /**
     * Retrieves the set of all behavior identifiers present in this behavior set.
     *
     * @return A set of behavior identifiers.
     */
    public Set<VxBehaviorId> getIds() {
        return behaviors.keySet();
    }

    /**
     * Retrieves the collection of all configured behavior instances present in this behavior set.
     *
     * @return A collection of behavior instances.
     */
    public Collection<VxBehavior> getBehaviors() {
        return behaviors.values();
    }

    /**
     * Retrieves a specific configured behavior instance by its identifier.
     *
     * @param id The identifier of the behavior to retrieve.
     * @param <T> The expected class type of the behavior.
     * @return The behavior instance if present, or null otherwise.
     */
    @SuppressWarnings("unchecked")
    public <T extends VxBehavior> T get(VxBehaviorId id) {
        return (T) behaviors.get(id);
    }

    /**
     * Checks whether this behavior set contains a behavior with the specified identifier.
     *
     * @param id The identifier to check.
     * @return True if the behavior is present, false otherwise.
     */
    public boolean contains(VxBehaviorId id) {
        return behaviors.containsKey(id);
    }

    /**
     * A fluent builder for incrementally assembling an immutable behavior set.
     */
    public static final class Builder {

        /**
         * The internal map tracking behaviors being added or removed during the building process.
         */
        private final LinkedHashMap<VxBehaviorId, VxBehavior> behaviors = new LinkedHashMap<>();

        /**
         * Private constructor to initialize a new empty builder.
         */
        private Builder() {
        }

        /**
         * Adds a configured behavior instance to the builder. If a behavior with the same identifier already exists, it is replaced.
         *
         * @param behavior The behavior instance to add.
         * @return This builder instance for fluent chaining.
         */
        public Builder add(VxBehavior behavior) {
            behaviors.put(behavior.getId(), behavior);
            return this;
        }

        /**
         * Removes a behavior from the builder by its identifier.
         *
         * @param id The identifier of the behavior to remove.
         * @return This builder instance for fluent chaining.
         */
        public Builder remove(VxBehaviorId id) {
            behaviors.remove(id);
            return this;
        }

        /**
         * Constructs and returns the final immutable behavior set based on the current state of the builder.
         *
         * @return A newly constructed immutable behavior set.
         */
        public VxBehaviorSet build() {
            return new VxBehaviorSet(behaviors);
        }
    }
}