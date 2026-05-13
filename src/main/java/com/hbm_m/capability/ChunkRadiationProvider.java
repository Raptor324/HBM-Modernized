//? if forge {
package com.hbm_m.capability;

// Данный класс предоставляет capability для хранения данных о радиации в чанке.
// Он реализует ICapabilitySerializable для сохранения и загрузки данных в NBT тег чанка.
// Данные включают уровень радиации от блоков и фоновую радиацию.
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.interfaces.IChunkRadiation;
import com.hbm_m.main.MainRegistry;

public class ChunkRadiationProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IChunkRadiation> CHUNK_RADIATION_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private static final String NBT_KEY_BLOCK = "blockRadiation";
    private static final String NBT_KEY_AMBIENT = "ambientRadiation";

    private IChunkRadiation chunkRadiation = null;
    private final LazyOptional<IChunkRadiation> optional = LazyOptional.of(this::getOrCreate);

    private IChunkRadiation getOrCreate() {
        if (this.chunkRadiation == null) {
            this.chunkRadiation = new ChunkRadiation();
        }
        return this.chunkRadiation;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CHUNK_RADIATION_CAPABILITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        IChunkRadiation radiation = getOrCreate();
        if (radiation.getBlockRadiation() > 1e-6F || radiation.getAmbientRadiation() > 1e-6F) {
            CompoundTag tag = new CompoundTag();
            if (radiation.getBlockRadiation() > 1e-6F) {
                tag.putFloat(NBT_KEY_BLOCK, radiation.getBlockRadiation());
            }
            if (radiation.getAmbientRadiation() > 1e-6F) {
                tag.putFloat(NBT_KEY_AMBIENT, radiation.getAmbientRadiation());
            }

            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.debug("Serializing ChunkRadiation: {}", tag);
            }
            return tag;
        }

        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        IChunkRadiation radiation = getOrCreate();
        if (nbt.contains(NBT_KEY_BLOCK, Tag.TAG_FLOAT)) {
            radiation.setBlockRadiation(nbt.getFloat(NBT_KEY_BLOCK));
        } else {
            radiation.setBlockRadiation(0);
        }

        if (nbt.contains(NBT_KEY_AMBIENT, Tag.TAG_FLOAT)) {
            radiation.setAmbientRadiation(nbt.getFloat(NBT_KEY_AMBIENT));
        } else {
            radiation.setAmbientRadiation(0);
        }

        if (ModClothConfig.get().enableDebugLogging) {
            if (nbt.size() > 0) {
                MainRegistry.LOGGER.debug("Deserialized ChunkRadiation: block={}, ambient={}",
                        radiation.getBlockRadiation(), radiation.getAmbientRadiation());
            }
        }
    }
}
//?}