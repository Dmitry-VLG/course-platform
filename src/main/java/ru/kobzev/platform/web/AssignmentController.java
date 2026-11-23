package ru.kobzev.platform.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.service.AssignmentService;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AssignmentController {
    private final AssignmentService service;

    public AssignmentController(AssignmentService service) { this.service = service; }

    @PostMapping("/assignments/{id}/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.WorkView submit(@PathVariable int id, @Valid @RequestBody Inputs.Work data) {
        return service.submit(id, data);
    }

    @PatchMapping("/submissions/{id}/grade")
    public Views.WorkView grade(@PathVariable int id, @Valid @RequestBody Inputs.Grade data) {
        return service.grade(id, data);
    }

    @GetMapping("/assignments/{id}/submissions")
    public List<Views.WorkView> solutions(@PathVariable int id) {
        return service.solutions(id);
    }

    @GetMapping("/users/{id}/assignments")
    public List<Views.TaskView> assignments(@PathVariable int id) {
        return service.tasksFor(id);
    }

    @GetMapping("/users/{id}/submissions")
    public List<Views.WorkView> submissions(@PathVariable int id) {
        return service.submittedBy(id);
    }
}
