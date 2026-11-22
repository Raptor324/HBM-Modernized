#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Полная программная транскрипция SiloComponent.java (1.7.10, 1394 строки) в
structure-template silo.nbt для HBM-Modernized.

Оригинал - кодовая структура (world/gen/component/SiloComponent.java); NBT-версии
не существует, поэтому каждый вызов размещения из addComponentParts воспроизведён
здесь 1:1 в том же порядке: наземный комплекс (асфальт, заборы+колючка, оборонные
платформы с турелью, здание доступа, палатка, обломки), большой люк силоса,
лестница вниз, синяя комната управления, офис, хранилища с сейфами и шкафами,
минные поля, стартовый зал с pad'ом и заряженной doomsday-ракетой, RTTY-факелы
(с реальными частотами в NBT), красный/жёлтый/зелёный/чёрный секторы.

Координаты: компонент фиксирован в неповернутой ориентации (coordBaseMode = SOUTH),
локальная (x,y,z) -> ячейка шаблона (x,y,z). Bounding box компонента 42x29x26 -
это только рамка планировщика; реальные размещения задают фактический объём:
x 0..42, y 0..36, z 0..26 (43x37x27). Высотная привязка оригинала (hpos ставит
пол y25 на высоту рельефа) воспроизводится проекцией на heightmap:
origin = WORLD_SURFACE_WG - (FLOOR_Y + 1).

Ячейки, которых нет в шаблоне = structure void = нативный рельеф сохраняется -
это точно семантика оригинала (structure never fills untouched cells).

Мультиблоки BlockDummyable (pad, люк, турель, телекс) в порту собираются сами
через onPlace -> placeMultiblockStructure, поэтому в шаблоне ставится только
ядро, а footprint вокруг ядра оставлен воздухом (formation требует
replaceable-клетки).

Запуск:  python tools/structure_converter/gen_silo.py
Выход:   src/main/resources/data/hbm_m/structures/silo.nbt
         src/main/resources/data/hbm_m/worldgen/structure/silo.json
         src/main/resources/data/hbm_m/worldgen/structure_set/silo.json
         src/main/resources/data/hbm_m/worldgen/template_pool/silo_pool.json
"""
import json
import os
import pathlib
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import convert  # nbt_write/nbt_load, convert_state, POOL_TABLES, DATA_VERSION

NS = "hbm_m"
ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "src" / "main" / "resources" / "data" / NS

# ---------------------------------------------------------------------------
# Объём шаблона (фактические пределы размещений SiloComponent при SOUTH)
# ---------------------------------------------------------------------------
SX, SY, SZ = 43, 37, 27
FLOOR_Y = 25          # уровень пола наземного комплекса (якорь hpos оригинала)

DEV = []              # список отклонений для финального отчёта
def dev(msg):
    if msg not in DEV:
        DEV.append(msg)

# ---------------------------------------------------------------------------
# Точная реплика java.util.Random (LCG) - выбор блоков в random-fill'ах
# воспроизводим при фиксированном seed
# ---------------------------------------------------------------------------
_MASK48 = (1 << 48) - 1

class JavaRandom:
    def __init__(self, seed):
        self.seed = (seed ^ 0x5DEECE66D) & _MASK48

    def _next(self, bits):
        self.seed = (self.seed * 0x5DEECE66D + 0xB) & _MASK48
        return self.seed >> (48 - bits)

    def nextInt(self, bound=None):
        if bound is None:
            v = self._next(32)
            return v - (1 << 32) if v >= (1 << 31) else v
        if bound <= 0:
            raise ValueError("bound")
        if (bound & -bound) == bound:
            return (bound * self._next(31)) >> 31
        while True:
            bits = self._next(31)
            val = bits % bound
            if bits - val + (bound - 1) < (1 << 31):
                return val

    def nextFloat(self):
        return self._next(24) / float(1 << 24)

    def nextBoolean(self):
        return self._next(1) != 0

# SiloComponent(Random, minX, minZ): rand.nextInt() x2 (freq/freqHatch) + далее
rand = JavaRandom(0x5140)   # произвольный фиксированный seed -> детерминизм
freq = rand.nextInt()       # частота RTTY пуска (факел зала + приёмник пада)
freqHatch = rand.nextInt()  # частота RTTY люка (факел-приёмник + рычаг-отправитель)

# ---------------------------------------------------------------------------
# Сетка шаблона: entry = [name, props, nbt]; нет ячейки = structure void
# ---------------------------------------------------------------------------
grid = {}

def st(name, props=None, nbt=None):
    return [name, props or {}, nbt]

def put(x, y, z, entry):
    grid[(x, y, z)] = entry

def get(x, y, z):
    return grid.get((x, y, z))

# ---------------------------------------------------------------------------
# Свойства (конвенции convert.py)
# ---------------------------------------------------------------------------
STAIR_FACING = {0: "east", 1: "west", 2: "south", 3: "north"}
HFACING = {2: "north", 3: "south", 4: "west", 5: "east"}     # hbm_facing / chest
FORGE_DIR = {0: "down", 1: "up", 2: "north", 3: "south", 4: "west", 5: "east"}
TORCH_FACING = {1: "east", 2: "west", 3: "south", 4: "north"}
DOOR_FACING = {0: "east", 1: "south", 2: "west", 3: "north"}
TRAPDOOR_FACING = {0: "east", 1: "west", 2: "south", 3: "north"}
BED_FACING = {0: "south", 1: "west", 2: "north", 3: "east"}

def bstr(v):
    return "true" if v else "false"

def stairs_props(meta):
    return {"facing": STAIR_FACING.get(meta & 3, "east"),
            "half": "top" if meta & 4 else "bottom"}

# ---------------------------------------------------------------------------
# Блочные маппинги
# ---------------------------------------------------------------------------
# Портовые блоки БЕЗ blockstate-свойств; convert.py для них всё равно эмитит
# facing/axis (наследие 1.7.10-меты) - свойство вырезается, иначе шаблон
# загрузится битым состоянием.
PLAIN_BLOCKS = {
    NS + ":deco_steel",        # DecorBlock-замена турелей, панели, стены
    NS + ":concrete_pillar",
    NS + ":steel_beam",
    NS + ":hev_battery",
    NS + ":machine_microwave",
    NS + ":radio_telex",
    NS + ":machine_transformer",
    NS + ":hadron_coil_alloy",
    NS + ":deco_lead",
    NS + ":deco_beryllium",
    NS + ":deco_red_copper",
    NS + ":crate_steel", NS + ":crate_iron", NS + ":crate_metal",
    NS + ":crate_ammo", NS + ":crate_can",
    NS + ":barrel_corroded", NS + ":lox_barrel", NS + ":pink_barrel",
    NS + ":red_barrel",
    NS + ":reinforced_stone", NS + ":reinforced_glass",
    NS + ":concrete", NS + ":asphalt",
}

def hbm(name, meta=0):
    """1.7.10 registry-name + meta -> порт state. Сначала локальные дополнения
    (блоки, отсутствующие в convert.HBM_MAP), затем конвейер convert_state."""
    if name == "asphalt":
        return st(NS + ":asphalt")
    if name == "barbed_wire":  # 1.7.10 meta 2/3/4/5 = сторона крепления
        return st(NS + ":barbed_wire", {"facing": HFACING.get(meta & 7, "north")})
    if name in ("radio_torch_sender", "radio_torch_receiver"):
        # 1.7.10 BlockRadioTorch: meta = ForgeDirection.getOrientation(meta)
        return st(NS + ":" + name, {"facing": FORGE_DIR.get(meta & 7, "up")})
    if name == "silo_hatch_large":
        # порт: DoorBlock (facing/part_role/door_moving/open); ядро мультиблока
        return st(NS + ":silo_hatch_large", {"facing": FORGE_DIR.get(meta & 7, "north")})
    if name == "launch_pad_rusted":
        return st(NS + ":launch_pad_rusted", {"facing": FORGE_DIR.get(meta & 7, "north")})
    if name in ("hadron_coil_alloy", "machine_transformer"):
        return st(NS + ":" + name)
    if name in ("capacitor_copper", "red_cable_gauge", "anvil_iron"):
        # MachineCapacitorBlock / RedCableGaugeBlock / AnvilBlock: FACING
        return st(NS + ":" + name, {"facing": HFACING.get(meta & 7, "north")})
    if name == "reeds":
        dev("ModBlocks.reeds отсутствует в порту -> minecraft:grass (декор затопленной комнаты)")
        return st("minecraft:grass")
    if name == "deco_pipe_rim_green_rusted":
        dev("deco_pipe_rim_green_rusted не портирован -> deco_pipe_rim_rusted (axis из мета//4)")
        name = "deco_pipe_rim_rusted"
    nm, props = convert.convert_state("hbm:" + name, meta)
    if nm in PLAIN_BLOCKS:
        props = {}
    return st(nm, props)

def van(name, meta=0):
    """Ванильные блоки 1.7.10, использованные в SiloComponent."""
    if name == "air":
        return st("minecraft:air")
    if name == "chest":
        return st("minecraft:chest", {"facing": HFACING.get(meta & 7, "north"),
                                      "type": "single", "waterlogged": "false"})
    if name == "iron_bars":
        return st("minecraft:iron_bars")
    if name == "lever":
        # 1.7.10: 5/6/7 = напольное крепление (rotation), 1-4 = стена;
        # силос использует 6 (пол, "placed on ground") и 2 (южная стена)
        o = meta & 7
        on = bool(meta & 8)
        if o in (5, 6, 7):
            rot = {5: "north", 6: "east", 7: "south"}.get(o, "north")
            return st("minecraft:lever", {"face": "floor", "facing": rot,
                                          "powered": bstr(on)})
        return st("minecraft:lever", {"face": "wall",
                                      "facing": TORCH_FACING.get(o, "north"),
                                      "powered": bstr(on)})
    if name == "cauldron":
        return st("minecraft:cauldron")
    if name == "hopper":
        return st("minecraft:hopper", {"facing": HFACING.get(meta & 7, "down"),
                                       "enabled": "true"})
    if name == "heavy_weighted_pressure_plate":
        return st("minecraft:heavy_weighted_pressure_plate", {"powered": "false"})
    if name == "oak_stairs":
        return st("minecraft:oak_stairs", stairs_props(meta))
    if name == "planks":
        return st("minecraft:oak_planks")
    if name == "crafting_table":
        return st("minecraft:crafting_table")
    if name == "flower_pot":
        return st("minecraft:flower_pot")
    if name == "web":
        return st("minecraft:cobweb")
    if name == "water":
        return st("minecraft:water", {"level": str(min(meta & 7, 7))})
    if name == "dirt":
        return st("minecraft:dirt")
    if name == "trapdoor":
        return st("minecraft:oak_trapdoor",
                  {"facing": TRAPDOOR_FACING.get(meta & 3, "north"),
                   "half": "bottom", "open": "false", "powered": "false",
                   "waterlogged": "false"})
    if name == "bed":
        return st("minecraft:bed", {"facing": BED_FACING.get(meta & 3, "south"),
                                    "part": "head" if meta & 8 else "foot",
                                    "occupied": "false"})
    raise ValueError("vanilla block not handled: " + name)

# Лут-пулы: конвейерные таблицы + отсутствующие там пулы (в порту таблицы есть)
POOL = dict(convert.POOL_TABLES)
POOL.update({
    "POOL_VAULT_RUSTY": NS + ":chests/vault_rusty",
    "POOL_VAULT_LOCKERS": NS + ":crates/steel_crate_vault_lockers",
    "POOL_NUKE_TRASH": NS + ":crates/iron_crate_nuke_trash",
})

CONTAINER_BE = dict(convert.CONTAINER_BE)  # crate_steel/crate_iron/chest

# ---------------------------------------------------------------------------
# Примитивы размещения (имена = методы Component/SiloComponent)
# ---------------------------------------------------------------------------
def fillWithBlocks(x1, y1, z1, x2, y2, z2, entry):
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            for y in range(y1, y2 + 1):
                put(x, y, z, entry)

def fillWithMetadataBlocks(x1, y1, z1, x2, y2, z2, entry):
    fillWithBlocks(x1, y1, z1, x2, y2, z2, entry)

def fillWithAir(x1, y1, z1, x2, y2, z2):
    fillWithBlocks(x1, y1, z1, x2, y2, z2, van("air"))

def placeBlockAtCurrentPosition(entry, x, y, z):
    put(x, y, z, entry)

def fillWithRandomizedBlocks(x1, y1, z1, x2, y2, z2, r, selector):
    # порядок циклов x->z->y как в Component.fillWithRandomizedBlocks,
    # чтобы последовательность rand-вызовов совпадала с оригиналом
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            for y in range(y1, y2 + 1):
                put(x, y, z, selector(r))

def randomlyFillWithBlocks(r, rand_limit, x1, y1, z1, x2, y2, z2, entry):
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            for y in range(y1, y2 + 1):
                if r.nextFloat() <= rand_limit:
                    put(x, y, z, entry)

def placeCore(entry, x, y, z):
    # BlockDummyable-ядро: в шаблоне один блок; dummy-клетки формируются
    # в рантайме (onPlace -> placeMultiblockStructure)
    placeBlockAtCurrentPosition(entry, x, y, z)

def fillSpace(x, y, z, dim, entry):
    """fillSpace оригинала заполнял dummy-клетки тем же блоком. В порту мультиблок
    строится сам, поэтому footprint (по int[6] {up,down,N,S,W,E} + rotate(NORTH))
    оставляется воздухом - formation требует replaceable-клетки."""
    up, dn, n, s, w, e = dim
    if n != 0 or s != 0 or w != 0 or e != 0:  # rotate(dim, NORTH) меняет N<->S, W<->E
        n, s, w, e = s, n, e, w
    for xx in range(x - w, x + e + 1):
        for zz in range(z - n, z + s + 1):
            for yy in range(y - dn, y + up + 1):
                if (xx, yy, zz) == (x, y, z):
                    continue  # ядро уже поставлено placeCore - не затирать
                put(xx, yy, zz, van("air"))

def placeDoor(door_id, dir_meta, opens_right, is_open, x, y, z):
    """Component.placeDoor (дверь = 2 блока в 1.7.10). Порт: DoorBlock - ядро
    мультиблока 1x2, верхняя половина ставится рантаймом -> клетка над дверью
    оставляется воздухом. open всегда false (двери порта управляются BE)."""
    if is_open:
        dev("placeDoor(isOpen=true): порт-двери стартуют закрытыми (open=false)")
    _ = opens_right  # hinge в порту не хранится в blockstate
    entry = st(NS + ":" + door_id, {"facing": DOOR_FACING.get(dir_meta & 3, "north")})
    placeBlockAtCurrentPosition(entry, x, y, z)
    if get(x, y + 1, z) is None or get(x, y + 1, z)[0] != "minecraft:air":
        placeBlockAtCurrentPosition(van("air"), x, y + 1, z)

def placeBed(meta, x, y, z):
    placeBlockAtCurrentPosition(van("bed", meta), x, y, z)
    off = {0: (0, 1), 1: (-1, 0), 2: (0, -1), 3: (1, 0)}.get(meta & 3, (0, 1))
    placeBlockAtCurrentPosition(van("bed", meta | 8), x + off[0], y, z + off[1])

def placeLever(dir_meta, on, x, y, z):
    # SOUTH: getButtonMeta = identity; meta = dirMeta | (on ? 8 : 0)
    placeBlockAtCurrentPosition(van("lever", dir_meta | (8 if on else 0)), x, y, z)

def placeRandomBobble(x, y, z):
    dev("bobblehead TE -> minecraft:flower_pot (порт голов-фигурок отложен, конвенция convert.py)")
    placeBlockAtCurrentPosition(van("flower_pot"), x, y, z)

def placeFoundationUnderneath(entry, x1, z1, x2, z2, feature_y, depth=2):
    """Оригинал заливал вниз, пока шёл воздух/нестабильный блок (до 15). В шаблоне
    высотная привязка выполняется проекцией + beard_thin, поэтому заливается
    фиксированная плита в depth слоя под полом."""
    fillWithBlocks(x1, feature_y - depth + 1, z1, x2, feature_y, z2, entry)

def setRTTYFreq(x, y, z, channel):
    """TileEntityRadioTorchBase.channel = String.valueOf(freq); lastState = 0.
    Портовые BE: RadioTorchBaseBlockEntity#channel + #lastState."""
    e = get(x, y, z)
    if e is None:
        raise ValueError("setRTTYFreq: torch missing at %r" % ((x, y, z),))
    be_id = NS + ":radio_torch_" + ("sender_be" if "sender" in e[0] else "receiver_be")
    nbt = e[2] or {"id": be_id}
    nbt["id"] = be_id
    nbt["channel"] = str(channel)
    nbt["lastState"] = 0
    e[2] = nbt

def generateInvContents(entry, pool, x, y, z, meta=0, _rolls=0):
    """Component.generateInvContents: контейнер + лут. Rolls оригинала уходят в
    WeightedRandomChestContent при генерации; в порту роллы задаёт loot table,
    поэтому в BE пишется только LootTable."""
    block = entry[0]
    be = CONTAINER_BE.get(block)
    if be is None:
        # filing_cabinet -> crate_metal (без BE в порту) -> стальной ящик с той же таблицей
        dev("контейнер %s без BE в порту -> crate_steel + steel_crate_be (лут сохранён)" % block)
        entry = hbm("crate_steel", meta)
        be = CONTAINER_BE["hbm_m:crate_steel"]
    entry = [entry[0], entry[1], {"id": be, "LootTable": POOL[pool]}]
    placeBlockAtCurrentPosition(entry, x, y, z)

def generateLockableContents(entry, pool, x, y, z, meta=0, _amount=0, _mod=0.0):
    """Component.generateLockableContents: safe (TileEntityLockableBase, пины/мод).
    Портовые crate-BE не хранят пины - блокировка не воспроизводится."""
    dev("generateLockableContents: пины замка (TileEntityLockableBase) не переносятся в порт-BE")
    generateInvContents(entry, pool, x, y, z, meta)

def placeCoreLaunchpad(x, y, z):
    """SiloComponent.placeCoreLaunchpad + missileLoaded=true: в порту ракета
    лежит в инвентаре BE (SLOT_MISSILE = 0)."""
    entry = hbm("launch_pad_rusted", 2)  # SOUTH -> getOpposite -> NORTH (ordinal 2)
    entry[2] = {
        "id": NS + ":launch_pad_rusted_be",
        "inventory": {
            "Size": 7,
            "Items": [{"Slot": 0, "id": NS + ":missile_doomsday_rusted", "Count": 1}],
        },
    }
    placeBlockAtCurrentPosition(entry, x, y, z)

def makeExtra(_entry_block, x, y, z):
    """BlockDummyable.makeExtra - в порту клетки пада расставляет рантайм;
    убедимся, что они остались воздухом (уже залиты fillSpace-заменой)."""
    if get(x, y, z) is None:
        put(x, y, z, van("air"))

def fillWithMines(x1, y1, z1, x2, y2, z2):
    """fillWithMines: rand.nextInt(15)==0 и клетка воздух и под ней твёрдо ->
    mine_ap + TE (waitingForPlayer=true). В шаблоне проверка по сетке."""
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            for y in range(y1, y2 + 1):
                if rand.nextInt(15) != 0:
                    continue
                cur = get(x, y, z)
                below = get(x, y - 1, z)
                if cur is not None and cur[0] == "minecraft:air" and \
                   below is not None and below[0] != "minecraft:air":
                    put(x, y, z, st(NS + ":mine_ap", None,
                                    {"id": NS + ":landmine_be",
                                     "primed": True, "waiting": True}))

# ---------------------------------------------------------------------------
# BlockSelector'ы SiloComponent (точная последовательность rand-вызовов)
# ---------------------------------------------------------------------------
class ConcreteBricks:
    @staticmethod
    def pick(r):
        c = r.nextFloat()
        if c < 0.4:
            return hbm("brick_concrete")
        elif c < 0.7:
            return hbm("brick_concrete_mossy")
        elif c < 0.9:
            return hbm("brick_concrete_cracked")
        return hbm("brick_concrete_broken")
    def __call__(self, r):
        return self.pick(r)

class ConcreteStairs:
    def __init__(self):
        self.meta = 0
    def setMetadata(self, meta):
        self.meta = meta
    def __call__(self, r):
        c = r.nextFloat()
        if c < 0.4:
            return hbm("brick_concrete_stairs", self.meta)
        elif c < 0.7:
            return hbm("brick_concrete_mossy_stairs", self.meta)
        elif c < 0.9:
            return hbm("brick_concrete_cracked_stairs", self.meta)
        return hbm("brick_concrete_broken_stairs", self.meta)

class DestroyedBricks:
    def __call__(self, r):
        c = r.nextFloat()
        if c < 0.3:
            meta = 0
            c = r.nextFloat()
            if 0.4 <= c < 0.7:
                meta |= 1
            elif c < 0.9:
                meta |= 2
            else:
                meta |= 3
            return hbm("concrete_brick_slab", meta)
        elif c < 0.6:
            meta = r.nextInt(4)
            c = r.nextFloat()
            if c < 0.4:
                return hbm("brick_concrete_stairs", meta)
            elif c < 0.7:
                return hbm("brick_concrete_mossy_stairs", meta)
            elif c < 0.9:
                return hbm("brick_concrete_cracked_stairs", meta)
            return hbm("brick_concrete_broken_stairs", meta)
        elif c < 0.9:
            c = r.nextFloat()
            if c < 0.4:
                return hbm("brick_concrete")
            elif c < 0.7:
                return hbm("brick_concrete_mossy")
            elif c < 0.9:
                return hbm("brick_concrete_cracked")
            return hbm("brick_concrete_broken")
        return van("air")

class SiloSupplies:
    def __call__(self, r):
        c = r.nextFloat()
        if c < 0.2:
            return hbm("barrel_corroded")
        elif c < 0.4:
            return hbm("crate_can")
        elif c < 0.45:
            return hbm("red_barrel")
        elif c < 0.5:
            return hbm("pink_barrel")
        return van("air")

# ---------------------------------------------------------------------------
# Мета-хелперы Component (при coordBaseMode = SOUTH все identity)
# ---------------------------------------------------------------------------
def getStairMeta(m):  # 0=W,1=E,2=N,3=S (конвенция HBM) -> в 1.7.10 meta напрямую
    return m
def getDecoMeta(m):   # 2=S,3=N,4=E,5=W
    return m
def getPillarMeta(m):
    return m
def getDecoModelMeta(m):  # <<2 внутри; N=0,S=1,W=2,E=3
    return m << 2
def getCRTMeta(m):    # (m + coordBaseMode) % 4
    return m % 4

# ===========================================================================
# addComponentParts - полная транскрипция (номера строк оригинала в комментах)
# ===========================================================================
def addComponentParts():
    ConcreteBricksS = ConcreteBricks()
    ConcreteStairsS = ConcreteStairs()
    DestroyedBricksS = DestroyedBricks()
    Supplies = SiloSupplies()

    stairW = getStairMeta(0)
    stairE = getStairMeta(1)
    stairN = getStairMeta(2)
    stairS = getStairMeta(3)

    decoS = getDecoMeta(2)
    decoN = getDecoMeta(3)
    decoE = getDecoMeta(4)
    decoW = getDecoMeta(5)
    pillarWE = getPillarMeta(4)
    pillarNS = getPillarMeta(8)
    decoModelN = getDecoModelMeta(0)
    decoModelW = getDecoModelMeta(2)
    decoModelE = getDecoModelMeta(3)

    """ SURFACE """
    fillWithAir(13, 26, 2, 42, 36, 20)                                             # L72

    placeFoundationUnderneath(hbm("concrete_colored_ext", 0), 13, 2, 42, 20, 24)   # L74

    # Floor
    fillWithBlocks(13, 25, 2, 42, 25, 4, hbm("asphalt"))                           # L77
    fillWithBlocks(13, 25, 5, 34, 25, 9, hbm("asphalt"))                           # L78
    fillWithBlocks(13, 25, 10, 14, 25, 18, hbm("asphalt"))                         # L79
    fillWithBlocks(24, 25, 10, 35, 25, 12, hbm("asphalt"))                         # L80
    fillWithBlocks(24, 25, 13, 26, 25, 18, hbm("asphalt"))                         # L81
    fillWithBlocks(13, 25, 19, 42, 25, 20, hbm("asphalt"))                         # L82
    fillWithBlocks(40, 25, 5, 42, 25, 18, hbm("asphalt"))                          # L83
    fillWithBlocks(39, 25, 10, 39, 25, 12, hbm("asphalt"))                         # L84
    fillWithMetadataBlocks(15, 25, 10, 23, 25, 10, hbm("concrete_colored_ext", 5)) # L85
    fillWithMetadataBlocks(15, 25, 11, 15, 25, 17, hbm("concrete_colored_ext", 5)) # L86
    fillWithMetadataBlocks(15, 25, 18, 23, 25, 18, hbm("concrete_colored_ext", 5)) # L87
    fillWithMetadataBlocks(23, 25, 11, 23, 25, 17, hbm("concrete_colored_ext", 5)) # L88
    placeBlockAtCurrentPosition(hbm("concrete_colored_ext", 5), 16, 25, 11)        # L89
    placeBlockAtCurrentPosition(hbm("concrete_colored_ext", 5), 22, 25, 11)        # L90
    placeBlockAtCurrentPosition(hbm("concrete_colored_ext", 5), 22, 25, 17)        # L91

    fillWithRandomizedBlocks(27, 25, 13, 39, 25, 18, rand, ConcreteBricksS)        # L95
    fillWithBlocks(36, 25, 4, 38, 25, 4, hbm("concrete_smooth"))                   # L96
    fillWithBlocks(35, 25, 5, 39, 25, 9, hbm("concrete_smooth"))                   # L97
    # Fences
    fillWithBlocks(13, 26, 2, 13, 28, 2, hbm("deco_steel"))                        # L99
    fillWithBlocks(42, 26, 2, 42, 28, 2, hbm("deco_steel"))                        # L100
    fillWithBlocks(13, 26, 20, 13, 28, 20, hbm("deco_steel"))                      # L101
    fillWithBlocks(42, 26, 20, 42, 28, 20, hbm("deco_steel"))                      # L102
    # N-facing
    fillWithBlocks(38, 26, 2, 41, 27, 2, hbm("fence_metal"))                       # L104
    fillWithBlocks(34, 26, 2, 36, 27, 2, hbm("fence_metal"))                       # L105
    fillWithBlocks(30, 26, 2, 31, 27, 2, hbm("fence_metal"))                       # L106
    placeBlockAtCurrentPosition(hbm("fence_metal"), 28, 27, 2)                     # L107
    fillWithBlocks(22, 26, 2, 28, 26, 2, hbm("fence_metal"))                       # L108
    fillWithBlocks(23, 27, 2, 26, 27, 2, hbm("fence_metal"))                       # L109
    fillWithBlocks(18, 26, 2, 20, 26, 2, hbm("fence_metal"))                       # L110
    fillWithBlocks(14, 26, 2, 16, 26, 2, hbm("fence_metal"))                       # L111
    placeBlockAtCurrentPosition(hbm("fence_metal"), 14, 27, 2)                     # L112
    fillWithMetadataBlocks(38, 28, 2, 41, 28, 2, hbm("barbed_wire", 5))            # L113
    fillWithMetadataBlocks(35, 28, 2, 36, 28, 2, hbm("barbed_wire", 5))            # L114
    fillWithMetadataBlocks(23, 28, 2, 25, 28, 2, hbm("barbed_wire", 5))            # L115
    placeBlockAtCurrentPosition(hbm("barbed_wire", 5), 14, 28, 2)                  # L116
    # W-facing
    fillWithBlocks(13, 26, 3, 13, 27, 4, hbm("fence_metal"))                       # L118
    fillWithBlocks(13, 26, 5, 13, 26, 6, hbm("fence_metal"))                       # L119
    fillWithBlocks(13, 26, 9, 13, 27, 9, hbm("fence_metal"))                       # L120
    placeBlockAtCurrentPosition(hbm("fence_metal"), 13, 26, 11)                    # L121
    fillWithBlocks(13, 26, 12, 13, 27, 19, hbm("fence_metal"))                     # L122
    fillWithMetadataBlocks(13, 28, 3, 13, 28, 4, hbm("barbed_wire", 2))            # L123
    fillWithMetadataBlocks(13, 28, 15, 13, 28, 19, hbm("barbed_wire", 2))          # L124
    # E-facing
    fillWithBlocks(42, 26, 3, 42, 27, 4, hbm("fence_metal"))                       # L126
    placeBlockAtCurrentPosition(hbm("fence_metal"), 42, 26, 7)                     # L127
    fillWithBlocks(42, 26, 9, 42, 26, 12, hbm("fence_metal"))                      # L128
    placeBlockAtCurrentPosition(hbm("fence_metal"), 42, 26, 14)                    # L129
    fillWithBlocks(42, 26, 15, 42, 27, 19, hbm("fence_metal"))                     # L130
    fillWithMetadataBlocks(42, 28, 3, 42, 28, 4, hbm("barbed_wire", 3))            # L131
    fillWithMetadataBlocks(42, 28, 15, 42, 28, 19, hbm("barbed_wire", 3))          # L132
    # S-facing
    fillWithBlocks(14, 26, 20, 17, 27, 20, hbm("fence_metal"))                     # L134
    fillWithBlocks(18, 26, 20, 22, 26, 20, hbm("fence_metal"))                     # L135
    fillWithBlocks(20, 27, 20, 21, 27, 20, hbm("fence_metal"))                     # L136
    fillWithBlocks(24, 26, 20, 25, 26, 20, hbm("fence_metal"))                     # L137
    placeBlockAtCurrentPosition(hbm("fence_metal"), 27, 26, 20)                    # L138
    fillWithBlocks(29, 26, 20, 32, 27, 20, hbm("fence_metal"))                     # L139
    placeBlockAtCurrentPosition(hbm("fence_metal"), 33, 26, 20)                    # L140
    fillWithBlocks(35, 26, 20, 37, 26, 20, hbm("fence_metal"))                     # L141
    placeBlockAtCurrentPosition(hbm("fence_metal"), 36, 27, 20)                    # L142
    placeBlockAtCurrentPosition(hbm("fence_metal"), 39, 26, 20)                    # L143
    fillWithBlocks(40, 26, 20, 41, 27, 20, hbm("fence_metal"))                     # L144
    fillWithMetadataBlocks(14, 28, 20, 17, 28, 20, hbm("barbed_wire", 4))          # L145
    fillWithMetadataBlocks(29, 28, 20, 32, 28, 20, hbm("barbed_wire", 4))          # L146
    fillWithMetadataBlocks(40, 28, 20, 41, 28, 20, hbm("barbed_wire", 4))          # L147

    # Defense Platforms
    placeBlockAtCurrentPosition(hbm("concrete_pillar"), 27, 26, 13)                # L150
    placeBlockAtCurrentPosition(hbm("concrete_pillar"), 32, 26, 13)                # L151
    placeBlockAtCurrentPosition(hbm("concrete_pillar"), 27, 26, 18)                # L152
    placeBlockAtCurrentPosition(hbm("concrete_pillar"), 32, 26, 18)                # L153
    fillWithRandomizedBlocks(28, 26, 14, 31, 26, 17, rand, ConcreteBricksS)        # L154

    ConcreteStairsS.setMetadata(stairN)
    fillWithRandomizedBlocks(28, 26, 13, 31, 26, 13, rand, ConcreteStairsS)        # L164
    ConcreteStairsS.setMetadata(stairW)
    fillWithRandomizedBlocks(27, 26, 14, 27, 26, 17, rand, ConcreteStairsS)        # L166
    ConcreteStairsS.setMetadata(stairS)
    fillWithRandomizedBlocks(28, 26, 18, 31, 26, 18, rand, ConcreteStairsS)        # L168
    fillWithMetadataBlocks(27, 27, 13, 32, 27, 13, hbm("concrete_slab", 1))        # L169
    fillWithMetadataBlocks(27, 27, 14, 27, 27, 17, hbm("concrete_slab", 1))        # L170
    fillWithMetadataBlocks(27, 27, 18, 32, 27, 18, hbm("concrete_slab", 1))        # L171
    fillWithMetadataBlocks(32, 27, 14, 32, 27, 17, hbm("concrete_slab", 1))        # L172
    # Methusalem
    placeCore(hbm("turret_howard_damaged", 2), 29, 27, 15)        # L174
    fillSpace(29, 27, 15, [0, 0, 1, 0, 1, 0], hbm("turret_howard_damaged"))        # L175
    # Destroyed platform
    placeBlockAtCurrentPosition(hbm("concrete_pillar"), 34, 26, 13)                # L177
    placeBlockAtCurrentPosition(hbm("concrete_pillar"), 39, 26, 13)                # L178
    placeBlockAtCurrentPosition(hbm("concrete_pillar"), 34, 26, 18)                # L179
    placeBlockAtCurrentPosition(hbm("concrete_pillar"), 39, 26, 18)                # L180
    fillWithRandomizedBlocks(35, 26, 13, 38, 26, 13, rand, ConcreteBricksS)        # L181
    fillWithRandomizedBlocks(32, 26, 15, 34, 26, 17, rand, ConcreteBricksS)        # L182
    ConcreteStairsS.setMetadata(stairS)
    fillWithRandomizedBlocks(35, 26, 18, 38, 26, 18, rand, ConcreteStairsS)        # L183
    ConcreteStairsS.setMetadata(stairE)
    fillWithRandomizedBlocks(39, 26, 14, 39, 26, 15, rand, ConcreteStairsS)        # L185

    fillWithRandomizedBlocks(35, 26, 14, 38, 26, 17, rand, DestroyedBricksS)       # L189
    fillWithMetadataBlocks(33, 27, 15, 33, 27, 17, hbm("concrete_slab", 1))        # L190
    placeBlockAtCurrentPosition(hbm("concrete_slab", 1), 34, 27, 17)               # L191
    fillWithMetadataBlocks(34, 27, 18, 36, 27, 18, hbm("concrete_slab", 1))        # L192
    fillWithMetadataBlocks(37, 27, 13, 39, 27, 13, hbm("concrete_slab", 1))        # L193
    placeBlockAtCurrentPosition(hbm("concrete_slab", 1), 39, 27, 14)               # L194
    placeBlockAtCurrentPosition(hbm("deco_steel"), 37, 25, 15)                     # L195
    placeBlockAtCurrentPosition(hbm("deco_pipe_rim_rusted", 0), 37, 26, 15)        # L196
    placeBlockAtCurrentPosition(hbm("deco_steel"), 36, 25, 16)                     # L197
    placeBlockAtCurrentPosition(hbm("deco_pipe_quad_rusted", 0), 36, 26, 16)       # L198

    # Access Building (staircase not included)
    fillWithRandomizedBlocks(35, 26, 5, 39, 28, 5, rand, ConcreteBricksS)          # L201
    fillWithRandomizedBlocks(35, 26, 6, 35, 28, 9, rand, ConcreteBricksS)          # L202
    fillWithRandomizedBlocks(39, 26, 6, 39, 28, 9, rand, ConcreteBricksS)          # L203
    fillWithRandomizedBlocks(36, 26, 9, 38, 28, 10, rand, ConcreteBricksS)         # L204
    fillWithRandomizedBlocks(36, 27, 11, 38, 27, 11, rand, ConcreteBricksS)        # L205
    fillWithRandomizedBlocks(36, 26, 12, 38, 26, 12, rand, ConcreteBricksS)        # L206
    ConcreteStairsS.setMetadata(stairS)
    fillWithRandomizedBlocks(36, 28, 11, 38, 28, 11, rand, ConcreteStairsS)        # L208
    fillWithRandomizedBlocks(36, 27, 12, 38, 27, 12, rand, ConcreteStairsS)        # L209
    fillWithBlocks(36, 29, 5, 38, 29, 9, hbm("concrete"))                          # L210
    fillWithMetadataBlocks(35, 29, 5, 35, 29, 9, hbm("concrete_stairs", stairW))   # L211
    fillWithMetadataBlocks(36, 29, 10, 38, 29, 10, hbm("concrete_stairs", stairS)) # L212
    fillWithMetadataBlocks(39, 29, 5, 39, 29, 9, hbm("concrete_stairs", stairE))   # L213
    # Deco
    placeBlockAtCurrentPosition(van("iron_bars"), 35, 27, 7)                       # L215
    placeBlockAtCurrentPosition(van("iron_bars"), 39, 27, 7)                       # L216
    placeDoor("metal_door", 1, rand.nextBoolean(), False, 37, 26, 5)               # L217

    # Stuff not-bolted down
    # Tent
    for j in range(4, 9, 2):                                                       # L221
        placeBlockAtCurrentPosition(hbm("steel_beam", 2), 20, 26, j)
        fillWithMetadataBlocks(16, 26, j, 16, 27, j, hbm("steel_beam", 3))

    fillWithBlocks(16, 28, 4, 17, 28, 8, hbm("brick_slab"))                        # L226
    fillWithMetadataBlocks(18, 27, 4, 19, 27, 8, hbm("brick_slab", 8))             # L227
    fillWithBlocks(20, 27, 4, 20, 27, 8, hbm("brick_slab"))                        # L228
    fillWithMetadataBlocks(16, 28, 6, 17, 28, 6, hbm("brick_slab", 5))             # L229
    fillWithMetadataBlocks(18, 27, 6, 19, 27, 6, hbm("brick_slab", 13))            # L230
    placeBlockAtCurrentPosition(hbm("brick_slab", 5), 20, 27, 6)                   # L231
    # Supplies
    fillWithRandomizedBlocks(27, 26, 7, 29, 26, 9, rand, Supplies)                 # L235
    fillWithRandomizedBlocks(17, 26, 4, 19, 26, 8, rand, Supplies)                 # L236
    # Wreckage
    placeBlockAtCurrentPosition(hbm("barrel_corroded"), 32, 26, 5)                 # L239
    fillWithRandomizedBlocks(32, 26, 7, 32, 26, 7, rand, DestroyedBricksS)         # L240
    placeBlockAtCurrentPosition(hbm("barrel_corroded"), 31, 26, 9)                 # L241
    fillWithRandomizedBlocks(31, 26, 11, 32, 26, 11, rand, DestroyedBricksS)       # L242
    fillWithRandomizedBlocks(34, 26, 11, 34, 26, 11, rand, DestroyedBricksS)       # L243
    fillWithRandomizedBlocks(41, 26, 17, 41, 26, 17, rand, DestroyedBricksS)       # L244
    placeBlockAtCurrentPosition(hbm("concrete_slab", 1), 37, 26, 19)               # L245

    # Large Silo Hatch
    placeCore(hbm("silo_hatch_large", 2), 19, 26, 14)             # L248
    fillSpace(19, 26, 14, [0, 0, 3, 3, 3, 3], hbm("silo_hatch_large"))             # L249
    placeBlockAtCurrentPosition(hbm("radio_torch_receiver", 1), 16, 25, 17)        # L250
    setRTTYFreq(16, 25, 17, freqHatch)                                             # L251

    # Containers
    generateInvContents(van("chest", 2), "POOL_VERTIBIRD", 36, 26, 17, _rolls=5)   # L254

    """ Stairway """
    fillWithAir(37, 26, 9, 37, 27, 10)                                             # L257
    placeBlockAtCurrentPosition(van("air"), 37, 25, 10)                            # L258
    fillWithAir(37, 24, 11, 37, 26, 11)                                            # L259
    fillWithAir(37, 23, 12, 37, 25, 12)                                            # L260
    fillWithAir(37, 21, 13, 37, 24, 14)                                            # L261
    # bottoms
    for i in range(5):                                                             # L263
        fillWithRandomizedBlocks(36, 24 - i, 9 + i, 38, 24 - i, 9 + i, rand, ConcreteBricksS)
        placeBlockAtCurrentPosition(hbm("concrete_smooth_stairs", stairS), 37, 25 - i, 9 + i)

    # walls
    for i in range(36, 39, 2):                                                     # L269
        fillWithRandomizedBlocks(i, 26, 11, i, 26, 11, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(i, 25, 10, i, 25, 12, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(i, 24, 10, i, 24, 15, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(i, 23, 11, i, 23, 15, rand, ConcreteBricksS)
        fillWithMetadataBlocks(i, 22, 12, i, 22, 15, hbm("concrete_colored", 11))
        fillWithRandomizedBlocks(i, 21, 13, i, 21, 15, rand, ConcreteBricksS)

    fillWithBlocks(36, 20, 14, 38, 20, 15, hbm("concrete_smooth"))                 # L278
    fillWithAir(36, 21, 14, 36, 22, 14)                                            # L279

    """ Blue Control Room """
    # Air
    placeBlockAtCurrentPosition(van("air"), 36, 23, 17)                            # L283
    fillWithAir(34, 21, 13, 35, 23, 19)                                            # L284
    fillWithAir(33, 21, 13, 33, 23, 15)                                            # L285
    fillWithAir(29, 21, 16, 31, 23, 19)                                            # L286
    fillWithAir(29, 21, 12, 32, 23, 15)                                            # L287
    fillWithAir(28, 21, 10, 32, 23, 11)                                            # L288
    fillWithAir(27, 21, 7, 31, 23, 9)                                              # L289
    fillWithAir(27, 21, 5, 30, 23, 6)                                              # L290
    fillWithAir(27, 21, 4, 29, 23, 4)                                              # L291
    fillWithAir(27, 21, 3, 28, 23, 3)                                              # L292
    fillWithAir(26, 22, 7, 26, 23, 8)                                              # L293
    fillWithAir(25, 22, 7, 25, 23, 7)                                              # L294
    fillWithAir(24, 21, 2, 26, 23, 6)                                              # L295
    fillWithAir(22, 21, 5, 23, 23, 5)                                              # L296
    fillWithAir(16, 21, 1, 23, 23, 4)                                              # L297
    # Floor and Ceiling
    for i in range(20, 25, 4):                                                     # L299
        fillWithBlocks(15, i, 0, 23, i, 5, hbm("concrete_smooth"))
        fillWithBlocks(24, i, 1, 26, i, 6, hbm("concrete_smooth"))
        fillWithBlocks(25, i, 7, 26, i, 7, hbm("concrete_smooth"))
        placeBlockAtCurrentPosition(hbm("concrete_smooth"), 26, i, 8)
        fillWithBlocks(27, i, 2, 28, i, 6, hbm("concrete_smooth"))
        placeBlockAtCurrentPosition(hbm("concrete_smooth"), 29, i, 3)
        fillWithBlocks(29, i, 4, 30, i, 4, hbm("concrete_smooth"))
        fillWithBlocks(29, i, 5, 31, i, 6, hbm("concrete_smooth"))
        fillWithBlocks(27, i, 7, 32, i, 9, hbm("concrete_smooth"))
        fillWithBlocks(28, i, 10, 33, i, 20, hbm("concrete_smooth"))
        fillWithBlocks(34, i, 12, 35, i, 15, hbm("concrete_smooth"))
        fillWithBlocks(34, i, 16, 37, i, 20, hbm("concrete_smooth"))
    # Walls
    # Curve (N-facing)
    for i in range(21, 24, 2):                                                     # L315
        fillWithRandomizedBlocks(15, i, 0, 23, i, 0, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(24, i, 1, 26, i, 1, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(27, i, 2, 28, i, 2, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(29, i, 3, 29, i, 3, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(30, i, 4, 30, i, 4, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(31, i, 5, 31, i, 6, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(32, i, 7, 32, i, 9, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(33, i, 10, 33, i, 12, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(34, i, 12, 35, i, 12, rand, ConcreteBricksS)

    fillWithMetadataBlocks(15, 22, 0, 23, 22, 0, hbm("concrete_colored", 11))      # L327
    fillWithMetadataBlocks(24, 22, 1, 26, 22, 1, hbm("concrete_colored", 11))      # L328
    fillWithMetadataBlocks(27, 22, 2, 28, 22, 2, hbm("concrete_colored", 11))      # L329
    placeBlockAtCurrentPosition(hbm("concrete_colored", 11), 29, 22, 3)            # L330
    placeBlockAtCurrentPosition(hbm("concrete_colored", 11), 30, 22, 4)            # L331
    fillWithMetadataBlocks(31, 22, 5, 31, 22, 6, hbm("concrete_colored", 11))      # L332
    fillWithMetadataBlocks(32, 22, 7, 32, 22, 9, hbm("concrete_colored", 11))      # L333
    fillWithMetadataBlocks(33, 22, 10, 33, 22, 12, hbm("concrete_colored", 11))    # L334
    fillWithMetadataBlocks(34, 22, 12, 35, 22, 12, hbm("concrete_colored", 11))    # L335
    # W-facing side
    fillWithRandomizedBlocks(15, 21, 1, 15, 21, 4, rand, ConcreteBricksS)          # L337
    fillWithMetadataBlocks(15, 22, 1, 15, 22, 4, hbm("concrete_colored", 11))      # L338
    fillWithRandomizedBlocks(15, 23, 1, 15, 23, 4, rand, ConcreteBricksS)          # L339
    # Inner Curve (S-facing)
    for i in range(20, 24, 3):                                                     # L341
        fillWithRandomizedBlocks(15, i, 6, 16, i + 1, 6, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(22, i, 6, 23, i + 1, 6, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(24, i, 7, 24, i + 1, 7, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(25, i, 8, 25, i + 1, 8, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(26, i, 9, 26, i + 1, 9, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(27, i, 10, 27, i + 1, 11, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(27, i, 17, 27, i + 1, 18, rand, ConcreteBricksS)
    fillWithRandomizedBlocks(15, 21, 5, 18, 21, 5, rand, ConcreteBricksS)          # L350
    fillWithRandomizedBlocks(20, 21, 5, 21, 21, 5, rand, ConcreteBricksS)          # L351
    fillWithRandomizedBlocks(15, 23, 5, 21, 23, 5, rand, ConcreteBricksS)          # L352
    fillWithRandomizedBlocks(28, 21, 12, 28, 21, 13, rand, ConcreteBricksS)        # L353
    fillWithRandomizedBlocks(28, 21, 15, 28, 21, 20, rand, ConcreteBricksS)        # L354
    fillWithRandomizedBlocks(28, 23, 12, 28, 23, 20, rand, ConcreteBricksS)        # L355
    fillWithMetadataBlocks(15, 22, 6, 16, 22, 6, hbm("concrete_colored", 11))      # L356
    placeBlockAtCurrentPosition(hbm("concrete_colored", 11), 22, 22, 6)            # L357
    placeBlockAtCurrentPosition(hbm("reinforced_glass"), 23, 22, 6)                # L358
    placeBlockAtCurrentPosition(hbm("reinforced_glass"), 24, 22, 7)                # L359
    placeBlockAtCurrentPosition(hbm("reinforced_glass"), 25, 22, 8)                # L360
    placeBlockAtCurrentPosition(hbm("reinforced_glass"), 26, 22, 9)                # L361
    placeBlockAtCurrentPosition(hbm("reinforced_glass"), 27, 22, 10)               # L362
    placeBlockAtCurrentPosition(hbm("concrete_colored", 11), 27, 22, 11)           # L363
    fillWithMetadataBlocks(27, 22, 17, 27, 22, 18, hbm("concrete_colored", 11))    # L364
    fillWithMetadataBlocks(15, 22, 5, 18, 22, 5, hbm("concrete_colored", 11))      # L365
    fillWithMetadataBlocks(20, 22, 5, 21, 22, 5, hbm("concrete_colored", 11))      # L366
    fillWithMetadataBlocks(28, 22, 12, 28, 22, 13, hbm("concrete_colored", 11))    # L367
    fillWithMetadataBlocks(28, 22, 15, 28, 22, 20, hbm("concrete_colored", 11))    # L368
    # S-facing side
    fillWithRandomizedBlocks(29, 21, 20, 36, 21, 20, rand, ConcreteBricksS)        # L370
    fillWithMetadataBlocks(29, 22, 20, 36, 22, 20, hbm("concrete_colored", 11))    # L371
    fillWithRandomizedBlocks(29, 23, 20, 36, 23, 20, rand, ConcreteBricksS)        # L372
    # E-facing side
    fillWithRandomizedBlocks(37, 21, 15, 37, 21, 20, rand, ConcreteBricksS)        # L374
    fillWithMetadataBlocks(37, 22, 15, 37, 22, 20, hbm("concrete_colored", 11))    # L375
    fillWithRandomizedBlocks(37, 23, 15, 37, 23, 20, rand, ConcreteBricksS)        # L376
    fillWithRandomizedBlocks(37, 24, 15, 37, 24, 15, rand, ConcreteBricksS)        # L377
    # Internal walls
    fillWithRandomizedBlocks(32, 21, 16, 32, 21, 19, rand, ConcreteBricksS)        # L379
    fillWithMetadataBlocks(32, 22, 16, 32, 22, 19, hbm("concrete_colored", 11))    # L380
    fillWithRandomizedBlocks(32, 23, 16, 32, 23, 19, rand, ConcreteBricksS)        # L381
    # Arches
    ConcreteStairsS.setMetadata(stairS | 4)
    fillWithRandomizedBlocks(24, 23, 2, 26, 23, 2, rand, ConcreteStairsS)          # L384
    fillWithRandomizedBlocks(27, 23, 3, 28, 23, 3, rand, ConcreteStairsS)          # L385
    ConcreteStairsS.setMetadata(stairW | 4)
    fillWithRandomizedBlocks(30, 23, 5, 30, 23, 6, rand, ConcreteStairsS)          # L387
    fillWithRandomizedBlocks(31, 23, 7, 31, 23, 9, rand, ConcreteStairsS)          # L388
    # Doors
    placeDoor("door_bunker", 1, rand.nextBoolean(), True, 19, 21, 5)               # L390
    placeDoor("door_bunker", 2, rand.nextBoolean(), False, 28, 21, 14)             # L391
    # Deco
    # Computer area
    fillWithBlocks(33, 21, 19, 33, 23, 19, hbm("deco_steel"))                      # L404
    fillWithBlocks(33, 21, 17, 33, 23, 17, hbm("deco_steel"))                      # L405
    placeBlockAtCurrentPosition(hbm("tape_recorder", decoW), 33, 21, 18)           # L406
    placeBlockAtCurrentPosition(hbm("deco_crt", getCRTMeta(1) | 8), 33, 22, 18)    # L407
    placeBlockAtCurrentPosition(hbm("tape_recorder", decoW), 33, 23, 18)           # L408
    fillWithMetadataBlocks(33, 21, 16, 33, 23, 16, hbm("tape_recorder", decoW))    # L409
    placeBlockAtCurrentPosition(hbm("reinforced_stone_stairs", stairE | 4), 34, 21, 19)  # L410
    placeBlockAtCurrentPosition(hbm("brick_slab", 8), 34, 21, 18)                  # L411
    placeBlockAtCurrentPosition(van("heavy_weighted_pressure_plate"), 34, 22, 18)  # L412
    placeBlockAtCurrentPosition(hbm("capacitor_copper", decoE), 36, 21, 16)        # L413
    placeBlockAtCurrentPosition(hbm("deco_steel"), 36, 21, 17)                     # L414
    placeBlockAtCurrentPosition(hbm("hadron_coil_alloy"), 36, 21, 19)              # L415
    fillWithMetadataBlocks(36, 22, 16, 36, 23, 16, hbm("tape_recorder", decoE))    # L416
    placeBlockAtCurrentPosition(hbm("deco_computer", decoModelW), 36, 22, 17)      # L417
    fillWithMetadataBlocks(36, 21, 18, 36, 23, 18, hbm("tape_recorder", decoE))    # L418
    fillWithMetadataBlocks(36, 22, 19, 36, 23, 19, hbm("deco_crt", getCRTMeta(3) | 12))  # L419
    # Cabinets + Pipe
    fillWithBlocks(32, 21, 11, 32, 22, 11, hbm("deco_pipe_framed_green_rusted"))   # L421
    placeBlockAtCurrentPosition(hbm("deco_pipe_framed_green_rusted", pillarNS), 32, 23, 10)  # L422
    placeBlockAtCurrentPosition(hbm("deco_steel"), 32, 23, 11)                     # L423
    fillWithMetadataBlocks(32, 23, 12, 32, 23, 15, hbm("deco_pipe_framed_green_rusted", pillarNS))  # L424

    placeBlockAtCurrentPosition(hbm("turret_sentry_damaged"), 30, 21, 16)          # L426
    # Desk Area
    fillWithBlocks(27, 21, 9, 28, 21, 9, hbm("deco_steel"))                        # L428
    placeBlockAtCurrentPosition(hbm("deco_beryllium"), 26, 21, 8)                  # L429
    fillWithBlocks(25, 21, 7, 26, 21, 7, hbm("deco_steel"))                        # L430
    fillWithBlocks(24, 21, 5, 24, 21, 6, hbm("deco_steel"))                        # L431
    placeBlockAtCurrentPosition(hbm("deco_computer", decoModelN), 28, 22, 9)       # L432
    placeBlockAtCurrentPosition(van("lever", 6), 26, 22, 8)                        # L433
    placeBlockAtCurrentPosition(van("lever", 6), 25, 22, 7)                        # L434
    placeBlockAtCurrentPosition(van("oak_stairs", stairS), 28, 21, 7)              # L435
    placeBlockAtCurrentPosition(van("oak_stairs", stairW), 27, 21, 5)              # L436

    placeBlockAtCurrentPosition(hbm("tape_recorder", decoE), 30, 21, 5)            # L438
    placeBlockAtCurrentPosition(van("flower_pot"), 27, 21, 3)                      # L439

    placeBlockAtCurrentPosition(van("flower_pot"), 25, 22, 2)                      # L441

    placeCore(hbm("radio_telex", 5), 25, 21, 5)                    # L443
    fillSpace(25, 21, 5, [0, 0, 0, 0, 1, 0], hbm("radio_telex"))                   # L444
    placeBlockAtCurrentPosition(hbm("radio_torch_sender", 0), 26, 20, 8)           # L445
    setRTTYFreq(26, 20, 8, freq)                                                   # L446
    placeBlockAtCurrentPosition(hbm("radio_torch_sender", 0), 25, 20, 7)           # L447
    setRTTYFreq(25, 20, 7, freqHatch)                                              # L448

    # Machine/Small Desk Area
    placeBlockAtCurrentPosition(hbm("deco_pipe_framed_green_rusted", pillarWE), 23, 23, 1)  # L451
    fillWithMetadataBlocks(16, 23, 1, 19, 23, 1, hbm("deco_pipe_framed_green_rusted", pillarWE))  # L452
    placeBlockAtCurrentPosition(hbm("deco_steel"), 20, 21, 1)                      # L453
    placeBlockAtCurrentPosition(hbm("capacitor_copper", decoN), 20, 22, 1)         # L454
    placeBlockAtCurrentPosition(hbm("reinforced_stone_stairs", stairS | 4), 21, 21, 1)  # L455
    placeBlockAtCurrentPosition(hbm("deco_crt", getCRTMeta(2) | 4), 21, 22, 1)     # L456
    placeBlockAtCurrentPosition(hbm("deco_steel"), 22, 21, 1)                      # L457
    placeBlockAtCurrentPosition(hbm("capacitor_copper", decoN), 22, 22, 1)         # L458
    fillWithBlocks(20, 23, 1, 22, 23, 1, hbm("deco_steel"))                        # L459
    placeBlockAtCurrentPosition(hbm("hev_battery"), 23, 21, 1)                     # L460
    placeBlockAtCurrentPosition(van("oak_stairs", stairW), 18, 21, 2)              # L461
    fillWithBlocks(16, 21, 1, 16, 21, 3, hbm("deco_steel"))                        # L462

    placeBlockAtCurrentPosition(hbm("deco_computer", decoModelE), 16, 22, 2)       # L464
    placeBlockAtCurrentPosition(van("flower_pot"), 16, 22, 3)                      # L465
    placeRandomBobble(16, 22, 4)                                                   # L466

    # Containers
    generateInvContents(hbm("filing_cabinet", decoModelW), "POOL_FILING_CABINET", 31, 21, 17, _rolls=4)  # L469
    generateInvContents(hbm("filing_cabinet", decoModelW), "POOL_VAULT_LAB", 31, 21, 18, _rolls=6)       # L470
    generateInvContents(hbm("filing_cabinet", decoModelW), "POOL_FILING_CABINET", 31, 21, 19, _rolls=4)  # L471
    generateInvContents(hbm("filing_cabinet", decoModelW), "POOL_FILING_CABINET", 31, 22, 17, _rolls=4)  # L472
    generateInvContents(hbm("filing_cabinet", decoModelW), "POOL_FILING_CABINET", 31, 22, 19, _rolls=4)  # L473
    generateInvContents(hbm("crate_steel", 2), "POOL_OFFICE_TRASH", 29, 21, 19, _rolls=8)                # L474
    generateInvContents(hbm("filing_cabinet", decoModelE), "POOL_FILING_CABINET", 29, 21, 18, _rolls=4)  # L475
    generateInvContents(hbm("filing_cabinet", decoModelE), "POOL_FILING_CABINET", 29, 21, 17, _rolls=4)  # L476

    generateInvContents(hbm("filing_cabinet", decoModelW), "POOL_FILING_CABINET", 31, 21, 8, _rolls=5)   # L478

    generateInvContents(hbm("crate_steel", 3), "POOL_MACHINE_PARTS", 25, 21, 2, _rolls=4)                # L480

    generateInvContents(hbm("filing_cabinet", decoModelN), "POOL_FILING_CABINET", 23, 21, 5, _rolls=5)   # L482

    generateLockableContents(hbm("safe", decoW), "POOL_VAULT_RUSTY", 16, 21, 4, meta=decoW)              # L484

    """ Silo """
    #	TOP
    # Air
    fillWithAir(17, 21, 6, 21, 23, 6)                                              # L489
    fillWithAir(15, 21, 7, 23, 23, 10)                                             # L490
    fillWithAir(24, 21, 8, 24, 23, 10)                                             # L491
    fillWithAir(25, 21, 9, 25, 23, 10)                                             # L492
    fillWithAir(26, 21, 10, 26, 23, 10)                                            # L493
    fillWithAir(23, 21, 11, 26, 23, 17)                                            # L494
    fillWithAir(27, 21, 12, 27, 23, 16)                                            # L495
    fillWithAir(26, 21, 18, 26, 23, 18)                                            # L496
    fillWithAir(25, 21, 18, 25, 23, 19)                                            # L497
    fillWithAir(24, 21, 18, 24, 23, 20)                                            # L498
    fillWithAir(15, 21, 18, 23, 23, 21)                                            # L499
    fillWithAir(17, 21, 22, 21, 23, 22)                                            # L500
    fillWithAir(14, 21, 18, 14, 23, 20)                                            # L501
    fillWithAir(13, 21, 18, 13, 23, 19)                                            # L502
    fillWithAir(12, 21, 18, 12, 23, 18)                                            # L503
    fillWithAir(12, 21, 11, 15, 23, 17)                                            # L504
    fillWithAir(11, 21, 12, 11, 23, 16)                                            # L505
    fillWithAir(12, 21, 10, 12, 23, 10)                                            # L506
    fillWithAir(13, 21, 9, 13, 23, 10)                                             # L507
    fillWithAir(14, 21, 8, 14, 23, 10)                                             # L508
    # Floor
    fillWithBlocks(13, 20, 9, 13, 20, 11, hbm("concrete_smooth"))                  # L510
    fillWithBlocks(14, 20, 8, 14, 20, 9, hbm("concrete_smooth"))                   # L511
    fillWithBlocks(15, 20, 7, 16, 20, 8, hbm("concrete_smooth"))                   # L512
    fillWithBlocks(17, 20, 6, 21, 20, 7, hbm("concrete_smooth"))                   # L513
    fillWithBlocks(22, 20, 7, 23, 20, 8, hbm("concrete_smooth"))                   # L514
    fillWithBlocks(24, 20, 8, 24, 20, 9, hbm("concrete_smooth"))                   # L515
    placeBlockAtCurrentPosition(hbm("concrete_smooth"), 25, 20, 9)                 # L516
    fillWithBlocks(25, 20, 10, 26, 20, 11, hbm("concrete_smooth"))                 # L517
    fillWithBlocks(26, 20, 12, 27, 20, 16, hbm("concrete_smooth"))                 # L518
    fillWithBlocks(25, 20, 17, 26, 20, 18, hbm("concrete_smooth"))                 # L519
    fillWithBlocks(24, 20, 19, 25, 20, 19, hbm("concrete_smooth"))                 # L520
    placeBlockAtCurrentPosition(hbm("concrete_smooth"), 24, 20, 20)                # L521
    fillWithBlocks(22, 20, 20, 23, 20, 21, hbm("concrete_smooth"))                 # L522
    fillWithBlocks(17, 20, 21, 21, 20, 22, hbm("concrete_smooth"))                 # L523
    fillWithBlocks(15, 20, 20, 16, 20, 21, hbm("concrete_smooth"))                 # L524
    fillWithBlocks(14, 20, 19, 14, 20, 20, hbm("concrete_smooth"))                 # L525
    # grates
    fillWithMetadataBlocks(14, 20, 10, 15, 20, 18, hbm("steel_grate", 7))          # L527
    fillWithMetadataBlocks(13, 20, 12, 13, 20, 16, hbm("steel_grate", 7))          # L528
    fillWithMetadataBlocks(17, 20, 8, 21, 20, 8, hbm("steel_grate", 7))            # L529
    fillWithMetadataBlocks(15, 20, 9, 23, 20, 9, hbm("steel_grate", 7))            # L530
    fillWithMetadataBlocks(16, 20, 10, 22, 20, 10, hbm("steel_grate", 7))          # L531
    fillWithMetadataBlocks(23, 20, 10, 24, 20, 18, hbm("steel_grate", 7))          # L532
    fillWithMetadataBlocks(25, 20, 12, 25, 20, 16, hbm("steel_grate", 7))          # L533
    fillWithMetadataBlocks(22, 20, 19, 23, 20, 19, hbm("steel_grate", 7))          # L534
    fillWithMetadataBlocks(15, 20, 19, 16, 20, 19, hbm("steel_grate", 7))          # L535
    fillWithMetadataBlocks(16, 20, 18, 22, 20, 18, hbm("steel_grate", 7))          # L536
    # Ceiling
    fillWithBlocks(11, 24, 12, 11, 24, 16, hbm("concrete_smooth"))                 # L538
    fillWithBlocks(12, 24, 10, 15, 24, 18, hbm("concrete_smooth"))                 # L539
    fillWithBlocks(13, 24, 9, 15, 24, 9, hbm("concrete_smooth"))                   # L540
    fillWithBlocks(14, 24, 8, 15, 24, 8, hbm("concrete_smooth"))                   # L541
    fillWithBlocks(13, 24, 19, 15, 24, 19, hbm("concrete_smooth"))                 # L542
    fillWithBlocks(14, 24, 20, 15, 24, 20, hbm("concrete_smooth"))                 # L543

    fillWithBlocks(17, 24, 6, 21, 24, 6, hbm("concrete_smooth"))                   # L545
    fillWithBlocks(15, 24, 7, 23, 24, 7, hbm("concrete_smooth"))                   # L546
    fillWithBlocks(16, 24, 8, 22, 24, 10, hbm("concrete_smooth"))                  # L547

    fillWithBlocks(27, 24, 12, 27, 24, 16, hbm("concrete_smooth"))                 # L549
    fillWithBlocks(23, 24, 10, 26, 24, 18, hbm("concrete_smooth"))                 # L550
    fillWithBlocks(23, 24, 9, 25, 24, 9, hbm("concrete_smooth"))                   # L551
    fillWithBlocks(23, 24, 8, 24, 24, 8, hbm("concrete_smooth"))                   # L552
    fillWithBlocks(23, 24, 19, 25, 24, 19, hbm("concrete_smooth"))                 # L553
    fillWithBlocks(23, 24, 20, 24, 24, 20, hbm("concrete_smooth"))                 # L554

    fillWithBlocks(17, 24, 22, 21, 24, 22, hbm("concrete_smooth"))                 # L556
    fillWithBlocks(15, 24, 21, 23, 24, 21, hbm("concrete_smooth"))                 # L557
    fillWithBlocks(16, 24, 18, 22, 24, 20, hbm("concrete_smooth"))                 # L558
    # Walls
    fillWithRandomizedBlocks(14, 20, 7, 14, 24, 7, rand, ConcreteBricksS)          # L560
    fillWithRandomizedBlocks(13, 20, 8, 13, 24, 8, rand, ConcreteBricksS)          # L561
    fillWithRandomizedBlocks(12, 21, 9, 12, 24, 9, rand, ConcreteBricksS)          # L562
    fillWithRandomizedBlocks(11, 21, 10, 11, 24, 11, rand, ConcreteBricksS)        # L563
    fillWithRandomizedBlocks(10, 21, 12, 10, 24, 16, rand, ConcreteBricksS)        # L564
    fillWithRandomizedBlocks(11, 21, 17, 11, 24, 18, rand, ConcreteBricksS)        # L565
    fillWithRandomizedBlocks(12, 21, 19, 12, 24, 19, rand, ConcreteBricksS)        # L566
    fillWithRandomizedBlocks(13, 21, 20, 13, 24, 20, rand, ConcreteBricksS)        # L567
    fillWithRandomizedBlocks(14, 20, 21, 14, 24, 21, rand, ConcreteBricksS)        # L568
    fillWithRandomizedBlocks(15, 20, 22, 16, 24, 22, rand, ConcreteBricksS)        # L569
    fillWithRandomizedBlocks(17, 20, 23, 21, 24, 23, rand, ConcreteBricksS)        # L570
    fillWithRandomizedBlocks(22, 20, 22, 23, 24, 22, rand, ConcreteBricksS)        # L571
    fillWithRandomizedBlocks(24, 20, 21, 24, 24, 21, rand, ConcreteBricksS)        # L572
    fillWithRandomizedBlocks(25, 20, 20, 25, 24, 20, rand, ConcreteBricksS)        # L573
    fillWithRandomizedBlocks(26, 20, 19, 26, 24, 19, rand, ConcreteBricksS)        # L574

    #	CENTER
    # Air
    fillWithAir(17, 2, 12, 21, 25, 16)                                             # L578
    for i in range(5, 18, 4):                                                      # L579
        if ((i - 5) // 4) % 2 == 0:  # stairs facing N
            fillWithAir(17, i, 8, 20, i + 3, 9)
            fillWithAir(17, i, 10, 21, i + 2, 10)
            fillWithAir(17, i, 18, 21, i + 2, 20)
        else:                        # stairs facing S
            fillWithAir(18, i, 19, 21, i + 3, 20)
            fillWithAir(17, i, 18, 21, i + 2, 18)
            fillWithAir(17, i, 8, 21, i + 2, 10)
        fillWithAir(22, i, 10, 22, i + 2, 10)
        fillWithAir(22, i, 9, 23, i + 2, 9)
        fillWithAir(23, i, 10, 24, i + 2, 18)
        fillWithAir(25, i, 12, 25, i + 2, 16)
        fillWithAir(22, i, 19, 23, i + 2, 19)
        fillWithAir(22, i, 18, 22, i + 2, 18)
        fillWithAir(16, i, 18, 16, i + 2, 18)
        fillWithAir(15, i, 19, 16, i + 2, 19)
        fillWithAir(14, i, 10, 15, i + 2, 18)
        fillWithAir(13, i, 12, 13, i + 2, 16)
        fillWithAir(15, i, 9, 16, i + 2, 9)
        fillWithAir(16, i, 10, 16, i + 2, 10)
    for i in range(6, 23, 4):                                                      # L602
        fillWithAir(16, i, 11, 18, i + 1, 11)
        fillWithAir(16, i, 12, 16, i + 1, 13)
        fillWithAir(16, i, 15, 16, i + 1, 16)
        fillWithAir(16, i, 17, 18, i + 1, 17)
        fillWithAir(20, i, 17, 22, i + 1, 17)
        fillWithAir(22, i, 15, 22, i + 1, 16)
        fillWithAir(22, i, 12, 22, i + 1, 13)
        fillWithAir(20, i, 11, 22, i + 1, 11)
    # Supports
    fillWithRandomizedBlocks(22, 24, 17, 22, 24, 17, rand, ConcreteBricksS)        # L613
    fillWithRandomizedBlocks(17, 24, 17, 21, 25, 17, rand, ConcreteBricksS)        # L614
    fillWithRandomizedBlocks(16, 24, 17, 16, 24, 17, rand, ConcreteBricksS)        # L615
    fillWithRandomizedBlocks(16, 24, 12, 16, 25, 16, rand, ConcreteBricksS)        # L616
    fillWithRandomizedBlocks(16, 24, 11, 16, 24, 11, rand, ConcreteBricksS)        # L617
    fillWithRandomizedBlocks(17, 24, 11, 21, 25, 11, rand, ConcreteBricksS)        # L618
    fillWithRandomizedBlocks(22, 24, 11, 22, 24, 11, rand, ConcreteBricksS)        # L619
    fillWithRandomizedBlocks(22, 24, 12, 22, 25, 16, rand, ConcreteBricksS)        # L620

    fillWithRandomizedBlocks(19, 5, 11, 19, 23, 11, rand, ConcreteBricksS)         # L622
    fillWithRandomizedBlocks(22, 5, 14, 22, 23, 14, rand, ConcreteBricksS)         # L623
    fillWithRandomizedBlocks(19, 5, 17, 19, 23, 17, rand, ConcreteBricksS)         # L624
    fillWithRandomizedBlocks(16, 5, 14, 16, 23, 14, rand, ConcreteBricksS)         # L625
    # Grates + Railing
    for j in range(8, 21, 4):                                                      # L627
        for i in range(16, 23, 6):
            fillWithMetadataBlocks(i, j, 15, i, j, 16, hbm("steel_grate", 7))
            fillWithMetadataBlocks(i, j, 12, i, j, 13, hbm("steel_grate", 7))
            fillWithBlocks(i, j + 1, 15, i, j + 1, 16, hbm("fence_metal"))
            fillWithBlocks(i, j + 1, 12, i, j + 1, 13, hbm("fence_metal"))
        for k in range(11, 18, 6):
            fillWithMetadataBlocks(16, j, k, 18, j, k, hbm("steel_grate", 7))
            fillWithMetadataBlocks(20, j, k, 22, j, k, hbm("steel_grate", 7))
            fillWithBlocks(16, j + 1, k, 18, j + 1, k, hbm("fence_metal"))
            fillWithBlocks(20, j + 1, k, 22, j + 1, k, hbm("fence_metal"))
    # Floor
    for j in range(8, 17, 4):                                                      # L642
        fillWithBlocks(15, j, 11, 15, j, 17, hbm("concrete"))
        fillWithBlocks(16, j, 10, 22, j, 10, hbm("concrete"))
        fillWithBlocks(23, j, 11, 23, j, 17, hbm("concrete"))
        fillWithBlocks(16, j, 18, 22, j, 18, hbm("concrete"))
        fillWithBlocks(15, j, 9, 16, j, 9, hbm("concrete_smooth"))
        fillWithBlocks(14, j, 10, 15, j, 10, hbm("concrete_smooth"))
        fillWithBlocks(14, j, 11, 14, j, 17, hbm("concrete_smooth"))
        fillWithBlocks(13, j, 12, 13, j, 16, hbm("concrete_smooth"))
        fillWithBlocks(14, j, 18, 15, j, 18, hbm("concrete_smooth"))
        fillWithBlocks(15, j, 19, 16, j, 19, hbm("concrete_smooth"))

        if (j // 4) % 2 == 0:
            fillWithBlocks(20, j, 19, 21, j, 20, hbm("concrete_smooth"))
            fillWithBlocks(19, j, 19, 19, j + 1, 20, hbm("concrete_smooth"))
            fillWithBlocks(18, j, 19, 18, j + 2, 20, hbm("concrete_smooth"))
            fillWithBlocks(17, j, 19, 17, j + 3, 20, hbm("concrete_smooth"))
            for i in range(4):
                fillWithMetadataBlocks(20 - i, j + 1 + i, 19, 20 - i, j + 1 + i, 20,
                                       hbm("concrete_smooth_stairs", stairE))
        else:
            fillWithBlocks(17, j, 8, 18, j, 9, hbm("concrete_smooth"))
            fillWithBlocks(19, j, 8, 19, j + 1, 9, hbm("concrete_smooth"))
            fillWithBlocks(20, j, 8, 20, j + 2, 9, hbm("concrete_smooth"))
            fillWithBlocks(21, j, 8, 21, j + 3, 9, hbm("concrete_smooth"))
            for i in range(4):
                fillWithMetadataBlocks(18 + i, j + 1 + i, 8, 18 + i, j + 1 + i, 9,
                                       hbm("concrete_smooth_stairs", stairW))

        fillWithBlocks(22, j, 9, 23, j, 9, hbm("concrete_smooth"))
        fillWithBlocks(23, j, 10, 24, j, 10, hbm("concrete_smooth"))
        fillWithBlocks(24, j, 11, 24, j, 17, hbm("concrete_smooth"))
        fillWithBlocks(25, j, 12, 25, j, 16, hbm("concrete_smooth"))
        fillWithBlocks(23, j, 18, 24, j, 18, hbm("concrete_smooth"))
        fillWithBlocks(22, j, 19, 23, j, 19, hbm("concrete_smooth"))

    # Walls
    fillWithRandomizedBlocks(17, 5, 7, 21, 19, 7, rand, ConcreteBricksS)           # L679
    fillWithRandomizedBlocks(15, 4, 8, 16, 19, 8, rand, ConcreteBricksS)           # L680
    fillWithRandomizedBlocks(14, 4, 9, 14, 19, 9, rand, ConcreteBricksS)           # L681
    fillWithRandomizedBlocks(13, 4, 10, 13, 19, 11, rand, ConcreteBricksS)         # L682
    fillWithRandomizedBlocks(12, 5, 12, 12, 19, 16, rand, ConcreteBricksS)         # L683
    fillWithRandomizedBlocks(13, 4, 17, 13, 19, 18, rand, ConcreteBricksS)         # L684
    fillWithRandomizedBlocks(14, 4, 19, 14, 19, 19, rand, ConcreteBricksS)         # L685
    fillWithRandomizedBlocks(15, 4, 20, 16, 19, 20, rand, ConcreteBricksS)         # L686
    fillWithRandomizedBlocks(17, 5, 21, 21, 19, 21, rand, ConcreteBricksS)         # L687
    fillWithRandomizedBlocks(22, 4, 20, 23, 19, 20, rand, ConcreteBricksS)         # L688
    fillWithRandomizedBlocks(24, 4, 19, 24, 19, 19, rand, ConcreteBricksS)         # L689
    fillWithRandomizedBlocks(25, 4, 17, 25, 19, 18, rand, ConcreteBricksS)         # L690
    fillWithRandomizedBlocks(26, 5, 12, 26, 19, 16, rand, ConcreteBricksS)         # L691
    fillWithRandomizedBlocks(25, 4, 10, 25, 19, 11, rand, ConcreteBricksS)         # L692
    fillWithRandomizedBlocks(24, 4, 9, 24, 19, 9, rand, ConcreteBricksS)           # L693
    fillWithRandomizedBlocks(22, 4, 8, 23, 19, 8, rand, ConcreteBricksS)           # L694

    #	EXHAUST
    # dark area N/S
    fillWithMetadataBlocks(17, 0, 7, 21, 0, 21, hbm("concrete_colored", 7))        # L698
    fillWithMetadataBlocks(18, 1, 7, 20, 1, 7, hbm("concrete_colored", 7))         # L699
    fillWithMetadataBlocks(17, 1, 7, 17, 1, 12, hbm("concrete_colored", 7))        # L700
    fillWithMetadataBlocks(21, 1, 7, 21, 1, 12, hbm("concrete_colored", 7))        # L701
    fillWithMetadataBlocks(18, 1, 11, 18, 1, 12, hbm("concrete_colored", 7))       # L702
    fillWithMetadataBlocks(20, 1, 11, 20, 1, 12, hbm("concrete_colored", 7))       # L703
    fillWithMetadataBlocks(18, 1, 21, 20, 1, 21, hbm("concrete_colored", 7))       # L704
    fillWithMetadataBlocks(17, 1, 16, 17, 1, 21, hbm("concrete_colored", 7))       # L705
    fillWithMetadataBlocks(21, 1, 16, 21, 1, 21, hbm("concrete_colored", 7))       # L706
    fillWithMetadataBlocks(18, 1, 16, 18, 1, 17, hbm("concrete_colored", 7))       # L707
    fillWithMetadataBlocks(20, 1, 16, 20, 1, 17, hbm("concrete_colored", 7))       # L708
    # W/E
    fillWithMetadataBlocks(12, 0, 12, 16, 0, 16, hbm("concrete_colored", 7))       # L710
    fillWithMetadataBlocks(22, 0, 12, 26, 0, 16, hbm("concrete_colored", 7))       # L711
    fillWithMetadataBlocks(12, 1, 13, 12, 1, 15, hbm("concrete_colored", 7))       # L712
    fillWithMetadataBlocks(12, 1, 16, 16, 1, 16, hbm("concrete_colored", 7))       # L713
    fillWithMetadataBlocks(12, 1, 12, 16, 1, 12, hbm("concrete_colored", 7))       # L714
    fillWithMetadataBlocks(16, 1, 15, 17, 1, 15, hbm("concrete_colored", 7))       # L715
    fillWithMetadataBlocks(16, 1, 13, 17, 1, 13, hbm("concrete_colored", 7))       # L716
    fillWithMetadataBlocks(26, 1, 13, 26, 1, 15, hbm("concrete_colored", 7))       # L717
    fillWithMetadataBlocks(22, 1, 16, 26, 1, 16, hbm("concrete_colored", 7))       # L718
    fillWithMetadataBlocks(22, 1, 12, 26, 1, 12, hbm("concrete_colored", 7))       # L719
    fillWithMetadataBlocks(21, 1, 15, 22, 1, 15, hbm("concrete_colored", 7))       # L720
    fillWithMetadataBlocks(21, 1, 13, 22, 1, 13, hbm("concrete_colored", 7))       # L721
    # gray area walls
    fillWithBlocks(18, 2, 21, 20, 3, 21, hbm("concrete_smooth"))                   # L723
    fillWithBlocks(21, 2, 17, 21, 3, 21, hbm("concrete_smooth"))                   # L724
    fillWithBlocks(22, 2, 16, 26, 3, 16, hbm("concrete_smooth"))                   # L725
    fillWithBlocks(26, 2, 13, 26, 3, 15, hbm("concrete_smooth"))                   # L726
    fillWithBlocks(22, 2, 12, 26, 3, 12, hbm("concrete_smooth"))                   # L727
    fillWithBlocks(21, 2, 7, 21, 3, 11, hbm("concrete_smooth"))                    # L728
    fillWithBlocks(18, 2, 7, 20, 3, 7, hbm("concrete_smooth"))                     # L729
    fillWithBlocks(17, 2, 7, 17, 3, 11, hbm("concrete_smooth"))                    # L730
    fillWithBlocks(12, 2, 12, 16, 3, 12, hbm("concrete_smooth"))                   # L731
    fillWithBlocks(12, 2, 13, 12, 3, 15, hbm("concrete_smooth"))                   # L732
    fillWithBlocks(12, 2, 16, 16, 3, 16, hbm("concrete_smooth"))                   # L733
    fillWithBlocks(17, 2, 17, 17, 3, 21, hbm("concrete_smooth"))                   # L734
    # Floor
    fillWithBlocks(18, 2, 21, 20, 3, 21, hbm("concrete_smooth"))                   # L736
    fillWithBlocks(21, 2, 17, 21, 3, 21, hbm("concrete_smooth"))                   # L737
    fillWithBlocks(22, 2, 16, 26, 3, 16, hbm("concrete_smooth"))                   # L738
    fillWithBlocks(26, 2, 13, 26, 3, 15, hbm("concrete_smooth"))                   # L739
    fillWithBlocks(22, 2, 12, 26, 3, 12, hbm("concrete_smooth"))                   # L740
    fillWithBlocks(21, 2, 7, 21, 3, 11, hbm("concrete_smooth"))                    # L741
    fillWithBlocks(18, 2, 7, 20, 3, 7, hbm("concrete_smooth"))                     # L742
    fillWithBlocks(17, 2, 7, 17, 3, 11, hbm("concrete_smooth"))                    # L743
    fillWithBlocks(12, 2, 12, 16, 3, 12, hbm("concrete_smooth"))                   # L744
    fillWithBlocks(12, 2, 13, 12, 3, 15, hbm("concrete_smooth"))                   # L745
    fillWithBlocks(12, 2, 16, 16, 3, 16, hbm("concrete_smooth"))                   # L746
    fillWithBlocks(17, 2, 17, 17, 2, 21, hbm("concrete_smooth"))                   # L747
    fillWithBlocks(17, 4, 17, 21, 4, 21, hbm("concrete_smooth"))                   # L748
    fillWithBlocks(22, 4, 17, 23, 4, 19, hbm("concrete_smooth"))                   # L749
    fillWithBlocks(24, 4, 17, 24, 4, 18, hbm("concrete_smooth"))                   # L750
    fillWithBlocks(22, 4, 12, 26, 4, 16, hbm("concrete_smooth"))                   # L751
    fillWithBlocks(24, 4, 10, 24, 4, 11, hbm("concrete_smooth"))                   # L752
    fillWithBlocks(22, 4, 9, 23, 4, 11, hbm("concrete_smooth"))                    # L753
    fillWithBlocks(17, 4, 7, 21, 4, 11, hbm("concrete_smooth"))                    # L754
    fillWithBlocks(15, 4, 9, 16, 4, 11, hbm("concrete_smooth"))                    # L755
    fillWithBlocks(14, 4, 10, 14, 4, 11, hbm("concrete_smooth"))                   # L756
    fillWithBlocks(12, 4, 12, 16, 4, 16, hbm("concrete_smooth"))                   # L757
    fillWithBlocks(14, 4, 17, 14, 4, 18, hbm("concrete_smooth"))                   # L758
    fillWithBlocks(15, 4, 17, 16, 4, 19, hbm("concrete_smooth"))                   # L759
    # Stairs
    fillWithBlocks(19, 5, 8, 19, 5, 9, hbm("concrete_smooth"))                     # L761
    fillWithBlocks(20, 5, 8, 20, 6, 9, hbm("concrete_smooth"))                     # L762
    fillWithBlocks(21, 5, 8, 21, 7, 9, hbm("concrete_smooth"))                     # L763
    for i in range(4):                                                             # L764
        fillWithMetadataBlocks(18 + i, 5 + i, 8, 18 + i, 5 + i, 9,
                               hbm("concrete_smooth_stairs", stairW))
    # Railing and Deco
    placeBlockAtCurrentPosition(hbm("fence_metal"), 18, 5, 11)                     # L767
    fillWithBlocks(20, 5, 11, 22, 5, 11, hbm("fence_metal"))                       # L768
    fillWithBlocks(22, 5, 12, 22, 5, 13, hbm("fence_metal"))                       # L769
    fillWithBlocks(22, 5, 15, 22, 5, 17, hbm("fence_metal"))                       # L770
    placeBlockAtCurrentPosition(hbm("fence_metal"), 20, 5, 17)                     # L771
    fillWithBlocks(16, 5, 17, 18, 5, 17, hbm("fence_metal"))                       # L772
    fillWithBlocks(16, 5, 15, 16, 5, 16, hbm("fence_metal"))                       # L773
    fillWithBlocks(16, 5, 11, 16, 5, 13, hbm("fence_metal"))                       # L774
    placeBlockAtCurrentPosition(van("air"), 21, 5, 17)                             # L775
    placeBlockAtCurrentPosition(van("air"), 17, 5, 11)                             # L776

    fillWithMetadataBlocks(17, 2, 12, 17, 4, 12, hbm("ladder_steel", decoN))       # L778
    fillWithMetadataBlocks(21, 2, 16, 21, 4, 16, hbm("ladder_steel", decoS))       # L779
    # Launch Pad
    placeCoreLaunchpad(19, 1, 14)                                                  # L781
    fillSpace(19, 1, 14, [0, 0, 1, 1, 1, 1], hbm("launch_pad_rusted"))             # L782
    for i in range(0, 3, 2):                                                       # L783
        for k in range(0, 3, 2):
            makeExtra("launch_pad_rusted", 18 + i, 1, 13 + k)
    placeBlockAtCurrentPosition(hbm("radio_torch_receiver", 3), 19, 0, 14)         # L786
    setRTTYFreq(19, 0, 14, freq)                                                   # L787

    # Air
    fillWithAir(18, 1, 8, 20, 3, 10)                                               # L790
    fillWithAir(18, 2, 11, 20, 3, 11)                                              # L791
    fillWithAir(19, 1, 11, 19, 1, 12)                                              # L792
    fillWithAir(19, 1, 16, 19, 1, 17)                                              # L793
    fillWithAir(18, 2, 17, 20, 3, 17)                                              # L794
    fillWithAir(18, 1, 18, 20, 3, 20)                                              # L795
    fillWithAir(13, 1, 13, 15, 3, 15)                                              # L796
    fillWithAir(16, 2, 13, 16, 3, 15)                                              # L797
    fillWithAir(16, 1, 14, 17, 1, 14)                                              # L798
    fillWithAir(21, 1, 14, 22, 1, 14)                                              # L799
    fillWithAir(22, 2, 13, 22, 3, 15)                                              # L800
    fillWithAir(23, 1, 13, 25, 3, 15)                                              # L801

    """ Red Sector """
    # Air
    fillWithAir(2, 17, 9, 11, 18, 11)                                              # L805
    fillWithAir(2, 19, 10, 11, 19, 10)                                             # L806
    fillWithAir(2, 17, 13, 11, 18, 15)                                             # L807
    fillWithAir(2, 19, 14, 11, 19, 14)                                             # L808
    fillWithAir(8, 17, 17, 12, 18, 25)                                             # L809
    fillWithAir(9, 19, 17, 11, 19, 25)                                             # L810
    fillWithAir(2, 17, 17, 6, 18, 21)                                              # L811
    fillWithAir(3, 19, 17, 5, 19, 21)                                              # L812
    fillWithAir(2, 17, 22, 3, 18, 25)                                              # L813
    fillWithAir(3, 19, 22, 3, 19, 25)                                              # L814
    fillWithAir(5, 17, 23, 6, 19, 25)                                              # L815
    # Ceiling
    fillWithBlocks(1, 20, 8, 12, 20, 16, hbm("concrete_smooth"))                   # L817
    fillWithBlocks(1, 20, 17, 13, 20, 26, hbm("concrete_smooth"))                  # L818
    # Floor
    fillWithBlocks(2, 16, 7, 11, 16, 7, hbm("concrete_smooth"))                    # L820
    fillWithBlocks(1, 16, 8, 12, 16, 11, hbm("concrete_smooth"))                   # L821
    fillWithBlocks(1, 16, 12, 11, 16, 16, hbm("concrete_smooth"))                  # L822
    fillWithBlocks(1, 16, 17, 12, 16, 18, hbm("concrete_smooth"))                  # L823
    fillWithBlocks(1, 16, 19, 13, 16, 26, hbm("concrete_smooth"))                  # L824
    placeBlockAtCurrentPosition(hbm("concrete_smooth"), 12, 16, 14)                # L825
    # Walls
    # N
    fillWithRandomizedBlocks(2, 17, 7, 11, 19, 7, rand, ConcreteBricksS)           # L828
    fillWithRandomizedBlocks(11, 17, 8, 12, 17, 8, rand, ConcreteBricksS)          # L829
    fillWithRandomizedBlocks(8, 17, 8, 8, 17, 8, rand, ConcreteBricksS)            # L830
    fillWithRandomizedBlocks(5, 17, 8, 5, 17, 8, rand, ConcreteBricksS)            # L831
    fillWithRandomizedBlocks(1, 17, 8, 2, 17, 8, rand, ConcreteBricksS)            # L832
    fillWithRandomizedBlocks(1, 19, 8, 12, 19, 8, rand, ConcreteBricksS)           # L833
    fillWithMetadataBlocks(1, 18, 8, 2, 18, 8, hbm("concrete_colored", 14))        # L834
    placeBlockAtCurrentPosition(hbm("concrete_colored", 14), 5, 18, 8)             # L835
    placeBlockAtCurrentPosition(hbm("concrete_colored", 14), 8, 18, 8)             # L836
    fillWithMetadataBlocks(11, 18, 8, 12, 18, 8, hbm("concrete_colored", 14))      # L837
    # W
    fillWithMetadataBlocks(1, 18, 9, 1, 18, 25, hbm("concrete_colored", 14))       # L839
    # S
    fillWithMetadataBlocks(1, 18, 26, 13, 18, 26, hbm("concrete_colored", 14))     # L841
    # E
    fillWithMetadataBlocks(13, 18, 17, 13, 18, 25, hbm("concrete_colored", 14))    # L843
    fillWithMetadataBlocks(12, 18, 9, 12, 18, 16, hbm("concrete_colored", 14))     # L844
    fillWithMetadataBlocks(13, 18, 10, 13, 18, 11, hbm("concrete_colored", 14))    # L845
    # Internal
    fillWithMetadataBlocks(2, 18, 12, 11, 18, 12, hbm("concrete_colored", 14))     # L847
    fillWithMetadataBlocks(2, 18, 16, 11, 18, 16, hbm("concrete_colored", 14))     # L848
    fillWithMetadataBlocks(7, 18, 17, 7, 18, 25, hbm("concrete_colored", 14))      # L849
    fillWithMetadataBlocks(4, 18, 22, 6, 18, 22, hbm("concrete_colored", 14))      # L850
    fillWithMetadataBlocks(4, 18, 23, 4, 18, 25, hbm("concrete_colored", 14))      # L851
    for i in range(17, 20, 2):                                                     # L852
        # W
        fillWithRandomizedBlocks(1, i, 9, 1, i, 25, rand, ConcreteBricksS)
        # S
        fillWithRandomizedBlocks(1, i, 26, 13, i, 26, rand, ConcreteBricksS)
        # E
        fillWithRandomizedBlocks(13, i, 19, 13, i, 25, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(12, i, 9, 12, i, 11, rand, ConcreteBricksS)
        # Internal
        fillWithRandomizedBlocks(2, i, 12, 11, i, 12, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(2, i, 16, 11, i, 16, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(7, i, 17, 7, i, 25, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(4, i, 22, 6, i, 22, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(4, i, 23, 4, i, 25, rand, ConcreteBricksS)
    # Arches
    ConcreteStairsS.setMetadata(stairS | 4)
    fillWithRandomizedBlocks(2, 19, 9, 11, 19, 9, rand, ConcreteStairsS)           # L869
    fillWithRandomizedBlocks(2, 19, 13, 11, 19, 13, rand, ConcreteStairsS)         # L870
    ConcreteStairsS.setMetadata(stairN | 4)
    fillWithRandomizedBlocks(2, 19, 11, 11, 19, 11, rand, ConcreteStairsS)         # L872
    fillWithRandomizedBlocks(2, 19, 15, 11, 19, 15, rand, ConcreteStairsS)         # L873
    ConcreteStairsS.setMetadata(stairW | 4)
    fillWithRandomizedBlocks(12, 19, 17, 12, 19, 25, rand, ConcreteStairsS)        # L875
    fillWithRandomizedBlocks(6, 19, 17, 6, 19, 21, rand, ConcreteStairsS)          # L876
    ConcreteStairsS.setMetadata(stairE | 4)
    fillWithRandomizedBlocks(8, 19, 17, 8, 19, 25, rand, ConcreteStairsS)          # L878
    fillWithRandomizedBlocks(2, 19, 17, 2, 19, 25, rand, ConcreteStairsS)          # L879
    # Doors
    placeDoor("door_bunker", 2, rand.nextBoolean(), False, 12, 17, 14)             # L881
    placeDoor("door_bunker", 3, rand.nextBoolean(), False, 10, 17, 12)             # L882
    placeDoor("door_bunker", 1, rand.nextBoolean(), False, 10, 17, 16)             # L883
    placeDoor("door_bunker", 1, rand.nextBoolean(), False, 4, 17, 16)              # L884
    placeDoor("metal_door", 0, rand.nextBoolean(), False, 4, 17, 24)               # L885
    # Deco
    # Living Room
    placeBlockAtCurrentPosition(hbm("reinforced_stone_stairs", stairW | 4), 12, 17, 17)  # L888
    placeBlockAtCurrentPosition(van("cauldron"), 12, 17, 18)                       # L889
    fillWithMetadataBlocks(12, 17, 19, 12, 17, 20, hbm("reinforced_stone_stairs", stairW | 4))  # L890
    placeBlockAtCurrentPosition(hbm("machine_electric_furnace_off", decoE), 12, 17, 21)  # L891
    placeBlockAtCurrentPosition(hbm("deco_toaster", getCRTMeta(3) | 4), 12, 18, 17)      # L892
    placeLever(2, True, 12, 18, 18)                                                # L893
    placeBlockAtCurrentPosition(hbm("machine_microwave", decoE), 12, 18, 19)       # L894
    placeBlockAtCurrentPosition(hbm("hev_battery"), 12, 18, 20)                    # L895
    placeBlockAtCurrentPosition(van("oak_stairs", stairS), 8, 17, 17)              # L896
    fillWithMetadataBlocks(8, 17, 19, 9, 17, 19, hbm("reinforced_stone_stairs", stairS | 4))  # L897
    fillWithMetadataBlocks(8, 17, 20, 9, 17, 20, hbm("reinforced_stone_stairs", stairN | 4))  # L898
    placeBlockAtCurrentPosition(van("oak_stairs", stairN), 8, 17, 22)              # L899
    placeBlockAtCurrentPosition(van("oak_stairs", stairE), 10, 17, 23)             # L900
    placeBlockAtCurrentPosition(van("oak_stairs", stairS), 11, 17, 23)             # L901
    placeBlockAtCurrentPosition(van("oak_stairs", stairW), 12, 17, 23)             # L902
    fillWithMetadataBlocks(10, 17, 25, 12, 17, 25, hbm("reinforced_stone_stairs", stairN | 4))  # L903
    placeBlockAtCurrentPosition(hbm("deco_crt", getCRTMeta(0)), 11, 18, 25)        # L904

    # Bathroom
    placeBlockAtCurrentPosition(hbm("reinforced_stone"), 6, 17, 17)                # L907
    fillWithBlocks(6, 17, 18, 6, 17, 20, van("cauldron"))                          # L908
    placeBlockAtCurrentPosition(hbm("reinforced_stone"), 6, 17, 21)                # L909
    for i in range(3):                                                             # L910
        placeLever(2, True, 6, 18, 18 + i)
    placeBlockAtCurrentPosition(van("hopper", decoW), 6, 17, 24)                   # L912
    placeBlockAtCurrentPosition(van("trapdoor", decoModelW >> 2), 6, 18, 24)       # L913
    # Bedroom
    for i in range(3, 8, 2):                                                       # L915
        placeBlockAtCurrentPosition(hbm("reinforced_stone_stairs", stairN | 4), i, 17, 11)

    for i in range(4, 11, 3):                                                      # L918
        for j in range(17, 19):
            placeBed(1, i, j, 8)

    # Containers
    generateInvContents(hbm("crate_steel", 2), "POOL_VAULT_LOCKERS", 8, 17, 25, _rolls=6)  # L923

    generateInvContents(hbm("crate_steel", 2), "POOL_VAULT_LOCKERS", 2, 17, 11, _rolls=6)  # L925
    generateInvContents(hbm("crate_steel", 2), "POOL_EXPENSIVE", 4, 17, 11, _rolls=2)      # L926
    generateInvContents(hbm("crate_steel", 2), "POOL_VAULT_LOCKERS", 6, 17, 11, _rolls=6)  # L927
    generateInvContents(hbm("crate_steel", 2), "POOL_VAULT_LOCKERS", 8, 17, 11, _rolls=6)  # L928
    # Mines
    fillWithMines(2, 17, 9, 11, 17, 11)                                            # L930
    fillWithMines(9, 17, 17, 11, 17, 24)                                           # L931
    fillWithMines(5, 17, 23, 6, 17, 25)                                            # L932

    """ Yellow Sector """
    # Air
    fillWithAir(27, 13, 13, 33, 14, 15)                                            # L936
    fillWithAir(27, 15, 14, 33, 15, 14)                                            # L937
    fillWithAir(27, 13, 17, 33, 14, 21)                                            # L938
    fillWithAir(27, 15, 18, 33, 15, 20)                                            # L939
    fillWithAir(27, 13, 9, 29, 14, 11)                                             # L940
    fillWithAir(28, 15, 9, 28, 15, 11)                                             # L941
    fillWithAir(31, 13, 9, 33, 14, 11)                                             # L942
    fillWithAir(32, 15, 9, 32, 15, 11)                                             # L943
    # Ceiling + Floor
    for i in range(12, 17, 4):                                                     # L945
        fillWithBlocks(26, i, 17, 26, i, 22, hbm("concrete_smooth"))
        fillWithBlocks(27, i, 8, 34, i, 22, hbm("concrete_smooth"))
        fillWithBlocks(26, i, 8, 26, i, 11, hbm("concrete_smooth"))
    placeBlockAtCurrentPosition(hbm("concrete_smooth"), 26, 12, 14)                # L950
    # Walls
    # N
    fillWithMetadataBlocks(26, 14, 8, 34, 14, 8, hbm("concrete_colored", 4))       # L953
    # E
    fillWithMetadataBlocks(34, 14, 9, 34, 14, 21, hbm("concrete_colored", 4))      # L955
    # S
    fillWithMetadataBlocks(26, 14, 22, 34, 14, 22, hbm("concrete_colored", 4))     # L957
    # W
    fillWithMetadataBlocks(26, 14, 9, 26, 14, 21, hbm("concrete_colored", 4))      # L959
    fillWithMetadataBlocks(25, 14, 17, 25, 14, 18, hbm("concrete_colored", 4))     # L960
    fillWithMetadataBlocks(25, 14, 10, 25, 14, 11, hbm("concrete_colored", 4))     # L961
    # Internal
    fillWithMetadataBlocks(27, 14, 16, 33, 14, 16, hbm("concrete_colored", 4))     # L963
    fillWithMetadataBlocks(27, 14, 12, 33, 14, 12, hbm("concrete_colored", 4))     # L964
    fillWithMetadataBlocks(30, 14, 9, 30, 14, 11, hbm("concrete_colored", 4))      # L965
    for i in range(13, 16, 2):                                                     # L966
        # N
        fillWithRandomizedBlocks(26, i, 8, 34, i, 8, rand, ConcreteBricksS)
        # E
        fillWithRandomizedBlocks(34, i, 9, 34, i, 21, rand, ConcreteBricksS)
        # S
        fillWithRandomizedBlocks(26, i, 22, 34, i, 22, rand, ConcreteBricksS)
        # W
        fillWithRandomizedBlocks(26, i, 15, 26, i, 21, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(26, i, 9, 26, i, 13, rand, ConcreteBricksS)
        # Internal
        fillWithRandomizedBlocks(27, i, 16, 33, i, 16, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(27, i, 12, 33, i, 12, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(30, i, 9, 30, i, 11, rand, ConcreteBricksS)
    # Arches
    ConcreteStairsS.setMetadata(stairN | 4)
    fillWithRandomizedBlocks(27, 15, 21, 33, 15, 21, rand, ConcreteStairsS)        # L983
    fillWithRandomizedBlocks(27, 15, 15, 33, 15, 15, rand, ConcreteStairsS)        # L984
    ConcreteStairsS.setMetadata(stairS | 4)
    fillWithRandomizedBlocks(27, 15, 17, 33, 15, 17, rand, ConcreteStairsS)        # L986
    fillWithRandomizedBlocks(27, 15, 13, 33, 15, 13, rand, ConcreteStairsS)        # L987
    ConcreteStairsS.setMetadata(stairW | 4)
    fillWithRandomizedBlocks(33, 15, 9, 33, 15, 11, rand, ConcreteStairsS)         # L989
    fillWithRandomizedBlocks(29, 15, 9, 29, 15, 11, rand, ConcreteStairsS)         # L990
    ConcreteStairsS.setMetadata(stairE | 4)
    fillWithRandomizedBlocks(31, 15, 9, 31, 15, 11, rand, ConcreteStairsS)         # L992
    fillWithRandomizedBlocks(27, 15, 9, 27, 15, 11, rand, ConcreteStairsS)         # L993
    # Doors
    placeDoor("door_bunker", 0, rand.nextBoolean(), False, 26, 13, 14)             # L995
    placeDoor("door_bunker", 3, rand.nextBoolean(), False, 28, 13, 12)             # L996
    placeDoor("door_bunker", 3, rand.nextBoolean(), False, 32, 13, 12)             # L997
    placeDoor("door_bunker", 1, rand.nextBoolean(), False, 32, 13, 16)             # L998
    # Deco
    # Room 1
    placeBlockAtCurrentPosition(hbm("crate_ammo"), 27, 13, 9)                      # L1001
    placeBlockAtCurrentPosition(hbm("crate_can"), 27, 13, 10)                      # L1002
    placeBlockAtCurrentPosition(hbm("crate_can"), 27, 14, 9)                       # L1003
    placeBlockAtCurrentPosition(hbm("crate_can"), 28, 13, 9)                       # L1004
    placeBlockAtCurrentPosition(hbm("barrel_corroded"), 29, 13, 9)                 # L1005
    # Room 2
    placeBlockAtCurrentPosition(hbm("crate_can"), 31, 13, 9)                       # L1007
    placeBlockAtCurrentPosition(hbm("deco_computer", decoModelE), 31, 13, 11)      # L1008

    placeBlockAtCurrentPosition(hbm("crate_can"), 33, 13, 11)                      # L1010
    # Workshop
    placeBlockAtCurrentPosition(hbm("machine_transformer"), 33, 13, 17)            # L1012
    fillWithRandomizedBlocks(33, 13, 18, 33, 13, 20, rand, Supplies)               # L1013

    placeBlockAtCurrentPosition(hbm("anvil_iron", decoN), 31, 13, 21)              # L1015
    fillWithBlocks(28, 13, 18, 29, 13, 20, van("planks"))                          # L1016
    placeBlockAtCurrentPosition(van("crafting_table"), 29, 13, 19)                 # L1017
    placeBlockAtCurrentPosition(hbm("radiorec", decoE), 28, 14, 19)                # L1018
    placeBlockAtCurrentPosition(hbm("deco_toaster", getCRTMeta(1)), 28, 13, 17)    # L1019

    # Containers
    generateInvContents(hbm("crate_steel", 2), "POOL_SILO", 32, 13, 9, _rolls=6)   # L1022
    generateInvContents(hbm("safe", decoN), "POOL_MACHINE_PARTS", 33, 13, 9, _rolls=6)  # L1023

    generateInvContents(hbm("crate_steel", 2), "POOL_VAULT_LAB", 33, 13, 21, _rolls=8)  # L1025
    # Mines
    fillWithMines(27, 13, 13, 33, 13, 15)                                          # L1027

    """ Green Sector """
    # Air
    fillWithAir(1, 9, 13, 11, 10, 15)                                              # L1031
    fillWithAir(1, 11, 14, 8, 11, 14)                                              # L1032
    fillWithAir(1, 9, 7, 6, 10, 11)                                                # L1033
    fillWithAir(1, 11, 8, 6, 11, 10)                                               # L1034
    fillWithAir(7, 9, 7, 11, 10, 7)                                                # L1035
    fillWithAir(7, 9, 11, 11, 10, 11)                                              # L1036
    fillWithAir(2, 9, 17, 4, 11, 23)                                               # L1037
    fillWithAir(5, 9, 17, 5, 9, 18)                                                # L1038
    fillWithAir(5, 9, 22, 5, 9, 23)                                                # L1039
    fillWithAir(1, 9, 17, 1, 9, 18)                                                # L1040
    fillWithAir(1, 9, 22, 1, 9, 23)                                                # L1041
    fillWithAir(7, 9, 17, 11, 10, 23)                                              # L1042
    fillWithAir(8, 11, 17, 10, 11, 23)                                             # L1043
    # Floor + Ceiling
    placeBlockAtCurrentPosition(hbm("concrete_smooth"), 12, 8, 14)                 # L1045
    for i in range(8, 13, 4):                                                      # L1046
        fillWithBlocks(0, i, 6, 12, i, 11, hbm("concrete_smooth"))
        fillWithBlocks(0, i, 12, 11, i, 16, hbm("concrete_smooth"))
        fillWithBlocks(0, i, 17, 12, i, 24, hbm("concrete_smooth"))
    # Walls
    # N
    fillWithMetadataBlocks(0, 10, 6, 12, 10, 6, hbm("concrete_colored", 13))       # L1053
    # W
    fillWithMetadataBlocks(0, 10, 7, 0, 10, 23, hbm("concrete_colored", 13))       # L1055
    # S
    fillWithMetadataBlocks(0, 10, 24, 12, 10, 24, hbm("concrete_colored", 13))     # L1057
    # E
    fillWithMetadataBlocks(12, 10, 7, 12, 10, 23, hbm("concrete_colored", 13))     # L1059
    fillWithMetadataBlocks(13, 10, 17, 13, 10, 18, hbm("concrete_colored", 13))    # L1060
    fillWithMetadataBlocks(13, 10, 10, 13, 10, 11, hbm("concrete_colored", 13))    # L1061
    # Internal
    fillWithMetadataBlocks(1, 10, 12, 11, 10, 12, hbm("concrete_colored", 13))     # L1063
    fillWithMetadataBlocks(1, 10, 16, 11, 10, 16, hbm("concrete_colored", 13))     # L1064
    fillWithMetadataBlocks(6, 10, 17, 6, 10, 23, hbm("concrete_colored", 13))      # L1065
    for i in range(9, 12, 2):                                                      # L1066
        # N
        fillWithRandomizedBlocks(0, i, 6, 12, i, 6, rand, ConcreteBricksS)
        # W
        fillWithRandomizedBlocks(0, i, 7, 0, i, 23, rand, ConcreteBricksS)
        # S
        fillWithRandomizedBlocks(0, i, 24, 12, i, 24, rand, ConcreteBricksS)
        # E
        fillWithRandomizedBlocks(12, i, 17, 12, i, 23, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(12, i, 7, 12, i, 11, rand, ConcreteBricksS)
        # Internal
        fillWithRandomizedBlocks(1, i, 12, 11, i, 12, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(1, i, 16, 11, i, 16, rand, ConcreteBricksS)
        fillWithRandomizedBlocks(6, i, 17, 6, i, 23, rand, ConcreteBricksS)
    # Arches
    ConcreteStairsS.setMetadata(stairS | 4)
    fillWithRandomizedBlocks(1, 11, 7, 11, 11, 7, rand, ConcreteStairsS)           # L1083
    fillWithRandomizedBlocks(1, 11, 13, 11, 11, 13, rand, ConcreteStairsS)         # L1084
    ConcreteStairsS.setMetadata(stairN | 4)
    fillWithRandomizedBlocks(1, 11, 11, 11, 11, 11, rand, ConcreteStairsS)         # L1086
    fillWithRandomizedBlocks(1, 11, 15, 11, 11, 15, rand, ConcreteStairsS)         # L1087
    ConcreteStairsS.setMetadata(stairW | 4)
    fillWithRandomizedBlocks(11, 11, 17, 11, 11, 23, rand, ConcreteStairsS)        # L1089
    fillWithRandomizedBlocks(5, 11, 17, 5, 11, 18, rand, ConcreteStairsS)          # L1090
    fillWithRandomizedBlocks(5, 11, 22, 5, 11, 23, rand, ConcreteStairsS)          # L1091
    ConcreteStairsS.setMetadata(stairE | 4)
    fillWithRandomizedBlocks(7, 11, 17, 7, 11, 23, rand, ConcreteStairsS)          # L1093
    fillWithRandomizedBlocks(1, 11, 17, 1, 11, 18, rand, ConcreteStairsS)          # L1094
    fillWithRandomizedBlocks(1, 11, 22, 1, 11, 23, rand, ConcreteStairsS)          # L1095
    # Doors
    placeDoor("door_bunker", 2, rand.nextBoolean(), False, 12, 9, 14)              # L1097
    placeDoor("door_bunker", 1, rand.nextBoolean(), False, 9, 9, 16)               # L1098
    placeDoor("door_bunker", 1, rand.nextBoolean(), False, 3, 9, 16)               # L1099
    placeDoor("door_bunker", 3, rand.nextBoolean(), False, 3, 9, 12)               # L1100
    # Deco
    # Fuel Infrastructure
    fillWithMetadataBlocks(17, 11, 14, 18, 11, 14, hbm("deco_pipe_quad_rusted", pillarWE))  # L1103
    placeBlockAtCurrentPosition(hbm("deco_steel"), 16, 11, 14)                     # L1104
    fillWithMetadataBlocks(13, 11, 14, 15, 11, 14, hbm("deco_pipe_quad_rusted", pillarWE))  # L1105
    placeBlockAtCurrentPosition(hbm("deco_steel"), 12, 11, 14)                     # L1106
    fillWithMetadataBlocks(10, 11, 14, 11, 11, 14, hbm("deco_pipe_quad_rusted", pillarWE))  # L1107
    placeBlockAtCurrentPosition(hbm("deco_steel"), 9, 11, 14)                      # L1108
    placeBlockAtCurrentPosition(hbm("deco_pipe_quad_rusted", pillarNS), 9, 11, 15) # L1109
    placeBlockAtCurrentPosition(hbm("deco_steel"), 9, 11, 16)                      # L1110
    fillWithMetadataBlocks(9, 11, 17, 9, 11, 19, hbm("deco_pipe_quad_rusted", pillarNS))    # L1111
    placeBlockAtCurrentPosition(hbm("fluid_duct_gauge"), 9, 11, 20)                # L1112
    fillWithMetadataBlocks(9, 11, 21, 9, 11, 22, hbm("deco_pipe_quad_rusted", pillarNS))    # L1113
    placeBlockAtCurrentPosition(hbm("deco_steel"), 9, 11, 23)                      # L1114
    fillWithBlocks(9, 9, 23, 9, 10, 23, hbm("deco_pipe_framed_rusted"))            # L1115
    placeBlockAtCurrentPosition(hbm("deco_steel"), 9, 8, 23)                       # L1116
    placeBlockAtCurrentPosition(hbm("deco_pipe_quad_rusted", pillarWE), 10, 11, 20)  # L1117
    placeBlockAtCurrentPosition(hbm("deco_steel"), 11, 11, 20)                     # L1118
    fillWithBlocks(11, 9, 20, 11, 10, 20, hbm("deco_pipe_framed_rusted"))          # L1119
    placeBlockAtCurrentPosition(hbm("deco_steel"), 11, 8, 20)                      # L1120
    placeBlockAtCurrentPosition(hbm("deco_pipe_quad_rusted", pillarWE), 8, 11, 20) # L1121
    placeBlockAtCurrentPosition(hbm("deco_steel"), 7, 11, 20)                      # L1122
    fillWithBlocks(7, 9, 20, 7, 10, 20, hbm("deco_pipe_framed_rusted"))            # L1123
    placeBlockAtCurrentPosition(hbm("deco_steel"), 7, 8, 20)                       # L1124
    fillWithBlocks(8, 8, 18, 10, 8, 22, hbm("deco_lead"))                          # L1125
    # Barrels in tank room
    placeBlockAtCurrentPosition(hbm("lox_barrel"), 7, 9, 17)                       # L1127
    placeBlockAtCurrentPosition(hbm("pink_barrel"), 11, 9, 19)                     # L1128
    placeBlockAtCurrentPosition(hbm("pink_barrel"), 11, 9, 22)                     # L1129
    fillWithBlocks(11, 9, 23, 11, 10, 23, hbm("pink_barrel"))                      # L1130
    placeBlockAtCurrentPosition(hbm("pink_barrel"), 10, 9, 23)                     # L1131
    fillWithBlocks(7, 9, 23, 8, 9, 23, hbm("lox_barrel"))                          # L1132
    fillWithBlocks(7, 9, 21, 7, 9, 22, hbm("lox_barrel"))                          # L1133
    # Capacitor Room
    for i in range(1, 6, 4):                                                       # L1135
        fillWithMetadataBlocks(i, 10, 17, i, 10, 18, hbm("deco_pipe_quad_red", pillarNS))
        fillWithMetadataBlocks(i, 10, 22, i, 10, 23, hbm("deco_pipe_quad_red", pillarNS))
        fillWithBlocks(i, 9, 19, i, 9, 21, hbm("deco_lead"))
        fillWithMetadataBlocks(i, 10, 19, i, 10, 21, hbm("capacitor_copper", decoW if i == 1 else decoE))
        fillWithBlocks(i, 11, 19, i, 11, 21, hbm("deco_lead"))
    # Generator Room
    placeBlockAtCurrentPosition(hbm("barrel_corroded"), 1, 9, 11)                  # L1143
    fillWithBlocks(1, 9, 8, 1, 9, 9, hbm("barrel_corroded"))                       # L1144
    fillWithBlocks(1, 9, 7, 1, 10, 7, hbm("barrel_corroded"))                      # L1145
    placeBlockAtCurrentPosition(hbm("barrel_corroded"), 2, 9, 7)                   # L1146

    fillWithBlocks(7, 9, 10, 11, 9, 10, hbm("deco_lead"))                          # L1148
    fillWithBlocks(7, 10, 10, 11, 10, 10, hbm("hadron_coil_alloy"))                # L1149
    fillWithBlocks(7, 11, 10, 11, 11, 10, hbm("deco_lead"))                        # L1150
    fillWithBlocks(7, 9, 9, 11, 9, 9, hbm("hadron_coil_alloy"))                    # L1151
    fillWithBlocks(8, 10, 9, 11, 10, 9, hbm("deco_red_copper"))                    # L1152
    placeBlockAtCurrentPosition(hbm("red_cable_gauge", decoE), 7, 10, 9)           # L1153
    fillWithBlocks(7, 11, 9, 11, 11, 9, hbm("hadron_coil_alloy"))                  # L1154
    fillWithBlocks(7, 9, 8, 11, 9, 8, hbm("deco_lead"))                            # L1155
    fillWithBlocks(7, 10, 8, 11, 10, 8, hbm("hadron_coil_alloy"))                  # L1156
    fillWithBlocks(7, 11, 8, 11, 11, 8, hbm("deco_lead"))                          # L1157

    # Containers
    generateInvContents(hbm("crate_steel", 2), "POOL_NUKE_FUEL", 4, 9, 7, _rolls=5)  # L1160
    # Mines
    fillWithMines(1, 9, 7, 6, 9, 11)                                               # L1162
    fillWithMines(8, 9, 17, 10, 9, 22)                                             # L1163

    """ Black Sector """
    # Air
    fillWithAir(27, 5, 13, 31, 6, 15)                                              # L1167
    fillWithAir(27, 7, 14, 31, 7, 14)                                              # L1168
    fillWithAir(28, 2, 11, 31, 3, 15)                                              # L1169
    # Floor + Ceiling
    fillWithBlocks(28, 0, 11, 31, 0, 15, van("dirt"))                              # L1171
    randomlyFillWithBlocks(rand, 0.5, 28, 0, 11, 31, 0, 15, van("dirt", 2))        # L1172
    fillWithBlocks(27, 4, 11, 31, 4, 15, hbm("concrete_smooth"))                   # L1173
    fillWithBlocks(27, 8, 12, 32, 8, 16, hbm("concrete_smooth"))                   # L1174
    # Walls
    # N
    fillWithRandomizedBlocks(27, 0, 10, 32, 4, 10, rand, ConcreteBricksS)          # L1177
    fillWithRandomizedBlocks(27, 5, 12, 32, 5, 12, rand, ConcreteBricksS)          # L1178
    fillWithMetadataBlocks(27, 6, 12, 32, 6, 12, hbm("concrete_colored", 15))      # L1179
    fillWithRandomizedBlocks(27, 7, 12, 32, 7, 12, rand, ConcreteBricksS)          # L1180
    # E
    fillWithRandomizedBlocks(32, 0, 11, 32, 4, 12, rand, ConcreteBricksS)          # L1182
    fillWithRandomizedBlocks(32, 0, 13, 32, 5, 15, rand, ConcreteBricksS)          # L1183
    fillWithMetadataBlocks(32, 6, 13, 32, 6, 15, hbm("concrete_colored", 15))      # L1184
    fillWithRandomizedBlocks(32, 7, 13, 32, 7, 15, rand, ConcreteBricksS)          # L1185
    # S
    fillWithRandomizedBlocks(27, 0, 16, 32, 5, 16, rand, ConcreteBricksS)          # L1187
    fillWithMetadataBlocks(27, 6, 16, 32, 6, 16, hbm("concrete_colored", 15))      # L1188
    fillWithRandomizedBlocks(27, 7, 16, 32, 7, 16, rand, ConcreteBricksS)          # L1189
    # W
    fillWithRandomizedBlocks(27, 0, 11, 27, 3, 15, rand, ConcreteBricksS)          # L1191
    # Arches
    ConcreteStairsS.setMetadata(stairN | 4)
    fillWithRandomizedBlocks(27, 7, 15, 31, 7, 15, rand, ConcreteStairsS)          # L1194
    ConcreteStairsS.setMetadata(stairS | 4)
    fillWithRandomizedBlocks(27, 7, 13, 31, 7, 13, rand, ConcreteStairsS)          # L1196
    # Water
    fillWithBlocks(28, 1, 11, 31, 1, 15, van("water"))                             # L1198
    # Deco
    fillWithBlocks(26, 5, 14, 26, 6, 14, hbm("concrete_smooth"))                   # L1200
    fillWithMetadataBlocks(31, 2, 15, 31, 4, 15, hbm("ladder_steel", decoE))       # L1201
    # Top Room
    randomlyFillWithBlocks(rand, 0.15, 27, 5, 13, 30, 6, 15, van("web"))           # L1203
    randomlyFillWithBlocks(rand, 0.15, 31, 6, 13, 31, 6, 15, van("web"))           # L1204
    randomlyFillWithBlocks(rand, 0.15, 27, 7, 14, 31, 7, 14, van("web"))           # L1205

    # Flooded Room
    randomlyFillWithBlocks(rand, 0.15, 28, 2, 11, 31, 2, 15, hbm("reeds"))         # L1208
    fillWithMetadataBlocks(28, 3, 12, 28, 3, 15, hbm("deco_pipe_framed_green_rusted", pillarNS))  # L1209
    placeBlockAtCurrentPosition(hbm("deco_steel"), 28, 3, 11)                      # L1210
    placeBlockAtCurrentPosition(hbm("deco_pipe_rim_green_rusted"), 28, 2, 11)      # L1211
    placeBlockAtCurrentPosition(hbm("deco_steel"), 28, 0, 11)                      # L1212
    fillWithBlocks(31, 1, 11, 31, 1, 12, hbm("deco_beryllium"))                    # L1213
    fillWithMetadataBlocks(31, 2, 11, 31, 2, 12, hbm("tape_recorder", decoE))      # L1214

    placeBlockAtCurrentPosition(hbm("hev_battery"), 30, 2, 11)                     # L1216

    # Containers
    # L1219: guaranteed launch_key (ItemPoolsSingle-пул 1x launch_key)
    dev("launch_key-сейф -> minecraft:chest с явным Items NBT (гарантированный предмет)")
    placeBlockAtCurrentPosition(
        st("minecraft:chest",
           {"facing": HFACING.get(decoE & 7, "north"), "type": "single", "waterlogged": "false"},
           {"id": "minecraft:chest",
            "Items": [{"Slot": 0, "id": NS + ":launch_key", "Count": 1}]}),
        31, 5, 13)
    generateInvContents(hbm("crate_steel", 2), "POOL_NUKE_TRASH", 31, 5, 14, _rolls=5)   # L1220
    generateInvContents(hbm("safe", decoE), "POOL_FILING_CABINET", 31, 5, 15, _rolls=5)  # L1221

    generateInvContents(hbm("crate_iron", 2), "POOL_EXPENSIVE", 30, 1, 11, _rolls=7)     # L1223
    # Mines
    fillWithMines(27, 5, 13, 30, 5, 15)                                            # L1225

    return True

# ---------------------------------------------------------------------------
# Сборка шаблона
# ---------------------------------------------------------------------------
def build_template():
    palette, pal_index = [], {}

    def pid(entry):
        name, props, _ = entry
        key = (name, tuple(sorted(props.items())))
        if key not in pal_index:
            pal_index[key] = len(palette)
            e = {"Name": name}
            if props:
                e["Properties"] = {k: str(v) for k, v in props.items()}
            palette.append(e)
        return pal_index[key]

    blocks = []
    for (x, y, z), (name, props, nbt) in sorted(grid.items()):
        b = {"pos": [x, y, z], "state": pid((name, props, nbt))}
        if nbt:
            b["nbt"] = nbt
        blocks.append(b)

    return {
        "DataVersion": convert.DATA_VERSION,
        "size": [SX, SY, SZ],
        "entities": [],
        "palette": palette,
        "blocks": blocks,
    }

def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")

def main():
    addComponentParts()
    template = build_template()

    out_dir = OUT / "structures"
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "silo.nbt").write_bytes(convert.nbt_write(template))

    wg = OUT / "worldgen"
    # Поверхностный комплекс: верх шаблона - надземная часть, пол FLOOR_Y
    # садится на heightmap (аналог hpos оригинала: пол на высоте рельефа).
    # WORLD_SURFACE_WG = высота первого воздуха над рельефом => origin =
    # surface + 1 - (FLOOR_Y + 1); пол y25 -> поверхность.
    struct_json = {
        "type": "minecraft:jigsaw",
        "biomes": "#hbm_m:has_structure/flat",
        "spawn_overrides": {},
        "step": "surface_structures",
        "terrain_adaptation": "beard_thin",
        "start_pool": NS + ":silo_pool",
        "size": 1,
        "use_expansion_hack": False,
        "start_height": {"absolute": -(FLOOR_Y + 1)},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 116,
    }
    write_json(wg / "structure" / "silo.json", struct_json)

    set_json = {
        "structures": [{"structure": NS + ":silo", "weight": 1}],
        "placement": {
            "type": "minecraft:random_spread",
            "salt": zlib.crc32(b"hbm_struct_silo") & 0x7FFFFFFF,
            "spacing": 50, "separation": 12,
        },
    }
    write_json(wg / "structure_set" / "silo.json", set_json)

    pool_json = {
        "fallback": "minecraft:empty",
        "elements": [{
            "weight": 1,
            "element": {
                "element_type": "minecraft:single_pool_element",
                "projection": "rigid",
                "location": NS + ":silo",
                "processors": "hbm_m:connection_fix",
            },
        }],
    }
    write_json(wg / "template_pool" / "silo_pool.json", pool_json)

    print("silo.nbt: %d блоков, палитра %d, размер %dx%dx%d" %
          (len(template["blocks"]), len(template["palette"]), SX, SY, SZ))
    print("RTTY freq=%d freqHatch=%d" % (freq, freqHatch))
    print("отклонений: %d" % len(DEV))
    for d in DEV:
        print("  *", d)

if __name__ == "__main__":
    main()
