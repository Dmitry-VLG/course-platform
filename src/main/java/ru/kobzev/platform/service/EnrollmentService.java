package ru.kobzev.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.domain.*;
import ru.kobzev.platform.repository.*;
import java.util.List;
import java.time.Instant;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class EnrollmentService {
    private final Access access;
    private final EnrollmentRepository enrollments;
    private final CourseReviewRepository reviews;

    public EnrollmentService(Access access,
            EnrollmentRepository enrollments,
            CourseReviewRepository reviews) {
        this.access = access;
        this.enrollments = enrollments;
        this.reviews = reviews;
    }

    @Transactional
    public Views.Registration enroll(int courseId, int studentId) {
        Course course = access.get(Course.class, courseId);
        User student = access.withRole(studentId, Role.STUDENT);
        if (enrollments.existsByCourseIdAndUserId(courseId, studentId)) {
            throw PlatformException.conflict("Студент уже записан на курс");
        }
        Enrollment e = new Enrollment();
        e.setUser(student);
        e.setCourse(course);
        e.setStatus(EnrollmentStatus.ACTIVE);
        e.setEnrollDate(LocalDate.now());
        return Views.Registration.of(enrollments.save(e));
    }

    @Transactional
    public void withdraw(int courseId, int studentId) {
        Enrollment e = enrollments.findByCourseIdAndUserId(courseId, studentId)
                .orElseThrow(() -> PlatformException.missing("Запись на курс не найдена"));
        enrollments.delete(e);
    }

    public List<Views.Registration> roster(int courseId) {
        access.get(Course.class, courseId);
        return enrollments.findByCourseIdOrderById(courseId).stream().map(Views.Registration::of).toList();
    }

    public List<Views.Registration> coursesFor(int studentId) {
        access.withRole(studentId, Role.STUDENT);
        return enrollments.findByUserIdOrderById(studentId).stream().map(Views.Registration::of).toList();
    }

    @Transactional
    public Views.Review review(int courseId, Inputs.ReviewData data) {
        Course course = access.get(Course.class, courseId);
        CourseReview r = new CourseReview();
        r.setStudent(access.participant(data.studentId(), course));
        r.setCourse(course);
        r.setRating(data.rating());
        r.setComment(data.comment());
        r.setCreatedAt(Instant.now());
        return Views.Review.of(reviews.save(r));
    }

    public List<Views.Review> reviews(int courseId) {
        access.get(Course.class, courseId);
        return reviews.findByCourseIdOrderById(courseId).stream().map(Views.Review::of).toList();
    }
}
