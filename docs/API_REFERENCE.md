# HBM-M Energy System API Reference

## Quick Reference Card

### Core Interfaces

#### ILongEnergyStorage
```java
long receiveEnergy(long maxReceive, boolean simulate);
long extractEnergy(long maxExtract, boolean simulate);
long getEnergyStored();
long getMaxEnergyStored();
boolean canExtract();
boolean canReceive();
```

### Core Classes

#### BlockEntityEnergyStorage
```java
// Constructor
new BlockEntityEnergyStorage(long capacity, long maxReceive, long maxExtract)

// Methods
void setEnergy(long energy)
long getMaxReceive()
long getMaxExtract()
```

#### ItemEnergyStorage
```java
// Constructor
new ItemEnergyStorage(ItemStack stack, long capacity, long maxReceive, long maxExtract)

// Methods
void setEnergy(long energy)
```

#### WireNetworkManager
```java
// Singleton
WireNetworkManager.get()

// Methods
void onWireAdded(Level level, BlockPos pos)
void onWireRemoved(Level level, BlockPos pos)
void onWireChanged(Level level, BlockPos pos)
long requestEnergy(Level level, BlockPos pos, long maxRequest, boolean simulate)
```

### Utilities

#### EnergyFormatter
```java
String format(long energy)              // "1.5M"
String formatWithUnit(long energy, String unit)  // "1.5M HE"
String formatFE(long energy)            // "1.5M HE"
String formatRate(long rate)            // "1.5M HE/t"
```

#### LongDataPacker
```java
int packHigh(long value)                // Upper 32 bits
int packLow(long value)                 // Lower 32 bits
long unpack(int high, int low)          // Combine to long
```

### Capability Registration

```java
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ModCapabilities.register(event);
    }
}
```

### Capability Providers

#### For BlockEntity
```java
private final BlockEntityEnergyStorage energy = 
    new BlockEntityEnergyStorage(capacity, maxReceive, maxExtract);
private final LazyOptional<ILongEnergyStorage> energyCap = 
    LazyOptional.of(() -> energy);

@Override
public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
    if (cap == ModCapabilities.LONG_ENERGY) {
        return energyCap.cast();
    }
    return super.getCapability(cap, side);
}

@Override
public void invalidateCaps() {
    super.invalidateCaps();
    energyCap.invalidate();
}
```

#### For Item
```java
@Override
public <T> LazyOptional<T> getCapability(ItemStack stack, Capability<T> cap) {
    if (cap == ModCapabilities.LONG_ENERGY && !stack.isEmpty()) {
        ItemEnergyStorage storage = new ItemEnergyStorage(
            stack, CAPACITY, MAX_RECEIVE, MAX_EXTRACT
        );
        return LazyOptional.of(() -> storage).cast();
    }
    return LazyOptional.empty();
}
```

### Energy Transfer Patterns

#### Push Energy to Neighbors
```java
for (Direction dir : Direction.values()) {
    BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
    if (neighbor != null) {
        neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite())
            .ifPresent(storage -> {
                if (storage.canReceive()) {
                    long extracted = energy.extractEnergy(maxTransfer, false);
                    long received = storage.receiveEnergy(extracted, false);
                    if (received < extracted) {
                        energy.receiveEnergy(extracted - received, false);
                    }
                }
            });
    }
}
```

#### Pull Energy from Neighbors
```java
for (Direction dir : Direction.values()) {
    BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
    if (neighbor != null) {
        neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite())
            .ifPresent(storage -> {
                if (storage.canExtract()) {
                    long needed = energy.getMaxEnergyStored() - energy.getEnergyStored();
                    long extracted = storage.extractEnergy(needed, false);
                    energy.receiveEnergy(extracted, false);
                }
            });
    }
}
```

#### Request from Wire Network
```java
long needed = ENERGY_PER_TICK;
long received = WireNetworkManager.get()
    .requestEnergy(level, pos, needed, false);
if (received >= needed) {
    // Do work
}
```

### NBT Serialization

```java
@Override
protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    tag.putLong("Energy", energy.getEnergyStored());
}

@Override
public void load(CompoundTag tag) {
    super.load(tag);
    energy.setEnergy(tag.getLong("Energy"));
}
```

### Network Sync (ContainerData)

```java
// In BlockEntity
public int getEnergyHigh() {
    return LongDataPacker.packHigh(energy.getEnergyStored());
}

public int getEnergyLow() {
    return LongDataPacker.packLow(energy.getEnergyStored());
}

// In Menu
public long getEnergy() {
    return LongDataPacker.unpack(data.get(0), data.get(1));
}
```

## Constants and Limits

```java
// Energy values
public static final long FE = 1L;
public static final long kFE = 1_000L;
public static final long MFE = 1_000_000L;
public static final long GFE = 1_000_000_000L;
public static final long TFE = 1_000_000_000_000L;

// Practical limits
public static final long MAX_INT_ENERGY = Integer.MAX_VALUE; // 2,147,483,647
public static final long MAX_LONG_ENERGY = Long.MAX_VALUE;   // 9,223,372,036,854,775,807

// Recommended machine values
public static final long SMALL_MACHINE_CAPACITY = 50_000L;    // 50k FE
public static final long MEDIUM_MACHINE_CAPACITY = 500_000L;  // 500k FE
public static final long LARGE_MACHINE_CAPACITY = 5_000_000L; // 5M FE

public static final long LOW_CONSUMPTION = 20L;      // 20 FE/t
public static final long MEDIUM_CONSUMPTION = 100L;  // 100 FE/t
public static final long HIGH_CONSUMPTION = 500L;    // 500 FE/t
```

## Battery Priority System

```java
public enum Priority {
    LOW,     // Ordinal 0 - extracted last
    NORMAL,  // Ordinal 1 - default
    HIGH     // Ordinal 2 - extracted first
}

// Usage in MachineBatteryBlockEntity
private Priority priority = Priority.NORMAL;

// Wire network automatically sorts by priority.ordinal() descending
```

## Thread Safety Notes

- All energy operations are synchronized via locks
- `BlockEntityEnergyStorage` uses `energyLock`
- `ItemEnergyStorage` uses `stackLock`
- `WireNetworkManager` uses `ReentrantReadWriteLock`
- Safe to call from any thread

## Common Patterns

### Generator Pattern
```java
// Produces energy internally
energy = new BlockEntityEnergyStorage(capacity, 0L, maxOutput);

// In tick
if (isBurning()) {
    energy.receiveEnergy(generationRate, false);
}
pushEnergyToNeighbors();
```

### Consumer Pattern
```java
// Consumes energy internally
energy = new BlockEntityEnergyStorage(capacity, maxInput, 0L);

// In tick
if (canWork() && energy.getEnergyStored() >= cost) {
    energy.extractEnergy(cost, false);
    doWork();
}
```

### Battery Pattern
```java
// Stores and transfers energy
energy = new BlockEntityEnergyStorage(capacity, maxIO, maxIO);

// In tick
balanceWithNeighbors();
```

### Wire-Powered Pattern
```java
// No internal storage, pulls from network
// In tick
long received = WireNetworkManager.get()
    .requestEnergy(level, pos, needed, false);
```

## Error Handling

```java
// Always check for null
if (be == null) return;

// Check capability presence
LazyOptional<ILongEnergyStorage> cap = 
    be.getCapability(ModCapabilities.LONG_ENERGY, side);
if (!cap.isPresent()) return;

// Resolve safely
cap.ifPresent(storage -> {
    // Use storage here
});

// Or with resolve
ILongEnergyStorage storage = cap.resolve().orElse(null);
if (storage == null) return;
```

## Performance Tips

1. **Cache capabilities** - Don't resolve every tick
2. **Limit neighbor checks** - Check only when needed
3. **Use wire networks** - More efficient than per-block transfers
4. **Batch operations** - Transfer in larger chunks when possible
5. **Set appropriate limits** - Don't set maxTransfer higher than needed

## Debugging

```java
// Log energy state
LOGGER.debug("Energy: {}/{} FE", 
    energy.getEnergyStored(), 
    energy.getMaxEnergyStored());

// Check capability
LOGGER.debug("Has cap: {}", 
    be.getCapability(ModCapabilities.LONG_ENERGY).isPresent());

// Trace transfers
LOGGER.trace("Transferred {} FE from {} to {}", 
    amount, fromPos, toPos);
```

## Integration Examples

### With Forge Energy (int-based)
```java
// Automatically handled by ForgeToLongWrapper
// Just query both capabilities:
LazyOptional<ILongEnergyStorage> longCap = 
    be.getCapability(ModCapabilities.LONG_ENERGY, side);
LazyOptional<IEnergyStorage> forgeCap = 
    be.getCapability(ForgeCapabilities.ENERGY, side);
```

### With Custom Energy Systems
```java
// Create your own wrapper implementing ILongEnergyStorage
public class CustomToLongWrapper implements ILongEnergyStorage {
    private final CustomEnergyStorage custom;
    
    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        return custom.addEnergy((int)Math.min(maxReceive, Integer.MAX_VALUE), simulate);
    }
    // ... implement other methods
}
```

## Version History

- **v0.1.0-alpha**: Initial implementation
  - Long-based energy storage
  - Wire network manager with Union-Find
  - Battery priority system
  - Forge Energy compatibility

## See Also

- `docs/ENERGY_SYSTEM.md` - Full documentation
- `docs/MACHINE_TUTORIAL.md` - Step-by-step tutorial
- `docs/EnergySystemExamples.java` - Code examples
