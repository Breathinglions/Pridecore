package com.prideregion.community;

import java.util.List;

public final class QuizQuestionValidatorTest {
    private static int checks;
    private static void verify(boolean result, String label) {
        checks++;
        if (!result) throw new AssertionError(label);
    }
    private static void expectFailure(List<QuizEngine.Question> items) {
        try { QuizQuestionValidator.validate(items); throw new AssertionError("accepted invalid bank"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static QuizEngine.Question q(String id, QuizEngine.Kind kind, String answer) {
        return new QuizEngine.Question(id,kind,kind==QuizEngine.Kind.TRIVIA?"Who?":"Unscramble",
            answer,List.of(),QuizEngine.Difficulty.EASY,0);
    }
    public static void main(String[] args) {
        var t=q("t1",QuizEngine.Kind.TRIVIA,"Pikachu");
        var s=q("s1",QuizEngine.Kind.SCRAMBLE,"Gengar");
        verify(QuizQuestionValidator.validate(List.of(t,s))==2,"valid two categories");
        expectFailure(List.of(t,t,s));
        expectFailure(List.of(t));
        expectFailure(List.of(s));
        expectFailure(List.of(t,q("s1",QuizEngine.Kind.SCRAMBLE,"AAA")));
        expectFailure(List.of(t,q("s1",QuizEngine.Kind.SCRAMBLE,"A")));
        expectFailure(List.of(t,new QuizEngine.Question("s1",QuizEngine.Kind.SCRAMBLE,"  ","Gengar",
             List.of(),QuizEngine.Difficulty.EASY,0)));
        expectFailure(List.of(t,q("bad id",QuizEngine.Kind.SCRAMBLE,"Gengar")));
        verify(QuizQuestionValidator.validate(List.of(t,
             new QuizEngine.Question("s1",QuizEngine.Kind.SCRAMBLE,"U","Mr. Mime",
                 List.of(),QuizEngine.Difficulty.MEDIUM,0)))==2,"valid punctuation in scramble");
        System.out.println("PASS "+checks+" question-bank validation checks");
    }
}
