# Платформа для онлайн-курсов по ORM и Hibernate.

Spring Boot-приложение для управления курсами, которая позволит вести расписание занятий, раздавать задания студентам, собирать их решения и проводить тестирование знаний. Данные хранятся в базе PostgreSQL.

## Запуск в Docker

Нужен Docker с поддержкой Compose. Откройте PowerShell в каталоге с `compose.yaml`. Команда сгенерирует пароль для новой локальной базы и запустит оба сервиса.

```powershell
# Выполнить один раз для новой базы. Compose читает пароль из .env.
if (!(Test-Path .env)) { "DB_PASSWORD=$([guid]::NewGuid().ToString('N'))" | Set-Content -Encoding ascii .env }
docker compose up --build -d
docker compose logs -f app
```

Для Bash на Linux или macOS выполните в каталоге репозитория:

```bash
if [ ! -e .env ]; then
  (set -eC; umask 077
   password=$(openssl rand -hex 32)
   printf 'DB_PASSWORD=%s\n' "$password" > .env) || exit 1
fi
docker compose up --build -d
docker compose logs -f app
```

Откройте [каталог курсов](http://localhost:8080). Команда `docker compose down` останавливает сервисы и сохраняет данные в именованном томе. Для повторного запуска используйте тот же файл `.env`. Порт приложения можно сменить через `APP_PORT`. PostgreSQL доступен в сети Compose. Если Docker не может разрешить имя Maven Central из контейнера сборки, используйте описанный ниже запуск JAR с локальным PostgreSQL либо настройте Maven-прокси и доверенные сертификаты в своей Docker-среде.

## Первый сценарий

1. Откройте курс «ORM и Hibernate». В списке участников выберите **Марию Орлову** и нажмите **Записаться на курс**.
2. В первом модуле раскройте задание **Спроектировать связь**, введите текст и нажмите **Отправить решение**. Повторная отправка того же задания этим участником возвращает HTTP 409.
3. Нажмите **Открыть тест**, отметьте ответы и завершите тест. Появятся дата попытки и баллы.
4. Переключитесь на **Антона Лебедева**. У него уже есть проверенное решение с комментарием преподавателя и результат теста. Выставить оценку другому решению можно через API (пример ниже).

При первом запуске профиля `dev` в пустом каталоге пользователей создаются два курса, два модуля курса ORM, уроки, задания, тест, преподаватель и два студента. Инициализация выполняется одной транзакцией. При повторном запуске существующие данные сохраняются. Демо-наполнение отключается переменной `DEMO_ENABLED=false`.

## Запуск с локальным PostgreSQL

Нужны JDK 17, Maven 3.9 и PostgreSQL. Maven загружает зависимости, перечисленные в `pom.xml`. В PostgreSQL создайте отдельного пользователя и базу. Команды для `psql` под администратором:

```sql
CREATE ROLE courses LOGIN;
\password courses
CREATE DATABASE courses OWNER courses;
```

В PowerShell задайте те же параметры подключения. Пароль вводится в приглашении консоли.

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/courses'
$env:DB_USER = 'courses'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Пароль PostgreSQL' -AsSecureString)).Password
mvn clean verify
java -jar target/course-platform-1.0.0.jar
```

Для разработки можно выполнить `mvn spring-boot:run`. В **IntelliJ IDEA** откройте `pom.xml` как проект Maven, выберите JDK 17 и разрешите импорт зависимостей. В конфигурации запуска `PlatformApplication` задайте `DB_URL`, `DB_USER`, `DB_PASSWORD`.

| Настройка | Назначение |
|---|---|
| `dev` (по умолчанию) | PostgreSQL, схема Hibernate `update`, демо-данные на первом запуске |
| `test` | H2 в памяти в режиме PostgreSQL или отдельная тестовая PostgreSQL по `TEST_DB_URL`, создание схемы перед тестами и удаление после |
| `DDL_AUTO` | Управление схемой для профиля dev, например `update` или `validate` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Подключение профиля dev |
| `DEMO_ENABLED` | Включить или отключить начальное наполнение |
| `APP_PORT` | Внешний порт приложения в Docker Compose, по умолчанию 8080 |

## Работа через API

Полный список маршрутов, полей и кодов ответа приведён в [справочнике API](docs/api.md). В примере PowerShell идентификаторы берутся из ответов текущей базы. Сначала отправьте решение от имени Марии через интерфейс.

```powershell
$api = 'http://localhost:8080/api'
$course = (Invoke-RestMethod "$api/courses") | Where-Object title -eq 'ORM и Hibernate'
$users = Invoke-RestMethod "$api/users"
$maria = $users | Where-Object email -eq 'maria@example.org'
$structure = Invoke-RestMethod "$api/courses/$($course.id)"
$task = $structure.modules[0].lessons[0].assignments[0]
$works = Invoke-RestMethod "$api/assignments/$($task.id)/submissions"
$work = $works | Where-Object studentId -eq $maria.id
$grade = @{teacherId=$course.teacher.id; score=9; feedback='Связь описана верно'} | ConvertTo-Json
Invoke-RestMethod "$api/submissions/$($work.id)/grade" -Method Patch -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($grade))
Invoke-RestMethod "$api/users/$($maria.id)/submissions"
```

## Модель и архитектура

| Пакет | Ответственность |
|---|---|
| `domain` | 15 JPA-сущностей и перечисления |
| `repository` | 15 репозиториев Spring Data, запросы по студенту, курсу и заданию |
| `service` | Транзакции, роли, правила записи, оценивание, вычисление результата теста |
| `api` | Форматы запросов и ответов, валидация, централизованная обработка ошибок |
| `web` | REST-контроллеры каталога, пользователей, записей, заданий и тестов |
| `config` | Начальное наполнение демо-данными |
| `resources/static` | Браузерный интерфейс, использующий тот же REST API |

| Область | Сущности и связи |
|---|---|
| Пользователи | User и Profile связаны один к одному. У пользователя уникальная почта |
| Курсы | Course ссылается на преподавателя User и Category. Course ↔ Tag — многие ко многим через `course_tag` |
| Программа | Course → CourseModule → Lesson → Assignment. Классу CourseModule соответствует блок MODULE диаграммы |
| Запись | Enrollment связывает User и Course, хранит дату и статус. Пара пользователь–курс уникальна |
| Решения | Submission связывает студента и Assignment. Одна работа на пару студент–задание. Статус `SUBMITTED` до проверки и `GRADED` после выставления оценки |
| Тесты | У модуля до одного Quiz. Quiz содержит Question с вариантами AnswerOption. QuizSubmission хранит результат попытки |
| Отзывы | CourseReview связывает курс и студента, хранит оценку, комментарий и дату |

Удаление курса каскадно удаляет программу, записи, работы, отзывы и результаты его тестов. Категории, пользователи и общие теги сохраняются. При отписке удаляется только запись на курс, история решений и тестов остаётся. Срок `dueDate` виден студенту. После этой даты отправка решения остаётся доступной. `timeLimit` показывает плановую длительность теста. История сданных работ и тестов видна после отписки.

Коллекции загружаются лениво, `spring.jpa.open-in-view=false`. Для структуры курса `findDetailedById` подгружает модули, категорию и преподавателя через `@EntityGraph`. Остальные данные считываются при построении DTO внутри транзакции, с пакетной загрузкой Hibernate. Сериализация ответа не обходит граф сущностей.

## Проверка

```shell
mvn clean verify
```

По умолчанию тесты используют H2 в памяти. `QuizScoringTest` проверяет точный подсчёт баллов, частичные ответы и чужие варианты. `PlatformIntegrationTest` поднимает Spring, создаёт схему и выполняет операции через сервисы, репозитории и MockMvc. Среди проверок — каскады, сохранность общих тегов, дубликаты, роли, диапазоны оценок, закрытая Hibernate-сессия и доступ к заранее загруженному графу. Отчёты Maven находятся в `target/surefire-reports/`.

Для прогона на PostgreSQL создайте **отдельную пустую базу** с владельцем, имеющим право создавать таблицы. Профиль тестов пересоздаёт и удаляет таблицы. Укажите подключение только к этой базе:

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/platform_test'
$env:TEST_DB_USER = 'courses'
$env:TEST_DB_PASSWORD = $env:DB_PASSWORD
mvn test
```

`DemoScenarioTest` проверяет, что при первом запуске доступны курсы, проверенная работа, результат теста и файлы интерфейса. Повторная инициализация сохраняет количество пользователей и курсов.
