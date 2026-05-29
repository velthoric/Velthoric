/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.builtin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.xmx.velthoric.builtin.block.BlockRenderer;
import net.xmx.velthoric.builtin.block.BlockRigidBody;
import net.xmx.velthoric.builtin.box.BoxRenderer;
import net.xmx.velthoric.builtin.box.BoxRigidBody;
import net.xmx.velthoric.builtin.cloth.ClothRenderer;
import net.xmx.velthoric.builtin.cloth.ClothSoftBody;
import net.xmx.velthoric.builtin.marble.MarbleRenderer;
import net.xmx.velthoric.builtin.marble.MarbleRigidBody;
import net.xmx.velthoric.builtin.rope.RopeRenderer;
import net.xmx.velthoric.builtin.rope.RopeSoftBody;
import net.xmx.velthoric.builtin.sphere.SphereRenderer;
import net.xmx.velthoric.builtin.sphere.SphereRigidBody;
import net.xmx.velthoric.core.behavior.impl.VxSummonableBehavior;
import net.xmx.velthoric.core.body.VxBodyType;
import net.xmx.velthoric.core.body.persistence.behavior.VxPersistenceBehavior;
import net.xmx.velthoric.core.body.registry.VxBodyRegistry;
import net.xmx.velthoric.core.network.internal.behavior.VxNetSyncBehavior;
import net.xmx.velthoric.core.network.synchronization.behavior.VxSyncBehavior;
import net.xmx.velthoric.core.physics.buoyancy.behavior.VxBuoyancyBehavior;
import net.xmx.velthoric.core.ragdoll.body.VxBodyPartRigidBody;
import net.xmx.velthoric.core.ragdoll.body.VxRagdollBodyPartRenderer;
import net.xmx.velthoric.item.chaincreator.body.VxChainPartRenderer;
import net.xmx.velthoric.item.chaincreator.body.VxChainPartRigidBody;

/**
 * A central registry for all built-in physics body types. This class handles
 * the registration of server-side body logic and client-side rendering factories.
 * <p>
 * Each body type is defined using the composition-based {@link VxBodyType.Builder},
 * with Jolt shape providers and persistence handlers registered declaratively.
 *
 * @author xI-Mx-Ix
 */
public class VxRegisteredBodies {

    // --- Rigid Bodies ---

    public static final VxBodyType<BlockRigidBody> BLOCK = VxBodyType.Builder
            .<BlockRigidBody>create(BlockRigidBody::new)
            .rigidProvider(BlockRigidBody::createJoltBody)
            .behaviors(b -> b
                    .add(new VxBuoyancyBehavior())
                    .add(new VxNetSyncBehavior())
                    .add(new VxSyncBehavior())
                    .add(new VxPersistenceBehavior(BlockRigidBody::writePersistence, BlockRigidBody::readPersistence)))
            .build(ResourceLocation.tryBuild("velthoric", "block"));

    public static final VxBodyType<SphereRigidBody> SPHERE = VxBodyType.Builder
            .<SphereRigidBody>create(SphereRigidBody::new)
            .rigidProvider(SphereRigidBody::createJoltBody)
            .behaviors(b -> b
                    .add(new VxSummonableBehavior())
                    .add(new VxBuoyancyBehavior())
                    .add(new VxNetSyncBehavior())
                    .add(new VxSyncBehavior())
                    .add(new VxPersistenceBehavior(SphereRigidBody::writePersistence, SphereRigidBody::readPersistence)))
            .build(ResourceLocation.tryBuild("velthoric", "sphere"));

    public static final VxBodyType<BoxRigidBody> BOX = VxBodyType.Builder
            .<BoxRigidBody>create(BoxRigidBody::new)
            .rigidProvider(BoxRigidBody::createJoltBody)
            .behaviors(b -> b
                    .add(new VxSummonableBehavior())
                    .add(new VxBuoyancyBehavior())
                    .add(new VxNetSyncBehavior())
                    .add(new VxSyncBehavior())
                    .add(new VxPersistenceBehavior(BoxRigidBody::writePersistence, BoxRigidBody::readPersistence)))
            .build(ResourceLocation.tryBuild("velthoric", "box"));

    public static final VxBodyType<MarbleRigidBody> MARBLE = VxBodyType.Builder
            .<MarbleRigidBody>create(MarbleRigidBody::new)
            .rigidProvider(MarbleRigidBody::createJoltBody)
            .behaviors(b -> b
                    .add(new VxSummonableBehavior())
                    .add(new VxBuoyancyBehavior())
                    .add(new VxNetSyncBehavior())
                    .add(new VxSyncBehavior())
                    .add(new VxPersistenceBehavior(MarbleRigidBody::writePersistence, MarbleRigidBody::readPersistence)))
            .build(ResourceLocation.tryBuild("velthoric", "marble"));

    // --- Soft Bodies ---

    public static final VxBodyType<ClothSoftBody> CLOTH = VxBodyType.Builder
            .<ClothSoftBody>create(ClothSoftBody::new)
            .softProvider(ClothSoftBody::createJoltBody)
            .behaviors(b -> b
                    .add(new VxSummonableBehavior())
                    .add(new VxNetSyncBehavior())
                    .add(new VxSyncBehavior())
                    .add(new VxPersistenceBehavior(ClothSoftBody::writePersistence, ClothSoftBody::readPersistence)))
            .build(ResourceLocation.tryBuild("velthoric", "cloth"));

    public static final VxBodyType<RopeSoftBody> ROPE = VxBodyType.Builder
            .<RopeSoftBody>create(RopeSoftBody::new)
            .softProvider(RopeSoftBody::createJoltBody)
            .behaviors(b -> b
                    .add(new VxSummonableBehavior())
                    .add(new VxNetSyncBehavior())
                    .add(new VxSyncBehavior())
                    .add(new VxPersistenceBehavior(RopeSoftBody::writePersistence, RopeSoftBody::readPersistence)))
            .build(ResourceLocation.tryBuild("velthoric", "rope"));

    // --- Internal Bodies ---

    public static final VxBodyType<VxChainPartRigidBody> CHAIN_PART = VxBodyType.Builder
            .<VxChainPartRigidBody>create(VxChainPartRigidBody::new)
            .rigidProvider(VxChainPartRigidBody::createJoltBody)
            .behaviors(b -> b
                    .add(new VxBuoyancyBehavior())
                    .add(new VxNetSyncBehavior())
                    .add(new VxSyncBehavior())
                    .add(new VxPersistenceBehavior(VxChainPartRigidBody::writePersistence, VxChainPartRigidBody::readPersistence)))
            .build(ResourceLocation.tryBuild("velthoric", "chain_part"));

    public static final VxBodyType<VxBodyPartRigidBody> BODY_PART = VxBodyType.Builder
            .<VxBodyPartRigidBody>create(VxBodyPartRigidBody::new)
            .rigidProvider(VxBodyPartRigidBody::createJoltBody)
            .behaviors(b -> b
                    .add(new VxBuoyancyBehavior())
                    .add(new VxNetSyncBehavior())
                    .add(new VxSyncBehavior())
                    .add(new VxPersistenceBehavior(VxBodyPartRigidBody::writePersistence, VxBodyPartRigidBody::readPersistence)))
            .build(ResourceLocation.tryBuild("velthoric", "body_part"));

    /**
     * Registers all server-side physics body types. This should be called
     * during the server initialization phase.
     */
    public static void register() {
        var registry = VxBodyRegistry.getInstance();
        registry.register(BLOCK);
        registry.register(SPHERE);
        registry.register(BOX);
        registry.register(MARBLE);
        registry.register(CLOTH);
        registry.register(ROPE);
        registry.register(CHAIN_PART);
        registry.register(BODY_PART);
    }

    /**
     * Registers factories for client-side instantiation of physics bodies.
     * This should only be called on the client.
     */
    @Environment(EnvType.CLIENT)
    public static void registerClientFactories() {
        var registry = VxBodyRegistry.getInstance();
        registry.registerClientFactory(BLOCK.getTypeId(), (type, id) -> new BlockRigidBody(type, id));
        registry.registerClientFactory(SPHERE.getTypeId(), (type, id) -> new SphereRigidBody(type, id));
        registry.registerClientFactory(BOX.getTypeId(), (type, id) -> new BoxRigidBody(type, id));
        registry.registerClientFactory(MARBLE.getTypeId(), (type, id) -> new MarbleRigidBody(type, id));
        registry.registerClientFactory(CLOTH.getTypeId(), (type, id) -> new ClothSoftBody(type, id));
        registry.registerClientFactory(ROPE.getTypeId(), (type, id) -> new RopeSoftBody(type, id));
        registry.registerClientFactory(CHAIN_PART.getTypeId(), (type, id) -> new VxChainPartRigidBody(type, id));
        registry.registerClientFactory(BODY_PART.getTypeId(), (type, id) -> new VxBodyPartRigidBody(type, id));
    }

    /**
     * Registers all client-side renderers for physics bodies.
     * This must be called on the client after factories are registered.
     */
    @Environment(EnvType.CLIENT)
    public static void registerClientRenderers() {
        var registry = VxBodyRegistry.getInstance();
        registry.registerClientRenderer(BLOCK.getTypeId(), new BlockRenderer());
        registry.registerClientRenderer(SPHERE.getTypeId(), new SphereRenderer());
        registry.registerClientRenderer(BOX.getTypeId(), new BoxRenderer());
        registry.registerClientRenderer(MARBLE.getTypeId(), new MarbleRenderer());
        registry.registerClientRenderer(CLOTH.getTypeId(), new ClothRenderer());
        registry.registerClientRenderer(ROPE.getTypeId(), new RopeRenderer());
        registry.registerClientRenderer(CHAIN_PART.getTypeId(), new VxChainPartRenderer());
        registry.registerClientRenderer(BODY_PART.getTypeId(), new VxRagdollBodyPartRenderer());
    }
}