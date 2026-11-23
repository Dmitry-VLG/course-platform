package ru.kobzev.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.domain.*;
import ru.kobzev.platform.repository.*;
import java.util.List;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class AssignmentService {
    private final Access access;
    private final AssignmentRepository tasks;
    private final SubmissionRepository submissions;

    public AssignmentService(Access access,
            AssignmentRepository tasks,
            SubmissionRepository submissions) {
        this.access = access;
        this.tasks = tasks;
        this.submissions = submissions;
    }

    @Transactional
    public Views.WorkView submit(int taskId, Inputs.Work work) {
        Assignment task = access.get(Assignment.class, taskId);
        User student = access.participant(work.studentId(), task.getLesson().getModule().getCourse());
        if (submissions.existsByAssignmentIdAndStudentId(taskId, student.getId())) {
            throw PlatformException.conflict("Решение уже отправлено");
        }
        Submission s = new Submission();
        s.setAssignment(task);
        s.setStudent(student);
        s.setSubmittedAt(Instant.now());
        s.setContent(work.content());
        return Views.WorkView.of(submissions.save(s));
    }

    @Transactional
    public Views.WorkView grade(int id, Inputs.Grade grade) {
        Submission s = access.get(Submission.class, id);
        Assignment a = s.getAssignment();
        access.requireTeacher(a.getLesson().getModule().getCourse(), grade.teacherId());
        if (grade.score() < 0 || grade.score() > a.getMaxScore()) {
            throw PlatformException.invalid("Допустимая оценка от 0 до " + a.getMaxScore());
        }
        s.setScore(grade.score());
        s.setFeedback(grade.feedback());
        return Views.WorkView.of(s);
    }

    public List<Views.TaskView> tasksFor(int studentId) {
        access.withRole(studentId, Role.STUDENT);
        return tasks.findForStudent(studentId).stream().map(Views.TaskView::of).toList();
    }

    public List<Views.WorkView> submittedBy(int studentId) {
        access.withRole(studentId, Role.STUDENT);
        return submissions.findByStudentIdOrderById(studentId).stream().map(Views.WorkView::of).toList();
    }

    public List<Views.WorkView> solutions(int taskId) {
        access.get(Assignment.class, taskId);
        return submissions.findByAssignmentIdOrderById(taskId).stream().map(Views.WorkView::of).toList();
    }
}
