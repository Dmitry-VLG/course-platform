package ru.kobzev.platform.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.service.QuizService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class QuizController {
    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    @PostMapping("/modules/{id}/quiz")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.QuizView create(@PathVariable int id, @RequestParam("teacherId") int teacherId,
                                 @Valid @RequestBody Inputs.QuizData data) {
        return service.create(id, teacherId, data);
    }

    @GetMapping("/quizzes/{id}")
    public Views.QuizView quiz(@PathVariable int id) {
        return service.get(id);
    }

    @PutMapping("/quizzes/{id}")
    public Views.QuizView update(@PathVariable int id, @RequestParam("teacherId") int teacherId,
                                 @Valid @RequestBody Inputs.QuizData data) {
        return service.update(id, teacherId, data);
    }

    @DeleteMapping("/quizzes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id, @RequestParam("teacherId") int teacherId) {
        service.delete(id, teacherId);
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
