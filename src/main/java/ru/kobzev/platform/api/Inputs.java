package ru.kobzev.platform.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.kobzev.platform.domain.QuestionType;
import ru.kobzev.platform.domain.Role;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Inputs {
    private Inputs() {}

    public record Person(@NotBlank @Size(max = 255) String name, @NotBlank @Email @Size(max = 255) String email,
                         @NotNull Role role) {}
    public record ProfileData(String bio, @Size(max = 2048) String avatarUrl) {}
    public record Name(@NotBlank @Size(max = 255) String name) {}
    public record CourseData(@NotBlank @Size(max = 255) String title, String description,
                             @NotNull @Positive Integer categoryId, @NotNull @Positive Integer teacherId,
                             @Size(max = 255) String duration, LocalDate startDate,
                             Set<@NotNull @Positive Integer> tagIds) {}
    public record ModuleData(@NotBlank @Size(max = 255) String title, @NotNull @Positive Integer orderIndex,
                             String description) {}
    public record LessonData(@NotBlank @Size(max = 255) String title, @NotBlank String content,
                             @Size(max = 2048) String videoUrl) {}
    public record Task(@NotBlank @Size(max = 255) String title, @NotBlank String description,
                       @NotNull LocalDate dueDate, @NotNull @Positive Integer maxScore) {}
    public record Work(@NotNull @Positive Integer studentId, @NotBlank String content) {}
    public record Grade(@NotNull @Positive Integer teacherId, @NotNull @PositiveOrZero Integer score,
                        String feedback) {}
    public record OptionData(@NotBlank String text, @NotNull Boolean correct) {}
    public record QuestionData(@NotBlank String text, @NotNull QuestionType type,
                               @NotNull @Size(min = 2) List<@NotNull @Valid OptionData> options) {}
    public record QuizData(@NotBlank @Size(max = 255) String title, @Positive Integer timeLimit,
                           @NotEmpty List<@NotNull @Valid QuestionData> questions) {}
    public record Attempt(@NotNull @Positive Integer studentId,
                          @NotEmpty Map<@NotNull @Positive Integer, @NotEmpty Set<@NotNull @Positive Integer>> answers) {}
    public record ReviewData(@NotNull @Positive Integer studentId, @NotNull @Min(1) @Max(5) Integer rating,
                             String comment) {}
}
