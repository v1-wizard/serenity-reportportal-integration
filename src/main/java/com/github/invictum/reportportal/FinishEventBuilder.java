package com.github.invictum.reportportal;

import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

/**
 * Support class that allows to construct {@link FinishTestItemRQ} event in fluent way
 */
public class FinishEventBuilder {

    private final FinishTestItemRQ finishEvent = new FinishTestItemRQ();

    public FinishEventBuilder withEndTime(ZonedDateTime start, long duration) {
        ZonedDateTime end = start.plus(duration, ChronoUnit.MILLIS);
        finishEvent.setEndTime(Date.from(end.toInstant()));
        return this;
    }

    public FinishEventBuilder withStatus(Status status) {
        finishEvent.setStatus(status.toString());
        return this;
    }

    /**
     * Attaches a Report Portal {@link Issue} (defect type + comment) to the finish event.
     * Pass {@code null} to clear any previously set issue.
     */
    public FinishEventBuilder withIssue(Issue issue) {
        finishEvent.setIssue(issue);
        return this;
    }

    public FinishTestItemRQ build() {
        Objects.requireNonNull(finishEvent.getStatus(), "Status must not be null");
        Objects.requireNonNull(finishEvent.getEndTime(), "End date must not be null");
        return finishEvent;
    }
}
