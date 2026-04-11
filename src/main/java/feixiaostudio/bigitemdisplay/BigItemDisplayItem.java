package feixiaostudio.bigitemdisplay;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class BigItemDisplayItem extends DecorationItem {
    public BigItemDisplayItem(EntityType<? extends AbstractDecorationEntity> type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected boolean canPlaceOn(PlayerEntity player, Direction side, ItemStack stack, BlockPos pos) {
        return !player.getWorld().isOutOfHeightLimit(pos) && player.canPlaceOn(pos, side, stack);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos blockPos = context.getBlockPos();
        Direction direction = context.getSide();
        BlockPos blockPos2 = blockPos.offset(direction);
        PlayerEntity playerEntity = context.getPlayer();
        ItemStack itemStack = context.getStack();
        if (playerEntity != null && !this.canPlaceOn(playerEntity, direction, itemStack, blockPos2)) {
            return ActionResult.FAIL;
        } else {
            World world = context.getWorld();
            BigItemDisplayEntity bigItemDisplayEntity = new BigItemDisplayEntity(world, blockPos2, direction);

            NbtCompound nbtCompound = itemStack.getNbt();
            if (nbtCompound != null) {
                EntityType.loadFromEntityNbt(world, playerEntity, bigItemDisplayEntity, nbtCompound);
            }

            if (bigItemDisplayEntity.canStayAttached()) {
                if (!world.isClient) {
                    bigItemDisplayEntity.onPlace();
                    world.emitGameEvent(playerEntity, GameEvent.ENTITY_PLACE, bigItemDisplayEntity.getPos());
                    world.spawnEntity(bigItemDisplayEntity);
                }

                itemStack.decrement(1);
                return ActionResult.success(world.isClient);
            } else {
                return ActionResult.CONSUME;
            }
        }
    }

}
