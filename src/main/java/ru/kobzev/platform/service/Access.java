package ru.kobzev.platform.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import ru.kobzev.platform.domain.*;
import ru.kobzev.platform.repository.EnrollmentRepository;

@Component
public class Access {
    private final EntityManager em;
    private final EnrollmentRepository enrollments;

    public Access(EntityManager em, EnrollmentRepository enrollments) {
        this.em = em;
        this.enrollments = enrollments;
    }

    public <T> T get(Class<T> type, int id) {
        T value = em.find(type, id);
        if (value == null) throw PlatformException.missing(type.getSimpleName() + " #" + id + " не найден");
        return value;
    }

    public User withRole(int id, Role role) {
        User user = get(User.class, id);
        if (user.getRole() != role) throw PlatformException.invalid("Требуется роль " + role);
        return user;
    }

    public void requireTeacher(Course course, int teacherId) {
        if (course.getTeacher().getId() != teacherId) {
            throw PlatformException.forbidden("Действие доступно преподавателю этого курса");
        }
    }

    public User participant(int studentId, Course course) {
        User student = withRole(studentId, Role.STUDENT);
        if (!enrollments.existsByCourseIdAndUserId(course.getId(), studentId)) {
            throw PlatformException.forbidden("Студент не записан на курс");
        }
        return student;
    }
}
