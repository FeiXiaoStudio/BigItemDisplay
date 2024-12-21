package hrcaoc.bigitemdisplay;

import net.minecraft.data.client.Model;
import net.minecraft.data.client.TextureKey;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class CustomModels {
    public static final Model NORMAL_DISPLAY = block("big_item_display", "map=false", TextureKey.PARTICLE, TextureKey.BACK);

    private static Model make(TextureKey... requiredTextureKeys) {
        return new Model(Optional.empty(), Optional.empty(), requiredTextureKeys);
    }

    private static Model item(String parent, TextureKey... requiredTextureKeys) {
        return new Model(Optional.of(new Identifier(BigItemDisplay.MOD_ID, "item/" + parent)), Optional.empty(), requiredTextureKeys);
    }

    private static Model block(String parent, TextureKey... requiredTextureKeys) {
        return new Model(Optional.of(new Identifier(BigItemDisplay.MOD_ID, "block/" + parent)), Optional.empty(), requiredTextureKeys);
    }

    private static Model block(String parent, String variant, TextureKey... requiredTextureKeys) {
        return new Model(Optional.of(new Identifier(BigItemDisplay.MOD_ID, "block/" + parent)), Optional.of(variant), requiredTextureKeys);
    }

    public static void initialize() {}
}
