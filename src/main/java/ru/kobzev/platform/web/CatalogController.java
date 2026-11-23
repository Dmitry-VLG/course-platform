package ru.kobzev.platform.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.service.CatalogService;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService service) { this.service = service; }

    @GetMapping("/courses")
    public List<Views.CourseCard> courses() {
        return service.courses();
    }

    @GetMapping("/courses/{id}")
    public Views.CourseDetail detail(@PathVariable int id) {
        return service.detail(id);
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.CourseCard createCourse(@Valid @RequestBody Inputs.CourseData data) {
        return service.saveCourse(null, data);
    }

    @PutMapping("/courses/{id}")
    public Views.CourseCard updateCourse(@PathVariable int id, @Valid @RequestBody Inputs.CourseData data) {
        return service.saveCourse(id, data);
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(@PathVariable int id) {
        service.deleteCourse(id);
    }

    @PostMapping("/courses/{id}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.ModuleView createModule(@PathVariable int id, @RequestParam("teacherId") int teacherId, @Valid @RequestBody Inputs.ModuleData data) {
        return service.saveModule(null, id, teacherId, data);
    }

    @PutMapping("/modules/{id}")
    public Views.ModuleView updateModule(@PathVariable int id, @RequestParam("teacherId") int teacherId, @Valid @RequestBody Inputs.ModuleData data) {
        return service.saveModule(id, null, teacherId, data);
    }

    @DeleteMapping("/modules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModule(@PathVariable int id, @RequestParam("teacherId") int teacherId) {
        service.deleteModule(id, teacherId);
    }

    @PostMapping("/modules/{id}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.LessonView createLesson(@PathVariable int id, @RequestParam("teacherId") int teacherId, @Valid @RequestBody Inputs.LessonData data) {
        return service.saveLesson(null, id, teacherId, data);
    }

    @PutMapping("/lessons/{id}")
    public Views.LessonView updateLesson(@PathVariable int id, @RequestParam("teacherId") int teacherId, @Valid @RequestBody Inputs.LessonData data) {
        return service.saveLesson(id, null, teacherId, data);
    }

    @DeleteMapping("/lessons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLesson(@PathVariable int id, @RequestParam("teacherId") int teacherId) {
        service.deleteLesson(id, teacherId);
    }

    @PostMapping("/lessons/{id}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.TaskView createTask(@PathVariable int id, @RequestParam("teacherId") int teacherId, @Valid @RequestBody Inputs.Task data) {
        return service.saveTask(null, id, teacherId, data);
    }

    @PutMapping("/assignments/{id}")
    public Views.TaskView updateTask(@PathVariable int id, @RequestParam("teacherId") int teacherId, @Valid @RequestBody Inputs.Task data) {
        return service.saveTask(id, null, teacherId, data);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable int id, @RequestParam("teacherId") int teacherId) {
        service.deleteTask(id, teacherId);
    }
}
