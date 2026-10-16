package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineBatterySocketBlock;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

public class MachineBatterySocketBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    public MachineBatterySocketBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
    }

    @Override
    public String[] getPartNames() {
        return new String[] { "Socket", "Battery", "Capacitor" };
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        return false;
    }

    // На NeoForge подклассы переопределяют getQuadsForModelDataNeo; forge-only override означал,
    // что база рисовала обе части всегда и без поворота по FACING.
    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
            ModelData modelData, @Nullable RenderType renderType) {
    //?} elif neoforge {
    /*@Override
    protected List<BakedQuad> getQuadsForModelDataNeo(@Nullable BlockState state, @Nullable Direction side,
            RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
    *///?}
        List<BakedQuad> quads = new ArrayList<>();
        int rotationY = getRotationYForFacing(state);
        Direction querySide = getUnrotatedSide(side, rotationY);

        BakedModel socket = getPart("Socket");
        if (socket != null) {
            List<BakedQuad> socketQuads = socket.getQuads(state, querySide, rand, modelData, renderType);
            quads.addAll(rotationY != 0 ? ModelHelper.transformQuadsByFacing(socketQuads, rotationY) : socketQuads);
        }


        return quads;
    }


    private static int getRotationYForFacing(@Nullable BlockState state) {
        if (state == null || !state.hasProperty(MachineBatterySocketBlock.FACING)) return 0;
        return (switch (state.getValue(MachineBatterySocketBlock.FACING)) {
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        }) % 360;
    }

    private static Direction getUnrotatedSide(@Nullable Direction side, int rotationY) {
        if (side == null || side.getAxis() == Direction.Axis.Y || rotationY == 0) return side;
        int steps = (4 - (rotationY / 90)) % 4;
        Direction r = side;
        for (int i = 0; i < steps; i++) {
            r = r.getClockWise(Direction.Axis.Y);
        }
        return r;
    }

}
