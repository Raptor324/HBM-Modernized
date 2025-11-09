# HBM Nuclear Tech Modernized Energy System

## Overview

The HBM-M energy system is a completely rewritten system for energy generation, storage, and transmission between machines in Minecraft 1.20.1 on Forge.

## Key Features

### 1. Long-based Energy

Unlike the standard Forge energy system (which uses `int` and is limited to ~2.1 billion FE), our system uses `long` for energy storage and transmission, allowing work with much larger values (up to ~9.2 quintillion FE).

### 2. Backward Compatibility

The system is fully compatible with standard Forge Energy through wrapper adapters:
- `ForgeToLongWrapper` - converts `IEnergyStorage` to `ILongEnergyStorage`
- `LongToForgeWrapper` - converts `ILongEnergyStorage` to `IEnergyStorage`

### 3. Efficient Wire Network

Wire Network Manager uses the Union-Find algorithm for efficient wire network management:
- Automatic merging of connected wires into components
- Fast energy source lookup
- Minimal overhead when adding/removing wires
- Battery priority support

## Architecture

### Core Components

```
com.hbm_m.energy/
├── ILongEnergyStorage.java           # Main interface for energy storage
├── BlockEntityEnergyStorage.java     # Implementation for blocks
├── ItemEnergyStorage.java            # Implementation for items
├── WireNetworkManager.java           # Wire network manager
├── ForgeToLongWrapper.java           # Forge -> Long adapter
├── LongToForgeWrapper.java           # Long -> Forge adapter
└── LongDataPacker.java               # Pack long into int for sync
```

### ILongEnergyStorage Interface

```java
public interface ILongEnergyStorage {
    // Receive energy
    long receiveEnergy(long maxReceive, boolean simulate);
    
    // Extract energy
    long extractEnergy(long maxExtract, boolean simulate);
    
    // Get current energy
    long getEnergyStored();
    
    // Get max capacity
    long getMaxEnergyStored();
    
    // Can extract?
    boolean canExtract();
    
    // Can receive?
    boolean canReceive();
}
```

## Usage

### Creating Energy Storage for Blocks

```java
public class MyMachineBlockEntity extends BlockEntity {
    private final BlockEntityEnergyStorage energy = 
        new BlockEntityEnergyStorage(
            100000L,  // capacity
            1000L,    // maxReceive per tick
            1000L     // maxExtract per tick
        );
    
    private final LazyOptional<ILongEnergyStorage> energyCap = 
        LazyOptional.of(() -> energy);
    
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ModCapabilities.LONG_ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }
}
```

### Creating Energy Storage for Items

```java
public class MyBatteryItem extends Item {
    public static final long CAPACITY = 1000000L;
    
    @Override
    public <T> LazyOptional<T> getCapability(ItemStack stack, Capability<T> cap) {
        if (cap == ModCapabilities.LONG_ENERGY) {
            ItemEnergyStorage storage = new ItemEnergyStorage(
                stack, CAPACITY, 1000L, 1000L
            );
            return LazyOptional.of(() -> storage).cast();
        }
        return LazyOptional.empty();
    }
}
```

### Working with Wires

Wires automatically detect neighboring blocks with energy and create networks. The Wire Manager (`WireNetworkManager`) automatically:

1. Detects new wires when placed
2. Merges wires into components
3. Finds energy sources (machines, batteries)
4. Distributes energy by priority

```java
// Example: Request energy from wire network
long needed = 1000L;
long received = WireNetworkManager.get()
    .requestEnergy(level, wirePos, needed, false);
```

### Battery Priority System

Batteries can have three priority levels:
- `HIGH` (2) - highest priority, extracted first
- `NORMAL` (1) - normal priority (default)
- `LOW` (0) - lowest priority, extracted last

Example in `MachineBatteryBlockEntity`:
```java
public enum Priority {
    LOW,    // 0
    NORMAL, // 1
    HIGH    // 2
}
```

## Thread Safety

All critical energy operations are protected via `synchronized` blocks:
- `BlockEntityEnergyStorage` uses `energyLock`
- `ItemEnergyStorage` uses `stackLock`
- `WireNetworkManager` uses `ReentrantReadWriteLock`

## Capability Registration

Capability is registered in `ModCapabilities`:

```java
public static void register(RegisterCapabilitiesEvent event) {
    event.register(ILongEnergyStorage.class);
}
```

Registration is called in the main mod class:

```java
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ModCapabilities.register(event);
    }
}
```

## Optimization

### Wire Network Manager

- **Union-Find**: O(α(n)) for component union and find
- **Spanning Tree**: Minimum edges for connectivity
- **Source Caching**: Source lists recalculated only on topology changes
- **Source Limit**: Maximum 2048 sources per component to prevent lag

### Energy Transfer

- **BFS with Limit**: Maximum 2000 nodes when searching for sources
- **Prioritization**: Sources sorted by priority before extraction
- **Lazy Evaluation**: Sources checked only on energy request

## Debugging

### Logging

System uses `MainRegistry.LOGGER` for debug messages:
- Warnings about invalid values
- Errors on negative energy values
- Network topology info (when debug enabled)

### Safety Checks

- Automatic correction of negative values
- Overflow check on addition
- Constructor parameter validation

## Machine Examples

### Generator

```java
public class GeneratorBlockEntity extends BlockEntity {
    private final BlockEntityEnergyStorage energy = 
        new BlockEntityEnergyStorage(10000L, 0L, 100L);
    
    public static void serverTick(Level level, BlockPos pos, 
                                  BlockState state, GeneratorBlockEntity be) {
        if (be.isBurning()) {
            // Generate 20 FE/t
            be.energy.receiveEnergy(20L, false);
        }
        
        // Distribute energy to neighbors
        be.distributeEnergy();
    }
    
    private void distributeEnergy() {
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor != null) {
                neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite())
                    .ifPresent(storage -> {
                        if (storage.canReceive()) {
                            long extracted = energy.extractEnergy(100L, false);
                            long received = storage.receiveEnergy(extracted, false);
                            // Return unused
                            if (received < extracted) {
                                energy.receiveEnergy(extracted - received, false);
                            }
                        }
                    });
            }
        }
    }
}
```

### Consumer

```java
public class MachineBlockEntity extends BlockEntity {
    private final BlockEntityEnergyStorage energy = 
        new BlockEntityEnergyStorage(50000L, 1000L, 0L);
    
    public static void serverTick(Level level, BlockPos pos, 
                                  BlockState state, MachineBlockEntity be) {
        if (be.canWork() && be.energy.getEnergyStored() >= 100L) {
            // Consume 100 FE for work
            be.energy.extractEnergy(100L, false);
            be.doWork();
        }
    }
}
```

## Compatibility with Other Mods

Thanks to `ForgeToLongWrapper`, our system can:
- Receive energy from standard Forge generators
- Send energy to standard Forge machines
- Work with any mods using Forge Energy

The adapter automatically clips values to `Integer.MAX_VALUE` when necessary.

## Performance

### Benchmarks (Theoretical)

- **Union-Find operation**: < 1 μs
- **Find sources in 1000-wire network**: < 1 ms
- **Energy transfer through 100 wires**: < 100 μs
- **Topology update on wire add**: < 5 ms

### Recommendations

1. Avoid extremely large networks (>10000 wires)
2. Use batteries to buffer energy
3. Set battery priorities to optimize extraction
4. Don't create circular dependencies in energy system

## Future Improvements

- [ ] Visualization of energy flow in wires
- [ ] Energy consumption/generation statistics
- [ ] Wireless energy transmission
- [ ] Transformers for voltage conversion
- [ ] Energy loss in long wires
- [ ] Different wire types with varying throughput

## FAQ

**Q: Why use long instead of int?**  
A: Standard int is limited to 2.1 billion, which is insufficient for late-game in HBM Nuclear Tech.

**Q: Is the system compatible with other energy systems?**  
A: Yes, Forge Energy is supported via `ForgeToLongWrapper`. For other systems, custom adapters can be written.

**Q: How does battery priority work?**  
A: When requesting energy, sources are sorted by priority (high to low), and extraction proceeds in that order.

**Q: What happens when a wire is removed from the network?**  
A: `WireNetworkManager` automatically recalculates network components and updates source lists.

**Q: Is the system thread-safe?**  
A: Yes, all critical operations are protected by synchronization.

## Contact

Questions and suggestions: [GitHub Issues](https://github.com/Raptor324/HBM-Modernized/issues)
