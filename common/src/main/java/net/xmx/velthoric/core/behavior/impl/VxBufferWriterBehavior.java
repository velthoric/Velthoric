/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.behavior.impl;

import net.xmx.velthoric.core.behavior.VxBehavior;
import net.xmx.velthoric.core.behavior.VxBehaviorId;
import net.xmx.velthoric.core.body.server.VxBufferWriter;
import net.xmx.velthoric.init.VxMainClass;

/**
 * A wrapper behavior that indicates whether physics data should be extracted for a body.
 * <p>
 * This is a simple yes/no wrapper that marks bodies for physics data extraction.
 * The actual extraction logic is handled by {@link VxBufferWriter}.
 *
 * @author xI-Mx-Ix
 */
public class VxBufferWriterBehavior implements VxBehavior {

    /**
     * The unique identifier for this behavior.
     * Consumed by the behavior manager for bitmask allocation and dispatch.
     */
    public static final VxBehaviorId ID = new VxBehaviorId(VxMainClass.MODID, "BufferWriter");

    /**
     * Default constructor for the buffer writer behavior.
     */
    public VxBufferWriterBehavior() {
    }

    /**
     * Retrieves the unique identifier for this behavior.
     *
     * @return The behavior ID.
     */
    @Override
    public VxBehaviorId getId() {
        return ID;
    }
}