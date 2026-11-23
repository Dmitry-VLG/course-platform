package ru.kobzev.platform.service;

import ru.kobzev.platform.domain.QuestionType;
import java.util.*;

public final class QuizScoring {
    private QuizScoring() {}

    public record Key(int id, QuestionType type, Set<Integer> available, Set<Integer> correct) {}

    public static int score(List<Key> keys, Map<Integer, Set<Integer>> answers) {
        if (answers.size() != keys.size()) throw PlatformException.invalid("Нужен ответ на каждый вопрос");
        int score = 0;
        for (Key key : keys) {
            Set<Integer> selected = answers.get(key.id());
            if (selected == null || selected.isEmpty() || !key.available().containsAll(selected)) {
                throw PlatformException.invalid("Недопустимый ответ на вопрос #" + key.id());
            }
            if (key.type() == QuestionType.SINGLE_CHOICE && selected.size() != 1) {
                throw PlatformException.invalid("Выберите ровно один вариант в вопросе #" + key.id());
            }
            if (selected.equals(key.correct())) score++;
        }
        return score;
    }
}
