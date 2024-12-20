package hrcaoc.bigitemdisplay;

import com.mojang.logging.LogUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
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
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.OptionalInt;

public class BigItemDisplayEntity extends AbstractDecorationEntity {
    private static final Logger BIG_ITEM_DISPLAY_LOGGER = LogUtils.getLogger();
    private static final TrackedData<ItemStack> ITEM_STACK = DataTracker.registerData(BigItemDisplayEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Integer> ROTATION = DataTracker.registerData(BigItemDisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public BigItemDisplayEntity(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
        super(entityType, world);
    }

    public BigItemDisplayEntity(World world, BlockPos pos, Direction facing) {
        super(CustomEntityType.BIG_ITEM_DISPLAY, world, pos);
        this.setFacing(facing);
    }

    @Override
    protected void initDataTracker() {
        this.getDataTracker().startTracking(ITEM_STACK, ItemStack.EMPTY);
        this.getDataTracker().startTracking(ROTATION, 0);
    }

    @Override
    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.0F;
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
        this.updateAttachmentPosition();
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
    public void kill() {
        this.removeFromFrame(this.getHeldItemStack());
        super.kill();
    }

    public SoundEvent getPlaceSound() {
        return SoundEvents.ENTITY_ITEM_FRAME_PLACE;
    }

    public SoundEvent getBreakSound() {
        return SoundEvents.ENTITY_ITEM_FRAME_BREAK;
    }

    public SoundEvent getAddItemSound() {
        return SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM;
    }

    public SoundEvent getRemoveItemSound() {
        return SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM;
    }

    public SoundEvent getRotateItemSound() {
        return SoundEvents.ENTITY_ITEM_FRAME_ROTATE_ITEM;
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
    public void onPlace() {
        this.playSound(this.getPlaceSound(), 1.0F, 1.0F);
    }

    @Override
    public void onBreak(@Nullable Entity entity) {
        this.playSound(this.getBreakSound(), 1.0F, 1.0F);
        this.dropHeldStack(entity, true);
        this.emitGameEvent(GameEvent.BLOCK_CHANGE, entity);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        /* if (this.fixed) {
            return !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isSourceCreativePlayer() ? false : super.damage(source, amount);
        } else */ if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!source.isIn(DamageTypeTags.IS_EXPLOSION) && !this.getHeldItemStack().isEmpty()) {
            if (!this.getWorld().isClient) {
                this.dropHeldStack(source.getAttacker(), false);
                this.emitGameEvent(GameEvent.BLOCK_CHANGE, source.getAttacker());
                this.playSound(this.getRemoveItemSound(), 1.0F, 1.0F);
            }
            return true;
        } else {
            return super.damage(source, amount);
        }
    }

    public void setHeldItemStack(ItemStack stack) {
        this.setHeldItemStack(stack, true);
    }

    public void setHeldItemStack(ItemStack value, boolean update) {
        if (!value.isEmpty()) {
            value = value.copyWithCount(1);
        }

        this.setAsStackHolder(value);
        this.getDataTracker().set(ITEM_STACK, value);
        if (!value.isEmpty()) {
            this.playSound(this.getAddItemSound(), 1.0F, 1.0F);
        }

        if (update && this.attachmentPos != null) {
            this.getWorld().updateComparators(this.attachmentPos, Blocks.AIR);
        }
    }

    public ItemStack getHeldItemStack() {
        return this.getDataTracker().get(ITEM_STACK);
    }

    private void dropHeldStack(@Nullable Entity entity, boolean alwaysDrop) {
        // if (!this.fixed) {
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
                    // if (this.random.nextFloat() < this.itemDropChance) {
                        this.dropStack(itemStack);
                    // }
                }
            }
        // }
    }

    private void setAsStackHolder(ItemStack stack) {
        if (!stack.isEmpty()) { // && stack.getFrame() != this) {
            stack.setHolder(this);
        }

        this.updateAttachmentPosition();
    }

    public OptionalInt getMapId() {
        ItemStack itemStack = this.getHeldItemStack();
        if (itemStack.isOf(Items.FILLED_MAP)) {
            Integer integer = FilledMapItem.getMapId(itemStack);
            if (integer != null) {
                return OptionalInt.of(integer);
            }
        }

        return OptionalInt.empty();
    }

    public boolean containsMap() {
        return this.getMapId().isPresent();
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
                BigItemDisplayEntity.this.setHeldItemStack(stack);
                return true;
            }
        } : super.getStackReference(mappedIndex);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (data.equals(ITEM_STACK)) {
            this.setAsStackHolder(this.getHeldItemStack());
        }
    }

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
        this.getDataTracker().set(ROTATION, value % 8);
        if (updateComparators && this.attachmentPos != null) {
            this.getWorld().updateComparators(this.attachmentPos, Blocks.AIR);
        }
    }

    public void setRotation(int value) {
        this.setRotation(value, true);
    }

    public int getRotation() {
        return this.getDataTracker().get(ROTATION);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        boolean bl = !this.getHeldItemStack().isEmpty();
        boolean bl2 = !itemStack.isEmpty();
        /* if (this.fixed) {
            return ActionResult.PASS;
        } else */ if (!this.getWorld().isClient) {
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
        super.writeCustomDataToNbt(nbt);
        if (!this.getHeldItemStack().isEmpty()) {
            nbt.put("Item", this.getHeldItemStack().writeNbt(new NbtCompound()));
            nbt.putByte("ItemRotation", (byte)this.getRotation());
            // nbt.putFloat("ItemDropChance", this.itemDropChance);
        }

        nbt.putByte("Facing", (byte)this.facing.getId());
        // nbt.putBoolean("Invisible", this.isInvisible());
        // nbt.putBoolean("Fixed", this.fixed);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
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
            /* if (nbt.contains("ItemDropChance", NbtElement.NUMBER_TYPE)) {
                this.itemDropChance = nbt.getFloat("ItemDropChance");
            } */
        }

        this.setFacing(Direction.byId(nbt.getByte("Facing")));
        // this.setInvisible(nbt.getBoolean("Invisible"));
        // this.fixed = nbt.getBoolean("Fixed");
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, this.facing.getId(), this.getDecorationBlockPos());
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        this.setFacing(Direction.byId(packet.getEntityData()));
    }

    @Override
    public ItemStack getPickBlockStack() {
        ItemStack itemStack = this.getHeldItemStack();
        return itemStack.isEmpty() ? this.getAsItemStack() : itemStack.copy();
    }

    protected ItemStack getAsItemStack() {
        return new ItemStack(Items.ITEM_FRAME);
    }

    @Override
    public Vec3d getSyncedPos() {
        return Vec3d.of(this.attachmentPos);
    }

    @Override
    public float getBodyYaw() {
        Direction direction = this.getHorizontalFacing();
        int i = direction.getAxis().isVertical() ? 90 * direction.getDirection().offset() : 0;
        return (float) MathHelper.wrapDegrees(180 + direction.getHorizontal() * 90 + this.getRotation() * 45 + i);
    }

}
