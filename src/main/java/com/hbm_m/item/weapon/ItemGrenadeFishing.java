package com.hbm_m.item.weapon;

import com.hbm_m.entity.item.EntityItemBuoyant;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code com.hbm.items.weapon.ItemGrenadeFishing} ({@code stick_dynamite_fishing}): Fische treiben auf. */
public class ItemGrenadeFishing extends ItemGenericGrenade {

    public ItemGrenadeFishing(int fuse, Properties properties) {
        super(fuse, properties);
    }

    @Override
    public void explode(Entity grenade, LivingEntity thrower, Level world, double x, double y, double z) {
        world.explode(null, x, y + 0.25D, z, 3F, Level.ExplosionInteraction.NONE);

        int iX = Mth.floor(x);
        int iY = Mth.floor(y);
        int iZ = Mth.floor(z);

        for (int i = 0; i < 15; i++) {
            int rX = iX + world.random.nextInt(15) - 7;
            int rY = iY + world.random.nextInt(15) - 7;
            int rZ = iZ + world.random.nextInt(15) - 7;

            if (world.getFluidState(new BlockPos(rX, rY, rZ)).is(FluidTags.WATER) && world instanceof ServerLevel server) {
                ItemStack loot = getRandomLoot(server, new Vec3(rX + 0.5, rY + 0.5, rZ + 0.5));
                if (!loot.isEmpty()) {
                    EntityItemBuoyant item = new EntityItemBuoyant(world, rX + 0.5, rY + 0.5, rZ + 0.5, loot.copy());
                    item.setDeltaMovement(item.getDeltaMovement().x, 1, item.getDeltaMovement().z);
                    world.addFreshEntity(item);
                }
            }
        }
    }

    /**
     * {@code FishingHooks.getRandomFishable(rand, chance, luck 0, speed 100)}: die Geschwindigkeit 100 drueckt Muell- und
     * Schatzchance unter null, also immer ein Fisch (Beutetabelle gameplay/fishing/fish).
     */
    public static ItemStack getRandomLoot(ServerLevel world, Vec3 pos) {
        //? if < 1.21.1 {
        LootTable table = world.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING_FISH);
        //?} else {
        /*LootTable table = world.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING_FISH);
        *///?}
        LootParams params = new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN, pos)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY).create(LootContextParamSets.FISHING);
        var list = table.getRandomItems(params);
        return list.isEmpty() ? ItemStack.EMPTY : list.get(0);
    }

    @Override
    public int getMaxTimer() {
        return 60;
    }

    @Override
    public double getBounceMod() {
        return 0.5D;
    }
}
