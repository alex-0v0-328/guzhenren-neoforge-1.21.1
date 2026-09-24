package com.unknown.guzhenren.registry.entity;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.BoarGuEntity;
import com.unknown.guzhenren.entity.HopeGuEntity;
import com.unknown.guzhenren.entity.RhinocerosBeetleGuEntity;
import com.unknown.guzhenren.entity.WildBoarEntity;
import com.unknown.guzhenren.registry.item.ModItems;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The entity types this mod registers.
 *
 * <p>Hope Gu, three boar Gu and four rhinoceros beetle Gu variants are naturally spawning ambient entities;
 * the wild boar is a naturally spawning creature. Hope Gu's client mote is emitted by its entity class.
 * Gu capture is a bare right click and is never gated on awakening [开窍]; the wild boar has no capture path.
 *
 * @author Alex
 * @version 1.0.0
 * @see HopeGuEntity
 * @since 1.0.0
 */

public final class ModEntityTypes {

    private ModEntityTypes() {}
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Guzhenren.MOD_ID);
    private static final float MOTE_WIDTH = 0.4F;
    private static final float MOTE_HEIGHT = 0.4F;
    private static final int TRACKING_CHUNKS = 8;
    private static final float WILD_BOAR_WIDTH = 1.1F;
    private static final float WILD_BOAR_HEIGHT = 1.2F;
    public static final DeferredHolder<EntityType<?>, EntityType<HopeGuEntity>> HOPE_GU_ENTITY =
            ENTITY_TYPES.register("hope_gu_entity", () -> EntityType.Builder
                    .<HopeGuEntity>of((type, level) ->
                                    new HopeGuEntity(type, level, ModItems.HOPE_GU),
                            MobCategory.AMBIENT)
                    .sized(MOTE_WIDTH, MOTE_HEIGHT)
                    .clientTrackingRange(TRACKING_CHUNKS)
                    .build("hope_gu_entity"));
    public static final DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> WHITE_BOAR_GU_ENTITY =
            boarGu("white_boar_gu_entity", ModItems.WHITE_BOAR_GU);
    public static final DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> BLACK_BOAR_GU_ENTITY =
            boarGu("black_boar_gu_entity", ModItems.BLACK_BOAR_GU);
    public static final DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> FLOWER_BOAR_GU_ENTITY =
            boarGu("flower_boar_gu_entity", ModItems.FLOWER_BOAR_GU);
    public static final DeferredHolder<EntityType<?>, EntityType<RhinocerosBeetleGuEntity>> HORIZONTAL_CRASH_GU_ENTITY =
            beetle("horizontal_crash_gu_entity", ModItems.HORIZONTAL_CRASH_GU);
    public static final DeferredHolder<EntityType<?>, EntityType<RhinocerosBeetleGuEntity>> VERTICAL_CRASH_GU_ENTITY =
            beetle("vertical_crash_gu_entity", ModItems.VERTICAL_CRASH_GU);
    public static final DeferredHolder<EntityType<?>, EntityType<RhinocerosBeetleGuEntity>> CHARGING_CRASH_GU_4_ENTITY =
            beetle("charging_crash_gu_4_entity", ModItems.CHARGING_CRASH_GU_4);
    public static final DeferredHolder<EntityType<?>, EntityType<RhinocerosBeetleGuEntity>> CHARGING_CRASH_GU_5_ENTITY =
            beetle("charging_crash_gu_5_entity", ModItems.CHARGING_CRASH_GU_5);
    public static final DeferredHolder<EntityType<?>, EntityType<WildBoarEntity>> WILD_BOAR =
            ENTITY_TYPES.register("wild_boar", () -> EntityType.Builder
                    .<WildBoarEntity>of(WildBoarEntity::new, MobCategory.CREATURE)
                    .sized(WILD_BOAR_WIDTH, WILD_BOAR_HEIGHT)
                    .clientTrackingRange(TRACKING_CHUNKS)
                    .build("wild_boar"));
    private static DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> boarGu(String name, Supplier<Item> caughtGu) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .<BoarGuEntity>of((type, level) -> new BoarGuEntity(type, level, caughtGu), MobCategory.AMBIENT)
                .sized(MOTE_WIDTH, MOTE_HEIGHT)
                .clientTrackingRange(TRACKING_CHUNKS)
                .build(name));
    }
    private static DeferredHolder<EntityType<?>, EntityType<RhinocerosBeetleGuEntity>> beetle(
            String name, Supplier<Item> caughtGu) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .<RhinocerosBeetleGuEntity>of((type, level) -> new RhinocerosBeetleGuEntity(type, level, caughtGu),
                        MobCategory.AMBIENT)
                .sized(MOTE_WIDTH, MOTE_HEIGHT)
                .clientTrackingRange(TRACKING_CHUNKS)
                .build(name));
    }
    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
