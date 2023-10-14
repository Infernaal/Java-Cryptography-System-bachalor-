# UA

## Створення виконуваного файлу через Artifacts в IntelliJ IDEA для програми криптозахисту носіїв

**Цей документ пояснює процес створення виконуваного файлу для програми криптозахисту носіїв в IntelliJ IDEA.**

### Крок 1: Налаштування проекту
- Переконайтеся, що проект відкритий в IntelliJ IDEA та готовий для збірки.

### Крок 2: Налаштування Artifacts
- Увійдіть у меню "File" (Файл) та оберіть "Project Structure" (Структура проекту).
- У вікні "Project Structure" виберіть "Artifacts" (Артефакти) зліва.
- Клацніть на "+" для додавання нового артефакту та виберіть "JAR" -> "From modules with dependencies" (З модулів з залежностями).
- Оберіть модуль **MainStart.java** із програмою криптозахисту носіїв та переконайтеся, що всі необхідні залежності відзначені ( **бібліотеки JavaFX, commons-lang3-3.12.0** ).
- В розділі "Output directory" (Директорія виводу) виберіть місце, де Ви хочете зберегти ваш виконуваний файл (наприклад, "out/artifacts/DataEncryption").
- Натисніть "OK" для закриття вікна "Project Structure".

### Крок 3: Збірка проекту
- Увійдіть у меню "Build" (Збірка) та оберіть "Build Artifacts" (Збірка артефактів).
- Виберіть артефакт, який Ви створили у Кроці 2 та натисніть "Build".
- IntelliJ IDEA автоматично збере проект та створить виконуваний файл у вказаній директорії.

### Крок 4: Запуск виконуваного файлу
- Тепер Ви можете знайти та запустити виконуваний файл у вказаній директорії, який дозволить зашифрувати будь-який Ваш носій.

# EN

## Creating an executable file through Artifacts in IntelliJ IDEA for a media encryption program

**This document explains the process of creating an executable file for a media encryption program in IntelliJ IDEA.**

### Step 1: Project Setup
- Make sure the project is open in IntelliJ IDEA and ready for building.
### Step 2: Artifacts Configuration
- Go to the "File" menu and select "Project Structure."
- In the "Project Structure" window, choose "Artifacts" on the left.
- Click on the "+" to add a new artifact and select "JAR" -> "From modules with dependencies."
- Choose the **MainStart.java** module with the media encryption program and ensure that all necessary dependencies are selected ( **JavaFX libraries, commons-lang3-3.12.0** ).
- In the "Output directory" section, select the location where you want to save your executable file (e.g., "out/artifacts/DataEncryption").
- Press "OK" to close the "Project Structure" window.
### Step 3: Project Build
- Go to the "Build" menu and select "Build Artifacts."
- Choose the artifact you created in Step 2 and click "Build."
- IntelliJ IDEA will automatically build the project and create the executable file in the specified directory.
### Step 4: Running the Executable File
- Now you can find and run the executable file in the specified directory, which will allow you to encrypt any of your media.
