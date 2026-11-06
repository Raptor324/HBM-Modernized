package com.hbm_m.client.render.weapon;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.AbstractMultipartBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Composite BakedModel for firearms and handheld weapons.
 * Holds named BakedModel parts parsed via native ObjModel loader.
 * World chunk rendering is skipped; geometry is drawn via Nucleus VBO or BEWLR.
 */
@OnlyIn(Dist.CLIENT)
public class WeaponMultipartBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private final ResourceLocation modelLocation;
    private final String[] partNames;
    private final Set<String> partNameSet;

    public WeaponMultipartBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, ResourceLocation modelLocation) {
        super(parts, transforms != null ? transforms : ItemTransforms.NO_TRANSFORMS);
        this.modelLocation = modelLocation;
        this.partNames = parts.keySet().stream().sorted().toArray(String[]::new);
        this.partNameSet = Set.of(this.partNames);
    }

    @Nullable
    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    @Override
    public String[] getPartNames() {
        return partNames;
    }

    public Set<String> getPartNameSet() {
        return partNameSet;
    }

    public Set<String> getAvailablePartNames() {
        return Collections.unmodifiableSet(parts.keySet());
    }

    public boolean hasPart(String name) {
        return parts.containsKey(name);
    }

    public Map<String, BakedModel> getParts() {
        return Collections.unmodifiableMap(parts);
    }

    public Map<String, BakedModel> getPartModels() {
        return Collections.unmodifiableMap(parts);
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // Weapons never render as static world block chunks
        return state != null;
    }

    @Override
    public boolean isCustomRenderer() {
        // Handled via GunItemRenderer (BEWLR) / GunGL
        return true;
    }
}
