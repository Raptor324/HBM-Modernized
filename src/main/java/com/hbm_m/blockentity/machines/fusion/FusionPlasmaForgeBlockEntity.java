package com.hbm_m.blockentity.machines.fusion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fusion.IFusionPowerReceiver;
import com.hbm_m.api.fusion.PlasmaNetwork;
import com.hbm_m.api.fusion.PlasmaNetworkProvider;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.NodeNet;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.block.machines.fusion.FusionMultiblockBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineFusionPlasmaForgeMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.recipe.ModRecipes;
import com.hbm_m.recipe.PlasmaForgeRecipe;
import com.hbm_m.recipe.PlasmaForgeRecipe.CountedIngredient;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityFusionPlasmaForge} (1.7.10).
 *
 * <p>Die Plasmaschmiede haengt beidseitig im Plasmanetz: auf der einen Seite nimmt sie die
 * Leistung des Torus ab, auf der anderen reicht sie 75 % davon an die naechste Maschine weiter.
 * Radioaktive Isotope im Boosterslot vervierfachen die Verarbeitungsgeschwindigkeit, solange der
 * Boostervorrat laeuft.</p>
 */
public class FusionPlasmaForgeBlockEntity extends BaseMachineBlockEntity
        implements IFusionPowerReceiver, IFluidStandardReceiverMK2, NodeNet.ILoadedEntry {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_BOOSTER = 2;
    public static final int SLOT_INPUT_FIRST = 3;
    public static final int SLOT_INPUT_COUNT = 12;
    public static final int SLOT_OUTPUT = 15;
    public static final int INVENTORY_SIZE = 16;

    private static final long DEFAULT_MAX_POWER = 10_000_000L;

    public final FluidTank inputTank;

    public boolean didProcess;
    public double progress;

    public float plasmaRed;
    public float plasmaGreen;
    public float plasmaBlue;
    public long plasmaEnergy;
    public long plasmaEnergySync;
    public double neutronEnergy;
    public boolean connected;

    public int booster;
    public int maxBooster;

    private GenNode<PlasmaNetwork> receiverNode;
    private GenNode<PlasmaNetwork> providerNode;

    @Nullable
    private ResourceLocation selectedRecipeId = null;

    private int timeOffset = -1;

    // Animation (Client)
    public double prevRing;
    public double ring;
    public double ringSpeed;
    public double ringTarget;
    public int ringDelay;
    public final ForgeArm armStriker = new ForgeArm(ForgeArmType.STRIKER, this);
    public final ForgeArm armJet = new ForgeArm(ForgeArmType.JET, this);

    public FusionPlasmaForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_PLASMA_FORGE_BE.get(), pos, state, INVENTORY_SIZE, DEFAULT_MAX_POWER, Long.MAX_VALUE, 0L);
        this.inputTank = new FluidTank(16_000);
    }

    // ═══════════════════════════════ Booster ═══════════════════════════════

    /** Ein Boosterisotop und die Anzahl Ticks, die es traegt (1:1 aus {@code boosters}). */
    public record Booster(Item item, int duration) {}

    private static List<Booster> boosters;

    /**
     * 1:1 aus der statischen {@code boosters}-Liste des Originals - seit der Umstellung auf das
     * einheitliche Materialregister sind alle zwanzig Isotopenformen vorhanden.
     */
    public static List<Booster> getBoosters() {
        if (boosters == null) {
            List<Booster> list = new ArrayList<>();
            add(list, ModMaterials.CO60,  MaterialShape.NUGGET,      20);
            add(list, ModMaterials.CO60,  MaterialShape.BILLET,     120);
            add(list, ModMaterials.CO60,  MaterialShape.INGOT,      200);
            add(list, ModMaterials.CO60,  MaterialShape.POWDER,     200);
            add(list, ModMaterials.SR90,  MaterialShape.NUGGET,      40);
            add(list, ModMaterials.SR90,  MaterialShape.POWDER_TINY, 40);
            add(list, ModMaterials.SR90,  MaterialShape.BILLET,     240);
            add(list, ModMaterials.SR90,  MaterialShape.INGOT,      400);
            add(list, ModMaterials.SR90,  MaterialShape.POWDER,     400);
            add(list, ModMaterials.AU198, MaterialShape.NUGGET,      60);
            add(list, ModMaterials.AU198, MaterialShape.BILLET,     360);
            add(list, ModMaterials.AU198, MaterialShape.INGOT,      600);
            add(list, ModMaterials.AU198, MaterialShape.POWDER,     600);
            add(list, ModMaterials.I131,  MaterialShape.POWDER_TINY, 60);
            add(list, ModMaterials.I131,  MaterialShape.POWDER,     600);
            add(list, ModMaterials.XE135, MaterialShape.POWDER_TINY, 60);
            add(list, ModMaterials.XE135, MaterialShape.POWDER,     600);
            add(list, ModMaterials.CS137, MaterialShape.POWDER_TINY, 50);
            add(list, ModMaterials.CS137, MaterialShape.POWDER,     500);
            add(list, ModMaterials.AT209, MaterialShape.POWDER,   1_200);
            boosters = List.copyOf(list);
        }
        return boosters;
    }

    private static void add(List<Booster> list, ModMaterials mat, MaterialShape shape, int duration) {
        if (!ModMaterialItems.has(mat, shape)) return;
        Item item = ModMaterialItems.item(mat, shape);
        if (item != null) list.add(new Booster(item, duration));
    }

    // ═══════════════════════════════ Tick ═══════════════════════════════

    public static void tick(Level level, BlockPos pos, BlockState state, FusionPlasmaForgeBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.serverTick(serverLevel, pos, state);
        } else {
            be.clientTick();
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {

        ensureNetworkInitialized();

        this.plasmaEnergySync = this.plasmaEnergy;
        this.plasmaEnergy = 0;

        // Boosterslot leeren, sobald der alte Booster aufgebraucht ist.
        ItemStack boosterStack = getInventory().getStackInSlot(SLOT_BOOSTER);
        if (booster <= 0 && !boosterStack.isEmpty()) {
            for (Booster b : getBoosters()) {
                if (boosterStack.is(b.item())) {
                    this.maxBooster = this.booster = b.duration();
                    boosterStack.shrink(1);
                    getInventory().setStackInSlot(SLOT_BOOSTER, boosterStack);
                    break;
                }
            }
        }

        Direction rot = state.getValue(FusionMultiblockBlock.FACING).getOpposite().getClockWise();
        if (receiverNode == null || receiverNode.expired) receiverNode = createNode(level, pos, rot);
        if (providerNode == null || providerNode.expired) providerNode = createNode(level, pos, rot.getOpposite());

        if (receiverNode != null && receiverNode.hasValidNet()) receiverNode.net.addReceiver(this);
        // Technisch ungenutzt, aber im Original vorhanden - fuer spaetere Plasmaverbraucher.
        if (providerNode != null && providerNode.hasValidNet()) providerNode.net.addProvider(this);

        PlasmaForgeRecipe recipe = getRecipe(level);
        if (recipe != null) setEnergyCapacity(Math.max(recipe.getPower() * 100, 100_000L));
        else setEnergyCapacity(Math.max(DEFAULT_MAX_POWER, 100_000L));

        chargeFromBatterySlot(SLOT_BATTERY);

        for (NodeDirPos con : getConPos(pos, state)) {
            trySubscribe(level, con.getX(), con.getY(), con.getZ(), con.getDir());
            if (inputTank.getTankType() != Fluids.EMPTY) {
                trySubscribe(inputTank.getTankType(), level, con.getPos(), con.getDir());
            }
        }

        double speed = booster > 0 ? 4D : 1D;
        boolean ignition = recipe == null || recipe.getIgnitionTemp() <= this.plasmaEnergySync;

        moduleUpdate(recipe, speed, ignition);

        long powerReceived = (long) Math.ceil(this.plasmaEnergySync * 0.75);

        this.connected = providerNode != null && providerNode.hasValidNet()
                && !providerNode.net.receiverEntries.isEmpty();

        if (providerNode != null && providerNode.hasValidNet()) {
            for (Entry<?, ?> entry : providerNode.net.receiverEntries.entrySet()) {
                if (entry.getKey() instanceof IFusionPowerReceiver rec && powerReceived > 0) {
                    rec.receiveFusionPower(powerReceived, this.neutronEnergy, plasmaRed, plasmaGreen, plasmaBlue);
                }
            }
        }

        if (this.didProcess && this.booster > 0) this.booster--;
        this.neutronEnergy = 0D;

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        this.armStriker.updateArm();
        this.armJet.updateArm();

        this.prevRing = this.ring;

        if (!didProcess || level == null) return;

        if (this.ring != this.ringTarget) {
            double ringDelta = Math.abs(this.ringTarget - this.ring);
            if (ringDelta <= this.ringSpeed) this.ring = this.ringTarget;
            if (this.ringTarget > this.ring) this.ring += this.ringSpeed;
            if (this.ringTarget < this.ring) this.ring -= this.ringSpeed;
            if (this.ringTarget == this.ring) {
                double sub = ringTarget >= 360 ? -360D : 360D;
                this.ringTarget += sub;
                this.ring += sub;
                this.prevRing += sub;
                this.ringDelay = 100 + level.random.nextInt(41);
            }
        } else {
            if (this.ringDelay > 0) this.ringDelay--;
            if (this.ringDelay <= 0) {
                this.ringTarget += (level.random.nextDouble() + 1) * 60 * (level.random.nextBoolean() ? -1 : 1);
                this.ringSpeed = 2.5D;
            }
        }
    }

    private GenNode<PlasmaNetwork> createNode(ServerLevel level, BlockPos pos, Direction dir) {
        BlockPos nodePos = pos.offset(dir.getStepX() * 5, 2, dir.getStepZ() * 5);
        GenNode<PlasmaNetwork> node = UniNodespace.getNode(level, nodePos, PlasmaNetworkProvider.THE_PROVIDER);
        if (node != null) return node;

        node = new GenNode<>(PlasmaNetworkProvider.THE_PROVIDER, nodePos)
                .setConnections(new NodeDirPos(pos.offset(dir.getStepX() * 6, 2, dir.getStepZ() * 6), dir));
        UniNodespace.createNode(level, node);
        return node;
    }

    /** Original: {@code getConPos()} - je fuenf Anschluesse vorn und hinten. */
    public NodeDirPos[] getConPos(BlockPos pos, BlockState state) {
        Direction dir = state.getValue(FusionMultiblockBlock.FACING);
        Direction rot = dir.getClockWise();

        NodeDirPos[] result = new NodeDirPos[10];
        int i = 0;
        for (int s = -2; s <= 2; s++) {
            result[i++] = new NodeDirPos(pos.offset(dir.getStepX() * 6 + rot.getStepX() * s, 0,
                    dir.getStepZ() * 6 + rot.getStepZ() * s), dir);
        }
        for (int s = -2; s <= 2; s++) {
            result[i++] = new NodeDirPos(pos.offset(-dir.getStepX() * 6 + rot.getStepX() * s, 0,
                    -dir.getStepZ() * 6 + rot.getStepZ() * s), dir.getOpposite());
        }
        return result;
    }

    // ═════════════════════════════ Rezeptmodul ═════════════════════════════

    @Nullable
    public PlasmaForgeRecipe getRecipe(Level level) {
        if (selectedRecipeId == null) return null;
        return level.getRecipeManager().byKey(selectedRecipeId)
                .filter(r -> r instanceof PlasmaForgeRecipe)
                .map(r -> (PlasmaForgeRecipe) r)
                .orElse(null);
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    public void setSelectedRecipeId(@Nullable ResourceLocation id) {
        this.selectedRecipeId = id;
        this.progress = 0D;
        setChanged();
    }

    public static List<PlasmaForgeRecipe> getAllRecipes(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.PLASMA_FORGE_TYPE.get());
    }

    /** Original: {@code ModuleMachinePlasma.setupTanks} - Tankgroesse folgt dem Rezept. */
    private void setupTank(@Nullable PlasmaForgeRecipe recipe) {
        if (recipe == null) return;
        List<FluidStack> in = recipe.getFluidInputs();
        if (in.isEmpty()) {
            inputTank.resetTank();
            return;
        }
        FluidStack first = in.get(0);
        inputTank.conform(first.getFluid());
        inputTank.changeTankSize(Math.max(inputTank.getFill(),
                Math.max((int) first.getAmount() * 2, 16_000)));
    }

    private boolean hasInput(PlasmaForgeRecipe recipe) {
        List<CountedIngredient> inputs = recipe.getItemInputs();
        for (int i = 0; i < Math.min(inputs.size(), SLOT_INPUT_COUNT); i++) {
            CountedIngredient ci = inputs.get(i);
            ItemStack stack = getInventory().getStackInSlot(SLOT_INPUT_FIRST + i);
            if (!ci.ingredient().test(stack) || stack.getCount() < ci.count()) return false;
        }

        List<FluidStack> fluids = recipe.getFluidInputs();
        if (!fluids.isEmpty() && inputTank.getFill() < fluids.get(0).getAmount()) return false;

        return true;
    }

    private boolean canFitOutput(PlasmaForgeRecipe recipe) {
        ItemStack result = recipe.getOutput();
        if (result.isEmpty()) return true;
        ItemStack current = getInventory().getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) return true;
        if (current.getItem() != result.getItem()) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private boolean canProcess(@Nullable PlasmaForgeRecipe recipe) {
        if (recipe == null) return false;
        if (getEnergyStored() < recipe.getPower()) return false;
        if (!hasInput(recipe)) return false;
        return canFitOutput(recipe);
    }

    private void moduleUpdate(@Nullable PlasmaForgeRecipe recipe, double speed, boolean ignition) {
        setupTank(recipe);
        this.didProcess = false;

        if (ignition && canProcess(recipe)) {
            process(recipe, speed);
            this.didProcess = true;
        } else {
            this.progress = 0D;
        }
    }

    private void process(PlasmaForgeRecipe recipe, double speed) {
        setEnergyStored(getEnergyStored() - recipe.getPower());
        this.progress += Math.min(speed / recipe.getDuration(), 1D);

        if (this.progress >= 1D) {
            consumeInput(recipe);
            produceItem(recipe);
            if (canProcess(recipe)) this.progress -= 1D;
            else this.progress = 0D;
        }
    }

    private void consumeInput(PlasmaForgeRecipe recipe) {
        List<CountedIngredient> inputs = recipe.getItemInputs();
        for (int i = 0; i < Math.min(inputs.size(), SLOT_INPUT_COUNT); i++) {
            ItemStack stack = getInventory().getStackInSlot(SLOT_INPUT_FIRST + i);
            stack.shrink(inputs.get(i).count());
            getInventory().setStackInSlot(SLOT_INPUT_FIRST + i, stack);
        }

        List<FluidStack> fluids = recipe.getFluidInputs();
        if (!fluids.isEmpty()) {
            inputTank.setFill(inputTank.getFill() - (int) fluids.get(0).getAmount());
        }
    }

    private void produceItem(PlasmaForgeRecipe recipe) {
        ItemStack result = recipe.getOutput();
        if (result.isEmpty()) return;

        ItemStack current = getInventory().getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) {
            getInventory().setStackInSlot(SLOT_OUTPUT, result.copy());
        } else {
            current.grow(result.getCount());
            getInventory().setStackInSlot(SLOT_OUTPUT, current);
        }
    }

    // ═════════════════════════════ Plasma ═════════════════════════════

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        this.plasmaEnergy = fusionPower;
        this.neutronEnergy = neutronPower;
        this.plasmaRed = r;
        this.plasmaGreen = g;
        this.plasmaBlue = b;
    }

    // ═════════════════════════════ Standardkram ═════════════════════════════

    public int renderTimeOffset() {
        if (timeOffset == -1 && level != null) timeOffset = level.random.nextInt(30_000);
        return Math.max(timeOffset, 0);
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot == SLOT_BLUEPRINT) return stack.is(ModItems.BLUEPRINT_FOLDER.get());
        if (slot == SLOT_BOOSTER) {
            for (Booster b : getBoosters()) if (stack.is(b.item())) return true;
            return false;
        }
        if (slot >= SLOT_INPUT_FIRST && slot < SLOT_INPUT_FIRST + SLOT_INPUT_COUNT) {
            if (level == null) return true;
            PlasmaForgeRecipe recipe = getRecipe(level);
            if (recipe == null) return false;
            int index = slot - SLOT_INPUT_FIRST;
            List<CountedIngredient> inputs = recipe.getItemInputs();
            return index < inputs.size() && inputs.get(index).ingredient().test(stack);
        }
        return false;
    }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { inputTank }; }

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { inputTank }; }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) {
            if (receiverNode != null) UniNodespace.destroyNode(serverLevel, receiverNode);
            if (providerNode != null) UniNodespace.destroyNode(serverLevel, providerNode);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("inputTank", inputTank.writeNBT(new CompoundTag()));
        tag.putDouble("progress", progress);
        tag.putBoolean("didProcess", didProcess);
        tag.putBoolean("connected", connected);
        tag.putInt("booster", booster);
        tag.putInt("maxBooster", maxBooster);
        tag.putLong("plasma", plasmaEnergySync);
        tag.putFloat("plasmaR", plasmaRed);
        tag.putFloat("plasmaG", plasmaGreen);
        tag.putFloat("plasmaB", plasmaBlue);
        if (selectedRecipeId != null) tag.putString("recipe", selectedRecipeId.toString());
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("inputTank")) inputTank.readNBT(tag.getCompound("inputTank"));
        this.progress = tag.getDouble("progress");
        this.didProcess = tag.getBoolean("didProcess");
        this.connected = tag.getBoolean("connected");
        this.booster = tag.getInt("booster");
        this.maxBooster = tag.getInt("maxBooster");
        this.plasmaEnergySync = tag.getLong("plasma");
        this.plasmaRed = tag.getFloat("plasmaR");
        this.plasmaGreen = tag.getFloat("plasmaG");
        this.plasmaBlue = tag.getFloat("plasmaB");
        this.selectedRecipeId = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.plasma_forge");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineFusionPlasmaForgeMenu.create(id, inventory, this);
    }

    private AABB renderBounds = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (renderBounds == null) {
            renderBounds = new AABB(
                    worldPosition.getX() - 5, worldPosition.getY(), worldPosition.getZ() - 5,
                    worldPosition.getX() + 5, worldPosition.getY() + 6, worldPosition.getZ() + 6);
        }
        return renderBounds;
    }

    // ═════════════════════════════ Roboterarme ═════════════════════════════

    public static final Random RAND = new Random();

    /** 1:1 aus {@code TileEntityFusionPlasmaForge.strikerPositions}. */
    public static final double[][] STRIKER_POSITIONS = new double[][] {
            {20, -30, -20, 30},
            {45, -80, 15, 30},
            {30, -45, -10, 30},
            {15, -20, -30, 30},
            {0, 10, -55, 30}
    };

    /** 1:1 aus {@code TileEntityFusionPlasmaForge.jetPositions}. */
    public static final double[][] JET_POSITIONS = new double[][] {
            {10, 45, -120},
            {20, 45, -140},
            {0, 30, -80},
            {0, 40, -100},
            {30, 50, -160}
    };

    public enum ForgeArmState {
        REPOSITION, EXTEND1, EXTEND2, RETRACT1, RETRACT2, RETIRE
    }

    public enum ForgeArmType {
        /** Drehpunkt -> Unterarm, Unterarm -> Oberarm, Oberarm -> Halterung, Halterung -> Schlaeger, Schlaeger 1, Schlaeger 2. */
        STRIKER(6),
        /** Drehpunkt -> Unterarm, Unterarm -> Oberarm, Oberarm -> Duese, Duese (ungenutzt). */
        JET(4);

        public final int angleCount;

        ForgeArmType(int angleCount) {
            this.angleCount = angleCount;
        }
    }

    /** 1:1-Port der inneren Klasse {@code ForgeArm}. */
    public static class ForgeArm {

        public final ForgeArmType type;
        public ForgeArmState state = ForgeArmState.RETIRE;
        public final double[] angles;
        public final double[] prevAngles;
        public final double[] targetAngles;
        public final double[] speed;
        public int actionDelay = 0;

        private final FusionPlasmaForgeBlockEntity owner;

        public ForgeArm(ForgeArmType type, FusionPlasmaForgeBlockEntity owner) {
            this.type = type;
            this.owner = owner;
            this.angles = new double[type.angleCount];
            this.prevAngles = new double[type.angleCount];
            this.targetAngles = new double[type.angleCount];
            this.speed = new double[type.angleCount];

            for (int i = 0; i < speed.length; i++) {
                if (i < 3 || i == 4) speed[i] = 15;
                if (i == 3) speed[i] = 15;
                if (i > 4) speed[i] = 0.5;
            }
        }

        public void updateArm() {
            System.arraycopy(angles, 0, prevAngles, 0, angles.length);

            if (!owner.didProcess) this.state = ForgeArmState.RETIRE;
            if (this.state == ForgeArmState.RETIRE) this.actionDelay = 0;

            if (this.actionDelay > 0) {
                this.actionDelay--;
                return;
            }

            (type == ForgeArmType.STRIKER ? STRIKER_STATE_MACHINE : JET_STATE_MACHINE).accept(this);
        }

        /** true, wenn kein Gelenk mehr in Bewegung ist. */
        public boolean move() {
            boolean didMove = false;

            for (int i = 0; i < angles.length; i++) {
                if (angles[i] == targetAngles[i]) continue;
                didMove = true;

                double angle = angles[i];
                double target = targetAngles[i];
                double turn = speed[i];
                double delta = Math.abs(angle - target);

                if (delta <= turn) {
                    angles[i] = targetAngles[i];
                    continue;
                }
                if (angle < target) angles[i] += turn;
                else angles[i] -= turn;
            }

            return !didMove;
        }

        public void playStrikerSound() {
            if (owner.level == null) return;
            // Original: "hbm:item.boltgun" mit Lautstaerke 0,25 und Tonhoehe 1,25.
            owner.level.playLocalSound(owner.worldPosition.getX() + 0.5, owner.worldPosition.getY() + 0.5,
                    owner.worldPosition.getZ() + 0.5, com.hbm_m.sound.ModSounds.BOLTGUN.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.25F, 1.25F, false);
        }

        public double[] getPositions(float interp) {
            double[] pos = new double[this.angles.length];
            for (int i = 0; i < pos.length; i++) {
                pos[i] = this.prevAngles[i] + (this.angles[i] - this.prevAngles[i]) * interp;
            }
            return pos;
        }
    }

    public static final Consumer<ForgeArm> STRIKER_STATE_MACHINE = arm -> {
        switch (arm.state) {
            case REPOSITION -> {
                if (arm.move()) {
                    arm.actionDelay = 5;
                    arm.state = ForgeArmState.EXTEND1;
                    arm.targetAngles[4] = 0.5D;
                }
            }
            case EXTEND1 -> {
                if (arm.move()) {
                    arm.actionDelay = 0;
                    arm.state = ForgeArmState.RETRACT1;
                    arm.targetAngles[4] = 0D;
                    arm.playStrikerSound();
                }
            }
            case RETRACT1 -> {
                if (arm.move()) {
                    arm.actionDelay = 0;
                    arm.state = ForgeArmState.EXTEND2;
                    arm.targetAngles[5] = 0.5D;
                }
            }
            case EXTEND2 -> {
                if (arm.move()) {
                    arm.actionDelay = 0;
                    arm.state = ForgeArmState.RETRACT2;
                    arm.targetAngles[5] = 0D;
                    arm.playStrikerSound();
                }
            }
            case RETRACT2 -> {
                if (arm.move()) {
                    if (RAND.nextInt(3) == 0) {
                        arm.actionDelay = 10;
                        arm.state = ForgeArmState.REPOSITION;
                        choosePosition(arm, STRIKER_POSITIONS);
                    } else {
                        arm.actionDelay = 5;
                        arm.state = ForgeArmState.EXTEND1;
                        arm.targetAngles[4] = 0.5D;
                    }
                }
            }
            case RETIRE -> {
                for (int i = 0; i < arm.targetAngles.length; i++) arm.targetAngles[i] = 0;
                if (arm.move()) {
                    arm.actionDelay = 10;
                    arm.state = ForgeArmState.REPOSITION;
                    choosePosition(arm, STRIKER_POSITIONS);
                }
            }
        }
    };

    public static final Consumer<ForgeArm> JET_STATE_MACHINE = arm -> {
        switch (arm.state) {
            case REPOSITION -> {
                if (arm.move()) {
                    arm.actionDelay = 20 + RAND.nextInt(3) * 10;
                    arm.state = ForgeArmState.REPOSITION;
                    choosePosition(arm, JET_POSITIONS);
                }
            }
            case RETIRE -> {
                for (int i = 0; i < arm.targetAngles.length; i++) arm.targetAngles[i] = 0;
                if (arm.move()) {
                    arm.actionDelay = 10;
                    arm.state = ForgeArmState.REPOSITION;
                    choosePosition(arm, JET_POSITIONS);
                }
            }
            default -> { }
        }
    };

    public static void choosePosition(ForgeArm arm, double[][] positions) {
        double[] newPos = positions[RAND.nextInt(positions.length)];
        System.arraycopy(newPos, 0, arm.targetAngles, 0, newPos.length);
    }
}
