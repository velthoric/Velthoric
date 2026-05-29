/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.behavior.impl;

import net.xmx.velthoric.core.behavior.VxBehavior;
import net.xmx.velthoric.core.behavior.VxBehaviorId;
import net.xmx.velthoric.init.VxMainClass;

/**
 * A behavior indicating that a body type can be summoned via commands.
 *
 * @author xI-Mx-Ix
 */
public class VxSummonableBehavior implements VxBehavior {

    /**
     * The unique identifier for this specific behavior.
     */
    public static final VxBehaviorId ID = new VxBehaviorId(VxMainClass.MODID, "Summonable");

    /**
     * Constructs a new instance of the summonable behavior.
     */
    public VxSummonableBehavior() {
    }

    /**
     * Retrieves the unique identifier associated with this behavior.
     *
     * @return The identifier of the summonable behavior.
     */
    @Override
    public VxBehaviorId getId() {
        return ID;
    }
}