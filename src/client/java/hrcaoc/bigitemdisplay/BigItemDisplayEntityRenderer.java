package hrcaoc.bigitemdisplay;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class BigItemDisplayEntityRenderer extends EntityRenderer<BigItemDisplayEntity>  {
    private static final ModelIdentifier NORMAL_DISPLAY = ModelIdentifier.ofVanilla("item_frame", "map=false");
    private static final ModelIdentifier DISPLAY_WITH_MAP = ModelIdentifier.ofVanilla("item_frame", "map=true");
    private static final ModelIdentifier GLOW_DISPLAY = ModelIdentifier.ofVanilla("glow_item_frame", "map=false");
    private static final ModelIdentifier GLOW_DISPLAY_WITH_MAP = ModelIdentifier.ofVanilla("glow_item_frame", "map=true");
    public static final int GLOW_FRAME_BLOCK_LIGHT = 15;
    public boolean isPlayerPresent = false; // Add player detection here
    private final ItemRenderer itemRenderer;
    private final BlockRenderManager blockRenderManager;

    public BigItemDisplayEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.blockRenderManager = context.getBlockRenderManager();
    }

    public void render(BigItemDisplayEntity bigItemDisplayEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        super.render(bigItemDisplayEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light); // render label of the item inside display
        matrixStack.push();
        Direction direction = bigItemDisplayEntity.getHorizontalFacing();
        Vec3d itemPositionOffset = this.getPositionOffset(bigItemDisplayEntity, tickDelta);
        matrixStack.translate(-itemPositionOffset.getX(), -itemPositionOffset.getY(), -itemPositionOffset.getZ());
        double height = (double) 1/16;
        double centerToBlockCenter = (1 - height) / 2;
        matrixStack.translate((double)direction.getOffsetX() * centerToBlockCenter, (double)direction.getOffsetY() * centerToBlockCenter, (double)direction.getOffsetZ() * centerToBlockCenter);
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(bigItemDisplayEntity.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bigItemDisplayEntity.getYaw()));

        boolean isDisplayVisible = !bigItemDisplayEntity.isInvisible();
        ItemStack itemStack = bigItemDisplayEntity.getHeldItemStack();
        if (isDisplayVisible) { // Must be placed here
            renderDisplay(itemStack, matrixStack, vertexConsumerProvider, light);
        }

        if (!itemStack.isEmpty()) {
            if (isDisplayVisible) {
                matrixStack.translate(0.0F, 0.0F, 0.5F - (float) height);
            } else {
                matrixStack.translate(0.0F, 0.0F, 0.5F);
            }

            OptionalInt optionalInt = bigItemDisplayEntity.getMapId();
            int scaleFactor = 2;
            if (optionalInt.isPresent()) {
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) bigItemDisplayEntity.getRotation() % 4 * 90.0F + 180.0F));
                float h = (float) scaleFactor/128;
                matrixStack.scale(h, h, h);
                matrixStack.translate(-32.0F * (1 + scaleFactor), -32.0F * (1 + scaleFactor), -1.0F);
                MapState mapState = FilledMapItem.getMapState(optionalInt.getAsInt(), bigItemDisplayEntity.getWorld());
                if (mapState != null) {
                    int k = this.getLight(15728850, light);
                    MinecraftClient.getInstance().gameRenderer.getMapRenderer().draw(matrixStack, vertexConsumerProvider, optionalInt.getAsInt(), mapState, true, k);
                }
            } else {
                matrixStack.translate(+0.5F, +0.5F, -0.3F);
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) bigItemDisplayEntity.getRotation() * 360.0F / BigItemDisplayEntity.ROTATION_CHOICES));
                int l = this.getLight(15728880, light);
                // matrixStack.scale(0.5F, 0.5F, 0.5F);
                this.itemRenderer.renderItem(itemStack, ModelTransformationMode.FIXED, l, OverlayTexture.DEFAULT_UV, matrixStack, vertexConsumerProvider, bigItemDisplayEntity.getWorld(), bigItemDisplayEntity.getId());
            }
        }
        matrixStack.pop();
    }

    private void renderDisplay(ItemStack itemStack, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        BakedModelManager bakedModelManager = this.blockRenderManager.getModels().getModelManager();
        ModelIdentifier modelIdentifier = this.getModelId(itemStack);
        matrixStack.push();
        matrixStack.translate(-0.5F, -0.5F, -0.5F);
        this.blockRenderManager.getModelRenderer().render(matrixStack.peek(), vertexConsumerProvider.getBuffer(TexturedRenderLayers.getEntitySolid()), null, bakedModelManager.getModel(modelIdentifier), 1.0F, 1.0F, 1.0F, light, OverlayTexture.DEFAULT_UV);
        matrixStack.pop();
    }

    private ModelIdentifier getModelId(ItemStack stack) {
        if (stack.isOf(Items.FILLED_MAP)) {
            return isPlayerPresent ? GLOW_DISPLAY_WITH_MAP : DISPLAY_WITH_MAP;
        } else {
            return isPlayerPresent ? GLOW_DISPLAY : NORMAL_DISPLAY;
        }
    }

    private int getLight(int glowLight, int regularLight) {
        return isPlayerPresent ? glowLight : regularLight;
    }

    protected int getBlockLight(BigItemDisplayEntity bigItemDisplayEntity, BlockPos blockPos) {
        return bigItemDisplayEntity.getType() == EntityType.GLOW_ITEM_FRAME ? Math.max(GLOW_FRAME_BLOCK_LIGHT, super.getBlockLight(bigItemDisplayEntity, blockPos)) : super.getBlockLight(bigItemDisplayEntity, blockPos);
    }

    public Identifier getTexture(BigItemDisplayEntity bigItemDisplayEntity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public Vec3d getPositionOffset(BigItemDisplayEntity bigItemDisplayEntity, float tickDelta) {
        Direction direction = bigItemDisplayEntity.getHorizontalFacing();
        return new Vec3d((double)((float) direction.getOffsetX() * 0.3F), -0.25D, (double)((float) direction.getOffsetZ() * 0.3F));
    }

    // Copy the following methods from ItemFrameEntityRenderer
    protected boolean hasLabel(BigItemDisplayEntity bigItemDisplayEntity) {
        if (MinecraftClient.isHudEnabled() && !bigItemDisplayEntity.getHeldItemStack().isEmpty() && bigItemDisplayEntity.getHeldItemStack().hasCustomName() && this.dispatcher.targetedEntity == bigItemDisplayEntity) {
            double d = this.dispatcher.getSquaredDistanceToCamera(bigItemDisplayEntity);
            // float f = bigItemDisplayEntity.isSneaky() ? 32.0F : 64.0F;
            // return d < (double)(f * f);
            return d < (double) 64.0F * 64.0F;
        } else {
            return false;
        }
    }

    protected void renderLabelIfPresent(BigItemDisplayEntity bigItemDisplayEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        super.renderLabelIfPresent(bigItemDisplayEntity, bigItemDisplayEntity.getHeldItemStack().getName(), matrixStack, vertexConsumerProvider, i);
    }

}
