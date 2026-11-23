package ru.kobzev.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.kobzev.platform.api.Inputs.*;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.domain.*;
import ru.kobzev.platform.repository.*;
import ru.kobzev.platform.service.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformIntegrationTest {
    @Autowired DirectoryService directory;
    @Autowired CatalogService catalog;
    @Autowired EnrollmentService enrollment;
    @Autowired AssignmentService assignments;
    @Autowired QuizService quizzes;
    @Autowired CourseRepository courses;
    @Autowired CourseModuleRepository moduleRepository;
    @Autowired AssignmentRepository taskRepository;
    @Autowired SubmissionRepository submissionRepository;
    @Autowired EntityManagerFactory emf;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;

    int teacher, student, category, course, module, lesson, task;
    String suffix;

    @BeforeEach void createCourse() {
        suffix = UUID.randomUUID().toString();
        teacher = directory.saveUser(null, new Person("Teacher", "t" + suffix + "@example.org", Role.TEACHER)).id();
        student = directory.saveUser(null, new Person("Student", "s" + suffix + "@example.org", Role.STUDENT)).id();
        category = directory.saveCategory(null, new Name("Data " + suffix)).id();
        course = catalog.createCourse(courseData("Hibernate")).id();
        module = catalog.createModule(course, teacher, new ModuleData("Mapping", 1, null)).id();
        lesson = catalog.createLesson(module, teacher, new LessonData("Ownership", "Read the mapping", null)).id();
        task = catalog.createTask(lesson, teacher, new Task("Map a relation", "Explain the FK",
                LocalDate.now().plusDays(1), 10)).id();
    }

    CourseData courseData(String title) {
        return new CourseData(title, "Mapping", category, teacher, "4 weeks", LocalDate.now(), Set.of());
    }

    Views.QuizView quiz() {
        return quizzes.create(module, teacher, new QuizData("Quiz", 5, List.of(new QuestionData("Owner?",
                QuestionType.SINGLE_CHOICE, List.of(new OptionData("Module", true), new OptionData("Course", false))))));
    }

    @Test void schemaContainsAllEntitiesAndJoinTable() {
        assertEquals(15, emf.getMetamodel().getEntities().size());
        assertNotNull(jdbc.queryForObject("select count(*) from course_tag", Long.class));
    }

    @Test void courseCrudReloadsStructureAndCascadesDelete() {
        assertEquals(task, catalog.detail(course).modules().get(0).lessons().get(0).assignments().get(0).id());
        catalog.updateCourse(course, courseData("JPA"));
        assertEquals("JPA", catalog.detail(course).course().title());
        catalog.deleteCourse(course);
        assertFalse(courses.existsById(course));
        assertFalse(moduleRepository.existsById(module));
        assertFalse(taskRepository.existsById(task));
        assertEquals(teacher, directory.user(teacher).id());
    }

    @Test void cascadePersistSavesCourseAndModule() {
        Course draft = new Course();
        Course source = courses.findById(course).orElseThrow();
        draft.setTitle("Cascade");
        draft.setTeacher(source.getTeacher());
        draft.setCategory(source.getCategory());
        CourseModule child = new CourseModule();
        child.setTitle("Child");
        child.setCourse(draft);
        child.setOrderIndex(1);
        draft.getModules().add(child);
        courses.saveAndFlush(draft);
        assertEquals("Child", catalog.detail(draft.getId()).modules().get(0).title());
    }

    @Test void detachedCollectionFailsButFetchedGraphWorks() {
        Course detached = courses.findById(course).orElseThrow();
        assertThrows(LazyInitializationException.class, () -> detached.getModules().size());
        assertEquals(1, courses.findDetailedById(course).orElseThrow().getModules().size());
    }

    @Test void deletingCoursePreservesSharedTagAndCategory() {
        int tag = directory.saveTag(null, new Name("Tag " + suffix)).id();
        CourseData tagged = new CourseData("Tagged", null, category, teacher, null, null, Set.of(tag));
        catalog.updateCourse(course, tagged);
        int other = catalog.createCourse(tagged).id();
        catalog.deleteCourse(course);
        assertEquals(tag, catalog.detail(other).course().tags().get(0).id());
        assertEquals(category, catalog.detail(other).course().category().id());
    }

    @Test void enrollmentIsUniqueAndWithdrawalUpdatesBothLists() {
        enrollment.enroll(course, student);
        assertEquals(student, enrollment.roster(course).get(0).student().id());
        assertEquals(course, enrollment.coursesFor(student).get(0).course().id());
        assertThrows(PlatformException.class, () -> enrollment.enroll(course, student));
        enrollment.withdraw(course, student);
        assertTrue(enrollment.coursesFor(student).isEmpty());
        assertTrue(enrollment.roster(course).isEmpty());
    }

    @Test void teacherCannotEnrollAsStudent() {
        assertThrows(PlatformException.class, () -> enrollment.enroll(course, teacher));
    }

    @Test void unenrolledStudentCannotSubmit() {
        assertThrows(PlatformException.class, () -> assignments.submit(task, new Work(student, "answer")));
    }

    @Test void submissionIsUniqueAndGradePersists() {
        enrollment.enroll(course, student);
        assertEquals(task, assignments.tasksFor(student).get(0).id());
        var work = assignments.submit(task, new Work(student, "answer"));
        assertNull(work.score());
        assertEquals(Submission.Status.SUBMITTED, work.status());
        assertThrows(PlatformException.class, () -> assignments.submit(task, new Work(student, "again")));
        assignments.grade(work.id(), new Grade(teacher, 8, "Good"));
        assertEquals(8, assignments.solutions(task).get(0).score());
        assertEquals(Submission.Status.GRADED, assignments.solutions(task).get(0).status());
        assertEquals("Good", assignments.submittedBy(student).get(0).feedback());
    }

    @Test void invalidGradeAndWrongTeacherLeaveWorkUngraded() {
        enrollment.enroll(course, student);
        var work = assignments.submit(task, new Work(student, "answer"));
        assertThrows(PlatformException.class, () -> assignments.grade(work.id(), new Grade(teacher, 11, null)));
        assertThrows(PlatformException.class, () -> assignments.grade(work.id(), new Grade(student, 5, null)));
        assertNull(assignments.solutions(task).get(0).score());
    }

    @Test void quizResultIsComputedAndReadableFromBothSides() {
        var qz = quiz();
        var q = qz.questions().get(0);
        enrollment.enroll(course, student);
        var result = quizzes.take(qz.id(), new Attempt(student, Map.of(q.id(), Set.of(q.options().get(0).id()))));
        assertEquals(1, result.score());
        assertEquals(result.id(), quizzes.resultsForStudent(student).get(0).id());
        assertEquals(result.id(), quizzes.resultsForQuiz(qz.id()).get(0).id());
    }

    @Test void moduleAcceptsOnlyOneQuiz() {
        quiz();
        assertThrows(PlatformException.class, this::quiz);
    }

    @Test void invalidQuizIsRolledBack() {
        var invalid = new QuestionData("?", QuestionType.SINGLE_CHOICE,
                List.of(new OptionData("a", true), new OptionData("b", true)));
        assertThrows(PlatformException.class, () -> quizzes.create(module, teacher, new QuizData("Bad", null, List.of(invalid))));
        assertNull(catalog.detail(course).modules().get(0).quizId());
    }

    @Test void profileIsUpdatedInPlace() {
        var p = directory.saveProfile(student, new ProfileData("First", null));
        assertEquals(p.id(), directory.saveProfile(student, new ProfileData("Changed", null)).id());
        assertEquals("Changed", directory.profile(student).bio());
    }

    @Test void reviewsRequireEnrollmentAndAreUnique() {
        ReviewData review = new ReviewData(student, 5, "Clear");
        assertThrows(PlatformException.class, () -> enrollment.review(course, review));
        enrollment.enroll(course, student);
        enrollment.review(course, review);
        assertThrows(DataIntegrityViolationException.class, () -> enrollment.review(course, review));
        assertEquals("Clear", enrollment.reviews(course).get(0).comment());
    }

    @Test void deletingCourseRemovesResultsButKeepsUsers() {
        enrollment.enroll(course, student);
        var work = assignments.submit(task, new Work(student, "answer"));
        var quiz = quiz();
        var q = quiz.questions().get(0);
        quizzes.take(quiz.id(), new Attempt(student, Map.of(q.id(), Set.of(q.options().get(0).id()))));
        catalog.deleteCourse(course);
        assertFalse(submissionRepository.existsById(work.id()));
        assertTrue(quizzes.resultsForStudent(student).isEmpty());
        assertTrue(enrollment.coursesFor(student).isEmpty());
        assertEquals(student, directory.user(student).id());
    }

    @Test void restReturns404ForMissingCourse() throws Exception {
        http.perform(get("/api/courses/2147483647")).andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").exists());
    }

    @Test void restValidatesEmailAndReportsField() throws Exception {
        http.perform(post("/api/users").contentType("application/json")
                .content(json.writeValueAsBytes(new Person("Name", "invalid", Role.STUDENT))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
    }

    @Test void restEnrollReturnsCreatedThenConflictAndDeletes() throws Exception {
        String url = "/api/courses/" + course + "/enroll";
        String id = Integer.toString(student);
        http.perform(post(url).param("user", id)).andExpect(status().isCreated());
        http.perform(post(url).param("user", id)).andExpect(status().isConflict());
        http.perform(delete(url).param("user", id)).andExpect(status().isNoContent());
    }

    @Test
    void restRejectsFractionalGradeBeforeChangingSubmission() throws Exception {
        enrollment.enroll(course, student);
        var work = assignments.submit(task, new Work(student, "answer"));

        http.perform(patch("/api/submissions/{id}/grade", work.id()).contentType("application/json")
                        .content(json.writeValueAsBytes(Map.of("teacherId", teacher, "score", 10.9))))
                .andExpect(status().isBadRequest());

        assertNull(assignments.submittedBy(student).get(0).score());
    }

    @Test
    void restTeacherCanReplaceAndDeleteQuizBeforeFirstAttempt() throws Exception {
        var original = quiz();
        var replacement = new QuizData("Revised", 12, List.of(new QuestionData("New question?",
                QuestionType.SINGLE_CHOICE, List.of(new OptionData("Yes", true), new OptionData("No", false)))));
        String url = "/api/quizzes/" + original.id();

        http.perform(put(url).param("teacherId", Integer.toString(student)).contentType("application/json")
                        .content(json.writeValueAsBytes(replacement)))
                .andExpect(status().isForbidden());
        http.perform(put(url).param("teacherId", Integer.toString(teacher)).contentType("application/json")
                        .content(json.writeValueAsBytes(replacement)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].id").isNumber());
        var updated = quizzes.get(original.id());
        assertEquals("Revised", updated.title());
        assertEquals(List.of("New question?"), updated.questions().stream().map(Views.QuestionView::text).toList());

        http.perform(delete(url).param("teacherId", Integer.toString(teacher)))
                .andExpect(status().isNoContent());
        http.perform(get(url)).andExpect(status().isNotFound());
        assertNull(catalog.detail(course).modules().get(0).quizId());
        assertNotNull(quiz());
    }

    @Test
    void restPreservesQuizWithExistingAttempts() throws Exception {
        var original = quiz();
        var question = original.questions().get(0);
        enrollment.enroll(course, student);
        var attempt = quizzes.take(original.id(), new Attempt(student,
                Map.of(question.id(), Set.of(question.options().get(0).id()))));
        var replacement = new QuizData("Revised", 12, List.of(new QuestionData("New question?",
                QuestionType.SINGLE_CHOICE, List.of(new OptionData("Yes", true), new OptionData("No", false)))));
        String url = "/api/quizzes/" + original.id();

        http.perform(put(url).param("teacherId", Integer.toString(teacher)).contentType("application/json")
                        .content(json.writeValueAsBytes(replacement)))
                .andExpect(status().isConflict());
        http.perform(delete(url).param("teacherId", Integer.toString(teacher)))
                .andExpect(status().isConflict());

        assertEquals(original, quizzes.get(original.id()));
        var savedAttempt = quizzes.resultsForQuiz(original.id()).get(0);
        assertEquals(attempt.id(), savedAttempt.id());
        assertEquals(1, savedAttempt.score());
    }

    @Test void restQuizDoesNotRevealAnswerKey() throws Exception {
        http.perform(get("/api/quizzes/" + quiz().id())).andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].options[0].name").value("Module"))
                .andExpect(jsonPath("$..correct").isEmpty());
    }
}
