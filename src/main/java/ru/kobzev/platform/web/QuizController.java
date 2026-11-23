package ru.kobzev.platform.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.service.QuizService;
import java.util.List;

@RestController
@RequestMapping("/api")
public class QuizController {
    private final QuizService service;

    public QuizController(QuizService service) { this.service = service; }

    @PostMapping("/modules/{id}/quiz")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.QuizView create(@PathVariable int id, @RequestParam("teacherId") int teacherId, @Valid @RequestBody Inputs.QuizData data) {
        return service.create(id, teacherId, data);
    }

    @GetMapping("/quizzes/{id}")
    public Views.QuizView quiz(@PathVariable int id) {
        return service.get(id);
    }

    @PostMapping("/quizzes/{id}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.Result take(@PathVariable int id, @Valid @RequestBody Inputs.Attempt data) {
        return service.take(id, data);
    }

    @GetMapping("/quizzes/{id}/results")
    public List<Views.Result> results(@PathVariable int id) {
        return service.resultsForQuiz(id);
    }

    @GetMapping("/users/{id}/quiz-results")
    public List<Views.Result> studentResults(@PathVariable int id) {
        return service.resultsForStudent(id);
    }
}
