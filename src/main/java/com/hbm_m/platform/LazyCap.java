//? if neoforge {
/*package com.hbm_m.platform;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

/^*
 * [NeoForge 1.21.1] Nachbau von Forges {@code LazyOptional} fuer die Capability-Bruecke.
 * Die Forge-Zweige der BlockEntities ({@code getCapability}/{@code invalidateCaps}) werden auf NeoForge
 * 1:1 als {@code getHbmCapability}/{@code invalidateHbmCaps} gespiegelt; dieser Typ traegt dabei dieselbe
 * Semantik: Lieferant wird einmal faul aufgeloest und gemerkt, {@link #invalidate()} macht den Wert unbrauchbar.
 * NeoForge selbst bekommt am Ende nur den aufgeloesten Handler ({@link #orElse(Object)}), siehe
 * {@link HbmCaps#query}.
 ^/
public final class LazyCap<T> {

    // isValid=false у синглтона EMPTY: иначе addListener навсегда сохранял бы лямбды
    // (с захваченными BlockEntity) в общий для всего мода CopyOnWriteArrayList — утечка.
    private static final LazyCap<Void> EMPTY;
    static {
        LazyCap<Void> e = new LazyCap<>((Supplier<Void>) null);
        e.isValid = false;
        EMPTY = e;
    }

    private @Nullable Supplier<? extends T> supplier;
    private @Nullable T resolved;
    private boolean isValid = true;
    // Ленивый список: аллоцируется только при первом addListener. Каждый capability-запрос
    // (HbmCaps.get/item/defaults/query) создаёт LazyCap — CopyOnWriteArrayList на каждый
    // вызов мусорил хип в горячем пути труб/кабелей.
    private @Nullable java.util.List<Consumer<LazyCap<T>>> listeners;

    private LazyCap(@Nullable Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    private LazyCap(@Nullable T resolvedValue, boolean preResolved) {
        this.supplier = null;
        this.resolved = resolvedValue;
    }

    public static <T> LazyCap<T> of(@Nullable Supplier<? extends T> supplier) {
        return supplier == null ? empty() : new LazyCap<>(supplier);
    }

    /^* Fester Wert (null = leer). Ersatz fuer die Forge-Typumwandlung {@code (LazyOptional<X>) tank.getCapability()}. ^/
    @SuppressWarnings("unchecked")
    public static <T> LazyCap<T> ofObj(@Nullable Object value) {
        if (value == null) return empty();
        if (value instanceof LazyCap<?> lc) return (LazyCap<T>) lc;
        T v = (T) value;
        // Без лямбды: конструктор прямого значения (горячий путь capability-запросов).
        return new LazyCap<>(v, true);
    }

    @SuppressWarnings("unchecked")
    public static <T> LazyCap<T> empty() {
        return (LazyCap<T>) EMPTY;
    }

    @SuppressWarnings("unchecked")
    public <X> LazyCap<X> cast() {
        return (LazyCap<X>) this;
    }

    private @Nullable T getValue() {
        if (!isValid) return null;
        if (resolved != null) return resolved;
        if (supplier == null) return null;
        resolved = supplier.get();
        return resolved;
    }

    public boolean isPresent() {
        return getValue() != null;
    }

    public void ifPresent(Consumer<? super T> consumer) {
        T v = getValue();
        if (v != null) consumer.accept(v);
    }

    public T orElse(T other) {
        T v = getValue();
        return v != null ? v : other;
    }

    public T orElseGet(Supplier<? extends T> other) {
        T v = getValue();
        return v != null ? v : other.get();
    }

    public T orElseThrow() {
        T v = getValue();
        if (v == null) throw new IllegalStateException("LazyCap is empty");
        return v;
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        T v = getValue();
        if (v == null) throw exceptionSupplier.get();
        return v;
    }

    public Optional<T> resolve() {
        // Одно вычисление getValue(): isPresent() здесь звал supplier.get() второй раз
        return Optional.ofNullable(getValue());
    }

    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        T v = getValue();
        return v == null ? Optional.empty() : Optional.ofNullable(mapper.apply(v));
    }

    public <U> LazyCap<U> lazyMap(Function<? super T, ? extends U> mapper) {
        // Лениво: isPresent() здесь немедленно вычислял supplier.get(), убивая ленивость.
        // Плюс guard на EMPTY/resolved-null: маппинг пустого LazyCap не аллоцирует новый.
        if (!isValid || (resolved == null && supplier == null)) return empty();
        return of(() -> {
            T v = getValue();
            return v == null ? null : mapper.apply(v);
        });
    }

    /^* Forge: Listener bei Invalidierung. Wird bei {@link #invalidate()} aufgerufen. ^/
    public void addListener(Consumer<LazyCap<T>> listener) {
        if (listener != null) {
            if (!this.isValid) {
                listener.accept(this);
            } else {
                if (this.listeners == null) {
                    this.listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
                }
                this.listeners.add(listener);
            }
        }
    }

    public void invalidate() {
        if (this == EMPTY) return;
        this.isValid = false;
        this.resolved = null;
        if (this.listeners != null) {
            for (Consumer<LazyCap<T>> listener : this.listeners) {
                listener.accept(this);
            }
            this.listeners = null;
        }
    }
}
*///?}
