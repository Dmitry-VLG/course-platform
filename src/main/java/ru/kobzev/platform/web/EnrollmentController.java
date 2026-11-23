package ru.kobzev.platform.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.service.EnrollmentService;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EnrollmentController {
    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) { this.service = service; }

    @PostMapping("/courses/{id}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.Registration enroll(@PathVariable int id, @RequestParam("user") int user) {
        return service.enroll(id, user);
    }

    @DeleteMapping("/courses/{id}/enroll")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable int id, @RequestParam("user") int user) {
        service.withdraw(id, user);
    }

    @GetMapping("/courses/{id}/students")
    public List<Views.Registration> roster(@PathVariable int id) {
        return service.roster(id);
    }

    @GetMapping("/users/{id}/courses")
    public List<Views.Registration> courses(@PathVariable int id) {
        return service.coursesFor(id);
    }

    @PostMapping("/courses/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.Review review(@PathVariable int id, @Valid @RequestBody Inputs.ReviewData data) {
        return service.review(id, data);
    }

    @GetMapping("/courses/{id}/reviews")
    public List<Views.Review> reviews(@PathVariable int id) {
        return service.reviews(id);
    }
}
