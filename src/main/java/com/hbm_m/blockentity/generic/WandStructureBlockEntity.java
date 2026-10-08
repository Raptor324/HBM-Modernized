package com.hbm_m.blockentity.generic;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.world.gen.nbt.NBTStructure;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code BlockWandStructure.TileEntityWandStructure} (tileentity_wand_structure): Name, Groesse und Ausschlussliste
 * eines Strukturbereichs oberhalb des Blocks; speichert ihn als .nbt bzw. laedt eine Datei und baut sie auf.
 */
public class WandStructureBlockEntity extends BaseHbmBlockEntity implements IControlReceiver {

    public String name = "";

    public int sizeX = 1;
    public int sizeY = 1;
    public int sizeZ = 1;

    public Set<BlockState> blacklist = new HashSet<>();

    public WandStructureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAND_STRUCTURE.get(), pos, state);
    }

    /** Original {@code networkPackNT}: Zustand an die Clients schicken. */
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void saveStructure(Player player) {
        if (name.isEmpty()) {
            player.sendSystemMessage(Component.literal("Could not save: invalid name").withStyle(ChatFormatting.RED));
            return;
        }

        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            player.sendSystemMessage(Component.literal("Could not save: invalid dimensions").withStyle(ChatFormatting.RED));
            return;
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        boolean hadAir = blacklist.contains(air);
        blacklist.add(air);

        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        File file = NBTStructure.quickSaveArea(name + ".nbt", level, x, y + 1, z, x + sizeX - 1, y + sizeY, z + sizeZ - 1, blacklist);

        if (!hadAir) blacklist.remove(air);

        if (file == null) {
            player.sendSystemMessage(Component.literal("Failed to save structure").withStyle(ChatFormatting.RED));
            return;
        }

        Component fileText = Component.literal(file.getName()).withStyle(s -> s
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getParentFile().getAbsolutePath()))
                .withUnderlined(true));

        player.sendSystemMessage(Component.literal("Saved structure as ").append(fileText));
    }

    public void loadStructure(Player player) {
        if (name.isEmpty()) {
            player.sendSystemMessage(Component.literal("Could not load: no filename specified").withStyle(ChatFormatting.RED));
            return;
        }

        File structureFile = new File(NBTStructure.getStructureDirectory(), name + ".nbt");

        boolean debug = !level.hasNeighborSignal(worldPosition);
        boolean previousDebug = ModClothConfig.get().structureDebug;
        ModClothConfig.get().structureDebug = debug;

        try {
            NBTStructure structure = new NBTStructure(structureFile);

            sizeX = structure.getSizeX();
            sizeY = structure.getSizeY();
            sizeZ = structure.getSizeZ();

            structure.build(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), 0, false, true);

            // Original: Meta 0 -> der Block wird zum Speicherblock und behaelt Name, Groesse und Ausschlussliste
            String keepName = name;
            int kx = sizeX, ky = sizeY, kz = sizeZ;
            Set<BlockState> keepBlacklist = blacklist;
            level.setBlock(worldPosition, ModBlocks.WAND_STRUCTURE_SAVE.get().defaultBlockState(), 3);
            if (level.getBlockEntity(worldPosition) instanceof WandStructureBlockEntity saved) {
                saved.name = keepName;
                saved.sizeX = kx;
                saved.sizeY = ky;
                saved.sizeZ = kz;
                saved.blacklist = keepBlacklist;
                saved.sync();
            }

            player.sendSystemMessage(Component.literal("Structure loaded"));

        } catch (FileNotFoundException ex) {
            player.sendSystemMessage(Component.literal("Could not load: file not found").withStyle(ChatFormatting.RED));
        } finally {
            ModClothConfig.get().structureDebug = previousDebug;
        }
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putString("name", name);

        nbt.putInt("sizeX", sizeX);
        nbt.putInt("sizeY", sizeY);
        nbt.putInt("sizeZ", sizeZ);

        ListTag list = new ListTag();
        for (BlockState state : blacklist) list.add(NbtUtils.writeBlockState(state));
        nbt.put("blacklist", list);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        name = nbt.getString("name");

        sizeX = nbt.getInt("sizeX");
        sizeY = nbt.getInt("sizeY");
        sizeZ = nbt.getInt("sizeZ");

        blacklist = new HashSet<>();
        ListTag list = nbt.getList("blacklist", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            blacklist.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), list.getCompound(i)));
        }
    }

    /** Fuer die GUI: kompletter Datensatz wie {@code writeToNBT}. */
    public CompoundTag writeSettings() {
        CompoundTag tag = new CompoundTag();
        writeNbtData(tag, null);
        return tag;
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) { }

    @Override
    public void receiveControl(Player player, CompoundTag nbt) {
        readNbtData(nbt, null);
        sync();

        if (nbt.getBoolean("save")) {
            saveStructure(player);
        }

        if (nbt.getBoolean("load")) {
            loadStructure(player);
        }
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return AABB.ofSize(worldPosition.getCenter(), 1, 1, 1).expandTowards(sizeX, sizeY + 1, sizeZ).inflate(1);
    }
}
