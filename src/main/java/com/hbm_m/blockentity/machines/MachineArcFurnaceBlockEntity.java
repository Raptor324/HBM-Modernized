package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.PlatformHooks;

import java.util.List;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.interfaces.IUpgradeInfoProvider;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.inventory.menu.MachineArcFurnaceMenu;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.platform.StackNbt;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.ArcFurnaceRecipe;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemArcElectrode;
import com.hbm_m.item.industrial.ItemArcElectrodeBurnt;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.CrucibleUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineArcFurnaceLarge}: grosser Lichtbogenofen mit drei Elektroden, Deckel, 20 Eingabefeldern
 * und fuenf Warteschlangenplaetzen. Fest-Modus ersetzt die Eingaben durch das Ergebnis, Fluessig-Modus schmilzt sie zu
 * bis zu 128 Bloecken Schmelze, die bei offenem Deckel 2,875 Bloecke vor dem Kern ausgegossen wird. Speed-Upgrades
 * verkuerzen Dauer, Deckelzeit und Pause und erhoehen Verbrauch und Stapelgroesse.
 */
public class MachineArcFurnaceBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.fluids.IFluidStandardSenderMK2, IControlReceiver, IUpgradeInfoProvider {

    public static final int INVENTORY_SIZE = 30;

    public static final long maxPower = 2_500_000;
    public boolean liquidMode = false;
    public float progress;
    public boolean isProgressing;
    public boolean hasMaterial;
    public int delay;
    public int upgrade;

    /** Fuer den Hot/Cold-Elektroden-Zustandswechsel im Renderer (siehe MachineArcFurnaceRenderer). */
    public boolean isActive() {
        return isProgressing;
    }

    public float lid;
    public float prevLid;
    public int approachNum;
    public float syncLid;

    public final UpgradeManager upgradeManager = new UpgradeManager();
    private Item lastUpgradeItem = null;

    public byte[] electrodes = new byte[3];
    public static final byte ELECTRODE_NONE = 0;
    public static final byte ELECTRODE_FRESH = 1;
    public static final byte ELECTRODE_USED = 2;
    public static final byte ELECTRODE_DEPLETED = 3;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = Map.of(UpgradeType.SPEED, 3);

    public int getMaxInputSize() {
        return upgrade == 0 ? 1 : upgrade == 1 ? 4 : upgrade == 2 ? 8 : 16;
    }

    public static final int maxLiquid = MaterialShapes.BLOCK.q(128);
    public List<MaterialStack> liquids = new ArrayList<>();

    private static final int TANK_CAPACITY_MB = 4_000;
    private final FluidTank tank1 = new FluidTank(TANK_CAPACITY_MB);
    private final FluidTank tank2 = new FluidTank(TANK_CAPACITY_MB);

    public MachineArcFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_FURNACE_BE.get(), pos, state, INVENTORY_SIZE, maxPower, maxPower);
    }

    // ==================== IFluidUserMK2 / MK2-ÑÐµÑÑ ====================
    // ÐÑÐ¸Ð²ÑÐ·ÐºÐ° Ð¾Ð±ÑÐ°Ð±Ð¾ÑÑÐ¸ÐºÐ° Ð¶Ð¸Ð´ÐºÐ¾ÑÑÐ¸ Ð½Ð¸Ð¶Ðµ Ð¶Ð¸Ð²ÑÑ Ð² //? if forge, Ð° BaseMachineBlockEntity
    // Ð¾ÑÐ´Ð°ÑÑ NeoForge-Ð¾Ð±ÑÑÑÐºÑ ÑÐ¾Ð»ÑÐºÐ¾ ÑÐµÐ°Ð»Ð¸Ð·Ð°ÑÐ¸ÑÐ¼ IFluidUserMK2 â Ð±ÐµÐ· ÑÑÐ¾Ð³Ð¾ Ñ Ð¼Ð°ÑÐ¸Ð½Ñ
    // Ð½Ð° NeoForge Ð½Ðµ Ð±ÑÐ»Ð¾ fluid-ÐºÐ°Ð¿Ð°Ð±Ð¸Ð»Ð¸ÑÐ¸ Ð²Ð¾Ð¾Ð±ÑÐµ, Ð¸ ÑÑÑÐ±Ñ Ðº Ð½ÐµÐ¹ Ð½Ðµ Ð¿Ð¾Ð´ÐºÐ»ÑÑÐ°Ð»Ð¸ÑÑ.

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { tank1, tank2 }; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { tank1, tank2 }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(net.minecraft.world.level.material.Fluid fluid, net.minecraft.core.Direction fromDir) {
        return fromDir != null;
    }


    //? if forge {
    @Override
    protected void setupFluidCapability() {
        // Экспонируем главный бак (tank1) для внешних насосов/труб через базовый fluidHandlerOpt.
        setFluidHandler(tank1);
    }
    //?}

    public long getPower() { return energy; }

    private Direction getDir() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    private ItemStack slot(int i) { return inventory.getStackInSlot(i); }
    private void setSlot(int i, ItemStack s) { inventory.setStackInSlot(i, s == null ? ItemStack.EMPTY : s); }

    private void decrStackSize(int i, int n) {
        ItemStack s = slot(i).copy();
        s.shrink(n);
        setSlot(i, s.isEmpty() ? ItemStack.EMPTY : s);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineArcFurnaceBlockEntity be) {
        be.upgradeManager.checkSlots(be.inventory, 4, 4, VALID_UPGRADES);
        be.upgrade = be.upgradeManager.getLevel(UpgradeType.SPEED);

        if (!level.isClientSide()) be.serverTick((ServerLevel) level, pos);
        else be.clientTick(level, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {

        // Original setInventorySlotContents: Upgrade einstecken klickt
        Item up = slot(4).getItem();
        if (up != lastUpgradeItem) {
            if (up instanceof ItemMachineUpgrade && lastUpgradeItem != null)
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HbmSoundsNT.get("hbm:item.upgradePlug"), SoundSource.BLOCKS, 1.0F, 1.0F);
            lastUpgradeItem = up;
        }

        this.chargeFromBatterySlot(3);
        this.isProgressing = false;

        if (lid > 0) loadIngredients();

        if (energy > 0) {

            boolean ingredients = this.hasIngredients();
            boolean electrodes = this.hasElectrodes();

            int consumption = (int) (1_000 * Math.pow(5, upgrade));

            if (ingredients && electrodes && delay <= 0 && this.liquids.isEmpty()) {
                if (lid > 0) {
                    lid -= 1F / (60F / (upgrade * 0.5 + 1));
                    if (lid < 0) lid = 0;
                    this.progress = 0;
                } else {

                    if (energy >= consumption) {
                        int duration = 400 / (upgrade * 2 + 1);
                        this.progress += 1F / duration;
                        this.isProgressing = true;
                        this.energy -= consumption;
                        if (this.progress >= 1F) {
                            this.process();
                            this.progress = 0;
                            this.setChanged();
                            this.delay = (int) (120 / (upgrade * 0.5 + 1));
                            PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, 10F);
                        }
                    }
                }
            } else {
                if (this.delay > 0) delay--;
                this.progress = 0;
                if (lid < 1) {
                    lid += 1F / (60F / (upgrade * 0.5 + 1));
                    if (lid > 1) lid = 1;
                }
            }

            hasMaterial = ingredients;
        }

        this.decideElectrodeState();

        if (!hasMaterial) hasMaterial = this.hasIngredients();

        if (!this.liquids.isEmpty() && this.lid > 0F) {

            Direction dir = getDir();

            double[] impact = new double[3];
            MaterialStack didPour = CrucibleUtil.pourFullStack(level, pos.getX() + 0.5D + dir.getStepX() * 2.875D, pos.getY() + 1.25D, pos.getZ() + 0.5D + dir.getStepZ() * 2.875D, 6, true, this.liquids, MaterialShapes.INGOT.q(1), impact);

            if (didPour != null) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "foundry");
                data.putInt("color", didPour.material.moltenColor);
                data.putByte("dir", (byte) dir.get3DDataValue());
                data.putFloat("off", 0.625F);
                data.putFloat("base", 0.625F);
                data.putFloat("len", Math.max(1F, pos.getY() + 1 - (float) (Math.ceil(impact[1]) - 0.875)));
                com.hbm_m.particle.helper.IParticleCreator.sendPacket(level, pos.getX() + 0.5D + dir.getStepX() * 2.875D, pos.getY() + 1, pos.getZ() + 0.5D + dir.getStepZ() * 2.875D, 50, data);
            }
        }

        this.liquids.removeIf(o -> o.amount <= 0);

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick(Level level, BlockPos pos) {

        this.prevLid = this.lid;

        if (this.approachNum > 0) {
            this.lid = this.lid + ((this.syncLid - this.lid) / (float) this.approachNum);
            --this.approachNum;
        } else {
            this.lid = this.syncLid;
        }

        ClientSoundBootstrap.updateDoorSoundRaw(level, pos, "arc_lid", this.lid != this.prevLid,
                () -> loopSound(pos, "hbm:door.wgh_start", 0.75F, 1.0F));

        if ((lid == 1 || lid == 0) && lid != prevLid && !(this.prevLid == 0 && this.lid == 1)) {
            ClientSoundBootstrap.playOneShotSound(level, pos, HbmSoundsNT.get("hbm:door.wgh_stop"), 1F);
        }

        ClientSoundBootstrap.updateDoorSoundRaw(level, pos, "arc_hum", this.isProgressing,
                () -> loopSound(pos, "hbm:block.electricHum", 1.5F, 0.75F));

        Player me = null;
        for (Player p : level.players()) if (p.isLocalPlayer()) { me = p; break; }
        boolean near = me != null && me.distanceToSqr(pos.getX() + 0.5, pos.getY() + 4, pos.getZ() + 0.5) < 50 * 50;

        if (this.lid != this.prevLid && this.lid > this.prevLid && !(this.prevLid == 0 && this.lid == 1) && near) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "tower");
            data.putFloat("lift", 0.01F);
            data.putFloat("base", 0.5F);
            data.putFloat("max", 2F);
            data.putInt("life", 70 + level.random.nextInt(30));
            data.putDouble("posX", pos.getX() + 0.5 + level.random.nextGaussian() * 0.5);
            data.putDouble("posZ", pos.getZ() + 0.5 + level.random.nextGaussian() * 0.5);
            data.putDouble("posY", pos.getY() + 4);
            data.putBoolean("noWind", true);
            data.putFloat("alphaMod", prevLid / lid);
            data.putInt("color", 0x000000);
            data.putFloat("strafe", 0.05F);
            for (int i = 0; i < 3; i++) com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }

        if (this.lid != this.prevLid && this.lid < this.prevLid && this.lid > 0.5F && this.hasMaterial && near) {

            if (level.random.nextInt(5) == 0) {
                CompoundTag flame = new CompoundTag();
                flame.putString("type", "rbmkflame");
                flame.putDouble("posX", pos.getX() + 0.5 + level.random.nextGaussian() * 0.5);
                flame.putDouble("posZ", pos.getZ() + 0.5 + level.random.nextGaussian() * 0.5);
                flame.putDouble("posY", pos.getY() + 2.75);
                flame.putInt("maxAge", 50);
                for (int i = 0; i < 2; i++) com.hbm_m.particle.helper.ParticleEffectClient.effectNT(flame);
            }
        }
    }

    private static Object loopSound(BlockPos pos, String sound, float volume, float pitch) {
        try {
            return Class.forName("com.hbm_m.client.sound.ArcFurnaceLoopSoundFactory")
                    .getMethod("create", BlockPos.class, String.class, float.class, float.class).invoke(null, pos, sound, volume, pitch);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientSoundBootstrap.stopSpecificSound(level, worldPosition, "arc_lid");
            ClientSoundBootstrap.stopSpecificSound(level, worldPosition, "arc_hum");
        }
    }

    /**
     * Kanonische Rezeptsuche (ersetzt das FFA-{@code ArcFurnaceRecipes.getOutput(stack, liquid, level)}),
     * gespiegelt aus {@code MachineArcFurnaceMenu.findRecipe}: datengetriebene {@link ArcFurnaceRecipe}s
     * aus dem Vanilla-RecipeManager, gefiltert nach Ausgabeart (Fluessig-Modus: Fluid-Ausgang,
     * Fest-Modus: Item-Ausgang).
     */
    @Nullable
    private ArcFurnaceRecipe findRecipe(ItemStack stack) {
        if (level == null || stack.isEmpty()) return null;
        for (ArcFurnaceRecipe recipe : RecipeHooks.getAllRecipes(level, ArcFurnaceRecipe.Type.INSTANCE)) {
            if (!recipe.matchesInput(stack)) continue;
            boolean hasOutput = this.liquidMode ? recipe.hasFluidOutput1() || recipe.hasFluidOutput2() : recipe.hasItemOutput();
            if (hasOutput) return recipe;
        }
        return null;
    }

    /** Maschinen-Semantik (origin/machines): Fluid-Ausgang passt in den Tank (Typ + Restplatz). */
    private boolean fitsInTank(FluidTank tank, net.minecraft.world.level.material.Fluid fluid, int amount) {
        if (!tank.isEmpty() && tank.getStoredFluid() != fluid) return false;
        return tank.getSpaceMb() >= amount;
    }

    /** Moves items from the input queue to the main grid */
    public void loadIngredients() {

        boolean markDirty = false;

        for (int q /* queue */ = 25; q < 30; q++) {
            if (slot(q).isEmpty()) continue;
            ArcFurnaceRecipe recipe = findRecipe(slot(q));
            if (recipe == null) continue;
            int max = this.getMaxInputSize();
            int recipeMax = this.liquidMode ? max : slot(q).getMaxStackSize() / recipe.getOutput().getCount();
            max = Math.min(max, recipeMax);

            // add to existing stacks
            for (int i /* ingredient */ = 5; i < 25; i++) {
                if (slot(i).isEmpty()) continue;
                if (!StackNbt.sameItemSameTags(slot(q), slot(i))) continue;
                int toMove = Math.min(Math.min(slot(i).getMaxStackSize() - slot(i).getCount(), slot(q).getCount()), max - slot(i).getCount());
                if (toMove > 0) {
                    this.decrStackSize(q, toMove);
                    ItemStack grown = slot(i).copy();
                    grown.grow(toMove);
                    setSlot(i, grown);
                    markDirty = true;
                }
                if (slot(q).isEmpty()) break;
            }

            // add to empty slot
            if (!slot(q).isEmpty()) for (int i /* ingredient */ = 5; i < 25; i++) {
                if (!slot(i).isEmpty()) continue;
                int toMove = Math.min(max, slot(q).getCount());
                setSlot(i, slot(q).copyWithCount(toMove));
                this.decrStackSize(q, toMove);
                markDirty = true;
                if (slot(q).isEmpty()) break;
            }
        }

        if (markDirty) this.setChanged();
    }

    public void decideElectrodeState() {
        for (int i = 0; i < 3; i++) {

            ItemStack s = slot(i);
            if (!s.isEmpty()) {
                if (s.getItem() instanceof ItemArcElectrodeBurnt) { this.electrodes[i] = ELECTRODE_DEPLETED; continue; }
                if (s.getItem() instanceof ItemArcElectrode) {
                    if (this.isProgressing || ItemArcElectrode.getDurability(s) > 0) this.electrodes[i] = ELECTRODE_USED;
                    else this.electrodes[i] = ELECTRODE_FRESH;
                    continue;
                }
            }
            this.electrodes[i] = ELECTRODE_NONE;
        }
    }

    public void process() {

        for (int i = 5; i < 25; i++) {
            if (slot(i).isEmpty()) continue;
            ArcFurnaceRecipe recipe = findRecipe(slot(i));
            if (recipe == null) continue;

            if (!liquidMode && recipe.hasItemOutput()) {
                int amount = slot(i).getCount();
                ItemStack out = recipe.getOutput();
                out.setCount(out.getCount() * amount);
                setSlot(i, out);
            }

            if (liquidMode && (recipe.hasFluidOutput1() || recipe.hasFluidOutput2())) {

                while (!slot(i).isEmpty() && slot(i).getCount() > 0) {

                    boolean fits = (!recipe.hasFluidOutput1() || fitsInTank(tank1, recipe.getFluid1(), recipe.getFluidAmount1()))
                            && (!recipe.hasFluidOutput2() || fitsInTank(tank2, recipe.getFluid2(), recipe.getFluidAmount2()));
                    if (!fits) break;

                    this.decrStackSize(i, 1);
                    if (recipe.hasFluidOutput1()) tank1.fillMb(recipe.getFluid1(), recipe.getFluidAmount1());
                    if (recipe.hasFluidOutput2()) tank2.fillMb(recipe.getFluid2(), recipe.getFluidAmount2());
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            ItemStack s = slot(i).copy();
            if (ItemArcElectrode.damage(s)) {
                setSlot(i, new ItemStack(burnt(((ItemArcElectrode) s.getItem()).type)));
            } else {
                setSlot(i, s);
            }
        }
    }

    private static Item burnt(ItemArcElectrode.EnumElectrodeType type) {
        return switch (type) {
            case GRAPHITE -> ModItems.ARC_ELECTRODE_BURNT_GRAPHITE.get();
            case LANTHANIUM -> ModItems.ARC_ELECTRODE_BURNT_LANTHANIUM.get();
            case DESH -> ModItems.ARC_ELECTRODE_BURNT_DESH.get();
            case SATURNITE -> ModItems.ARC_ELECTRODE_BURNT_SATURNITE.get();
        };
    }

    public boolean hasIngredients() {

        for (int i = 5; i < 25; i++) {
            if (slot(i).isEmpty()) continue;
            if (findRecipe(slot(i)) != null) return true;
        }

        return false;
    }

    public boolean hasElectrodes() {
        for (int i = 0; i < 3; i++) {
            if (slot(i).isEmpty() || !(slot(i).getItem() instanceof ItemArcElectrode)) return false;
        }
        return true;
    }

    public void addToStack(MaterialStack matStack) {

        for (MaterialStack mat : liquids) {
            if (mat.material == matStack.material) {
                mat.amount += matStack.amount;
                return;
            }
        }

        liquids.add(matStack.copy());
    }

    public static int getStackAmount(List<MaterialStack> stack) {
        int amount = 0;
        for (MaterialStack mat : stack) amount += mat.amount;
        return amount;
    }

    public static int getStackAmount(MaterialStack[] stack) {
        int amount = 0;
        for (MaterialStack mat : stack) amount += mat.amount;
        return amount;
    }

    // ── Inventar ─────────────────────────────────────────────────────────────

    /** Original {@code isItemValidForSlot}. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 3) return stack.getItem() instanceof ItemArcElectrode;
        if (slot == 3) return isEnergyProviderItem(stack);
        if (slot == 4) return stack.getItem() instanceof ItemMachineUpgrade;
        return findRecipe(stack) != null;
    }

    /** Original {@code canInsertItem}. */
    public boolean canInsertItem(int slot, ItemStack stack) {
        if (slot < 3) return stack.getItem() instanceof ItemArcElectrode;
        if (slot >= 25) return findRecipe(stack) != null;
        return false;
    }

    /** Original {@code canExtractItem}. */
    public boolean canExtractItem(int slot, ItemStack stack) {
        if (slot < 3) return lid >= 1 && !(stack.getItem() instanceof ItemArcElectrode);
        if (slot > 4 && slot < 25) return lid > 0 && findRecipe(stack) == null;
        if (slot >= 25) return findRecipe(stack) == null;
        return false;
    }

    private static final int[] ACCESSIBLE = {
            0, 1, 2,
            5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            25, 26, 27, 28, 29 };

    //? if forge {
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> sided;

    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            if (sided == null) sided = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return ACCESSIBLE.length; }
                @Override public @NotNull ItemStack getStackInSlot(int s) { return inventory.getStackInSlot(ACCESSIBLE[s]); }
                @Override public @NotNull ItemStack insertItem(int s, @NotNull ItemStack stack, boolean simulate) {
                    int real = ACCESSIBLE[s];
                    if (!canInsertItem(real, stack)) return stack;
                    return inventory.insertItem(real, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int s, int amount, boolean simulate) {
                    int real = ACCESSIBLE[s];
                    ItemStack present = inventory.getStackInSlot(real);
                    if (present.isEmpty() || !canExtractItem(real, present)) return ItemStack.EMPTY;
                    return inventory.extractItem(real, amount, simulate);
                }
                @Override public int getSlotLimit(int s) { return inventory.getSlotLimit(ACCESSIBLE[s]); }
                @Override public boolean isItemValid(int s, @NotNull ItemStack stack) { return canInsertItem(ACCESSIBLE[s], stack); }
            });
            return sided.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (sided != null) sided.invalidate();
        sided = null;
    }
    //?} elif neoforge {
    /*private com.hbm_m.platform.LazyCap<net.minecraftforge.items.IItemHandler> sided;

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) {
            if (sided == null) sided = com.hbm_m.platform.LazyCap.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return ACCESSIBLE.length; }
                @Override public @NotNull ItemStack getStackInSlot(int s) { return inventory.getStackInSlot(ACCESSIBLE[s]); }
                @Override public @NotNull ItemStack insertItem(int s, @NotNull ItemStack stack, boolean simulate) {
                    int real = ACCESSIBLE[s];
                    if (!canInsertItem(real, stack)) return stack;
                    return inventory.insertItem(real, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int s, int amount, boolean simulate) {
                    int real = ACCESSIBLE[s];
                    ItemStack present = inventory.getStackInSlot(real);
                    if (present.isEmpty() || !canExtractItem(real, present)) return ItemStack.EMPTY;
                    return inventory.extractItem(real, amount, simulate);
                }
                @Override public int getSlotLimit(int s) { return inventory.getSlotLimit(ACCESSIBLE[s]); }
                @Override public boolean isItemValid(int s, @NotNull ItemStack stack) { return canInsertItem(ACCESSIBLE[s], stack); }
            });
            return sided.cast();
        }
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        if (sided != null) sided.invalidate();
        sided = null;
    }
    *///?}

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putBoolean("liquidMode", liquidMode);
        nbt.putFloat("progress", progress);
        nbt.putFloat("lid", lid);
        nbt.putInt("delay", delay);
        nbt.putBoolean("isProgressing", isProgressing);
        nbt.putBoolean("hasMaterial", hasMaterial);
        nbt.putByteArray("electrodes", electrodes);
        tank1.writeToNBT(nbt, "tank1");
        tank2.writeToNBT(nbt, "tank2");

        int count = liquids.size();
        nbt.putShort("count", (short) count);
        for (int i = 0; i < count; i++) {
            MaterialStack mat = liquids.get(i);
            nbt.putInt("m" + i, mat.material.id);
            nbt.putInt("a" + i, mat.amount);
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.liquidMode = nbt.getBoolean("liquidMode");
        this.progress = nbt.getFloat("progress");
        // Original deserialize: der Client naehert den Deckel ueber zwei Ticks an
        if (level != null && level.isClientSide) {
            this.syncLid = nbt.getFloat("lid");
            if (syncLid != 0 && syncLid != 1) this.approachNum = 2;
        } else {
            this.lid = nbt.getFloat("lid");
        }
        this.delay = nbt.getInt("delay");
        this.isProgressing = nbt.getBoolean("isProgressing");
        this.hasMaterial = nbt.getBoolean("hasMaterial");
        byte[] el = nbt.getByteArray("electrodes");
        if (el.length == 3) this.electrodes = el;
        tank1.readFromNBT(nbt, "tank1");
        tank2.readFromNBT(nbt, "tank2");

        int count = nbt.getShort("count");
        liquids.clear();

        for (int i = 0; i < count; i++) {
            NTMMaterial mat = Mats.matById.get(nbt.getInt("m" + i));
            if (mat != null) liquids.add(new MaterialStack(mat, nbt.getInt("a" + i)));
        }
    }

    // ── GUI / Steuerung ─────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineArcFurnaceLarge");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineArcFurnaceMenu(id, inv, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 256;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.getBoolean("liquid")) {
            this.liquidMode = !this.liquidMode;
            this.setChanged();
        }
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModBlocks.ARC_FURNACE.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (100 - 100 / (level * 2 + 1)) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + ((int) Math.pow(5, level) * 100 - 100) + "%").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 6, worldPosition.getZ() + 4);
        return bb;
    }

    @Override
    public @org.jetbrains.annotations.Nullable Object getItemHandler(@org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        return this.inventory;
    }

    @Override
    public @org.jetbrains.annotations.Nullable Object getFluidHandler(@org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (!isFluidSideAllowed(side)) return null;
        //? if forge {
        return null;
        //?} elif neoforge {
        /*return new com.hbm_m.api.fluids.NeoForgeFluidHandlerMK2(this);
        *///?}
    }
}
