//? if fabric {
/*package com.hbm_m.capability;

import com.hbm_m.interfaces.IChunkRadiation;
import com.hbm_m.lib.RefStrings;

import dev.onyxstudios.cca.api.v3.chunk.ChunkComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.chunk.ChunkComponentInitializer;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/^*
 * Fabric-only CCA регистрация chunk radiation.
 *
 * ВАЖНО: этот класс должен быть добавлен в fabric entrypoints
 * ("cardinal-components-chunk") в `fabric.mod.json`.
 ^/
public final class FabricChunkComponents implements ChunkComponentInitializer {
    public static final ComponentKey<IChunkRadiationComponent> CHUNK_RADIATION =
            ComponentRegistry.getOrCreate(new ResourceLocation(RefStrings.MODID, "chunk_radiation"), IChunkRadiationComponent.class);

    @Override
    public void registerChunkComponentFactories(ChunkComponentFactoryRegistry registry) {
        registry.register(CHUNK_RADIATION, chunk -> new ChunkRadiationComponent());
    }

    /^*
     * Узкий тип компонента для CCA (IChunkRadiation + Component lifecycle).
     ^/
    public interface IChunkRadiationComponent extends IChunkRadiation, AutoSyncedComponent {
        void readFromNbt(CompoundTag tag);
        void writeToNbt(CompoundTag tag);
    }

    public static final class ChunkRadiationComponent implements IChunkRadiationComponent {
        private static final String NBT_KEY_BLOCK = "blockRadiation";
        private static final String NBT_KEY_AMBIENT = "ambientRadiation";

        private final ChunkRadiation delegate = new ChunkRadiation();

        @Override
        public float getBlockRadiation() {
            return delegate.getBlockRadiation();
        }

        @Override
        public void setBlockRadiation(float value) {
            delegate.setBlockRadiation(value);
        }

        @Override
        public float getAmbientRadiation() {
            return delegate.getAmbientRadiation();
        }

        @Override
        public void setAmbientRadiation(float value) {
            delegate.setAmbientRadiation(value);
        }

        @Override
        public void copyFrom(IChunkRadiation source) {
            delegate.copyFrom(source);
        }

        @Override
        public void readFromNbt(CompoundTag tag) {
            if (tag.contains(NBT_KEY_BLOCK, Tag.TAG_FLOAT)) {
                setBlockRadiation(tag.getFloat(NBT_KEY_BLOCK));
            } else {
                setBlockRadiation(0);
            }

            if (tag.contains(NBT_KEY_AMBIENT, Tag.TAG_FLOAT)) {
                setAmbientRadiation(tag.getFloat(NBT_KEY_AMBIENT));
            } else {
                setAmbientRadiation(0);
            }
        }

        @Override
        public void writeToNbt(CompoundTag tag) {
            float block = getBlockRadiation();
            float ambient = getAmbientRadiation();
            if (block > 1e-6F) tag.putFloat(NBT_KEY_BLOCK, block);
            if (ambient > 1e-6F) tag.putFloat(NBT_KEY_AMBIENT, ambient);
        }

        @Override
        public boolean shouldSyncWith(ServerPlayer player) {
            // Сейчас синхронизация на клиента не требуется для базовой логики симуляции.
            // Отладка/оверлей может добавить отдельный sync-пакет.
            return false;
        }
    }
}
*///?}

