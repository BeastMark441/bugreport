# BugReports

## 📝 Краткое описание
BugReports - это мощный плагин для управления баг-репортами и предложениями на вашем сервере Minecraft. Плагин предоставляет удобный GUI-интерфейс для игроков и администраторов, систему уведомлений и гибкие настройки.

## ⚙️ Технические требования
- **Поддерживаемые версии Minecraft:** 1.19.x - 1.20.x
- **Минимальная версия Java:** Java 17
- **Зависимости:** 
  - PlaceholderAPI (обязательно)
  - Purpur/Paper/Spigot (рекомендуется Purpur)

## 🚀 Возможности
- Создание баг-репортов и предложений через удобный GUI
- Категории для разных типов репортов
- Система статусов репортов (Новый, В обработке, Завершено и т.д.)
- Уведомления для игроков и администраторов
- Черный список для блокировки спамеров
- Автоматическое создание бэкапов конфигурации
- Поддержка PlaceholderAPI
- Полностью настраиваемые сообщения
- Ограничения для предотвращения спама
- Статистика репортов
- Автоматическая проверка обновлений

## 🔄 Обновления

Плагин автоматически проверяет наличие новых версий при запуске сервера и уведомляет администраторов о доступных обновлениях.

* Проверка обновлений при запуске
* Уведомления для администраторов при входе
* Прямые ссылки на скачивание
* Возможность отключить проверку в конфиге

Настройка в `config.yml`:
```yaml
# Включить/выключить проверку обновлений
check-updates: true
```

## 📥 Установка
1. Скачайте последнюю версию плагина
2. Установите PlaceholderAPI, если еще не установлен
3. Поместите JAR файл в папку plugins
4. Перезапустите сервер
5. Настройте конфигурацию в `plugins/BugReports/config.yml`

## 📚 Команды

### Команды для игроков
| Команда | Описание | Права |
|---------|-----------|-------|
| `/bugreport` | Создать баг-репорт | bugreports.create |
| `/bugreport status` | Просмотр статуса репортов | bugreports.create |
| `/bugreport list` | Список всех ваших репортов | bugreports.create |
| `/suggestion` | Создать предложение | bugreports.create |
| `/reportstats` | Открыть GUI со статистикой | bugreports.create |

### Команды для администраторов
| Команда | Описание | Права |
|---------|-----------|-------|
| `/reportadmin gui` | Открыть админ-панель | bugreports.admin |
| `/reportadmin status <id> <статус>` | Изменить статус репорта | bugreports.admin |
| `/reportadmin delete <id>` | Удалить репорт | bugreports.admin |
| `/reportadmin blacklist <player>` | Добавить в черный список | bugreports.admin |
| `/reportadmin unblacklist <player>` | Удалить из черного списка | bugreports.admin |
| `/reportadmin reload` | Перезагрузить конфигурацию | bugreports.admin |

## 🔒 Права доступа
- `bugreports.create` - Создание репортов (по умолчанию: true)
- `bugreports.admin` - Доступ к админ-командам (по умолчанию: op)

## ⚡ PlaceholderAPI
Плагин добавляет следующие плейсхолдеры:
- `%bugreports_total%` - Общее количество репортов
- `%bugreports_player_total%` - Количество репортов игрока
- `%bugreports_player_active%` - Количество активных репортов игрока
- `%bugreports_new%` - Количество новых репортов
- `%bugreports_in_progress%` - Количество репортов в обработке

## 📁 Файлы конфигурации

### config.yml
- Настройки базы данных
- Категории репортов
- Статусы репортов
- Ограничения
- Настройки уведомлений
- Параметры админ-панели

### messages.yml
- Все сообщения плагина
- Поддержка цветовых кодов
- Настраиваемый префикс

## 🔄 Автоматические бэкапы
Плагин автоматически создает бэкапы конфигурационных файлов при каждом запуске сервера. Бэкапы хранятся в папке `plugins/BugReports/backups/`. Сохраняются последние 5 версий каждого файла.

## 🎨 Особенности админ-панели
- Цветовая кодировка статусов
- Фильтрация и сортировка репортов
- Массовые действия
- Быстрый доступ к игрокам (телепортация, сообщения)
- Детальная статистика
- Автоматическое обновление

## 🛠️ Для разработчиков
Плагин использует:
- Java 17+
- Maven для сборки
- SQLite для хранения данных
- Bukkit API
- PlaceholderAPI

## 🤝 Поддержка
Если у вас возникли вопросы или проблемы:
- Создайте issue на GitHub
- Свяжитесь со мной: https://vk.com/defaltmark

## 📜 Лицензия
Этот проект распространяется под лицензией MIT. Подробности в файле LICENSE. 