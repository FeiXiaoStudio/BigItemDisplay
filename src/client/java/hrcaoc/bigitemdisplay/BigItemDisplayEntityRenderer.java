package hrcaoc.bigitemdisplay;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
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
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class BigItemDisplayEntityRenderer extends EntityRenderer<BigItemDisplayEntity>  {
    private final ItemRenderer itemRenderer;
    private final BlockRenderManager blockRenderManager;

    public BigItemDisplayEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.blockRenderManager = context.getBlockRenderManager();
    }

    public Identifier getTexture(BigItemDisplayEntity bigItemDisplayEntity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    public void render(BigItemDisplayEntity bigItemDisplayEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        super.render(bigItemDisplayEntity, f, g, matrixStack, vertexConsumerProvider, i);
        matrixStack.push();
        Direction direction = bigItemDisplayEntity.getHorizontalFacing();
        Vec3d vec3d = this.getPositionOffset(bigItemDisplayEntity, g);
        matrixStack.translate(-vec3d.getX(), -vec3d.getY(), -vec3d.getZ());
        double d = (double)0.46875F;
        matrixStack.translate((double)direction.getOffsetX() * (double)0.46875F, (double)direction.getOffsetY() * (double)0.46875F, (double)direction.getOffsetZ() * (double)0.46875F);
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(bigItemDisplayEntity.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bigItemDisplayEntity.getYaw()));
        boolean bl = bigItemDisplayEntity.isInvisible();
        ItemStack itemStack = bigItemDisplayEntity.getHeldItemStack();
        if (!bl) {
            BakedModelManager bakedModelManager = this.blockRenderManager.getModels().getModelManager();
            ModelIdentifier modelIdentifier = this.getModelId(bigItemDisplayEntity, itemStack);
            matrixStack.push();
            matrixStack.translate(-0.5F, -0.5F, -0.5F);
            this.blockRenderManager.getModelRenderer().render(matrixStack.peek(), vertexConsumerProvider.getBuffer(TexturedRenderLayers.getEntitySolid()), (BlockState)null, bakedModelManager.getModel(modelIdentifier), 1.0F, 1.0F, 1.0F, i, OverlayTexture.DEFAULT_UV);
            matrixStack.pop();
        }

        if (!itemStack.isEmpty()) {
            OptionalInt optionalInt = bigItemDisplayEntity.getMapId();
            if (bl) {
                matrixStack.translate(0.0F, 0.0F, 0.5F);
            } else {
                matrixStack.translate(0.0F, 0.0F, 0.4375F);
            }

            int j = optionalInt.isPresent() ? bigItemDisplayEntity.getRotation() % 4 * 2 : bigItemDisplayEntity.getRotation();
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)j * 360.0F / 8.0F));
            if (optionalInt.isPresent()) {
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
                float h = 0.0078125F;
                matrixStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
                matrixStack.translate(-64.0F, -64.0F, 0.0F);
                MapState mapState = FilledMapItem.getMapState(optionalInt.getAsInt(), bigItemDisplayEntity.getWorld());
                matrixStack.translate(0.0F, 0.0F, -1.0F);
                if (mapState != null) {
                    int k = this.getLight(bigItemDisplayEntity, 15728850, i);
                    MinecraftClient.getInstance().gameRenderer.getMapRenderer().draw(matrixStack, vertexConsumerProvider, optionalInt.getAsInt(), mapState, true, k);
                }
            } else {
                int l = this.getLight(bigItemDisplayEntity, 15728880, i);
                matrixStack.scale(0.5F, 0.5F, 0.5F);
                this.itemRenderer.renderItem(itemStack, ModelTransformationMode.FIXED, l, OverlayTexture.DEFAULT_UV, matrixStack, vertexConsumerProvider, bigItemDisplayEntity.getWorld(), bigItemDisplayEntity.getId());
            }
        }

        matrixStack.pop();
    }
}
