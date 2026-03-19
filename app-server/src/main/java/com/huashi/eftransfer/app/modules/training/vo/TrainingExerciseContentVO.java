package com.huashi.eftransfer.app.modules.training.vo;

import java.util.List;

public record TrainingExerciseContentVO(
        String question,
        List<String> options,
        String explanation,
        String contextLevel,
        String sentence
) {
}
