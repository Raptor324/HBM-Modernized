package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class FT_VentRadiation extends FluidTrait {
    
    float radPerMB = 0;
    
    public FT_VentRadiation() { }
    
    public FT_VentRadiation(float rad) {
        this.radPerMB = rad;
    }
    
    public float getRadPerMB() {
        return this.radPerMB;
    }
    
    @Override
    public void onFluidRelease(Level level, BlockPos pos, FluidTank tank, int overflowAmount, FluidReleaseType type) {
        // 1:1 ChunkRadiationManager.proxy.incrementRad(world, x, y, z, overflowAmount * radPerMB) - nur Aufruf der oeffentlichen API
        com.hbm_m.radiation.ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), overflowAmount * radPerMB);
    }
    
    @Override
    public void addInfo(List<Component> info) {
        info.add(Component.literal("[").append(Component.translatable("hbmfluid.trait.radioactive")).append("]").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("radiation").value(radPerMB);
    }
    
    @Override
    public void deserializeJSON(JsonObject obj) {
        this.radPerMB = obj.get("radiation").getAsFloat();
    }
}