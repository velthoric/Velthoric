/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.behavior.impl;

import net.xmx.velthoric.core.behavior.VxBehavior;
import net.xmx.velthoric.core.behavior.VxBehaviorId;
import net.xmx.velthoric.init.VxMainClass;

/**
 * A behavior that allows a body to be removed by standard administrative commands like /vxkill.
 * <p>
 * This is intended for bodies that can be accidentally deleted by players.
 *
 * @author xI-Mx-Ix
 */
public class VxKillBehavior implements VxBehavior {

    /**
     * The unique identifier for this behavior.
     */
    public static final VxBehaviorId ID = new VxBehaviorId(VxMainClass.MODID, "Kill");

    /**
     * Default constructor for kill behavior.
     */
    public VxKillBehavior() {
    }

    /**
     * @return The unique identifier for this behavior.
     */
    @Override
    public VxBehaviorId getId() {
        return ID;
    }
}