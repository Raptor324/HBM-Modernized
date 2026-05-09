package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.machines.BatterySocketBlockEntity;
import com.hbm_m.block.machines.MachineBatterySocketBlock;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.Minecraft;
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
/*import net.minecraftforge.client.model.data.ModelData;
*///?}

public class MachineBatterySocketBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private static final ResourceLocation BATTERY_TEX = RefStrings.resourceLocation("block/machines/battery_socket");

    private final Map<Object, List<BakedQuad>> batteryQuadCache = new ConcurrentHashMap<>();
    private static final Object NULL_SIDE_KEY = new Object();

    public MachineBatterySocketBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
    }

    @Override
    public String[] getPartNames() {
        return new String[] { "Socket", "Battery" };
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        return false;
    }

    //? if forge {
    /*@Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
            ModelData modelData, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();
        int rotationY = getRotationYForFacing(state);
        Direction querySide = getUnrotatedSide(side, rotationY);

        BakedModel socket = getPart("Socket");
        if (socket != null) {
            List<BakedQuad> socketQuads = socket.getQuads(state, querySide, rand, modelData, renderType);
            quads.addAll(rotationY != 0 ? ModelHelper.transformQuadsByFacing(socketQuads, rotationY) : socketQuads);
        }

        boolean showBattery = modelData != null && Boolean.TRUE.equals(modelData.get(BatterySocketBlockEntity.HAS_INSERT));
        if (showBattery) {
            BakedModel battery = getPart("Battery");
            if (battery != null) {
                List<BakedQuad> batteryQuads = getRetexturedBatteryQuads(battery, querySide, rand);
                quads.addAll(rotationY != 0 ? ModelHelper.transformQuadsByFacing(batteryQuads, rotationY) : batteryQuads);
            }
        }

        return quads;
    }
    *///?}

    //? if fabric {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        List<BakedQuad> quads = new ArrayList<>();
        int rotationY = getRotationYForFacing(state);
        Direction querySide = getUnrotatedSide(side, rotationY);

        BakedModel socket = getPart("Socket");
        if (socket != null) {
            List<BakedQuad> socketQuads = socket.getQuads(state, querySide, rand);
            quads.addAll(rotationY != 0 ? ModelHelper.transformQuadsByFacing(socketQuads, rotationY) : socketQuads);
        }

        boolean showBattery = Boolean.TRUE.equals(FabricRenderDataBridge.get());
        if (showBattery) {
            BakedModel battery = getPart("Battery");
            if (battery != null) {
                List<BakedQuad> batteryQuads = getRetexturedBatteryQuads(battery, querySide, rand);
                quads.addAll(rotationY != 0 ? ModelHelper.transformQuadsByFacing(batteryQuads, rotationY) : batteryQuads);
            }
        }
        return quads;
    }
    //?}

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

    private List<BakedQuad> getRetexturedBatteryQuads(BakedModel battery, @Nullable Direction side, RandomSource rand) {
        Object key = side == null ? NULL_SIDE_KEY : side;
        return batteryQuadCache.computeIfAbsent(key, k -> {
            List<BakedQuad> out = new ArrayList<>();
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(BATTERY_TEX);
            //? if forge {
            /*for (BakedQuad q : battery.getQuads(null, side, rand, ModelData.EMPTY, null)) {
                out.add(retextureQuad(q, sprite));
            }
            *///?}
            //? if fabric {
            for (BakedQuad q : battery.getQuads(null, side, rand)) {
                out.add(retextureQuad(q, sprite));
            }
            //?}
            return out;
        });
    }

    private static BakedQuad retextureQuad(BakedQuad original, TextureAtlasSprite newSprite) {
        int[] oldData = original.getVertices();
        int[] newData = new int[oldData.length];
        System.arraycopy(oldData, 0, newData, 0, oldData.length);
        TextureAtlasSprite oldSprite = original.getSprite();
        if (oldSprite == null) return original;
        float oldUDiff = oldSprite.getU1() - oldSprite.getU0();
        float oldVDiff = oldSprite.getV1() - oldSprite.getV0();
        float newUDiff = newSprite.getU1() - newSprite.getU0();
        float newVDiff = newSprite.getV1() - newSprite.getV0();
        if (oldUDiff == 0 || oldVDiff == 0) return original;
        int vertexSize = oldData.length / 4;
        for (int i = 0; i < 4; i++) {
            int offset = i * vertexSize;
            float oldU = Float.intBitsToFloat(oldData[offset + 4]);
            float oldV = Float.intBitsToFloat(oldData[offset + 5]);
            float normU = (oldU - oldSprite.getU0()) / oldUDiff;
            float normV = (oldV - oldSprite.getV0()) / oldVDiff;
            newData[offset + 4] = Float.floatToRawIntBits(newSprite.getU0() + normU * newUDiff);
            newData[offset + 5] = Float.floatToRawIntBits(newSprite.getV0() + normV * newVDiff);
        }
        return new BakedQuad(newData, original.getTintIndex(), original.getDirection(), newSprite, original.isShade());
    }
}
