package feixiaostudio.bigitemdisplay;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BigItemDisplayEntityType {
    public static final EntityType<BigItemDisplayEntity> BIG_ITEM_DISPLAY = register(
            "big_item_display",
            EntityType.Builder.<BigItemDisplayEntity>create(BigItemDisplayEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .maxTrackingRange(10)
                    .trackingTickInterval(Integer.MAX_VALUE)
    );

    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, Identifier.of(BigItemDisplay.MOD_ID, id), type.build(id));
    }

    public static void initialize() {}
}
