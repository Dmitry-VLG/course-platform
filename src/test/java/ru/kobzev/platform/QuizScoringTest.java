package ru.kobzev.platform;

import org.junit.jupiter.api.Test;
import ru.kobzev.platform.service.*;
import ru.kobzev.platform.domain.QuestionType;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class QuizScoringTest {
    private final List<QuizScoring.Key> keys = List.of(
            new QuizScoring.Key(1, QuestionType.SINGLE_CHOICE, Set.of(10, 11), Set.of(10)),
            new QuizScoring.Key(2, QuestionType.MULTIPLE_CHOICE, Set.of(20, 21, 22), Set.of(20, 22)));

    @Test void fullScoreRequiresBothExactAnswers() {
        assertEquals(2, QuizScoring.score(keys, Map.of(1, Set.of(10), 2, Set.of(20, 22))));
    }

    @Test void partialMultipleChoiceEarnsNoPoint() {
        assertEquals(1, QuizScoring.score(keys, Map.of(1, Set.of(10), 2, Set.of(20))));
    }

    @Test void wrongAnswersCanEarnZero() {
        assertEquals(0, QuizScoring.score(keys, Map.of(1, Set.of(11), 2, Set.of(21))));
    }

    @Test void optionFromAnotherQuestionIsRejected() {
        assertThrows(PlatformException.class, () -> QuizScoring.score(keys, Map.of(1, Set.of(20), 2, Set.of(20))));
    }

    @Test void singleChoiceRejectsMultipleSelections() {
        assertThrows(PlatformException.class, () -> QuizScoring.score(keys, Map.of(1, Set.of(10, 11), 2, Set.of(20))));
    }

    @Test void missingQuestionIsRejected() {
        assertThrows(PlatformException.class, () -> QuizScoring.score(keys, Map.of(1, Set.of(10))));
    }

    @Test void unknownQuestionCannotReplaceARequiredQuestion() {
        assertThrows(PlatformException.class, () -> QuizScoring.score(keys, Map.of(1, Set.of(10), 3, Set.of(20))));
    }
}
