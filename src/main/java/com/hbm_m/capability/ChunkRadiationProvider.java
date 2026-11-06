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

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        IChunkRadiation radiation = getOrCreate();
        CompoundTag tag = new CompoundTag();
        if (radiation instanceof ChunkRadiation cr) {
            cr.saveNBT(tag);
        } else if (radiation.getAmbientRadiation() > 1e-6F) {
            tag.putFloat(ChunkRadiation.NBT_KEY_AMBIENT, radiation.getAmbientRadiation());
        }

        if (ModClothConfig.get().enableDebugLogging && tag.contains(ChunkRadiation.NBT_KEY_AMBIENT)) {
            MainRegistry.LOGGER.debug("Serializing ChunkRadiation: {}", tag);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        IChunkRadiation radiation = getOrCreate();
        if (radiation instanceof ChunkRadiation cr) {
            cr.loadNBT(nbt);
        } else {
            radiation.setAmbientRadiation(nbt != null && nbt.contains(ChunkRadiation.NBT_KEY_AMBIENT, Tag.TAG_FLOAT) ? nbt.getFloat(ChunkRadiation.NBT_KEY_AMBIENT) : 0F);
        }

        if (ModClothConfig.get().enableDebugLogging && nbt != null && nbt.size() > 0) {
            MainRegistry.LOGGER.debug("Deserialized ChunkRadiation: ambient={}", radiation.getAmbientRadiation());
        }
    }
}
//?}