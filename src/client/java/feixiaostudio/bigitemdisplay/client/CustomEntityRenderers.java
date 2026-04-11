package feixiaostudio.bigitemdisplay.client;

import feixiaostudio.bigitemdisplay.CustomEntityType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

@Environment(EnvType.CLIENT)
public class CustomEntityRenderers {

    public static void initialize() {
        EntityRendererRegistry.register(CustomEntityType.BIG_ITEM_DISPLAY, BigItemDisplayEntityRenderer::new);
    }
}
