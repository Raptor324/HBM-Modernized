//? if fabric {
package com.hbm_m.capability;

import com.hbm_m.lib.RefStrings;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric-only CCA регистрация component'а радиации игрока.
 *
 * Подключается через fabric.mod.json entrypoint:
 * "cardinal-components-entity": ["com.hbm_m.capability.FabricPlayerComponents"]
 */
public final class FabricPlayerComponents implements EntityComponentInitializer {
    public static final ComponentKey<IPlayerRadiationComponent> PLAYER_RADIATION =
            ComponentRegistry.getOrCreate(
                    new ResourceLocation(RefStrings.MODID, "player_radiation"),
                    IPlayerRadiationComponent.class
            );

    /**
     * Общий "persistent data" игрока для Fabric (аналог Forge getPersistentData()).
     * Используется для редких серверных счётчиков/кэшей (tick timers, cached flags).
     */
    public static final ComponentKey<IPersistentDataComponent> PLAYER_PERSISTENT_DATA =
            ComponentRegistry.getOrCreate(
                    new ResourceLocation(RefStrings.MODID, "player_persistent_data"),
                    IPersistentDataComponent.class
            );

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // Пишем/читаем компонент как часть данных Player (persist across relogs).
        // COPY/RESPAWN: радиацию сбрасываем (не копируем) — PlayerHandler отдельно делает reset на respawn.
        registry.registerForPlayers(PLAYER_RADIATION, player -> new PlayerRadiationComponent(), RespawnCopyStrategy.NEVER_COPY);
        registry.registerForPlayers(PLAYER_PERSISTENT_DATA, player -> new PlayerPersistentDataComponent(), RespawnCopyStrategy.ALWAYS_COPY);
    }

    public interface IPlayerRadiationComponent extends AutoSyncedComponent {
        float getRads();
        void setRads(float rads);

        void readFromNbt(CompoundTag tag);
        void writeToNbt(CompoundTag tag);
    }

    public static final class PlayerRadiationComponent implements IPlayerRadiationComponent {
        private static final String NBT_KEY = "radiationLevel";

        private float rads = 0.0F;

        @Override
        public float getRads() {
            return rads;
        }

        @Override
        public void setRads(float rads) {
            this.rads = Math.max(0.0F, rads);
        }

        @Override
        public void readFromNbt(CompoundTag tag) {
            if (tag.contains(NBT_KEY, Tag.TAG_FLOAT)) {
                setRads(tag.getFloat(NBT_KEY));
            } else {
                setRads(0.0F);
            }
        }

        @Override
        public void writeToNbt(CompoundTag tag) {
            float value = getRads();
            if (value > 1e-6F) {
                tag.putFloat(NBT_KEY, value);
            }
        }

        @Override
        public boolean shouldSyncWith(ServerPlayer player) {
            // Client HUD uses network packet; no need to auto-sync this component.
            return false;
        }
    }

    public interface IPersistentDataComponent extends AutoSyncedComponent {
        CompoundTag getData();
    }

    public static final class PlayerPersistentDataComponent implements IPersistentDataComponent {
        private static final String NBT_KEY = "data";

        private CompoundTag data = new CompoundTag();

        @Override
        public CompoundTag getData() {
            return data;
        }

        @Override
        public void readFromNbt(CompoundTag tag) {
            if (tag.contains(NBT_KEY, Tag.TAG_COMPOUND)) {
                data = tag.getCompound(NBT_KEY).copy();
            } else {
                data = new CompoundTag();
            }
        }

        @Override
        public void writeToNbt(CompoundTag tag) {
            if (!data.isEmpty()) {
                tag.put(NBT_KEY, data.copy());
            }
        }

        @Override
        public boolean shouldSyncWith(ServerPlayer player) {
            return false;
        }
    }

    public static float get(Player player) {
        return PLAYER_RADIATION.get(player).getRads();
    }

    public static void set(Player player, float rads) {
        PLAYER_RADIATION.get(player).setRads(rads);
    }

    public static CompoundTag getPersistentData(Player player) {
        return PLAYER_PERSISTENT_DATA.get(player).getData();
    }
}
//?}

