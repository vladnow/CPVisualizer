# Публикация CPVisualizer

Подготовленная версия: **1.1.0**, Paper 26.2, Java 25, CoreProtect API >=12.

## GitHub

1. Создайте публичный репозиторий с названием `CPVisualizer`.
2. Загрузите содержимое публичного архива исходников из папки `CPVisualizer/`.
   В корне репозитория должны находиться README.md и build.gradle.kts.
3. Выберите лицензию своего аддона и добавьте её файл LICENSE перед релизом.
   В текущей подготовке лицензия за владельца проекта не назначена.
4. Создайте релиз с тегом `v1.1.0`; приложите **CPVisualizer.jar** и описание
   из CHANGELOG.md. Укажите Paper 26.2 / Java 25 и обязательный CoreProtect.
5. Пользователи устанавливают CoreProtect отдельно и кладут аддон рядом с ним.

Публичный архив исключает серверный CoreProtect JAR, build/, .gradle/ и Git-данные.
Не загружайте в репозиторий весь локальный каталог вместе с libs/*.jar вручную:
веб-загрузка не применяет правила .gitignore автоматически. При публикации
через Git эти JAR исключены .gitignore.

Локальный CoreProtect используется только для компиляции. В публичной копии
Gradle получает `net.coreprotect:coreprotect:24.1` из Maven PlayPro; серверный
CoreProtect не включается в аддон. Чужие библиотеки тестирования тоже не входят
в релизный JAR. Аддон не содержит скопированной реализации CoreProtect.

## Другие площадки

- [Hangar](https://hangar.papermc.io/) — каталог плагинов экосистемы Paper.
- [Modrinth](https://modrinth.com/) — поддерживает проекты типа plugin.

При создании страницы выберите Paper и Minecraft 26.2, укажите CoreProtect
как обязательную внешнюю зависимость. В описание добавьте ограничения API v12
из README: общий блоковый поиск ALL охватывает загруженные миры, результаты
ограничены, тип контейнера восстанавливается по доступному журналу.
Предварительно проверьте плагин на тестовом сервере по TESTING.md.

Публикация на внешних площадках из этого чата ещё не выполнялась.

Официальные инструкции:

- [Создание репозитория GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/quickstart-for-repositories)
- [Публикация на Hangar](https://docs.papermc.io/misc/hangar-publishing/)
- [Типы проектов Modrinth](https://support.modrinth.com/en/articles/8800818-about-modrinth)
