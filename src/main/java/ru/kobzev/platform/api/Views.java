package ru.kobzev.platform.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import ru.kobzev.platform.domain.*;

public final class Views {
    private Views() {}

    public record Person(int id, String name, String email, Role role) {
        public static Person of(User u) { return new Person(u.getId(), u.getName(), u.getEmail(), u.getRole()); }
    }
    public record Named(int id, String name) {}
    public record ProfileView(int id, int userId, String bio, String avatarUrl) {}
    public record CourseCard(int id, String title, String description, Named category, Named teacher,
                             String duration, LocalDate startDate, List<Named> tags) {}
    public record CourseDetail(CourseCard course, List<ModuleView> modules) {}
    public record ModuleView(int id, String title, int orderIndex, String description, List<LessonView> lessons,
                             Integer quizId) {}
    public record LessonView(int id, String title, String content, String videoUrl, List<TaskView> assignments) {}
    public record TaskView(int id, int lessonId, String title, String description, LocalDate dueDate, int maxScore) {
        public static TaskView of(Assignment a) {
            return new TaskView(a.getId(), a.getLesson().getId(), a.getTitle(), a.getDescription(),
                    a.getDueDate(), a.getMaxScore());
        }
    }
    public record Registration(int id, Named student, Named course, LocalDate enrollDate, EnrollmentStatus status) {
        public static Registration of(Enrollment e) {
            return new Registration(e.getId(), new Named(e.getUser().getId(), e.getUser().getName()),
                    new Named(e.getCourse().getId(), e.getCourse().getTitle()), e.getEnrollDate(), e.getStatus());
        }
    }
    public record WorkView(int id, int assignmentId, int studentId, Instant submittedAt, String content,
                           Integer score, String feedback, Submission.Status status) {
        public static WorkView of(Submission s) {
            return new WorkView(s.getId(), s.getAssignment().getId(), s.getStudent().getId(),
                    s.getSubmittedAt(), s.getContent(), s.getScore(), s.getFeedback(), s.getStatus());
        }
    }
    public record QuestionView(int id, String text, QuestionType type, List<Named> options) {}
    public record QuizView(int id, String title, Integer timeLimit, List<QuestionView> questions) {}
    public record Result(int id, int quizId, int studentId, int score, Instant takenAt) {
        public static Result of(QuizSubmission r) {
            return new Result(r.getId(), r.getQuiz().getId(), r.getStudent().getId(), r.getScore(), r.getTakenAt());
        }
    }
    public record Review(int id, int courseId, int studentId, int rating, String comment, Instant createdAt) {
        public static Review of(CourseReview r) {
            return new Review(r.getId(), r.getCourse().getId(), r.getStudent().getId(),
                    r.getRating(), r.getComment(), r.getCreatedAt());
        }
    }
}
