/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.body;

import net.minecraft.resources.ResourceLocation;
import net.xmx.velthoric.core.behavior.VxBehavior;
import net.xmx.velthoric.core.behavior.VxBehaviorId;
import net.xmx.velthoric.core.behavior.VxBehaviorSet;
import net.xmx.velthoric.core.body.provider.VxJoltRigidProvider;
import net.xmx.velthoric.core.body.provider.VxJoltSoftProvider;
import net.xmx.velthoric.core.physics.world.VxPhysicsWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents an immutable definition of a physics body type.
 * <p>
 * A body type describes how a body is constructed, which behaviors it possesses by default
 * (via a {@link VxBehaviorSet}), and how its physical shape is provided to the Jolt engine.
 * <p>
 * Body types are assembled via the fluent {@link Builder}.
 *
 * @param <T> The specific class implementation of the body (e.g. BoxRigidBody).
 * @author xI-Mx-Ix
 */
public final class VxBodyType<T extends VxBody> {

    /**
     * The unique identifier representing this specific body type in registries.
     */
    private final ResourceLocation typeId;

    /**
     * The functional interface used to construct new instances of this body type.
     */
    private final Factory<T> factory;

    /**
     * The immutable collection of behavior configurations attached to this body type.
     */
    private final VxBehaviorSet behaviors;

    /**
     * The shape provider utilized when constructing rigid bodies, or null if this is a soft body.
     */
    @Nullable
    private final VxJoltRigidProvider rigidProvider;

    /**
     * The shape provider utilized when constructing soft bodies, or null if this is a rigid body.
     */
    @Nullable
    private final VxJoltSoftProvider softProvider;

    /**
     * Constructs a new immutable definition of a physics body type.
     *
     * @param typeId        The unique identifier representing this specific body type in registries.
     * @param factory       The functional interface used to construct new instances of this body type.
     * @param behaviors     The immutable collection of behavior configurations attached to this body type.
     * @param rigidProvider The shape provider utilized when constructing rigid bodies, or null if this is a soft body.
     * @param softProvider  The shape provider utilized when constructing soft bodies, or null if this is a rigid body.
     */
    private VxBodyType(ResourceLocation typeId, Factory<T> factory,
                       VxBehaviorSet behaviors, @Nullable VxJoltRigidProvider rigidProvider,
                       @Nullable VxJoltSoftProvider softProvider) {
        this.typeId = typeId;
        this.factory = factory;
        this.behaviors = behaviors;
        this.rigidProvider = rigidProvider;
        this.softProvider = softProvider;
    }

    /**
     * Instantiates a new physical body using this type definition.
     *
     * @param world The physics world.
     * @param id    The unique identifier.
     * @return A new body instance.
     */
    public T create(VxPhysicsWorld world, UUID id) {
        return this.factory.create(this, world, id);
    }

    /**
     * Retrieves the unique identifier of this body type.
     *
     * @return The unique identifier representing this specific body type in registries.
     */
    public ResourceLocation getTypeId() {
        return typeId;
    }

    /**
     * Determines whether this body type includes a specific behavior by default.
     *
     * @param id The behavior ID to check.
     * @return True if this type includes the behavior, false otherwise.
     */
    public boolean hasBehavior(VxBehaviorId id) {
        return behaviors.contains(id);
    }

    /**
     * Retrieves the configured instance of a behavior associated with this body type.
     *
     * @param id The behavior ID to retrieve.
     * @param <B> The specific behavior class type.
     * @return The behavior instance, or null if not present.
     */
    public <B extends VxBehavior> B getBehavior(VxBehaviorId id) {
        return behaviors.get(id);
    }

    /**
     * Retrieves the entire set of behaviors configured for this body type.
     *
     * @return The immutable collection of behavior configurations attached to this body type.
     */
    public VxBehaviorSet getBehaviors() {
        return behaviors;
    }

    /**
     * Retrieves the rigid shape provider if one is configured.
     *
     * @return The shape provider utilized when constructing rigid bodies, or null if this is a soft body.
     */
    @Nullable
    public VxJoltRigidProvider getRigidProvider() {
        return rigidProvider;
    }

    /**
     * Retrieves the soft shape provider if one is configured.
     *
     * @return The shape provider utilized when constructing soft bodies, or null if this is a rigid body.
     */
    @Nullable
    public VxJoltSoftProvider getSoftProvider() {
        return softProvider;
    }

    /**
     * Determines whether this body type represents a rigid body.
     *
     * @return True if this body type represents a rigid body, false otherwise.
     */
    public boolean isRigid() {
        return rigidProvider != null;
    }

    /**
     * Determines whether this body type represents a soft body.
     *
     * @return True if this body type represents a soft body, false otherwise.
     */
    public boolean isSoft() {
        return softProvider != null;
    }

    /**
     * Functional interface for instantiating body handles.
     *
     * @param <T> The specific class implementation of the body (e.g. BoxRigidBody).
     */
    @FunctionalInterface
    public interface Factory<T extends VxBody> {
        /**
         * Constructs the body handle.
         *
         * @param type  The immutable definition of a physics body type.
         * @param world The physics world.
         * @param id    The unique identifier.
         * @return A new body instance.
         */
        T create(VxBodyType<T> type, VxPhysicsWorld world, UUID id);
    }

    /**
     * Fluent builder for creating type definitions.
     *
     * @param <T> The specific class implementation of the body (e.g. BoxRigidBody).
     */
    public static class Builder<T extends VxBody> {
        
        /**
         * The factory used for instantiation.
         */
        private final Factory<T> factory;
        
        /**
         * The collection of behaviors to be applied.
         */
        private VxBehaviorSet behaviorSet;
        
        /**
         * The shape provider for rigid bodies.
         */
        private VxJoltRigidProvider rigidProvider;
        
        /**
         * The shape provider for soft bodies.
         */
        private VxJoltSoftProvider softProvider;

        /**
         * Private constructor to initialize the builder with a factory.
         * 
         * @param factory The factory used for instantiation.
         */
        private Builder(Factory<T> factory) {
            this.factory = factory;
        }

        /**
         * Initializes a new builder instance with the specified factory.
         *
         * @param factory The factory used for instantiation.
         * @param <T> The specific class implementation of the body (e.g. BoxRigidBody).
         * @return A new builder instance.
         */
        public static <T extends VxBody> Builder<T> create(Factory<T> factory) {
            return new Builder<>(factory);
        }

        /**
         * Configures the shape provider for a rigid body and applies default rigid behaviors if no behaviors are set.
         *
         * @param provider The shape provider for rigid bodies.
         * @return The current builder instance for fluent chaining.
         */
        public Builder<T> rigidProvider(VxJoltRigidProvider provider) {
            this.rigidProvider = provider;
            if (this.behaviorSet == null) {
                this.behaviorSet = VxBehaviorSet.rigid();
            }
            return this;
        }

        /**
         * Configures the shape provider for a soft body and applies default soft behaviors if no behaviors are set.
         *
         * @param provider The shape provider for soft bodies.
         * @return The current builder instance for fluent chaining.
         */
        public Builder<T> softProvider(VxJoltSoftProvider provider) {
            this.softProvider = provider;
            if (this.behaviorSet == null) {
                this.behaviorSet = VxBehaviorSet.soft();
            }
            return this;
        }

        /**
         * Explicitly sets the entire behavior set for this body type.
         *
         * @param behaviorSet The collection of behaviors to be applied.
         * @return The current builder instance for fluent chaining.
         */
        public Builder<T> behaviors(VxBehaviorSet behaviorSet) {
            this.behaviorSet = behaviorSet;
            return this;
        }

        /**
         * Modifies the current behavior set using a builder pattern.
         *
         * @param modifier The consumer that modifies the behavior set builder.
         * @return The current builder instance for fluent chaining.
         */
        public Builder<T> behaviors(Consumer<VxBehaviorSet.Builder> modifier) {
            VxBehaviorSet.Builder b = (this.behaviorSet != null)
                    ? this.behaviorSet.toBuilder()
                    : VxBehaviorSet.builder();
            modifier.accept(b);
            this.behaviorSet = b.build();
            return this;
        }

        /**
         * Constructs and returns the immutable body type definition.
         *
         * @param typeId The unique identifier representing this specific body type in registries.
         * @return A new immutable definition of a physics body type.
         */
        public VxBodyType<T> build(ResourceLocation typeId) {
            if (behaviorSet == null) {
                behaviorSet = VxBehaviorSet.empty();
            }

            return new VxBodyType<>(typeId, factory, behaviorSet, rigidProvider, softProvider);
        }
    }
}