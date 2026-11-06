package com.hbm_m.item;

/**
 * Порт {@code ItemKey} (1.7.10) — «настоящий» ключ. В оригинале только {@code key} и
 * {@code key_fake} были подклассами {@code ItemKey}; именно по этому классу
 * {@code TileEntityLockableBase.canAccess} пускала в замок — заготовки ({@code key_pin})
 * и навесные замки ({@code ItemLock}, тоже наследник {@code ItemKeyPin}) замок НЕ открывали,
 * даже с совпадающим кодом. Здесь то же разделение: ILockable проверяет
 * {@code instanceof ItemKey}.
 */
public class ItemKey extends ItemKeyPin {

    public ItemKey(Properties properties) {
        super(properties);
    }
}
