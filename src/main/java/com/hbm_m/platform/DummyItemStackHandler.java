package com.hbm_m.platform;

/**
 * Пустая заглушка-хендлер для меню, открытых на клиенте без BlockEntity
 * (например, при проигрывании реплея Flashback: виртуальный мир не содержит тайлов).
 *
 * Позволяет конструктору меню создать слоты и не уронить обработку пакета
 * ClientboundOpenScreenPacket. Все изменения содержимого молча отбрасываются.
 */
public final class DummyItemStackHandler extends ModItemStackHandler {

    public DummyItemStackHandler(int size) {
        super(size);
    }

    @Override
    protected void onContentsChanged(int slot) {
        // заглушка: на клиенте без тайла изменения никуда не сохраняются
    }
}
