package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class MachineFluidTankBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    public MachineFluidTankBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
    }

    @Override
    public String[] getPartNames() {
        // Эти имена должны совпадать с названиями групп/объектов внутри вашего .obj файла!
        return new String[]{"Frame", "Tank"};
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // Возвращаем true, потому что рендер в мире берет на себя BlockEntityRenderer.
        return true; 
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        // state == null означает, что модель рендерится как ПРЕДМЕТ (в инвентаре, в руке, на полу)
        if (state == null) {
            List<BakedQuad> quads = new ArrayList<>();
            BakedModel frame = getPart("Frame");
            BakedModel tank = getPart("Tank");
            
            if (frame != null) quads.addAll(frame.getQuads(null, side, rand, modelData, renderType));
            if (tank != null) quads.addAll(tank.getQuads(null, side, rand, modelData, renderType));
            
            return quads;
        }
        
        // В мире рисуется через BER, поэтому ничего не возвращаем
        return super.getQuads(state, side, rand, modelData, renderType);
    }
}