package com.allearning.backend.learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitAnswersRequest(
        @NotEmpty(message = "Answers list cannot be empty")
        List<@Valid SubmitAnswerItem> answers
) {}

