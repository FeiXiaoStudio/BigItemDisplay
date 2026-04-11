package feixiaostudio.bigitemdisplay.client;

import feixiaostudio.bigitemdisplay.BigItemDisplayEntityType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

@Environment(EnvType.CLIENT)
public class BigItemDisplayEntityRenderers {

    public static void initialize() {
        EntityRendererRegistry.register(BigItemDisplayEntityType.BIG_ITEM_DISPLAY, BigItemDisplayEntityRenderer::new);
    }
}
