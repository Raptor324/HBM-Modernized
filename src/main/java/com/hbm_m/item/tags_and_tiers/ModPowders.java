package com.hbm_m.item.tags_and_tiers;

// Перечисление всех слитков в моде с поддержкой многоязычности.
// Для каждого слитка можно задать переводы на разные языки.

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ModPowders {


    IRON("iron",
            "ru_ru", "Железный порошок",
            "en_us", "Iron Powder"),

    URANIUM("uranium",
            "ru_ru", "Урановый порошок",
            "en_us", "Uranium Powder"),

    COAL("coal",
            "ru_ru", "Угольный порошок",
            "en_us", "Coal Powder"),

    GOLD("gold",
            "ru_ru", "Золотой порошок",
            "en_us", "Golden Powder");



    // Чтобы добавить новый слиток, просто добавьте новую запись с его переводами

    private final String name;
    private final Map<String, String> translations;

    /**
     * Конструктор для слитков с поддержкой нескольких языков.
     * @param name Системное имя (например, "uranium")
     * @param translationPairs Пары строк: "код_языка", "перевод". Например: "ru_ru", "Слиток", "en_us", "Powder"
     */
    ModPowders(String name, String... translationPairs) {
        this.name = name;

        // Проверка, что количество элементов четное (пары ключ-значение)
        if (translationPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Translation pairs must be even for powder: " + name);
        }

        Map<String, String> translationMap = new HashMap<>();
        for (int i = 0; i < translationPairs.length; i += 2) {
            String locale = translationPairs[i];
            String translation = translationPairs[i + 1];
            translationMap.put(locale, translation);
        }
        // Делаем карту неизменяемой после создания
        this.translations = Collections.unmodifiableMap(translationMap);
    }

    public String getName() {
        return name;
    }

    /**
     * Получает перевод для указанного языка.
     * @param locale Код языка (например, "ru_ru").
     * @return Перевод или null, если он не найден.
     */
    public String getTranslation(String locale) {
        return translations.get(locale);
    }
}
