<div align="center">

# INTERNET MONITOR

### Контроль интернета в реальном времени

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-green?style=for-the-badge&logo=android" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Kotlin-1.9-blue?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Version-1.2.2-orange?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/License-MIT-purple?style=for-the-badge" alt="License">
</p>

<p align="center">
  <b>Cyberpunk-styled Android приложение для мониторинга интернет-цензуры</b><br>
  <sub>Отслеживайте блокировки, замеряйте скорость, будьте в курсе</sub>
</p>

<br>

<img width="200" height="400" src="https://github.com/user-attachments/assets/df8f0308-e2e1-43f5-b5d2-bbc86f1afcc5" alt="screenshot"/>

</div>

---

## Возможности

### Мониторинг сети

Приложение автоматически определяет уровень доступа к интернету, проверяя несколько групп сайтов:

| Состояние | Описание |
|-----------|----------|
| `FULL ACCESS` | Полный доступ - все сайты доступны |
| `RU ONLY` | Только российские сайты |
| `RKN LOCKDOWN` | Только сайты из "белого списка" РКН |
| `NO SIGNAL` | Нет интернета |

**Как это работает:**
- Фоновый сервис проверяет сайты каждые 90 секунд
- При смене состояния - мгновенное уведомление
- Постоянное уведомление в статус-баре

### Тест скорости

Встроенная функция Speedtest для замера скорости интернета:

- **Download** - скорость загрузки (параллельно с нескольких серверов)
- **Upload** - скорость выгрузки
- **Ping** - задержка соединения
- Анимированный датчик в стиле Ookla

### Настройка URL

Вы можете добавить свои сайты для проверки в каждой категории:

| Категория | Сайты по умолчанию |
|-----------|-------------------|
| Глобальные | ru.yummyani.me, wikipedia.org |
| Россия | sberbank.ru, tbank.ru |
| Белый список РКН | nalog.gov.ru, gosuslugi.ru |

---

## Установка

### Скачать APK

Перейдите в [Releases](https://github.com/EggZys/WhiteList-Checker/releases) и скачайте последнюю версию:

- `app-arm64-v8a-release.apk` - для современных 64-битных устройств
- `app-armeabi-v7a-release.apk` - для старых 32-битных устройств

### Сборка из исходников

```bash
# Клонировать репозиторий
git clone https://github.com/EggZys/WhiteList-Checker.git
cd WhiteList-Checker

# Собрать debug APK
./gradlew assembleDebug

# Установить на устройство
./gradlew installDebug
```

---

## Требования

| Компонент | Минимум |
|-----------|---------|
| Android | 8.0 (API 26) |
| Android Studio | Hedgehog (2023.1.1) |
| SDK | 34 |
| Java | 17 |

---

## Разрешения

| Разрешение | Зачем |
|------------|-------|
| `INTERNET` | Проверка доступности сайтов |
| `ACCESS_NETWORK_STATE` | Определение состояния сети |
| `ACCESS_WIFI_STATE` | Информация о Wi-Fi |
| `FOREGROUND_SERVICE` | Фоновый мониторинг |
| `FOREGROUND_SERVICE_DATA_SYNC` | Синхронизация данных |
| `POST_NOTIFICATIONS` | Уведомления о смене состояния |

---

## Как использовать

1. **Запустите приложение** - увидите текущий статус сети
2. **Нажмите ENGAGE** - запустите фоновый мониторинг
3. **Вкладка SPEEDTEST** - замерьте скорость интернета
4. **Настройте URL** - добавьте свои сайты для проверки

---

## Технологии

- **Kotlin** - язык разработки
- **Material3** - дизайн-система с кастомными drawable
- **OkHttp 4.12** - HTTP-клиент для проверки сайтов
- **Coroutines** - асинхронные операции
- **ViewPager2** - навигация между вкладками

---

## Структура проекта

```
app/src/main/java/com/eggzys/internetmonitor/
├── MainActivity.kt           # Главная активность с TabLayout
├── MonitorFragment.kt        # Фрагмент мониторинга
├── SpeedTestFragment.kt      # Фрагмент теста скорости
├── SpeedTestEngine.kt        # Логика теста скорости
├── SpeedTestGaugeView.kt     # Кастомный датчик скорости
├── MonitorService.kt         # Фоновый сервис
├── InternetStateChecker.kt   # Проверка состояния сети
├── NotificationHelper.kt     # Управление уведомлениями
├── InternetState.kt          # Перечисление состояний
└── UrlGroups.kt              # Конфигурация URL
```

---

## Лицензия

MIT License - см. [LICENSE](LICENSE)

---

<div align="center">

**Сделано с любовью к киберпанку**

[Report Bug](https://github.com/EggZys/WhiteList-Checker/issues) · [Request Feature](https://github.com/EggZys/WhiteList-Checker/issues)

</div>
