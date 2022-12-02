/*
 *  This file is part of CubicChunksConverter, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2017-2021 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.converter.lib.util.edittask;

import com.flowpowered.nbt.*;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.regionlib.impl.EntryLocation2D;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

public class RotateEditTask extends TranslationEditTask {
    private final Vector3i origin;
    public final int degrees;
    public RotateEditTask(BoundingBox srcBox, Vector3i origin, int degrees){
        srcBoxes.add(srcBox);
        dstBoxes.add(srcBox);
        this.origin = origin;
        if (degrees % 90 != 0) throw new IllegalArgumentException("Degrees must be divisible by 90");
        this.degrees = degrees;
    }

    public Vector3i rotateDst90Degrees(Vector3i dst){
        int newX = dst.getX();
        int newZ = dst.getZ();

        //Subtract origin from points
        newX-=this.origin.getX();
        newZ-=this.origin.getZ();

        //Swap X and Y
        int temp = newZ;
        newZ = newX;
        newX = temp;

        newZ*=-1;

        //Add origin to points
        newX+=this.origin.getX();
        newZ+=this.origin.getZ();

        return new Vector3i(newX, dst.getY(), newZ);
    }

    public Vector3i rotateDst(Vector3i dstPos, int degrees){
        int degree = degrees;
        while ((degree/=90) > 0){
            dstPos = this.rotateDst90Degrees(dstPos);
        }
        return dstPos;
    }

    public EntryLocation2D rotateDst(EntryLocation2D dstPos, int degrees){
        Vector3i temp = new Vector3i(dstPos.getEntryX(), 0, dstPos.getEntryZ());
        temp = rotateDst(temp, this.degrees);
        return new EntryLocation2D(temp.getX(), temp.getZ());
    }

    private byte rotateMetadata(MaterialData blockData, String blockName){
        int degree = degrees;
        byte result=blockData.getData();
        //TODO item frames do not work
        while ((degree/=90) > 0){
            if (blockName.equals("SIGN_POST"))
                result= (byte) ((((int) result) - 4) % 16); //TODO this doesn't work.
            else if (blockName.equals("WALL_SIGN"))
                result = (byte) (Math.abs(((((int) result) - 3) + 2) % 4) + 3); //TODO this doesn't work.
            else{
                if (result == 5) //TODO bad code
                    result=2;
                else if (result == 2)
                    result = 4;
                else if (result == 4)
                    result = 3;
                else if (result == 3)
                    result = 5;
            }
//                result = (byte) (Math.abs(((( (int) result) - 2) - 2) % 4) + 2);
        }
        return result;
    }

    private int rotateMetadata(int blockId, int metaData){
        if (blockId < 0){
            blockId+=256;
        }
        Material block = Material.getMaterial(blockId);
        MaterialData blockData = block.getNewData((byte) metaData);
        if (blockData instanceof Directional){
            metaData = this.rotateMetadata(blockData, block.name());
        }
        return metaData;
    }

    @Nonnull public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, EditTaskContext.EditTaskConfig config, CompoundTag cubeTag, long inCubePriority) {
        List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> outCubes = new ArrayList<>();

        // calculate offset
        Vector3i dstPos = this.rotateDst(cubePos, this.degrees);

        // adjusting new cube data to be valid
        CompoundMap level = (CompoundMap) cubeTag.getValue().get("Level").getValue();

        CompoundMap sectionDetails;
        try {
            sectionDetails = ((CompoundTag) ((List<?>) (level).get("Sections").getValue()).get(0)).getValue(); //POSSIBLE ARRAY OUT OF BOUNDS EXCEPTION ON A MALFORMED CUBE//POSSIBLE ARRAY OUT OF BOUNDS EXCEPTION ON A MALFORMED CUBE
        }
        catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            LOGGER.warning("Malformed cube at position (" + cubePos.getX() + ", " + cubePos.getY() + ", " + cubePos.getZ() + "), skipping!");
            return outCubes;
        }

        level.put(new IntTag("x", dstPos.getX()));
        level.put(new IntTag("y", dstPos.getY()));
        level.put(new IntTag("z", dstPos.getZ()));

        if (config.shouldRelightDst()) {
            this.markCubeForLightUpdates(level);
        }
        this.markCubePopulated(level);

        // Rotating Tile Entities
        for (int i=0; i< ((List<?>) (level).get("TileEntities").getValue()).size(); i++){
            CompoundMap tileEntity = ((CompoundTag) ((List<?>) (level).get("TileEntities").getValue()).get(i)).getValue();
            int xVal = ((Integer) tileEntity.get("z").getValue());
            int zVal = ((((Integer) tileEntity.get("x").getValue())-8)*-1)+7;
            tileEntity.put(new IntTag("x", xVal));
            tileEntity.put(new IntTag("z", zVal));
        }

        // Rotating Entities
        for (int i=0; i< ((List<?>) (level).get("Entities").getValue()).size(); i++){
            CompoundMap entity = ((CompoundTag) ((List<?>) (level).get("Entities").getValue()).get(i)).getValue();
            List<DoubleTag> pos = (List<DoubleTag>) entity.get("Pos").getValue();
            double xVal = (pos.get(2).getValue());
            double zVal = (((pos.get(0).getValue())-8)*-1)+7;
            double yVal = (pos.get(1).getValue());
            List<DoubleTag> newPos = Arrays.asList(new DoubleTag("", xVal), new DoubleTag("", yVal), new DoubleTag("", zVal));
            entity.put(new ListTag<>("Pos", DoubleTag.class, newPos));

            //Handle Item Frames
            String id = ((String) entity.get("id").getValue());
            if (id.equals("minecraft:item_frame")) {
                entity.put(new IntTag("TileX", (int) Math.floor(xVal)));
                entity.put(new IntTag("TileZ", (int) Math.ceil(zVal)));

                int facing = (int) ((Byte) entity.get("Facing").getValue());
                entity.put(new ByteTag("Facing", ((byte) ((facing+2) %4)) ));
            }
        }

        final byte[] blocks = (byte[]) sectionDetails.get("Blocks").getValue();
        final byte[] meta = (byte[]) sectionDetails.get("Data").getValue();

        byte[] newBlocks = new byte[blocks.length];
        byte[] newMeta = new byte[meta.length];

        int sideLen=16;
        int squareLen=sideLen*sideLen;

        for(int i=0; i<this.degrees; i+=90) {
            for (int y = 0; y < blocks.length / squareLen; y++) {
                for (int r = 0; r < sideLen; r++) {
                    for (int c = 0; c < sideLen; c++) {
                        //int newIndex = ((c*sideLen)+sideLen-1-r)+(y*squareLen);   //Counter Clockwise
                        int newIndex = (((sideLen - 1) - c)*sideLen) + r+ (y * squareLen); //Clockwise
                        int oldIndex = ((r * sideLen) + c) + (y * squareLen);
                        newBlocks[newIndex] = blocks[oldIndex];
                        int metaData = rotateMetadata(newBlocks[newIndex], EditTask.nibbleGetAtIndex(meta, oldIndex));
                        EditTask.nibbleSetAtIndex(newMeta, newIndex, metaData);
                    }
                }
            }
            System.arraycopy(newBlocks, 0, blocks, 0, blocks.length);
            System.arraycopy(newMeta, 0, meta, 0, meta.length);
        }

    outCubes.add(new ImmutablePair<>(dstPos, new ImmutablePair<>(inCubePriority + 1, cubeTag)));
    return outCubes;
    }

}


