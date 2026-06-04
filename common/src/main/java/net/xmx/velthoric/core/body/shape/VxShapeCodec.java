/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.body.shape;

import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.Vec3;
import net.xmx.velthoric.network.VxByteBuf;

import java.util.List;

/**
 * A codec for serializing and deserializing the {@link VxCollisionShape} hierarchy
 * to/from a byte buffer for network transmission and persistence.
 * <p>
 * Each shape type is identified by a single byte discriminator. Decorator and compound
 * shapes are serialized recursively.
 *
 * @author xI-Mx-Ix
 */
public final class VxShapeCodec {

    // --- Shape Type Discriminators ---
    private static final byte TYPE_BOX = 0;
    private static final byte TYPE_SPHERE = 1;
    private static final byte TYPE_CAPSULE = 2;
    private static final byte TYPE_CYLINDER = 3;
    private static final byte TYPE_CONVEX_HULL = 4;
    private static final byte TYPE_EMPTY = 5;
    private static final byte TYPE_SCALED = 6;
    private static final byte TYPE_ROTATED_TRANSLATED = 7;
    private static final byte TYPE_OFFSET_CENTER_OF_MASS = 8;
    private static final byte TYPE_STATIC_COMPOUND = 9;
    private static final byte TYPE_MUTABLE_COMPOUND = 10;
    private static final byte TYPE_TAPERED_CAPSULE = 11;
    private static final byte TYPE_TAPERED_CYLINDER = 12;
    private static final byte TYPE_TRIANGLE = 13;

    private VxShapeCodec() {
    }

    /**
     * Serializes a {@link VxCollisionShape} into the buffer.
     * <p>
     * Writes a type discriminator byte followed by the shape-specific parameters.
     * Decorator and compound shapes recurse into their children.
     *
     * @param buf   The destination buffer.
     * @param shape The shape to serialize.
     */
    public static void write(VxByteBuf buf, VxCollisionShape shape) {
        if (shape instanceof VxBoxShape box) {
            buf.writeByte(TYPE_BOX);
            writeVec3(buf, box.getHalfExtents());
            buf.writeFloat(box.getConvexRadius());
        } else if (shape instanceof VxSphereShape sphere) {
            buf.writeByte(TYPE_SPHERE);
            buf.writeFloat(sphere.getRadius());
        } else if (shape instanceof VxCapsuleShape capsule) {
            buf.writeByte(TYPE_CAPSULE);
            buf.writeFloat(capsule.getHalfHeight());
            buf.writeFloat(capsule.getRadius());
        } else if (shape instanceof VxCylinderShape cylinder) {
            buf.writeByte(TYPE_CYLINDER);
            buf.writeFloat(cylinder.getHalfHeight());
            buf.writeFloat(cylinder.getRadius());
            buf.writeFloat(cylinder.getConvexRadius());
        } else if (shape instanceof VxConvexHullShape hull) {
            buf.writeByte(TYPE_CONVEX_HULL);
            float[] points = hull.getPoints();
            buf.writeVarInt(points.length);
            for (float p : points) buf.writeFloat(p);
            buf.writeFloat(hull.getMaxConvexRadius());
        } else if (shape instanceof VxEmptyShape) {
            buf.writeByte(TYPE_EMPTY);
        } else if (shape instanceof VxScaledShape scaled) {
            buf.writeByte(TYPE_SCALED);
            writeVec3(buf, scaled.getScale());
            write(buf, scaled.getInner());
        } else if (shape instanceof VxRotatedTranslatedShape rts) {
            buf.writeByte(TYPE_ROTATED_TRANSLATED);
            writeVec3(buf, rts.getOffset());
            writeQuat(buf, rts.getRotation());
            write(buf, rts.getInner());
        } else if (shape instanceof VxOffsetCenterOfMassShape ocm) {
            buf.writeByte(TYPE_OFFSET_CENTER_OF_MASS);
            writeVec3(buf, ocm.getOffset());
            write(buf, ocm.getInner());
        } else if (shape instanceof VxStaticCompoundShape sc) {
            buf.writeByte(TYPE_STATIC_COMPOUND);
            List<VxStaticCompoundShape.ChildShape> children = sc.getChildren();
            buf.writeVarInt(children.size());
            for (VxStaticCompoundShape.ChildShape child : children) {
                writeVec3(buf, child.position());
                writeQuat(buf, child.rotation());
                write(buf, child.shape());
            }
        } else if (shape instanceof VxMutableCompoundShape mc) {
            buf.writeByte(TYPE_MUTABLE_COMPOUND);
            List<VxMutableCompoundShape.ChildShape> children = mc.getChildren();
            buf.writeVarInt(children.size());
            for (VxMutableCompoundShape.ChildShape child : children) {
                writeVec3(buf, child.position());
                writeQuat(buf, child.rotation());
                write(buf, child.shape());
            }
        } else if (shape instanceof VxTaperedCapsuleShape tc) {
            buf.writeByte(TYPE_TAPERED_CAPSULE);
            buf.writeFloat(tc.getHalfHeight());
            buf.writeFloat(tc.getTopRadius());
            buf.writeFloat(tc.getBottomRadius());
        } else if (shape instanceof VxTaperedCylinderShape tcy) {
            buf.writeByte(TYPE_TAPERED_CYLINDER);
            buf.writeFloat(tcy.getHalfHeight());
            buf.writeFloat(tcy.getTopRadius());
            buf.writeFloat(tcy.getBottomRadius());
            buf.writeFloat(tcy.getConvexRadius());
        } else if (shape instanceof VxTriangleShape tri) {
            buf.writeByte(TYPE_TRIANGLE);
            writeVec3(buf, tri.getV1());
            writeVec3(buf, tri.getV2());
            writeVec3(buf, tri.getV3());
            buf.writeFloat(tri.getConvexRadius());
        } else {
            throw new IllegalArgumentException("Unknown shape type: " + shape.getClass().getName());
        }
    }

    /**
     * Deserializes a {@link VxCollisionShape} from the buffer.
     *
     * @param buf The source buffer.
     * @return The deserialized shape instance.
     */
    public static VxCollisionShape read(VxByteBuf buf) {
        byte type = buf.readByte();
        switch (type) {
            case TYPE_BOX:
                return new VxBoxShape(readVec3(buf), buf.readFloat());
            case TYPE_SPHERE:
                return new VxSphereShape(buf.readFloat());
            case TYPE_CAPSULE:
                return new VxCapsuleShape(buf.readFloat(), buf.readFloat());
            case TYPE_CYLINDER:
                return new VxCylinderShape(buf.readFloat(), buf.readFloat(), buf.readFloat());
            case TYPE_CONVEX_HULL: {
                int len = buf.readVarInt();
                float[] points = new float[len];
                for (int i = 0; i < len; i++) points[i] = buf.readFloat();
                return new VxConvexHullShape(points, buf.readFloat());
            }
            case TYPE_EMPTY:
                return VxEmptyShape.INSTANCE;
            case TYPE_SCALED: {
                Vec3 scale = readVec3(buf);
                return new VxScaledShape(read(buf), scale);
            }
            case TYPE_ROTATED_TRANSLATED: {
                Vec3 offset = readVec3(buf);
                Quat rot = readQuat(buf);
                return new VxRotatedTranslatedShape(offset, rot, read(buf));
            }
            case TYPE_OFFSET_CENTER_OF_MASS:
                return new VxOffsetCenterOfMassShape(readVec3(buf), read(buf));
            case TYPE_STATIC_COMPOUND: {
                int count = buf.readVarInt();
                VxStaticCompoundShape compound = new VxStaticCompoundShape();
                for (int i = 0; i < count; i++) {
                    Vec3 pos = readVec3(buf);
                    Quat rot = readQuat(buf);
                    compound.addShape(read(buf), pos, rot);
                }
                return compound;
            }
            case TYPE_MUTABLE_COMPOUND: {
                int count = buf.readVarInt();
                VxMutableCompoundShape compound = new VxMutableCompoundShape();
                for (int i = 0; i < count; i++) {
                    Vec3 pos = readVec3(buf);
                    Quat rot = readQuat(buf);
                    compound.addShape(read(buf), pos, rot);
                }
                return compound;
            }
            case TYPE_TAPERED_CAPSULE:
                return new VxTaperedCapsuleShape(buf.readFloat(), buf.readFloat(), buf.readFloat());
            case TYPE_TAPERED_CYLINDER:
                return new VxTaperedCylinderShape(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
            case TYPE_TRIANGLE:
                return new VxTriangleShape(readVec3(buf), readVec3(buf), readVec3(buf), buf.readFloat());
            default:
                throw new IllegalArgumentException("Unknown shape type discriminator: " + type);
        }
    }

    // --- Vec3 / Quat helpers (avoid depending on VxByteBuf's Jolt helpers for raw ByteBuf usage) ---

    private static void writeVec3(VxByteBuf buf, Vec3 vec) {
        buf.writeFloat(vec.getX());
        buf.writeFloat(vec.getY());
        buf.writeFloat(vec.getZ());
    }

    private static Vec3 readVec3(VxByteBuf buf) {
        return new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    private static void writeQuat(VxByteBuf buf, Quat quat) {
        buf.writeFloat(quat.getX());
        buf.writeFloat(quat.getY());
        buf.writeFloat(quat.getZ());
        buf.writeFloat(quat.getW());
    }

    private static Quat readQuat(VxByteBuf buf) {
        return new Quat(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
}