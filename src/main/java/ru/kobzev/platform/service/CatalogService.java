package ru.kobzev.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.domain.*;
import ru.kobzev.platform.repository.*;
import java.util.List;
import org.springframework.data.domain.Sort;
import java.util.Comparator;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final Access access;
    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final AssignmentRepository tasks;

    public CatalogService(Access access,
            CourseRepository courses,
            CourseModuleRepository modules,
            LessonRepository lessons,
            AssignmentRepository tasks) {
        this.access = access;
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.tasks = tasks;
    }

    public List<Views.CourseCard> courses() {
        return courses.findAll(Sort.by("id")).stream().map(this::card).toList();
    }

    private Views.CourseCard card(Course c) {
        return new Views.CourseCard(c.getId(), c.getTitle(), c.getDescription(),
                new Views.Named(c.getCategory().getId(), c.getCategory().getName()),
                new Views.Named(c.getTeacher().getId(), c.getTeacher().getName()), c.getDuration(), c.getStartDate(),
                c.getTags().stream().map(t -> new Views.Named(t.getId(), t.getName()))
                        .sorted(Comparator.comparing(Views.Named::name)).toList());
    }

    @Transactional
    public Views.CourseCard saveCourse(Integer id, Inputs.CourseData data) {
        Course c = id == null ? new Course() : access.get(Course.class, id);
        c.setTeacher(access.withRole(data.teacherId(), Role.TEACHER));
        c.setCategory(access.get(Category.class, data.categoryId()));
        c.setTitle(data.title().strip());
        c.setDescription(data.description());
        c.setDuration(data.duration());
        c.setStartDate(data.startDate());
        c.getTags().clear();
        if (data.tagIds() != null) {
            for (int tagId : data.tagIds()) c.getTags().add(access.get(Tag.class, tagId));
        }
        return card(courses.save(c));
    }

    public Views.CourseDetail detail(int id) {
        Course c = courses.findDetailedById(id).orElseThrow(() -> PlatformException.missing("Курс не найден"));
        return new Views.CourseDetail(card(c), c.getModules().stream().map(this::moduleView).toList());
    }

    private Views.ModuleView moduleView(CourseModule m) {
        return new Views.ModuleView(m.getId(), m.getTitle(), m.getOrderIndex(), m.getDescription(),
                m.getLessons().stream().map(this::lessonView).toList(), m.getQuiz() == null ? null : m.getQuiz().getId());
    }

    private Views.LessonView lessonView(Lesson l) {
        return new Views.LessonView(l.getId(), l.getTitle(), l.getContent(), l.getVideoUrl(),
                l.getAssignments().stream().map(Views.TaskView::of).toList());
    }

    @Transactional
    public void deleteCourse(int id) { courses.delete(access.get(Course.class, id)); }

    @Transactional
    public Views.ModuleView saveModule(Integer id, Integer courseId, int teacherId, Inputs.ModuleData data) {
        CourseModule m = id == null ? new CourseModule() : access.get(CourseModule.class, id);
        Course c = id == null ? access.get(Course.class, courseId) : m.getCourse();
        access.requireTeacher(c, teacherId);
        m.setCourse(c);
        m.setTitle(data.title().strip());
        m.setDescription(data.description());
        m.setOrderIndex(data.orderIndex());
        return moduleView(modules.save(m));
    }

    @Transactional
    public Views.LessonView saveLesson(Integer id, Integer moduleId, int teacherId, Inputs.LessonData data) {
        Lesson l = id == null ? new Lesson() : access.get(Lesson.class, id);
        CourseModule m = id == null ? access.get(CourseModule.class, moduleId) : l.getModule();
        access.requireTeacher(m.getCourse(), teacherId);
        l.setModule(m);
        l.setTitle(data.title().strip());
        l.setContent(data.content());
        l.setVideoUrl(data.videoUrl());
        return lessonView(lessons.save(l));
    }

    @Transactional
    public Views.TaskView saveTask(Integer id, Integer lessonId, int teacherId, Inputs.Task data) {
        Assignment a = id == null ? new Assignment() : access.get(Assignment.class, id);
        Lesson l = id == null ? access.get(Lesson.class, lessonId) : a.getLesson();
        access.requireTeacher(l.getModule().getCourse(), teacherId);
        if (id != null && a.getSubmissions().stream().anyMatch(s -> s.getScore() != null && s.getScore() > data.maxScore())) {
            throw PlatformException.conflict("Максимум ниже уже выставленной оценки");
        }
        a.setLesson(l);
        a.setTitle(data.title().strip());
        a.setDescription(data.description());
        a.setDueDate(data.dueDate());
        a.setMaxScore(data.maxScore());
        return Views.TaskView.of(tasks.save(a));
    }

    @Transactional
    public void deleteModule(int id, int teacherId) {
        CourseModule item = access.get(CourseModule.class, id);
        access.requireTeacher(item.getCourse(), teacherId);
        modules.delete(item);
    }

    @Transactional
    public void deleteLesson(int id, int teacherId) {
        Lesson item = access.get(Lesson.class, id);
        access.requireTeacher(item.getModule().getCourse(), teacherId);
        lessons.delete(item);
    }

    @Transactional
    public void deleteTask(int id, int teacherId) {
        Assignment item = access.get(Assignment.class, id);
        access.requireTeacher(item.getLesson().getModule().getCourse(), teacherId);
        tasks.delete(item);
    }
}
