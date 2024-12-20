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
import net.minecraft.entity.decoration.AbstractDecorationEntity;
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
    public static final int ROTATION_CHOICES = 12;

    public BigItemDisplayEntity(EntityType<? extends ItemFrameEntity> entityType, World world) {
        super(entityType, world);
    }

    public BigItemDisplayEntity(World world, BlockPos pos, Direction facing) {
        super(CustomEntityType.BIG_ITEM_DISPLAY, world, pos, facing);
        this.setFacing(facing);
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

    @Override
    protected void updateAttachmentPosition() { // Partial bounding box
        if (this.facing != null) {
            double d = 0.46875;
            double e = (double)this.attachmentPos.getX() + 0.5 - (double)this.facing.getOffsetX() * 0.46875;
            double f = (double)this.attachmentPos.getY() + 0.5 - (double)this.facing.getOffsetY() * 0.46875;
            double g = (double)this.attachmentPos.getZ() + 0.5 - (double)this.facing.getOffsetZ() * 0.46875;
            this.setPos(e, f, g);
            double h = (double)this.getWidthPixels();
            double i = (double)this.getHeightPixels();
            double j = (double)this.getWidthPixels();
            Direction.Axis axis = this.facing.getAxis();
            switch (axis) {
                case X:
                    h = 1.0;
                    break;
                case Y:
                    i = 1.0;
                    break;
                case Z:
                    j = 1.0;
            }

            h /= 32.0;
            i /= 32.0;
            j /= 32.0;
            this.setBoundingBox(new Box(e - h, f - i, g - j, e + h, f + i, g + j));
        }
    }

    @Override
    public boolean canStayAttached() {
        if (!this.getWorld().isSpaceEmpty(this)) {
            return false;
        } else {
            BlockState blockState = this.getWorld().getBlockState(this.attachmentPos.offset(this.facing.getOpposite()));
            return (blockState.isSolid() || this.facing.getAxis().isHorizontal() && AbstractRedstoneGateBlock.isRedstoneGate(blockState)) && this.getWorld().getOtherEntities(this, this.getBoundingBox(), PREDICATE).isEmpty();
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
        ((AbstractDecorationEntity) this).move(movementType, movement);
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        ((AbstractDecorationEntity) this).addVelocity(deltaX, deltaY, deltaZ);
    }

    @Override
    public boolean shouldRender(double distance) {
        return ((Entity) this).shouldRender(distance);
    }

    @Override
    public int getWidthPixels() {
        return 32;
    }

    @Override
    public int getHeightPixels() {
        return 32;
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
            return ((AbstractDecorationEntity) this).damage(source, amount);
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
        } : ((AbstractDecorationEntity) this).getStackReference(mappedIndex);
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
        ((AbstractDecorationEntity) this).writeCustomDataToNbt(nbt);
        if (!this.getHeldItemStack().isEmpty()) {
            nbt.put("Item", this.getHeldItemStack().writeNbt(new NbtCompound()));
            nbt.putByte("ItemRotation", (byte)this.getRotation());
        }

        nbt.putByte("Facing", (byte)this.facing.getId());
        // nbt.putBoolean("Invisible", this.isInvisible());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        ((AbstractDecorationEntity) this).readCustomDataFromNbt(nbt);
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
        // this.setInvisible(nbt.getBoolean("Invisible"));
    }

    @Override
    public int getComparatorPower() {
        return this.getHeldItemStack().isEmpty() ? 0 : this.getRotation() % ROTATION_CHOICES + 1;
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
