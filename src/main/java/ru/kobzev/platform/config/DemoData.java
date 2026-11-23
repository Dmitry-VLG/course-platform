package ru.kobzev.platform.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.kobzev.platform.api.Inputs.*;
import ru.kobzev.platform.domain.*;
import ru.kobzev.platform.repository.UserRepository;
import ru.kobzev.platform.service.*;
import java.time.LocalDate;
import java.util.*;

@Component
@ConditionalOnProperty(name = "platform.demo.enabled", havingValue = "true")
public class DemoData implements ApplicationRunner {
    private final UserRepository users;
    private final DirectoryService directory;
    private final CatalogService catalog;
    private final EnrollmentService enrollment;
    private final AssignmentService assignments;
    private final QuizService quizzes;

    public DemoData(UserRepository users, DirectoryService directory, CatalogService catalog,
                    EnrollmentService enrollment, AssignmentService assignments, QuizService quizzes) {
        this.users = users;
        this.directory = directory;
        this.catalog = catalog;
        this.enrollment = enrollment;
        this.assignments = assignments;
        this.quizzes = quizzes;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() != 0) return;
        int teacher = directory.saveUser(null, new Person("Ирина Белова", "irina@example.org", Role.TEACHER)).id();
        int student = directory.saveUser(null, new Person("Антон Лебедев", "anton@example.org", Role.STUDENT)).id();
        directory.saveUser(null, new Person("Мария Орлова", "maria@example.org", Role.STUDENT));
        directory.saveProfile(teacher, new ProfileData("Преподаёт проектирование реляционных моделей.", null));
        int category = directory.saveCategory(null, new Name("Базы данных")).id();
        int tag = directory.saveTag(null, new Name("Hibernate")).id();
        LocalDate start = LocalDate.now();
        int course = catalog.createCourse(new CourseData("ORM и Hibernate",
                "От объектной модели до транзакционных запросов.", category, teacher, "4 недели", start, Set.of(tag))).id();
        catalog.createCourse(new CourseData("Проектирование схем SQL",
                "Ключи, связи и ограничения целостности.", category, teacher, "2 недели", start, Set.of()));
        int first = catalog.createModule(course, teacher, new ModuleData("Сущности и связи", 1,
                "Отображение объектов на таблицы")).id();
        int second = catalog.createModule(course, teacher, new ModuleData("Загрузка данных", 2,
                "Границы транзакции и выборки")).id();
        int lesson = catalog.createLesson(first, teacher, new LessonData("Владелец связи",
                "В паре Course и Module внешний ключ course_id хранится в таблице модулей. Поле Module.course управляет этой связью.", null)).id();
        int fetchLesson = catalog.createLesson(second, teacher, new LessonData("Ленивая коллекция",
                "Соберите DTO внутри транзакции, пока коллекция модулей доступна через открытый контекст.", null)).id();
        int task = catalog.createTask(lesson, teacher, new Task("Спроектировать связь",
                "Объясните назначение mappedBy на примере курса и модуля.", start.plusWeeks(1), 10)).id();
        catalog.createTask(fetchLesson, teacher, new Task("Запрос с JOIN FETCH",
                "Напишите JPQL для получения курса вместе с модулями.", start.plusWeeks(2), 10));
        var quiz = quizzes.create(first, teacher, new QuizData("Проверка модели", 10, List.of(
                new QuestionData("Где хранится внешний ключ курса?", QuestionType.SINGLE_CHOICE, List.of(
                        new OptionData("В таблице модулей", true), new OptionData("В таблице курсов", false))),
                new QuestionData("Какие аннотации описывают связи сущностей?", QuestionType.MULTIPLE_CHOICE, List.of(
                        new OptionData("@ManyToOne", true), new OptionData("@OneToMany", true), new OptionData("@Column", false))))));
        enrollment.enroll(course, student);
        int submission = assignments.submit(task, new Work(student, "mappedBy ссылается на поле course у Module.")).id();
        assignments.grade(submission, new Grade(teacher, 10, "Верно указана владеющая сторона."));
        var one = quiz.questions().get(0);
        var two = quiz.questions().get(1);
        quizzes.take(quiz.id(), new Attempt(student, Map.of(one.id(), Set.of(one.options().get(0).id()),
                two.id(), Set.of(two.options().get(0).id(), two.options().get(1).id()))));
        enrollment.review(course, new ReviewData(student, 5, "Пример с внешним ключом помог разобраться в mappedBy."));
    }
}
