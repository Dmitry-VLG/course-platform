package ru.kobzev.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.domain.*;
import ru.kobzev.platform.repository.*;
import java.util.List;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class QuizService {
    private final Access access;
    private final QuizRepository quizzes;
    private final QuizSubmissionRepository results;

    public QuizService(Access access,
            QuizRepository quizzes,
            QuizSubmissionRepository results) {
        this.access = access;
        this.quizzes = quizzes;
        this.results = results;
    }

    @Transactional
    public Views.QuizView create(int moduleId, int teacherId, Inputs.QuizData data) {
        CourseModule module = access.get(CourseModule.class, moduleId);
        access.requireTeacher(module.getCourse(), teacherId);
        if (module.getQuiz() != null) throw PlatformException.conflict("У модуля уже есть тест");
        Quiz quiz = new Quiz();
        quiz.setModule(module);
        quiz.setTitle(data.title().strip());
        quiz.setTimeLimit(data.timeLimit());
        for (Inputs.QuestionData source : data.questions()) addQuestion(quiz, source);
        module.setQuiz(quiz);
        quizzes.save(quiz);
        return view(quiz);
    }

    private void addQuestion(Quiz quiz, Inputs.QuestionData source) {
        long correct = source.options().stream().filter(Inputs.OptionData::correct).count();
        if (correct == 0 || (source.type() == QuestionType.SINGLE_CHOICE && correct != 1)) {
            throw PlatformException.invalid("Проверьте число верных вариантов для вопроса «" + source.text() + "»");
        }
        Question q = new Question();
        q.setType(source.type());
        q.setText(source.text());
        q.setQuiz(quiz);
        quiz.getQuestions().add(q);
        for (Inputs.OptionData input : source.options()) {
            AnswerOption option = new AnswerOption();
            option.setText(input.text());
            option.setCorrect(input.correct());
            option.setQuestion(q);
            q.getOptions().add(option);
        }
    }

    private Views.QuizView view(Quiz quiz) {
        return new Views.QuizView(quiz.getId(), quiz.getTitle(), quiz.getTimeLimit(),
                quiz.getQuestions().stream().map(q -> new Views.QuestionView(q.getId(), q.getText(), q.getType(),
                        q.getOptions().stream().map(o -> new Views.Named(o.getId(), o.getText())).toList())).toList());
    }

    public Views.QuizView get(int id) { return view(access.get(Quiz.class, id)); }

    private QuizScoring.Key key(Question q) {
        Set<Integer> available = new HashSet<>();
        Set<Integer> correct = new HashSet<>();
        for (AnswerOption option : q.getOptions()) {
            available.add(option.getId());
            if (option.isCorrect()) correct.add(option.getId());
        }
        return new QuizScoring.Key(q.getId(), q.getType(), available, correct);
    }

    @Transactional
    public Views.Result take(int id, Inputs.Attempt attempt) {
        Quiz quiz = access.get(Quiz.class, id);
        User student = access.participant(attempt.studentId(), quiz.getModule().getCourse());
        int score = QuizScoring.score(quiz.getQuestions().stream().map(this::key).toList(), attempt.answers());
        QuizSubmission r = new QuizSubmission();
        r.setQuiz(quiz);
        r.setStudent(student);
        r.setScore(score);
        r.setTakenAt(Instant.now());
        return Views.Result.of(results.save(r));
    }

    public List<Views.Result> resultsForQuiz(int id) {
        access.get(Quiz.class, id);
        return results.findByQuizIdOrderById(id).stream().map(Views.Result::of).toList();
    }

    public List<Views.Result> resultsForStudent(int id) {
        access.withRole(id, Role.STUDENT);
        return results.findByStudentIdOrderById(id).stream().map(Views.Result::of).toList();
    }
}
