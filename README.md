# monitor-app

Мониторы оборудования для завода. Пока здесь один модуль, `pump-monitor`: прототип к лабораторной по контролю
насоса. Загружаем json с записью давления и оборотов, программа проверяет формат, считает время работы, остановок,
пропусков и недостоверных данных, время по режимам и рисует графики.

Другие мониторы и сервисы добавляются как новые gradle-модули рядом с `pump-monitor`.

## Запуск

Нужна Java 17 или новее.

```
gradlew.bat bootRun
```

(на Linux/macOS `./gradlew bootRun`)

Дальше открыть http://localhost:8080 и перетащить на страницу файл из `pump-monitor/src/test/resources/data/`.

При `bootRun` страница берётся прямо из `web/src/main/resources/static`, так что после правок в html/css/js
достаточно обновить страницу.

Собрать один jar:

```
gradlew.bat bootJar
java -jar pump-monitor/build/libs/pump-monitor.jar
```

## Тесты

```
gradlew.bat test
```

`ExpectedResultsTest` прогоняет все файлы из `pump-monitor/src/test/resources/data/` и `data/extra/`
и сравнивает с `expected_results.json` и `expected_extra.json`. Чтобы добавить свой случай, кладём файл
в `data/extra/` и дописываем ожидаемый ответ в `expected_extra.json`.

## Структура

```
pump-monitor/                     монитор насоса, Kotlin + Spring Boot, собирает jar
  src/main/kotlin/com/ryamal/pumpmonitor/
    config/                       бины
    core/component/               данные: записи, интервалы, итоги, правила
    core/operation/read/          расчёт интервалов и операция анализа
    input/comm/                   REST-контроллер, ответы в response/
    output/port/                  интерфейс чтения записи
    output/adapter/json/          чтение и проверка json, чтение rules.json
  src/main/resources/             application.yml, rules.json
  src/test/kotlin/                тесты, сквозные в usecase/, правила слоёв в arch/
  src/test/resources/data/        файлы из задания и ответы к ним
    extra/                        наши пограничные случаи
web/                              страница монитора насоса, только статика
  src/main/resources/static/      html, css, js, ECharts
    mock/                         сохранённые ответы сервера, /?mock=05_long_record (в jar не попадают)
```

`pump-monitor` подключает `web` как зависимость и отдаёт страницу из classpath.

## API

`POST /api/analyze`, в теле содержимое json-файла.

- 200: `totals` (поля как в `expected_results.json`), `samples` и `segments` для графика
- 422: ошибка формата, `category`, `path` (например `samples[0].rpm`) и `message`
- 400: тело не json

`segments` это участки `[start_s, end_s)` с состоянием `working` (+ `mode`), `stopped`, `gap` или `invalid`.
