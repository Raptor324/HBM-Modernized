package com.hbm_m.blockentity.crates;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.TungstenCrateMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityCrateTungsten} ({@code ILaserable}): ein Strahl des Kernemitters schmilzt den ganzen Inhalt
 * nach Ofenrezept, ueber 10 Mio. HE wird Polonium zu Yharonit und ein benutzter Tiegel wieder neu.
 * Nach jedem Treffer glueht die Kiste 5 Ticks nach (Original: Metadate 1) und speit Flammen und Rauch.
 */
public class TungstenCrateBlockEntity extends BaseCrateBlockEntity implements com.hbm_m.interfaces.ILaserable {

    private int heatTimer;
    /** Original: Blockmetadate 1 = heiss. */
    private boolean hot;

    public TungstenCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TUNGSTEN_CRATE_BE.get(), pos, state, CrateType.TUNGSTEN.getSlotCount());
    }

    public boolean isHot() {
        return hot;
    }

    /** 1:1 {@code updateEntity}. */
    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, TungstenCrateBlockEntity be) {
        if (!level.isClientSide) {
            if (be.heatTimer > 0)
                be.heatTimer--;

            if (!be.hot && be.heatTimer > 0) be.setHot(true);
            if (be.hot && be.heatTimer <= 0) be.setHot(false);
        }

        if (be.hot && level.isClientSide) {
            net.minecraft.util.RandomSource rand = level.random;
            double x = pos.getX(), y = pos.getY(), z = pos.getZ();
            net.minecraft.core.particles.SimpleParticleType flame = net.minecraft.core.particles.ParticleTypes.FLAME;
            net.minecraft.core.particles.SimpleParticleType smoke = net.minecraft.core.particles.ParticleTypes.SMOKE;

            level.addParticle(flame, x + rand.nextDouble(), y + 1.1, z + rand.nextDouble(), 0.0, 0.0, 0.0);
            level.addParticle(smoke, x + rand.nextDouble(), y + 1.1, z + rand.nextDouble(), 0.0, 0.0, 0.0);

            level.addParticle(flame, x - 0.1, y + rand.nextDouble(), z + rand.nextDouble(), 0.0, 0.0, 0.0);
            level.addParticle(smoke, x - 0.1, y + rand.nextDouble(), z + rand.nextDouble(), 0.0, 0.0, 0.0);

            level.addParticle(flame, x + 1.1, y + rand.nextDouble(), z + rand.nextDouble(), 0.0, 0.0, 0.0);
            level.addParticle(smoke, x + 1.1, y + rand.nextDouble(), z + rand.nextDouble(), 0.0, 0.0, 0.0);

            level.addParticle(flame, x + rand.nextDouble(), y + rand.nextDouble(), z - 0.1, 0.0, 0.0, 0.0);
            level.addParticle(smoke, x + rand.nextDouble(), y + rand.nextDouble(), z - 0.1, 0.0, 0.0, 0.0);

            level.addParticle(flame, x + rand.nextDouble(), y + rand.nextDouble(), z + 1.1, 0.0, 0.0, 0.0);
            level.addParticle(smoke, x + rand.nextDouble(), y + rand.nextDouble(), z + 1.1, 0.0, 0.0, 0.0);
        }
    }

    /** Wie {@code setBlockMetadataWithNotify(..., 3)}: Zustand speichern und an die Clients schicken. */
    private void setHot(boolean hot) {
        this.hot = hot;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (hot) tag.putBoolean("hot", true);
    }

    @Override
    protected void readNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        hot = tag.getBoolean("hot");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.crate_tungsten");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TungstenCrateMenu(containerId, playerInventory, this);
    }

    private static net.minecraft.world.item.Item item(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", id));
    }

    /** Original {@code addEnergy}. */
    @Override
    public boolean addLaserEnergy(net.minecraft.world.level.Level level, BlockPos pos, long energy, net.minecraft.core.Direction beamDirection) {
        heatTimer = 5;
        net.minecraft.world.item.Item polonium = item("billet_polonium");
        net.minecraft.world.item.Item yharonite = item("billet_yharonite");
        net.minecraft.world.item.Item crucible = item("crucible_sword");

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            net.minecraft.world.item.ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            net.minecraft.world.item.ItemStack result = com.hbm_m.platform.recipe.RecipeHooks
                    .getRecipeFor(level, net.minecraft.world.item.crafting.RecipeType.SMELTING, stack)
                    .map(r -> r.getResultItem(level.registryAccess()).copy()).orElse(null);

            if (stack.is(polonium) && energy > 10_000_000L) result = new net.minecraft.world.item.ItemStack(yharonite);
            if (stack.is(crucible) && stack.getDamageValue() > 0 && energy > 10_000_000L) result = new net.minecraft.world.item.ItemStack(crucible);

            int size = stack.getCount();
            if (result != null && !result.isEmpty() && result.getCount() * size <= result.getMaxStackSize()) {
                net.minecraft.world.item.ItemStack out = result.copy();
                out.setCount(result.getCount() * size);
                itemHandler.setStackInSlot(i, out);
            }
        }
        return true;
    }

    /** Original {@code canExtractItem}: kein Polonium, kein benutzter Tiegel und nichts Schmelzbares. */
    @Override
    protected boolean canExtractAutomation(int slot, net.minecraft.world.item.ItemStack stack) {
        if (isLocked()) return false;
        if (stack.is(item("billet_polonium"))) return false;
        if (stack.is(item("crucible_sword")) && stack.getDamageValue() > 0) return false;
        return level == null || com.hbm_m.platform.recipe.RecipeHooks.getRecipeFor(level, net.minecraft.world.item.crafting.RecipeType.SMELTING, stack).isEmpty();
    }
}
