package ru.kobzev.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.kobzev.platform.config.DemoData;
import ru.kobzev.platform.domain.Role;
import ru.kobzev.platform.service.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "platform.demo.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoScenarioTest {
    @Autowired DemoData demo;
    @Autowired CatalogService catalog;
    @Autowired DirectoryService directory;
    @Autowired AssignmentService assignments;
    @Autowired QuizService quizzes;
    @Autowired MockMvc http;

    @Test void firstStartCreatesCompleteCatalogAndRepeatIsIdempotent() throws Exception {
        assertEquals(2, catalog.courses().size());
        assertEquals(3, directory.users().size());
        var course = catalog.courses().stream().filter(c -> c.title().equals("ORM и Hibernate")).findFirst().orElseThrow();
        var program = catalog.detail(course.id()).modules();
        assertEquals(2, program.size());
        assertEquals(1, program.get(0).lessons().get(0).assignments().size());
        var students = directory.users().stream().filter(u -> u.role() == Role.STUDENT).toList();
        assertEquals(2, students.size());
        int enrolled = students.stream().filter(u -> !assignments.submittedBy(u.id()).isEmpty()).findFirst().orElseThrow().id();
        assertEquals(10, assignments.submittedBy(enrolled).get(0).score());
        assertEquals(2, quizzes.resultsForStudent(enrolled).get(0).score());
        demo.run(new DefaultApplicationArguments());
        assertEquals(2, catalog.courses().size());
        assertEquals(3, directory.users().size());
        http.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(containsString("Каталог курсов")));
        http.perform(get("/platform.js")).andExpect(status().isOk());
        http.perform(get("/platform.css")).andExpect(status().isOk());
    }
}
