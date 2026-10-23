package com.prideregion.community;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Prevents bad configuration entries from taking down live rounds.
 * Validated before question bank is published to the quiz scheduler.
 */
public final class QuizQuestionValidator {
    private QuizQuestionValidator() {}

    public static int validate(List<QuizEngine.Question> questions) {
        if (questions == null || questions.isEmpty()) throw new IllegalArgumentException("Question bank is empty");
        Set<String> ids = new HashSet<>();
        int scrambles = 0, trivia = 0;
        for (QuizEngine.Question q : questions) {
            if (q == null || !q.id().matches("[a-zA-Z0-9_.-]{1,64}") || !ids.add(q.id())) {
                throw new IllegalArgumentException("Invalid or duplicate question ID");
            }
            if (q.answer() == null || q.answer().isBlank() || q.answer().length() > 80 ||
                q.prompt() == null || q.prompt().isBlank() || q.prompt().length() > 220 ||
                QuizEngine.normalize(q.answer()).isBlank())
                throw new IllegalArgumentException("Invalid question content: " + q.id());
            if (q.kind() == QuizEngine.Kind.SCRAMBLE) {
                String cleaned = q.answer().replaceAll("[^\\p{L}\\p{N}]", "").toUpperCase(Locale.ROOT);
                if (cleaned.length() < 3 || cleaned.length() > 30 ||
                    cleaned.codePoints().distinct().count() < 2)
                    throw new IllegalArgumentException("Unscrambleable answer: " + q.id());
                scrambles++;
            } else {
                trivia++;
            }
            if (q.aliases().size() > 12) throw new IllegalArgumentException("Too many aliases: " + q.id());
            for (String alias : q.aliases()) {
                if (alias == null || alias.length() > 80 || QuizEngine.normalize(alias).isBlank()) {
                    throw new IllegalArgumentException("Invalid alias in: " + q.id());
                }
            }
        }
        if (trivia == 0 || scrambles == 0)
            throw new IllegalArgumentException("Bank must contain both trivia and scramble entries");
        return questions.size();
    }
}
