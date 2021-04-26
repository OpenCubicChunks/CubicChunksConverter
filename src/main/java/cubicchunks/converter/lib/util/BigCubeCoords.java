package cubicchunks.converter.lib.util;

import cubicchunks.regionlib.impl.EntryLocation3D;

/**
 * A class that contains helper-methods for many CubicChunks related things.
 * <p>
 * General Notes:
 * <ul>
 * <li>If a parameter is called <b>val</b> such as in the method {@link Coords#blockToLocal} it refers to a single dimension of that coordinate (x, y or z).</li>
 * <li>If a parameter is called <b>pos</b> and is of type <b>long</b> it refers to the entire coordinate compressed into a long. For example {@link SectionPos#asLong()}</li>
 * </ul>
 */
@SuppressWarnings("JavadocReference") public class BigCubeCoords {

    public static final int NO_HEIGHT = Integer.MIN_VALUE + 32;

    private static final int LOG2_BLOCK_SIZE = MathUtil.log2(32);

    private static final int BLOCK_SIZE_MINUS_1 = 32 - 1;
    private static final int BLOCK_SIZE_DIV_2 = 32 / 2;
    private static final int BLOCK_SIZE_DIV_16 = 32 / 16;
    private static final int BLOCK_SIZE_DIV_32 = 32 / 32;

    private static final int POS_TO_INDEX_MASK = getPosToIndexMask();
    private static final int INDEX_TO_POS_MASK = POS_TO_INDEX_MASK >> 4;

    private static final int INDEX_TO_N_X = 0;
    private static final int INDEX_TO_N_Y = LOG2_BLOCK_SIZE - 4;
    private static final int INDEX_TO_N_Z = INDEX_TO_N_Y * 2;

    private static int getPosToIndexMask() {
        int mask = 0;
        for (int i = 32 / 2; i >= 16; i /= 2) {
            mask += i;
        }
        return mask;
    }

    /**
     * Gets the middle block between two given blocks
     *
     * @return The central position as a BlockPos
     */
    public static Vector3i midPos(Vector3i p1, Vector3i p2) {
        //bitshifting each number and then adding the result - this rounds the number down and prevents overflow
        return new Vector3i((p1.getX() >> 1) + (p2.getX() >> 1) + (p1.getX() & p2.getX() & 1),
                (p1.getY() >> 1) + (p2.getY() >> 1) + (p1.getY() & p2.getY() & 1),
                (p1.getZ() >> 1) + (p2.getZ() >> 1) + (p1.getZ() & p2.getZ() & 1));
    }

    /**
     * Gets the offset of a {@link BlockPos} inside it's {@link BigCube}
     *
     * @param val A single value of the position
     *
     * @return The position relative to the {@link BigCube} this block is in
     */
    public static int blockToLocal(int val) {
        return val & (32 - 1);
    }

    /** See {@link Coords#blockToLocal} */
    public static int localX(Vector3i pos) {
        return blockToLocal(pos.getX());
    }

    /** See {@link Coords#blockToLocal} */
    public static int localY(Vector3i pos) {
        return blockToLocal(pos.getY());
    }

    /** See {@link Coords#blockToLocal} */
    public static int localZ(Vector3i pos) {
        return blockToLocal(pos.getZ());
    }

    /**
     * @param val A single dimension of the {@link BlockPos} (eg: {@link BlockPos#getY()})
     *
     * @return That coordinate as a CubePos
     */
    public static int blockToCube(int val) {
        return val >> LOG2_BLOCK_SIZE;
    }

    /** See {@link Coords#blockToCube(int)} */
    public static int blockToCube(double blockPos) {
        return blockToCube((int) Math.floor(blockPos));
    }

    /**
     * @param blockVal A single dimension of the {@link BlockPos} (eg: {@link BlockPos#getY()})
     *
     * @return That coordinate as a CubePos
     */
    public static int blockCeilToCube(int blockVal) {
        return -((-blockVal) >> LOG2_BLOCK_SIZE);
    }

    /**
     * @param cubeVal Single dimension of a {@link CubePos}
     * @param localVal Offset of the block from the cube
     *
     * @return Sum of cubeVal as {@link BlockPos} and localVal
     */
    public static int localToBlock(int cubeVal, int localVal) {
        return cubeToMinBlock(cubeVal) + localVal;
    }

    /**
     * @param cubeVal A single dimension of a {@link CubePos}
     *
     * @return The minimum {@link BlockPos} inside that {@link BigCube}
     */
    public static int cubeToMinBlock(int cubeVal) {
        return cubeVal << LOG2_BLOCK_SIZE;
    }

    /**
     * @param cubeVal A single dimension of a {@link CubePos}
     *
     * @return The maximum {@link BlockPos} inside that {@link BigCube}
     */
    public static int cubeToMaxBlock(int cubeVal) {
        return cubeToMinBlock(cubeVal) + BLOCK_SIZE_MINUS_1;
    }

    public static int cubeToCenterBlock(int cubeVal) {
        return localToBlock(cubeVal, BLOCK_SIZE_DIV_2);
    }

    /** See {@link Coords#blockToIndex(int, int, int)} */
    public static int blockToIndex16(int blockXVal, int blockYVal, int blockZVal) {
        return 0;
    }

    /** See {@link Coords#blockToIndex(int, int, int)} */
    public static int blockToIndex32(int blockXVal, int blockYVal, int blockZVal) {
        //1 bit
        final int mask = POS_TO_INDEX_MASK;
        return (blockXVal & mask) >> 4 | (blockYVal & mask) >> 3 | (blockZVal & mask) >> 2;
    }

    /** See {@link Coords#blockToIndex(int, int, int)} */
    @SuppressWarnings("PointlessBitwiseExpression")
    public static int blockToIndex64(int blockXVal, int blockYVal, int blockZVal) {
        //2 bit
        //1011101010001, 1010101011100, 1101011101010
        final int mask = POS_TO_INDEX_MASK;
        return (blockXVal & mask) >> 4 | (blockYVal & mask) >> 2 | (blockZVal & mask) >> 0;
    }

    /** See {@link Coords#blockToIndex(int, int, int)} */
    public static int blockToIndex128(int blockXVal, int blockYVal, int blockZVal) {
        //3 bit
        //1011101010001, 1010101011100, 1101011101010
        final int mask = POS_TO_INDEX_MASK;
        return (blockXVal & mask) >> 4 | (blockYVal & mask) >> 1 | (blockZVal & mask) << 2;
    }

    /**
     * @param idx Index of the {@link ChunkSection} within it's {@link BigCube}
     *
     * @return The X offset (as a  {@link SectionPos}) from it's {@link CubePos} (as a  {@link SectionPos})
     */
    public static int indexToX(int idx) {
        return idx >> INDEX_TO_N_X & INDEX_TO_POS_MASK;
    }

    /**
     * @param idx Index of the {@link ChunkSection} within it's {@link BigCube}
     *
     * @return The Y offset (as a  {@link SectionPos}) from it's {@link CubePos} (as a  {@link SectionPos})
     */
    public static int indexToY(int idx) {
        return idx >> INDEX_TO_N_Y & INDEX_TO_POS_MASK;
    }

    /**
     * @param idx Index of the {@link ChunkSection} within it's {@link BigCube}
     *
     * @return The Z offset (as a  {@link SectionPos}) from it's {@link CubePos} (as a  {@link SectionPos})
     */
    public static int indexToZ(int idx) {
        return idx >> INDEX_TO_N_Z & INDEX_TO_POS_MASK;
    }

    /**
     * @param val Single dimension of a {@link SectionPos}
     *
     * @return That {@link SectionPos} dimension as a single dimension of a {@link CubePos}
     */
    public static int sectionToCube(int val) {
        return val >> (LOG2_BLOCK_SIZE - 4);
    }

    /**
     * @param sectionX A section X
     * @param sectionY A section Y
     * @param sectionZ A section Z
     *
     * @return The index of the {@link ChunkSection} that the specified {@link SectionPos} describes inside it's {@link BigCube}
     */
    public static int sectionToIndex32(int sectionX, int sectionY, int sectionZ) {
        return blockToIndex32(sectionX << 4, sectionY << 4, sectionZ << 4);
    }

    public static int indexToSectionX(int idx) {
        return indexToX(idx << 4);
    }

    public static int indexToSectionY(int idx) {
        return indexToY(idx << 4);
    }

    public static int indexToSectionZ(int idx) {
        return indexToZ(idx << 4);
    }

    /**
     * @param cubeVal A single dimension of the {@link CubePos}
     * @param sectionOffset The {@link SectionPos} offset from the {@link CubePos} as a {@link SectionPos}. Suggest you use {@link Coords#indexToX}, {@link Coords#indexToY}, {@link
     *     Coords#indexToZ} to get this offset
     *
     * @return The cubeVal as a sectionVal
     */
    public static int cubeToSection(int cubeVal, int sectionOffset) {
        return cubeVal << (LOG2_BLOCK_SIZE - 4) | sectionOffset;
    }

    public static int sectionToCubeCeil(int viewDistance) {
        return MathUtil.ceilDiv(viewDistance, 2);
    }

    public static int sectionToCubeRenderDistance(int viewDistance) {
        return Math.max(3, sectionToCubeCeil(viewDistance));
    }

    /**
     * @param blockVal A single dimension of a {@link BlockPos}
     *
     * @return That blockVal as a sectionVal
     */
    public static int blockToSection(int blockVal) {
        return blockVal >> 4;
    }

    /**
     * @param sectionVal A single dimension of a {@link SectionPos}
     *
     * @return That sectionVal as a blockVal
     */
    public static int sectionToMinBlock(int sectionVal) {
        return sectionVal << 4;
    }

    public static int blockToSectionLocal(int pos) {
        return pos & 0xF;
    }

    /**
     * @param cubePos The {@link CubePos}
     * @param i The index of the {@link ChunkSection} inside the {@link CubePos}
     *
     * @return The {@link SectionPos} of the {@link ChunkSection} at index i
     */
    public static EntryLocation3D sectionPosByIndex(EntryLocation3D cubePos, int i) {
        return new EntryLocation3D(cubeToSection(cubePos.getEntryX(), indexToX(i)), cubeToSection(cubePos.getEntryY(), indexToY(i)), cubeToSection(cubePos.getEntryZ(),
                indexToZ(i)));
    }

    public static int blockToCubeLocalSection(int x) {
        return (x >> 4) & 1;

    }

    public static int cubeLocalSection(int section) {
        return section & 1;
    }

    public static Vector3i sectionPosToMinBlockPos(EntryLocation3D sectionPos) {
        return new Vector3i(sectionToMinBlock(sectionPos.getEntryX()), sectionToMinBlock(sectionPos.getEntryY()), sectionToMinBlock(sectionPos.getEntryZ()));
    }
}