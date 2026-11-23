package ru.kobzev.platform.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.domain.Assignment;
import ru.kobzev.platform.domain.Category;
import ru.kobzev.platform.domain.Course;
import ru.kobzev.platform.domain.CourseModule;
import ru.kobzev.platform.domain.Lesson;
import ru.kobzev.platform.domain.Role;
import ru.kobzev.platform.domain.Tag;
import ru.kobzev.platform.repository.AssignmentRepository;
import ru.kobzev.platform.repository.CourseModuleRepository;
import ru.kobzev.platform.repository.CourseRepository;
import ru.kobzev.platform.repository.LessonRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final Access access;
    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final AssignmentRepository tasks;

    public CatalogService(Access access, CourseRepository courses, CourseModuleRepository modules,
                          LessonRepository lessons, AssignmentRepository tasks) {
        this.access = access;
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.tasks = tasks;
    }

    public List<Views.CourseCard> courses() {
        return courses.findAll(Sort.by("id")).stream().map(this::card).toList();
    }

    public Views.CourseDetail detail(int id) {
        Course course = courses.findDetailedById(id)
                .orElseThrow(() -> PlatformException.missing("Курс не найден"));
        return new Views.CourseDetail(card(course), course.getModules().stream().map(this::moduleView).toList());
    }

    @Transactional
    public Views.CourseCard createCourse(Inputs.CourseData data) {
        return saveCourse(new Course(), data);
    }

    @Transactional
    public Views.CourseCard updateCourse(int id, Inputs.CourseData data) {
        return saveCourse(access.get(Course.class, id), data);
    }

    @Transactional
    public void deleteCourse(int id) {
        courses.delete(access.get(Course.class, id));
    }

    @Transactional
    public Views.ModuleView createModule(int courseId, int teacherId, Inputs.ModuleData data) {
        CourseModule module = new CourseModule();
        module.setCourse(access.get(Course.class, courseId));
        return saveModule(module, teacherId, data);
    }

    @Transactional
    public Views.ModuleView updateModule(int id, int teacherId, Inputs.ModuleData data) {
        return saveModule(access.get(CourseModule.class, id), teacherId, data);
    }

    @Transactional
    public void deleteModule(int id, int teacherId) {
        CourseModule module = access.get(CourseModule.class, id);
        access.requireTeacher(module.getCourse(), teacherId);
        modules.delete(module);
    }

    @Transactional
    public Views.LessonView createLesson(int moduleId, int teacherId, Inputs.LessonData data) {
        Lesson lesson = new Lesson();
        lesson.setModule(access.get(CourseModule.class, moduleId));
        return saveLesson(lesson, teacherId, data);
    }

    @Transactional
    public Views.LessonView updateLesson(int id, int teacherId, Inputs.LessonData data) {
        return saveLesson(access.get(Lesson.class, id), teacherId, data);
    }

    @Transactional
    public void deleteLesson(int id, int teacherId) {
        Lesson lesson = access.get(Lesson.class, id);
        access.requireTeacher(lesson.getModule().getCourse(), teacherId);
        lessons.delete(lesson);
    }

    @Transactional
    public Views.TaskView createTask(int lessonId, int teacherId, Inputs.Task data) {
        Assignment assignment = new Assignment();
        assignment.setLesson(access.get(Lesson.class, lessonId));
        return saveTask(assignment, teacherId, data);
    }

    @Transactional
    public Views.TaskView updateTask(int id, int teacherId, Inputs.Task data) {
        return saveTask(access.get(Assignment.class, id), teacherId, data);
    }

    @Transactional
    public void deleteTask(int id, int teacherId) {
        Assignment assignment = access.get(Assignment.class, id);
        access.requireTeacher(assignment.getLesson().getModule().getCourse(), teacherId);
        tasks.delete(assignment);
    }

    private Views.CourseCard saveCourse(Course course, Inputs.CourseData data) {
        course.setTeacher(access.withRole(data.teacherId(), Role.TEACHER));
        course.setCategory(access.get(Category.class, data.categoryId()));
        course.setTitle(data.title().strip());
        course.setDescription(data.description());
        course.setDuration(data.duration());
        course.setStartDate(data.startDate());
        course.getTags().clear();
        if (data.tagIds() != null) {
            for (int tagId : data.tagIds()) {
                course.getTags().add(access.get(Tag.class, tagId));
            }
        }
        return card(courses.save(course));
    }

    private Views.ModuleView saveModule(CourseModule module, int teacherId, Inputs.ModuleData data) {
        access.requireTeacher(module.getCourse(), teacherId);
        module.setTitle(data.title().strip());
        module.setDescription(data.description());
        module.setOrderIndex(data.orderIndex());
        return moduleView(modules.save(module));
    }

    private Views.LessonView saveLesson(Lesson lesson, int teacherId, Inputs.LessonData data) {
        access.requireTeacher(lesson.getModule().getCourse(), teacherId);
        lesson.setTitle(data.title().strip());
        lesson.setContent(data.content());
        lesson.setVideoUrl(data.videoUrl());
        return lessonView(lessons.save(lesson));
    }

    private Views.TaskView saveTask(Assignment assignment, int teacherId, Inputs.Task data) {
        access.requireTeacher(assignment.getLesson().getModule().getCourse(), teacherId);
        boolean exceedsNewMaximum = assignment.getSubmissions().stream()
                .anyMatch(work -> work.getScore() != null && work.getScore() > data.maxScore());
        if (exceedsNewMaximum) {
            throw PlatformException.conflict("Максимум ниже уже выставленной оценки");
        }
        assignment.setTitle(data.title().strip());
        assignment.setDescription(data.description());
        assignment.setDueDate(data.dueDate());
        assignment.setMaxScore(data.maxScore());
        return Views.TaskView.of(tasks.save(assignment));
    }

    private Views.CourseCard card(Course course) {
        var category = course.getCategory();
        var teacher = course.getTeacher();
        var tags = course.getTags().stream()
                .map(tag -> new Views.Named(tag.getId(), tag.getName()))
                .sorted(Comparator.comparing(Views.Named::name))
                .toList();
        return new Views.CourseCard(course.getId(), course.getTitle(), course.getDescription(),
                new Views.Named(category.getId(), category.getName()), new Views.Named(teacher.getId(), teacher.getName()),
                course.getDuration(), course.getStartDate(), tags);
    }

    private Views.ModuleView moduleView(CourseModule module) {
        var quiz = module.getQuiz();
        return new Views.ModuleView(module.getId(), module.getTitle(), module.getOrderIndex(), module.getDescription(),
                module.getLessons().stream().map(this::lessonView).toList(), quiz == null ? null : quiz.getId());
    }

    private Views.LessonView lessonView(Lesson lesson) {
        return new Views.LessonView(lesson.getId(), lesson.getTitle(), lesson.getContent(), lesson.getVideoUrl(),
                lesson.getAssignments().stream().map(Views.TaskView::of).toList());
    }
}
