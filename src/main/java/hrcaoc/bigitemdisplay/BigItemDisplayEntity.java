package hrcaoc.bigitemdisplay;

import com.mojang.logging.LogUtils;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class BigItemDisplayEntity extends ItemFrameEntity {
    private static final Logger BIG_ITEM_DISPLAY_LOGGER = LogUtils.getLogger();
    private static final TrackedData<ItemStack> ITEM_STACK = DataTracker.registerData(BigItemDisplayEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Integer> ROTATION = DataTracker.registerData(BigItemDisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final int ROTATION_CHOICES = 24;

    public BigItemDisplayEntity(EntityType<? extends ItemFrameEntity> entityType, World world) {
        super(entityType, world);
    }

    public BigItemDisplayEntity(World world, BlockPos pos, Direction facing) {
        super(CustomEntityType.BIG_ITEM_DISPLAY, world, pos, facing);
        this.setFacing(facing);
    }

    @Override
    protected void initDataTracker() {
        this.getDataTracker().startTracking(ITEM_STACK, ItemStack.EMPTY);
        this.getDataTracker().startTracking(ROTATION, 0);
    }

    @Override
    protected void setFacing(Direction facing) { // Item frames can be placed horizontally
        Validate.notNull(facing);
        this.facing = facing;
        if (facing.getAxis().isHorizontal()) {
            this.setPitch(0.0F);
            this.setYaw((float)(this.facing.getHorizontal() * 90));
        } else {
            this.setPitch((float)(-90 * facing.getDirection().offset()));
            this.setYaw(0.0F);
        }

        this.prevPitch = this.getPitch();
        this.prevYaw = this.getYaw();
        this.updateAttachmentPosition(); // This method is modified in this class
    }

    public Direction getFacing() { // == this.getHorizontalFacing();
        return this.facing;
    }

    @Override
    public int getWidthPixels() {
        return 26;
    }

    @Override
    public int getHeightPixels() {
        return 26;
    }

    public int getWidthBlocks() {
        return (this.getWidthPixels() - 1) / 16 + 1;
    }

    public int getHeightBlocks() {
        return (this.getHeightPixels() - 1) / 16 + 1;
    }

    // Copied from AbstractDecorationEntity (for PaintingEntity) and modified that
    private double offsetForEvenLength(int i) {
        // return (double) ((w%2 - 1) / 2); // w = length in blocks
        return (i-1) % 32 >= 16 ? 0.5 : 0.0; // w = length in pixels
    }

    @Override
    protected void updateAttachmentPosition() { // Partial bounding box
        if (this.facing != null) {
            double height = (double) 1/16;
            double centerToBlockCenter = (1 - height) / 2;
            double centerX;
            double centerY;
            double centerZ;
            double halfLengthX;
            double halfLengthY;
            double halfLengthZ;
            double centerOffsetWidth = this.offsetForEvenLength(this.getWidthPixels());
            double centerOffsetHeight = this.offsetForEvenLength(this.getHeightPixels());
            Direction.Axis axis = this.facing.getAxis();
            switch (axis) {
                case X:
                    centerX = (double)this.attachmentPos.getX() + 0.5 - centerToBlockCenter * (double)this.facing.getOffsetX();
                    centerY = (double)this.attachmentPos.getY() + 0.5 + centerOffsetHeight;
                    centerZ = (double)this.attachmentPos.getZ() + 0.5 - centerOffsetWidth * (double)this.facing.getOffsetX(); // this.facing is opposite to the direction of the camera, so placed on right <-> minus z
                    halfLengthX = height / 2;
                    halfLengthY = (double)this.getHeightPixels() / 32;
                    halfLengthZ = (double)this.getWidthPixels() / 32;
                    break;
                case Y:
                    centerX = (double)this.attachmentPos.getX() + 0.5 + centerOffsetWidth; // East
                    centerY = (double)this.attachmentPos.getY() + 0.5 - centerToBlockCenter * (double)this.facing.getOffsetY();
                    centerZ = (double)this.attachmentPos.getZ() + 0.5 - centerOffsetHeight; // North
                    halfLengthX = (double)this.getWidthPixels() / 32;
                    halfLengthY = height / 2;
                    halfLengthZ = (double)this.getHeightPixels() / 32;
                    break;
                case Z:
                    centerX = (double)this.attachmentPos.getX() + 0.5 + centerOffsetWidth * (double)this.facing.getOffsetZ(); // Sign of centerOffset similar with above
                    centerY = (double)this.attachmentPos.getY() + 0.5 + centerOffsetHeight;
                    centerZ = (double)this.attachmentPos.getZ() + 0.5 - centerToBlockCenter * (double)this.facing.getOffsetZ();
                    halfLengthX = (double)this.getWidthPixels() / 32;
                    halfLengthY = (double)this.getHeightPixels() / 32;
                    halfLengthZ = height / 2;
                    break;
                default: // Code never reached at runtime, wrote this to pass IDE check
                    centerX = (double)this.attachmentPos.getX() + 0.5 - centerOffsetWidth * (double)this.facing.getOffsetX();
                    centerY = (double)this.attachmentPos.getY() + 0.5 - centerOffsetWidth * (double)this.facing.getOffsetY();
                    centerZ = (double)this.attachmentPos.getZ() + 0.5 - centerOffsetWidth * (double)this.facing.getOffsetZ();
                    // Below is NOT actual measure of the vertices, far more complicated
                    halfLengthX = (double)this.getWidthPixels() / 32 * (double)this.facing.getOffsetX();
                    halfLengthY = (double)this.getHeightPixels() / 32;
                    halfLengthZ = (double)this.getWidthPixels() / 32 * (double)this.facing.getOffsetZ();
            }

            this.setPos(centerX, centerY, centerZ);
            this.setBoundingBox(new Box(centerX - halfLengthX, centerY - halfLengthY, centerZ - halfLengthZ, centerX + halfLengthX, centerY + halfLengthY, centerZ + halfLengthZ));
        }
    }

    // Copied from AbstractDecorationEntity (the judgement of PaintingEntity) and modified that
    @Override
    public boolean canStayAttached() {
        if (!this.getWorld().isSpaceEmpty(this)) {
            return false;
        } else {
            int w = getWidthBlocks();
            int h = getHeightBlocks();
            BlockPos blockPos = this.attachmentPos.offset(this.facing.getOpposite());
            Direction.Axis axis = this.facing.getAxis();
            Direction directionWidth;
            Direction directionHeight = switch (axis) { // Enhanced Switch, provided by IDE
                case X -> {
                    directionWidth = this.facing.getOffsetX() > 0 ? Direction.NORTH : Direction.SOUTH; // North = -Z; same sign as in updateAttachmentPosition
                    yield Direction.UP;
                }
                case Y -> {
                    directionWidth = Direction.EAST; // East = +X; same sign as in updateAttachmentPosition
                    yield Direction.NORTH; // North = -Z; same sign as in updateAttachmentPosition
                }
                case Z -> {
                    directionWidth = this.facing.getOffsetZ() > 0 ? Direction.EAST : Direction.WEST; // East = +X; same sign as in updateAttachmentPosition
                    yield Direction.UP;
                }
                default -> { // Code never reached at runtime, wrote this to pass IDE check
                    directionWidth = Direction.EAST;
                    yield Direction.UP;
                }
            };
            /* Original Switch expression:
            switch (axis) {
                case X:
                    directionWidth = this.facing.getOffsetX() > 0 ? Direction.NORTH : Direction.SOUTH; // North = -Z; same sign as in updateAttachmentPosition
                    directionHeight = Direction.UP;
                    break;
                case Y:
                    directionWidth = Direction.EAST; // East = +X; same sign as in updateAttachmentPosition
                    directionHeight = Direction.NORTH; // North = -Z; same sign as in updateAttachmentPosition
                    break;
                case Z:
                    directionWidth = this.facing.getOffsetZ() > 0 ? Direction.EAST : Direction.WEST; // East = +X; same sign as in updateAttachmentPosition
                    directionHeight = Direction.UP;
                    break;
                default: // Code never reached at runtime, wrote this to pass IDE check
                    directionWidth = Direction.EAST;
                    directionHeight = Direction.UP;
            } */
            BlockPos.Mutable mutable = new BlockPos.Mutable();

            int m = (w - 1) / -2;
            int n = (h - 1) / -2;
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    mutable.set(blockPos).move(directionWidth, i + m).move(directionHeight, j + n);
                    BlockState blockState = this.getWorld().getBlockState(mutable);
                    if (!( blockState.isSolid() || axis.isHorizontal() && AbstractRedstoneGateBlock.isRedstoneGate(blockState) )) { // && evaluated before ||
                        return false;
                    }
                }
            }

            return this.getWorld().getOtherEntities(this, this.getBoundingBox(), PREDICATE).isEmpty();
            /* Below: judgement of ItemFrameEntity, only detects the attached block
            BlockState blockState = this.getWorld().getBlockState(this.attachmentPos.offset(this.facing.getOpposite()));
            return (blockState.isSolid() || this.facing.getAxis().isHorizontal() && AbstractRedstoneGateBlock.isRedstoneGate(blockState)) && this.getWorld().getOtherEntities(this, this.getBoundingBox(), PREDICATE).isEmpty();
            */
        }
    }

    /* Didn't copy this from PaintingEntity due to performance concerns
    @Override
	public void tick() {
		if (!this.getWorld().isClient) {
			this.attemptTickInVoid();
			if (this.obstructionCheckCounter++ == 100) {
				this.obstructionCheckCounter = 0;
				if (!this.isRemoved() && !this.canStayAttached()) {
					this.discard();
					this.onBreak(null);
				}
			}
		}
	} */

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        // Copied from AbstractDecorationEntity.move(MovementType movementType, Vec3d movement);
        if (!this.getWorld().isClient && !this.isRemoved() && movement.lengthSquared() > 0.0) {
            this.kill();
            this.onBreak(null);
        }
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        // Copied from AbstractDecorationEntity.addVelocity(double deltaX, double deltaY, double deltaZ)
        if (!this.getWorld().isClient && !this.isRemoved() && deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 0.0) {
            this.kill();
            this.onBreak(null);
        }
    }

    @Override
    public boolean shouldRender(double distance) {
        // Copied from Entity.shouldRender(double distance)
        double d = this.getBoundingBox().getAverageSideLength();
        if (Double.isNaN(d)) {
            d = 1.0;
        }
        d *= 64.0 * getRenderDistanceMultiplier();
        return distance < d * d;
    }

    @Override
    public void onBreak(@Nullable Entity entity) {
        this.playSound(this.getBreakSound(), 1.0F, 1.0F);
        this.dropHeldStack(entity, true); // This method is modified in this class
        this.emitGameEvent(GameEvent.BLOCK_CHANGE, entity);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!source.isIn(DamageTypeTags.IS_EXPLOSION) && !this.getHeldItemStack().isEmpty()) {
            if (!this.getWorld().isClient) {
                this.dropHeldStack(source.getAttacker(), false); // This method is modified in this class
                this.emitGameEvent(GameEvent.BLOCK_CHANGE, source.getAttacker());
                this.playSound(this.getRemoveItemSound(), 1.0F, 1.0F);
            }
            return true;
        } else {
            // Copied from AbstractDecorationEntity.damage(DamageSource source, float amount);
            if (this.isInvulnerableTo(source)) {
                return false;
            } else {
                if (!this.isRemoved() && !this.getWorld().isClient) {
                    this.kill();
                    this.scheduleVelocityUpdate();
                    this.onBreak(source.getAttacker());
                }

                return true;
            }
        }
    }

    @Override
    public void setHeldItemStack(ItemStack stack) {
        this.setHeldItemStack(stack, true);
    }

    @Override
    public void setHeldItemStack(ItemStack value, boolean update) {
        if (!value.isEmpty()) {
            value = value.copyWithCount(1);
        }

        this.setAsStackHolder(value); // This method is modified in this class
        this.getDataTracker().set(ITEM_STACK, value);
        if (!value.isEmpty()) {
            this.playSound(this.getAddItemSound(), 1.0F, 1.0F);
        }

        if (update && this.attachmentPos != null) {
            this.getWorld().updateComparators(this.attachmentPos, Blocks.AIR);
        }
    }

    // Copied private method from super class and modified that
    private void dropHeldStack(@Nullable Entity entity, boolean alwaysDrop) {
        ItemStack itemStack = this.getHeldItemStack();
        this.setHeldItemStack(ItemStack.EMPTY);
        if (!this.getWorld().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
            if (entity == null) {
                this.removeFromFrame(itemStack);
            }
        } else {
            if (entity instanceof PlayerEntity playerEntity && playerEntity.getAbilities().creativeMode) {
                this.removeFromFrame(itemStack);
                return;
            }

            if (alwaysDrop) {
                this.dropStack(this.getAsItemStack());
            }

            if (!itemStack.isEmpty()) {
                itemStack = itemStack.copy();
                this.removeFromFrame(itemStack);
                this.dropStack(itemStack);
            }
        }
    }

    private void setAsStackHolder(ItemStack stack) {
        if (!stack.isEmpty() && stack.getFrame() != this) {
            stack.setHolder(this);
        }

        this.updateAttachmentPosition(); // This method is modified in this class
    }

    @Override
    public StackReference getStackReference(int mappedIndex) {
        return mappedIndex == 0 ? new StackReference() {
            @Override
            public ItemStack get() {
                return BigItemDisplayEntity.this.getHeldItemStack();
            }

            @Override
            public boolean set(ItemStack stack) {
                BigItemDisplayEntity.this.setHeldItemStack(stack); // This method is modified in this class
                return true;
            }
        } : StackReference.EMPTY; // Copied from Entity.getStackReference(int mappedIndex);
    }

    @Override
    public ItemStack getHeldItemStack() {
        return this.getDataTracker().get(ITEM_STACK);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (data.equals(ITEM_STACK)) {
            this.setAsStackHolder(this.getHeldItemStack()); // This method is modified in this class
        }
    }

    // Copied private method from super class
    private void removeFromFrame(ItemStack itemStack) {
        this.getMapId().ifPresent(i -> {
            MapState mapState = FilledMapItem.getMapState(i, this.getWorld());
            if (mapState != null) {
                mapState.removeFrame(this.attachmentPos, this.getId());
                mapState.setDirty(true);
            }
        });
        itemStack.setHolder(null);
    }

    @Override
    public int getRotation() {
        return this.getDataTracker().get(ROTATION);
    }

    private void setRotation(int value, boolean updateComparators) {
        this.getDataTracker().set(ROTATION, value % ROTATION_CHOICES);
        if (updateComparators && this.attachmentPos != null) {
            this.getWorld().updateComparators(this.attachmentPos, Blocks.AIR);
        }
    }

    @Override
    public void setRotation(int value) {
        this.setRotation(value, true);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        boolean bl = !this.getHeldItemStack().isEmpty();
        boolean bl2 = !itemStack.isEmpty();
        if (!this.getWorld().isClient) {
            if (!bl) {
                if (bl2 && !this.isRemoved()) {
                    if (itemStack.isOf(Items.FILLED_MAP)) {
                        MapState mapState = FilledMapItem.getMapState(itemStack, this.getWorld());
                        if (mapState != null && mapState.iconCountNotLessThan(256)) {
                            return ActionResult.FAIL;
                        }
                    }

                    this.setHeldItemStack(itemStack);
                    this.emitGameEvent(GameEvent.BLOCK_CHANGE, player);
                    if (!player.getAbilities().creativeMode) {
                        itemStack.decrement(1);
                    }
                }
            } else {
                this.playSound(this.getRotateItemSound(), 1.0F, 1.0F);
                this.setRotation(this.getRotation() + 1);
                this.emitGameEvent(GameEvent.BLOCK_CHANGE, player);
            }

            return ActionResult.CONSUME;
        } else {
            return !bl && !bl2 ? ActionResult.PASS : ActionResult.SUCCESS;
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        // Copied from AbstractDecorationEntity.writeCustomDataToNbt(NbtCompound nbt)
        BlockPos blockPos = this.getDecorationBlockPos();
        nbt.putInt("TileX", blockPos.getX());
        nbt.putInt("TileY", blockPos.getY());
        nbt.putInt("TileZ", blockPos.getZ());
        //
        if (!this.getHeldItemStack().isEmpty()) {
            nbt.put("Item", this.getHeldItemStack().writeNbt(new NbtCompound()));
            nbt.putByte("ItemRotation", (byte)this.getRotation());
        }

        nbt.putByte("Facing", (byte)this.facing.getId());
        nbt.putBoolean("Invisible", this.isInvisible());
        // Plan to add nbt data representing width and height here
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        // Copied from AbstractDecorationEntity.readCustomDataFromNbt(NbtCompound nbt)
        BlockPos blockPos = new BlockPos(nbt.getInt("TileX"), nbt.getInt("TileY"), nbt.getInt("TileZ"));
        if (!blockPos.isWithinDistance(this.getBlockPos(), 16.0)) {
            BIG_ITEM_DISPLAY_LOGGER.error("Hanging entity at invalid position: {}", blockPos);
        } else {
            this.attachmentPos = blockPos;
        }
        //
        NbtCompound nbtCompound = nbt.getCompound("Item");
        if (nbtCompound != null && !nbtCompound.isEmpty()) {
            ItemStack itemStack = ItemStack.fromNbt(nbtCompound);
            if (itemStack.isEmpty()) {
                BIG_ITEM_DISPLAY_LOGGER.warn("Unable to load item from: {}", nbtCompound);
            }

            ItemStack itemStack2 = this.getHeldItemStack();
            if (!itemStack2.isEmpty() && !ItemStack.areEqual(itemStack, itemStack2)) {
                this.removeFromFrame(itemStack2);
            }

            this.setHeldItemStack(itemStack, false);
            this.setRotation(nbt.getByte("ItemRotation"), false);
        }

        this.setFacing(Direction.byId(nbt.getByte("Facing")));
        this.setInvisible(nbt.getBoolean("Invisible"));
    }

    @Override
    public int getComparatorPower() {
        return this.getHeldItemStack().isEmpty() ? 0 : (this.getRotation() % ROTATION_CHOICES) / 2 + 1;
    }

    @Override
    public ItemStack getPickBlockStack() {
        ItemStack itemStack = this.getHeldItemStack();
        return itemStack.isEmpty() ? this.getAsItemStack() : itemStack.copy();
    }

    @Override
    protected ItemStack getAsItemStack() {
        return new ItemStack(CustomItems.BIG_ITEM_DISPLAY);
    }

}
