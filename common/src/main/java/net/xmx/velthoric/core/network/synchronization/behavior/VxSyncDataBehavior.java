/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.network.synchronization.behavior;

import net.xmx.velthoric.core.behavior.VxBehavior;
import net.xmx.velthoric.core.behavior.VxBehaviorId;
import net.xmx.velthoric.core.network.synchronization.VxSynchronizedData;
import net.xmx.velthoric.init.VxMainClass;

import java.util.function.Consumer;

/**
 * A behavior that defines synchronized data fields for a physics body.
 *
 * @author xI-Mx-Ix
 */
public class VxSyncDataBehavior implements VxBehavior {

    /**
     * The unique identifier for this specific behavior.
     */
    public static final VxBehaviorId ID = new VxBehaviorId(VxMainClass.MODID, "SyncData");

    /**
     * A consumer responsible for configuring the synchronized data builder.
     */
    private final Consumer<VxSynchronizedData.Builder> definer;

    /**
     * Constructs a new instance of the synchronized data behavior.
     *
     * @param definer A consumer used to define custom synchronized fields for the body.
     */
    public VxSyncDataBehavior(Consumer<VxSynchronizedData.Builder> definer) {
        this.definer = definer;
    }

    /**
     * Applies the configured synchronized data definitions to the provided builder.
     *
     * @param builder The synchronized data builder to configure.
     */
    public void defineSyncData(VxSynchronizedData.Builder builder) {
        if (definer != null) {
            definer.accept(builder);
        }
    }

    /**
     * Retrieves the unique identifier associated with this behavior.
     *
     * @return The identifier of the synchronized data behavior.
     */
    @Override
    public VxBehaviorId getId() {
        return ID;
    }
}