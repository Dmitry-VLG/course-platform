package ru.kobzev.platform.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.service.DirectoryService;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DirectoryController {
    private final DirectoryService service;

    public DirectoryController(DirectoryService service) { this.service = service; }

    @GetMapping("/users")
    public List<Views.Person> users() {
        return service.users();
    }

    @GetMapping("/users/{id}")
    public Views.Person user(@PathVariable int id) {
        return service.user(id);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.Person createUser(@Valid @RequestBody Inputs.Person data) {
        return service.saveUser(null, data);
    }

    @PutMapping("/users/{id}")
    public Views.Person updateUser(@PathVariable int id, @Valid @RequestBody Inputs.Person data) {
        return service.saveUser(id, data);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable int id) {
        service.deleteUser(id);
    }

    @GetMapping("/users/{id}/profile")
    public Views.ProfileView profile(@PathVariable int id) {
        return service.profile(id);
    }

    @PutMapping("/users/{id}/profile")
    public Views.ProfileView saveProfile(@PathVariable int id, @Valid @RequestBody Inputs.ProfileData data) {
        return service.saveProfile(id, data);
    }

    @GetMapping("/categories")
    public List<Views.Named> categories() {
        return service.categories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.Named createCategory(@Valid @RequestBody Inputs.Name data) {
        return service.saveCategory(null, data);
    }

    @PutMapping("/categories/{id}")
    public Views.Named updateCategory(@PathVariable int id, @Valid @RequestBody Inputs.Name data) {
        return service.saveCategory(id, data);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable int id) {
        service.deleteCategory(id);
    }

    @GetMapping("/tags")
    public List<Views.Named> tags() {
        return service.tags();
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.Named createTag(@Valid @RequestBody Inputs.Name data) {
        return service.saveTag(null, data);
    }

    @PutMapping("/tags/{id}")
    public Views.Named updateTag(@PathVariable int id, @Valid @RequestBody Inputs.Name data) {
        return service.saveTag(id, data);
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable int id) {
        service.deleteTag(id);
    }
}
