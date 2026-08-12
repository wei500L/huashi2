package com.huashi.eftransfer.app.modules.practice.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PracticeSpellingAnalyzerTest {

    @Test
    void exactMatchIsNotAnalyzedAsError() {
        assertThat(PracticeSpellingAnalyzer.analyze("succéder", List.of("succéder"))).isEqualTo("EXACT");
    }

    @Test
    void missingAccentIsAccentOrthography() {
        assertThat(PracticeSpellingAnalyzer.analyze("succeder", List.of("succéder"))).isEqualTo("ACCENT_ORTHOGRAPHY");
    }

    @Test
    void oneLetterSubstitutionIsReplacedLetter() {
        assertThat(PracticeSpellingAnalyzer.analyze("succader", List.of("succéder"))).isEqualTo("REPLACED_LETTER");
    }

    @Test
    void shorterCandidateIsMissingLetter() {
        assertThat(PracticeSpellingAnalyzer.analyze("sucéder", List.of("succéder"))).isEqualTo("MISSING_LETTER");
    }

    @Test
    void longerCandidateIsExtraLetter() {
        assertThat(PracticeSpellingAnalyzer.analyze("succéders", List.of("succéder"))).isEqualTo("EXTRA_LETTER");
    }

    @Test
    void smallDistanceIsClose() {
        assertThat(PracticeSpellingAnalyzer.analyze("succederrr", List.of("succéder"))).isEqualTo("CLOSE");
    }

    @Test
    void farGuessIsDistant() {
        assertThat(PracticeSpellingAnalyzer.analyze("bonjour", List.of("succéder"))).isEqualTo("DISTANT");
    }
}
