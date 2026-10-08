package com.hbm_m.blockentity.decorations;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.tile.IRepairable;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.missile.EntityBobmazon;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.special.ItemKitCustom;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityLanternBehemoth}: die Signallaterne. Kaputt (Weltgenerierung) laesst sie sich mit dem
 * Schweissbrenner reparieren; danach ruft sie 400 Ticks lang per Horn und schickt eine Bobmazon-Rakete mit
 * Vorraeten. Wird sie zerstoert, sinkt der Ruf aller Spieler im Umkreis von 50 Bloecken.
 */
public class LanternBehemothBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IRepairable {

    public boolean isBroken = false;
    public int comTimer = -1;
    private boolean unloaded = false;

    public LanternBehemothBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANTERN_BEHEMOTH_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LanternBehemothBlockEntity be) {
        if (!level.isClientSide) be.updateServer(level);
    }

    private void updateServer(Level world) {
        int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();

        if (comTimer == 360) world.playSound(null, xCoord, yCoord, zCoord, HbmSoundsNT.get("hbm:block.hornNearSingle"), SoundSource.BLOCKS, 10F, 1F);
        if (comTimer == 280) world.playSound(null, xCoord, yCoord, zCoord, HbmSoundsNT.get("hbm:block.hornFarSingle"), SoundSource.BLOCKS, 10000F, 1F);
        if (comTimer == 220) world.playSound(null, xCoord, yCoord, zCoord, HbmSoundsNT.get("hbm:block.hornNearDual"), SoundSource.BLOCKS, 10F, 1F);
        if (comTimer == 100) world.playSound(null, xCoord, yCoord, zCoord, HbmSoundsNT.get("hbm:block.hornFarDual"), SoundSource.BLOCKS, 10000F, 1F);

        if (comTimer == 0) {
            List<Player> players = world.getEntitiesOfClass(Player.class, new AABB(xCoord - 10, yCoord - 10, zCoord - 10, xCoord + 11, yCoord + 11, zCoord + 11));
            Player first = players.isEmpty() ? null : players.get(0);
            boolean bonus = first != null && HbmPlayerProps.get(first).reputation >= 10;
            EntityBobmazon shuttle = new EntityBobmazon(world);
            shuttle.setPos(xCoord + 0.5 + world.random.nextGaussian() * 10, 300, zCoord + 0.5 + world.random.nextGaussian() * 10);
            ItemStack payload = ItemKitCustom.create("Supplies", null, 0xffffff, 0x008000,
                    new ItemStack(ModItems.INTEGRATED_CIRCUIT.get(), 4 + world.random.nextInt(4)),
                    new ItemStack(ModItems.ADVANCED_CIRCUIT.get(), 4 + world.random.nextInt(2)),
                    bonus ? new ItemStack(ModItems.GEM_ALEXANDRITE.get()) : new ItemStack(Items.DIAMOND, 6 + world.random.nextInt(6)),
                    new ItemStack(Blocks.POPPY));
            shuttle.payload = payload;

            world.addFreshEntity(shuttle);
        }

        if (comTimer >= 0) {
            comTimer--;
        }

        // networkPackNT(250): nur der Zustand isBroken wird synchronisiert
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.unloaded = true;
    }

    /** Original {@code invalidate}: der Abbau kostet allen Spielern in der Naehe einen Rufpunkt (bis -25). */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (unloaded || level == null || level.isClientSide) return;
        int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(xCoord - 50, yCoord - 50, zCoord - 50, xCoord + 51, yCoord + 51, zCoord + 51));
        for (Player player : players) {
            HbmPlayerProps props = HbmPlayerProps.get(player);
            if (props.reputation > -25) props.reputation--;
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putBoolean("isBroken", isBroken);
        nbt.putInt("comTimer", comTimer);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        isBroken = nbt.getBoolean("isBroken");
        comTimer = nbt.getInt("comTimer");
    }

    @Override
    public boolean isDamaged() {
        return isBroken;
    }

    private final List<RepairStack> repair = new ArrayList<>();

    @Override
    public List<RepairStack> getRepairMaterials() {

        if (!repair.isEmpty())
            return repair;

        repair.add(new RepairStack(Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), 2));
        repair.add(new RepairStack(Ingredient.of(ModItems.INTEGRATED_CIRCUIT.get()), 1));
        return repair;
    }

    @Override
    public void repair() {
        this.isBroken = false;
        this.comTimer = 400;
        this.setChanged();
    }

    @Override
    public void tryExtinguish(Level world, BlockPos pos, EnumExtinguishType type) { }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), worldPosition.getX() + 1, worldPosition.getY() + 6, worldPosition.getZ() + 1);
    }
}
