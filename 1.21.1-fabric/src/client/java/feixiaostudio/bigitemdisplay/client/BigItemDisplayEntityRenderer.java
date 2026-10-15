package feixiaostudio.bigitemdisplay.client;

import feixiaostudio.bigitemdisplay.BigItemDisplay;
import feixiaostudio.bigitemdisplay.BigItemDisplayEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
// import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class BigItemDisplayEntityRenderer extends EntityRenderer<BigItemDisplayEntity>  {
    public static final int GLOW_FRAME_BLOCK_LIGHT = 5;
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
        Direction direction = bigItemDisplayEntity.getFacing();
        Vec3d itemPositionOffset = this.getPositionOffset(bigItemDisplayEntity, tickDelta);
        matrixStack.translate(-itemPositionOffset.getX(), -itemPositionOffset.getY(), -itemPositionOffset.getZ());
        double height = (double) 1/16;
        double centerToBlockCenter = (1 - height) / 2;
        matrixStack.translate((double)direction.getOffsetX() * centerToBlockCenter, (double)direction.getOffsetY() * centerToBlockCenter, (double)direction.getOffsetZ() * centerToBlockCenter);
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(bigItemDisplayEntity.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bigItemDisplayEntity.getYaw()));

        boolean isDisplayVisible = BigItemDisplayClient.renderRules.get("showDisplay") && !bigItemDisplayEntity.isInvisible();
        ItemStack itemStack = bigItemDisplayEntity.getHeldItemStack();
        int w = bigItemDisplayEntity.getWidthBlocks();
        int h = bigItemDisplayEntity.getHeightBlocks();
        if (isDisplayVisible) { // Must be placed here
            BakedModelManager bakedModelManager = this.blockRenderManager.getModels().getModelManager();
            matrixStack.push();
            matrixStack.translate(-0.5F, -0.5F, -0.5F);
            this.blockRenderManager.getModelRenderer().render(matrixStack.peek(), vertexConsumerProvider.getBuffer(TexturedRenderLayers.getEntitySolid()), null, bakedModelManager.getModel(this.getModelId(itemStack)), 1.0F, 1.0F, 1.0F, light, OverlayTexture.DEFAULT_UV);
            matrixStack.pop();
            /*int m = (w - 1) / -2;
            int n = (h - 1) / -2;
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    renderDisplay(matrixStack, vertexConsumerProvider, bakedModelManager.getModel(this.getModelId(itemStack)), light, - i - m, j + n - 1); // When facing the item, +i = right, +j = up
                }
            }*/
        }

        if (!itemStack.isEmpty()) {
            if (isDisplayVisible) {
                matrixStack.translate(0.0F, 0.0F, 0.5F - (float) height);
            } else {
                matrixStack.translate(0.0F, 0.0F, 0.5F);
            }

            MapIdComponent mapIdComponent = bigItemDisplayEntity.getMapId(itemStack);
            int scaleFactor = Math.min(w,h);
            if (mapIdComponent != null) {
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) bigItemDisplayEntity.getRotation() % 4 * 90.0F + 180.0F));
                float s = (float) scaleFactor/128;
                matrixStack.scale(s, s, s);
                matrixStack.translate(-64.0F, -64.0F, -1.0F);
                MapState mapState = FilledMapItem.getMapState(mapIdComponent, bigItemDisplayEntity.getWorld());
                if (mapState != null) {
                    int k = this.getLight(15728850, light);
                    MinecraftClient.getInstance().gameRenderer.getMapRenderer().draw(matrixStack, vertexConsumerProvider, mapIdComponent, mapState, true, k);
                }
            } else {
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) bigItemDisplayEntity.getRotation() * 360.0F / BigItemDisplayEntity.ROTATION_CHOICES));
                int l = this.getLight(15728880, light);
                float itemScaleFactor = 0.5F * scaleFactor * (isDisplayVisible ? 1.25F : 1.75F);
                matrixStack.scale(itemScaleFactor, itemScaleFactor, itemScaleFactor);
                this.itemRenderer.renderItem(itemStack, ModelTransformationMode.FIXED, l, OverlayTexture.DEFAULT_UV, matrixStack, vertexConsumerProvider, bigItemDisplayEntity.getWorld(), bigItemDisplayEntity.getId());
            }
        }
        matrixStack.pop();
    }

    /*private void renderDisplay(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BakedModel bakedModel, int light, float shiftX, float shiftY) {
        matrixStack.push();
        matrixStack.translate(shiftX, shiftY, -0.5F);
        this.blockRenderManager.getModelRenderer().render(matrixStack.peek(), vertexConsumerProvider.getBuffer(TexturedRenderLayers.getEntitySolid()), null, bakedModel, 1.0F, 1.0F, 1.0F, light, OverlayTexture.DEFAULT_UV);
        matrixStack.pop();
    }*/

    private ModelIdentifier getModelId(ItemStack stack) {
        String variant = "glow=" + BigItemDisplayClient.renderRules.get("isDisplayGlowing") + "," + "map=" + stack.isOf(Items.FILLED_MAP); // Must be dict order!
        return new ModelIdentifier(
            Identifier.of(BigItemDisplay.MOD_ID, "big_item_display")
            , variant);
    }

    private int getLight(int glowLight, int regularLight) {
        return BigItemDisplayClient.renderRules.get("isDisplayGlowing") ? glowLight : regularLight;
    }

    protected int getBlockLight(BigItemDisplayEntity bigItemDisplayEntity, BlockPos blockPos) {
        return BigItemDisplayClient.renderRules.get("isDisplayGlowing") ? Math.max(GLOW_FRAME_BLOCK_LIGHT, super.getBlockLight(bigItemDisplayEntity, blockPos)) : super.getBlockLight(bigItemDisplayEntity, blockPos);
    }

    public Identifier getTexture(BigItemDisplayEntity bigItemDisplayEntity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public Vec3d getPositionOffset(BigItemDisplayEntity bigItemDisplayEntity, float tickDelta) {
        Direction direction = bigItemDisplayEntity.getFacing();
        return new Vec3d(direction.getOffsetX() * 0.3F, -0.25D, direction.getOffsetZ() * 0.3F);
    }

    // Copy the following methods from ItemFrameEntityRenderer
    protected boolean hasLabel(BigItemDisplayEntity bigItemDisplayEntity) {
        if (MinecraftClient.isHudEnabled() && !bigItemDisplayEntity.getHeldItemStack().isEmpty() && bigItemDisplayEntity.getHeldItemStack().contains(DataComponentTypes.CUSTOM_NAME) && this.dispatcher.targetedEntity == bigItemDisplayEntity) {
            double d = this.dispatcher.getSquaredDistanceToCamera(bigItemDisplayEntity);
            return d < (double) 64.0F * 64.0F;
        } else {
            return false;
        }
    }

    protected void renderLabelIfPresent(BigItemDisplayEntity bigItemDisplayEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f) {
        super.renderLabelIfPresent(bigItemDisplayEntity, bigItemDisplayEntity.getHeldItemStack().getName(), matrixStack, vertexConsumerProvider, i, f);
    }

}
