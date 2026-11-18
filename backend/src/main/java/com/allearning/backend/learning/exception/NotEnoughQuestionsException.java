package com.allearning.backend.learning.exception;

public class NotEnoughQuestionsException extends RuntimeException {
    private final int requested;
    private final int found;
    private final Long chapterId;
    private final String reason;

    public NotEnoughQuestionsException(int requested, int found, Long chapterId, String reason) {
        super("Not enough questions: requested " + requested + ", found " + found + (chapterId != null ? " for chapter " + chapterId : "") + ". " + reason);
        this.requested = requested;
        this.found = found;
        this.chapterId = chapterId;
        this.reason = reason;
    }

    public int getRequested() { return requested; }
    public int getFound() { return found; }
    public Long getChapterId() { return chapterId; }
    public String getReason() { return reason; }
}

