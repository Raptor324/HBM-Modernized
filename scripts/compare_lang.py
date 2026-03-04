import re
import os

# Путь к вашему файлу
FILE_PATH = 'src\main\java\com\hbm_m\datagen\ModLanguageProvider.java'

def parse_keys(block_content):
    """
    Ищет все вызовы add(KEY, "VALUE"); и возвращает множество KEY.
    Игнорирует закомментированные строки.
    """
    keys = set()
    
    # Регулярное выражение разбирает строку вида:
    # add(  КЛЮЧ  ,  "ЗНАЧЕНИЕ"  );
    # Группа 1: Ключ (может быть строкой или вызовом метода .get())
    # Группа 2: Значение (игнорируем, но матчим, чтобы убедиться в структуре)
    # ^\s* гарантирует, что мы не берем строки, начинающиеся с // (комментарии) если они не отбиты табами,
    # но лучше проверять комментарии явно.
    
    # Regex объяснение:
    # ^\s*add\s*\(       -> начало строки, пробелы, 'add', скобка
    # \s*(.+?)\s*        -> захват ключа (ленивый), убираем пробелы
    # ,\s*               -> запятая
    # "(?:[^"\\]|\\.)*"  -> значение в кавычках (учитывает экранирование \")
    # \s*\);             -> закрывающая скобка и точка с запятой
    pattern = re.compile(r'^\s*add\s*\(\s*(.+?)\s*,\s*"(?:[^"\\]|\\.)*"\s*\);', re.MULTILINE)

    for match in pattern.finditer(block_content):
        # Получаем ключ
        raw_key = match.group(1).strip()
        
        # Проверка на комментарии (если вдруг regex захватил строку с // в начале)
        # Получаем полную строку совпадения
        full_match = match.group(0)
        # Находим эту строку в тексте, чтобы проверить, что перед ней нет //
        # (Упрощенно: в данном формате кода // обычно в начале строки)
        
        keys.add(raw_key)
        
    return keys

def main():
    if not os.path.exists(FILE_PATH):
        print(f"Ошибка: Файл {FILE_PATH} не найден.")
        return

    with open(FILE_PATH, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Находим начало блока switch и кейсов
    try:
        # Разбиваем файл по меткам кейсов
        # Все что после 'case "ru_ru":' и до 'case "en_us":' — это русский блок
        part_after_ru = content.split('case "ru_ru":')[1]
        ru_block = part_after_ru.split('case "en_us":')[0]
        
        # Все что после 'case "en_us":' — это английский блок
        # Берем до конца файла (или до последнего break/скобки, но для парсинга add это не критично)
        en_block = part_after_ru.split('case "en_us":')[1]
        
    except IndexError:
        print("Ошибка парсинга: Не удалось найти секции 'case \"ru_ru\":' или 'case \"en_us\":' в файле.")
        return

    # 2. Извлекаем ключи
    ru_keys = parse_keys(ru_block)
    en_keys = parse_keys(en_block)

    # 3. Сравниваем
    missing_in_en = ru_keys - en_keys
    missing_in_ru = en_keys - ru_keys

    # 4. Вывод результатов
    print(f"Всего ключей в RU: {len(ru_keys)}")
    print(f"Всего ключей в EN: {len(en_keys)}")
    print("-" * 50)

    if not missing_in_en and not missing_in_ru:
        print("✅ Локализации полностью синхронизированы!")
    else:
        if missing_in_en:
            print(f"🔴 ЕСТЬ В RU, НО НЕТ В EN ({len(missing_in_en)} шт.):")
            for k in sorted(missing_in_en):
                print(f"   MISSING IN EN: {k}")
            print("-" * 50)

        if missing_in_ru:
            print(f"🔵 ЕСТЬ В EN, НО НЕТ В RU ({len(missing_in_ru)} шт.):")
            for k in sorted(missing_in_ru):
                print(f"   MISSING IN RU: {k}")

if __name__ == "__main__":
    main()