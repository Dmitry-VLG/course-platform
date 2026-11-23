# Справочник REST API

Базовый путь — `/api`. Тело запроса и ответ используют JSON. Для запросов с телом укажите `Content-Type: application/json`. Числовые ID возвращаются сервером. Даты имеют формат `YYYY-MM-DD`, время событий — ISO 8601 UTC. Метод PUT полностью заменяет редактируемые поля.

| Метод | Путь относительно /api | Тело | Результат |
|---|---|---|---|
| GET | `/users` | — | 200 · список Person |
| GET | `/users/{id}` | — | 200 · Person |
| POST | `/users` | Person | 201 · Person |
| PUT | `/users/{id}` | Person | 200 · Person |
| DELETE | `/users/{id}` | — | 204 |
| GET | `/users/{id}/profile` | — | 200 · ProfileView |
| PUT | `/users/{id}/profile` | ProfileData | 200 · ProfileView |
| GET | `/categories` | — | 200 · список Named |
| POST | `/categories` | Name | 201 · Named |
| PUT | `/categories/{id}` | Name | 200 · Named |
| DELETE | `/categories/{id}` | — | 204 |
| GET | `/tags` | — | 200 · список Named |
| POST | `/tags` | Name | 201 · Named |
| PUT | `/tags/{id}` | Name | 200 · Named |
| DELETE | `/tags/{id}` | — | 204 |
| GET | `/courses` | — | 200 · список CourseCard |
| GET | `/courses/{id}` | — | 200 · CourseDetail |
| POST | `/courses` | CourseData | 201 · CourseCard |
| PUT | `/courses/{id}` | CourseData | 200 · CourseCard |
| DELETE | `/courses/{id}` | — | 204 |
| POST | `/courses/{id}/modules?teacherId={id}` | ModuleData | 201 · ModuleView |
| PUT | `/modules/{id}?teacherId={id}` | ModuleData | 200 · ModuleView |
| DELETE | `/modules/{id}?teacherId={id}` | — | 204 |
| POST | `/modules/{id}/lessons?teacherId={id}` | LessonData | 201 · LessonView |
| PUT | `/lessons/{id}?teacherId={id}` | LessonData | 200 · LessonView |
| DELETE | `/lessons/{id}?teacherId={id}` | — | 204 |
| POST | `/lessons/{id}/assignments?teacherId={id}` | Task | 201 · TaskView |
| PUT | `/assignments/{id}?teacherId={id}` | Task | 200 · TaskView |
| DELETE | `/assignments/{id}?teacherId={id}` | — | 204 |
| POST | `/courses/{id}/enroll?user={id}` | — | 201 · Registration |
| DELETE | `/courses/{id}/enroll?user={id}` | — | 204 |
| GET | `/courses/{id}/students` | — | 200 · список Registration |
| GET | `/users/{id}/courses` | — | 200 · список Registration |
| POST | `/courses/{id}/reviews` | ReviewData | 201 · Review |
| GET | `/courses/{id}/reviews` | — | 200 · список Review |
| POST | `/assignments/{id}/submit` | Work | 201 · WorkView |
| PATCH | `/submissions/{id}/grade` | Grade | 200 · WorkView |
| GET | `/assignments/{id}/submissions` | — | 200 · список WorkView |
| GET | `/users/{id}/assignments` | — | 200 · список TaskView |
| GET | `/users/{id}/submissions` | — | 200 · список WorkView |
| POST | `/modules/{id}/quiz?teacherId={id}` | QuizData | 201 · QuizView |
| GET | `/quizzes/{id}` | — | 200 · QuizView |
| POST | `/quizzes/{id}/attempts` | Attempt | 201 · Result |
| GET | `/quizzes/{id}/results` | — | 200 · список Result |
| GET | `/users/{id}/quiz-results` | — | 200 · список Result |

## Поля запросов

Знаком `?` отмечены необязательные поля. Вложенные объекты также проходят валидацию.

| Тело | Поля |
|---|---|
| Person | `name`, `email`, `role` (`STUDENT`, `TEACHER`, `ADMIN`) |
| ProfileData | `bio?`, `avatarUrl?` |
| Name | `name` |
| CourseData | `title`, `categoryId`, `teacherId`, `description?`, `duration?`, `startDate?`, `tagIds?` (массив ID) |
| ModuleData | `title`, `orderIndex` (от 1), `description?` |
| LessonData | `title`, `content`, `videoUrl?` |
| Task | `title`, `description`, `dueDate`, `maxScore` (> 0) |
| Work | `studentId`, `content` |
| Grade | `teacherId`, `score`, `feedback?` |
| QuizData | `title`, `timeLimit?` (минуты), `questions` (массив, структура ниже) |
| Attempt | `studentId`, `answers` (ID вопроса → массив ID вариантов) |
| ReviewData | `studentId`, `rating` (1–5), `comment?` |

### Создание теста

`POST /api/modules/{id}/quiz?teacherId={idПреподавателя}`

```json
{
  "title": "Связи JPA",
  "timeLimit": 10,
  "questions": [{
    "text": "Какая аннотация описывает связь «многие к одному»?",
    "type": "SINGLE_CHOICE",
    "options": [
      {"text": "@ManyToOne", "correct": true},
      {"text": "@OneToMany", "correct": false}
    ]
  }]
}
```

Для `SINGLE_CHOICE` нужен ровно один правильный вариант. Для `MULTIPLE_CHOICE` нужен хотя бы один. В каждом вопросе минимум два варианта. При чтении теста `questions[].options[]` содержит `id` и `name` (текст варианта). Ключ ответов остаётся на сервере. Один балл начисляется за точное совпадение выбранного и правильного множеств. На каждый вопрос нужен ответ. Разрешено несколько прохождений с отдельными результатами. Поле `timeLimit` задаёт плановую длительность.

### Ошибки

| HTTP | Когда возвращается |
|---|---|
| 400 | Неверный JSON, пустой заголовок, некорректная почта, неподходящая роль, балл вне диапазона, чужой вариант ответа |
| 403 | Пользователь не записан на курс или указанный преподаватель не ведёт курс |
| 404 | Объект с указанным ID отсутствует |
| 409 | Повторная запись, повторное решение, второй тест в модуле, конфликт уникальности или внешнего ключа |

Ответ содержит `status`, `title`, `detail`. Ошибки полей включают массив `errors`. Идентификаторы участников передаются явно. Выбор участника в браузере переключает контекст работы; проверка личности по паролю или токену в этот API не входит.
