/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.network.synchronization.behavior;

import net.xmx.velthoric.core.behavior.VxBehavior;
import net.xmx.velthoric.core.behavior.VxBehaviorId;
import net.xmx.velthoric.init.VxMainClass;

/**
 * A behavior indicating that a physics body's custom synchronized data
 * should be synchronized between the server and the client.
 *
 * @author xI-Mx-Ix
 */
public class VxSynchronizedDataBehavior implements VxBehavior {

    /**
     * The unique identifier for the synchronized data behavior.
     */
    public static final VxBehaviorId ID = new VxBehaviorId(VxMainClass.MODID, "CustomDataSync");

    /**
     * Default constructor.
     */
    public VxSynchronizedDataBehavior() {
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