/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.entity.interaction;

/**
 * Duck interface to attach native collision state directly to entities.
 *
 * @author xI-Mx-Ix
 */
public interface VxEntityAttachment {

    /**
     * @return the server ground body index
     */
    int velthoric$getServerGroundBody();

    /**
     * @param value the server ground body index
     */
    void velthoric$setServerGroundBody(int value);

    /**
     * @return the client ground body index
     */
    int velthoric$getClientGroundBody();

    /**
     * @param value the client ground body index
     */
    void velthoric$setClientGroundBody(int value);

    /**
     * @return the scaling factor for body displacement dragging
     */
    float velthoric$getGroundDragScale();

    /**
     * @param value the scaling factor for body displacement dragging
     */
    void velthoric$setGroundDragScale(float value);
}