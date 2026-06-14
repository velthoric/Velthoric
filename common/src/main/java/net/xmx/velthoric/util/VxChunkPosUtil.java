/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.util;

import net.minecraft.world.level.ChunkPos;

/**
 * Utility class for ChunkPos-related operations to ensure compatibility
 * across different Minecraft versions.
 *
 * @author timtaran
 */
public class VxChunkPosUtil {
    /**
     * Packs the X and Z coordinates of a {@link ChunkPos} into a single 64-bit value.
     * <p>
     * The lower 32 bits contain the X coordinate and the upper 32 bits contain
     * the Z coordinate.
     *
     * @param chunkPos the chunk position to pack
     * @return a packed long containing the X and Z coordinates
     */
    public static long packLong(ChunkPos chunkPos) {
        //? if >=26.1 {
        /*return packLong(chunkPos.x(), chunkPos.z());
        *///? } else {
        return packLong(chunkPos.x, chunkPos.z);
        //? }
    }

    public static long packLong(int x, int z) {
        return (long) x & 0xFFFFFFFFL | ((long) z & 0xFFFFFFFFL) << 32;
    }

    /**
     * Unpacks a chunk position from a 64-bit value.
     *
     * @param packedPos the packed value
     * @return the unpacked chunk position
     */
    public static ChunkPos unpackLong(long packedPos) {
        return new ChunkPos((int) packedPos, (int) (packedPos >> 32));
    }
}
