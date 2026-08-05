//? if neoforge {
/*package com.hbm_m.capability;

import com.hbm_m.interfaces.IChunkRadiation;
import com.hbm_m.lib.RefStrings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, RefStrings.MODID);

    public static final Supplier<AttachmentType<IChunkRadiation>> CHUNK_RADIATION = ATTACHMENT_TYPES.register("chunk_radiation",
        () -> AttachmentType.builder(() -> (IChunkRadiation) new ChunkRadiation())
            .serialize(new IAttachmentSerializer<CompoundTag, IChunkRadiation>() {
                @Override
                public IChunkRadiation read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                    IChunkRadiation rad = new ChunkRadiation();
                    rad.setAmbientRadiation(tag.contains("ambientRadiation") ? tag.getFloat("ambientRadiation") : 0f);
                    return rad;
                }
                @Override
                public CompoundTag write(IChunkRadiation rad, HolderLookup.Provider provider) {
                    CompoundTag tag = new CompoundTag();
                    float ambient = rad.getAmbientRadiation();
                    if (ambient > 1e-6F) tag.putFloat("ambientRadiation", ambient);
                    return tag;
                }
            }).build()
    );
}
*///?}