package feixiaostudio.bigitemdisplay;

import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

public class BigItemDisplayEntity extends ItemFrameEntity {
    public static final int ROTATION_CHOICES = 24;

    public BigItemDisplayEntity(EntityType<? extends ItemFrameEntity> entityType, World world) {
        super(entityType, world);
    }

    public BigItemDisplayEntity(World world, BlockPos pos, Direction facing) {
        super(BigItemDisplayEntityType.BIG_ITEM_DISPLAY, world, pos, facing);
        this.setFacing(facing);
    }

    public Direction getFacing() { // == this.getHorizontalFacing();
        return this.facing;
    }

    public int getWidthPixels() {
        return 28;
    }

    public int getHeightPixels() {
        return 28;
    }

    public int getWidthBlocks() {
        return (this.getWidthPixels() - 1) / 16 + 1;
    }

    public int getHeightBlocks() {
        return (this.getHeightPixels() - 1) / 16 + 1;
    }

    // Copied from private double PaintingEntity.getOffset(int length) and modified that
    private double getOffset(int w) {
        // return (double) ((w%2 - 1) / 2); // w = length in blocks
        return (w-1) % 32 >= 16 ? 0.5 : 0.0; // w = length in pixels
    }

    @Override
    // protected final void AbstractDecorationEntity.updateattachedBlockPosition() is now final.
    // Cases are handled in calculateBoundingBox(BlockPos pos, Direction side).
    protected Box calculateBoundingBox(BlockPos pos, Direction side) {
        Vec3d center = Vec3d.ofCenter(pos).offset(side, -0.46875D);
        Direction.Axis axis = side.getAxis();
        double w = this.getOffset(this.getWidthPixels());
        double h = this.getOffset(this.getHeightPixels());

        double x = 0.0625D, y = 0.0625D, z = 0.0625D;
        Direction direction;
        switch (axis) {
            case X -> {
                y = 0.0625D * this.getHeightPixels();
                z = 0.0625D * this.getWidthPixels();
                direction = side.rotateYCounterclockwise();
                center = center.offset(direction, w).offset(Direction.UP, h);
            }
            case Y -> {
                x = 0.0625D * this.getWidthPixels();
                z = 0.0625D * this.getHeightPixels();
                center = center.offset(Direction.EAST, w).offset(Direction.NORTH, h);
            }
            case Z -> {
                x = 0.0625D * this.getWidthPixels();
                y = 0.0625D * this.getHeightPixels();
                direction = side.rotateYCounterclockwise();
                center = center.offset(direction, w).offset(Direction.UP, h);
            }
        }
        return Box.of(center, x, y, z);
    }

    @Override
    public boolean canStayAttached() {
        if (this.fixed) {
            return true;
        } else if (!this.getWorld().isSpaceEmpty(this)) {
            return false;
        } else {
            // Modified according to AbstractDecorationEntity.canStayAttached();
            boolean bl = BlockPos.stream(this.getAttachmentBox()).allMatch((pos) -> {
                BlockState blockState = this.getWorld().getBlockState(pos);
                return blockState.isSolid() || this.facing.getAxis().isHorizontal() && AbstractRedstoneGateBlock.isRedstoneGate(blockState);
            });
            return bl && this.getWorld().getOtherEntities(this, this.getBoundingBox(), PREDICATE).isEmpty();
        }
    }

    @Override
    // Copied from Entity.shouldRender(double distance)
    public boolean shouldRender(double distance) {
        double d = this.getBoundingBox().getAverageSideLength();
        if (Double.isNaN(d)) {
            d = 1.0;
        }
        d *= 64.0 * getRenderDistanceMultiplier();
        return distance < d * d;
    }

    @Override
    protected void setRotation(int value, boolean updateComparators) {
        this.getDataTracker().set(ROTATION, value % ROTATION_CHOICES);
        if (updateComparators && this.attachedBlockPos != null) {
            this.getWorld().updateComparators(this.attachedBlockPos, Blocks.AIR);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        // Copied from BlockAttachedEntity.writeCustomDataToNbt(NbtCompound nbt)
        BlockPos blockPos = this.getAttachedBlockPos();
        nbt.putInt("TileX", blockPos.getX());
        nbt.putInt("TileY", blockPos.getY());
        nbt.putInt("TileZ", blockPos.getZ());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        // Copied from BlockAttachedEntity.readCustomDataFromNbt(NbtCompound nbt)
        BlockPos blockPos = new BlockPos(nbt.getInt("TileX"), nbt.getInt("TileY"), nbt.getInt("TileZ"));
        if (!blockPos.isWithinDistance(this.getBlockPos(), 16.0D)) {
            BigItemDisplay.LOGGER.error("Block-attached entity at invalid position: {}", blockPos);
        } else {
            this.attachedBlockPos = blockPos;
        }
    }

    @Override
    public int getComparatorPower() {
        return this.getHeldItemStack().isEmpty() ? 0 : (this.getRotation() % ROTATION_CHOICES) / 2 + 1;
    }

    @Override
    protected ItemStack getAsItemStack() {
        return new ItemStack(BigItemDisplayItems.BIG_ITEM_DISPLAY);
    }

    @Override
    public float getBodyYaw() {
        Direction direction = this.getHorizontalFacing();
        int i = direction.getAxis().isVertical() ? 90 * direction.getDirection().offset() : 0;
        return MathHelper.wrapDegrees(180 + direction.getHorizontal() * 90 + this.getRotation() * 360.0F / ROTATION_CHOICES + i);
    }

}
