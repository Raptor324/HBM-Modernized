package com.hbm_m.interfaces;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.IFluidUserMK2;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * Порт com.hbm.tileentity.IFluidCopiable (1.7.10): копирка типов жидкостей в баках.
 * Оригинал хранил числовые fluid-ID; в этом порте типов жидкостей у Fluid нет —
 * поэтому в NBT пишутся registry-имена (список строк "fluids"), семантика та же:
 * обычная вставка берёт тип по индексу (первый принимающий бак).
 */
public interface IFluidCopiable extends ICopiable {

    /** Оригинал getFluidIDToCopy: типы всех непустых баков. */
    default List<String> getFluidsToCopy() {
        IFluidUserMK2 tile = (IFluidUserMK2) this;
        List<String> types = new ArrayList<>();
        for (FluidTank tank : tile.getAllTanks()) {
            Fluid type = tank.getTankType();
            if (type != Fluids.EMPTY && type != ModFluids.NONE.getSource())
                types.add(BuiltInRegistries.FLUID.getKey(type).toString());
        }
        return types;
    }

    /**
     * Оригинал getTankToPaste: первый принимающий бак.
     * Здесь принимаем и "чистый" ресивер — многие машины порта реализуют
     * только IFluidStandardReceiverMK2 (в 1.7.10 трансивер был у всех).
     */
    default FluidTank getTankToPaste() {
        if (this instanceof IFluidStandardTransceiverMK2 transceiver) {
            FluidTank[] tanks = transceiver.getReceivingTanks();
            return tanks != null && tanks.length > 0 ? tanks[0] : null;
        }
        if (this instanceof IFluidStandardReceiverMK2 receiver) {
            FluidTank[] tanks = receiver.getReceivingTanks();
            return tanks != null && tanks.length > 0 ? tanks[0] : null;
        }
        return null;
    }

    @Override
    default CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        List<String> types = getFluidsToCopy();
        if (!types.isEmpty()) {
            ListTag list = new ListTag();
            for (String type : types) list.add(StringTag.valueOf(type));
            nbt.put("fluids", list);
        }
        return nbt;
    }

    @Override
    default void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        FluidTank tank = getTankToPaste();
        if (tank == null) return;
        ListTag list = nbt.getList("fluids", Tag.TAG_STRING);
        if (!list.isEmpty() && index < list.size()) {
            tank.setTankType(fluidFromString(list.getString(index)));
        }
    }

    @Override
    default String[] infoForDisplay(Level level, BlockPos pos) {
        List<String> types = getFluidsToCopy();
        String[] names = new String[types.size()];
        for (int i = 0; i < types.size(); i++) {
            names[i] = FluidType.forFluid(fluidFromString(types.get(i))).getUnlocalizedName();
        }
        return names;
    }

    static Fluid fluidFromString(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        return id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
    }
}
