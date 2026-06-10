package com.github.invictum.reportportal;

import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@RunWith(JUnit4.class)
public class FinishEventBuilderTest {

    @Test
    public void withStatusTest() {
        FinishTestItemRQ event = new FinishEventBuilder()
                .withStatus(Status.CANCELLED)
                .withEndTime(ZonedDateTime.now(), 5)
                .build();
        Assert.assertEquals("CANCELLED", event.getStatus());
    }

    @Test(expected = NullPointerException.class)
    public void withNullStatusTest() {
        new FinishEventBuilder().withEndTime(ZonedDateTime.now(), 5).build();
    }

    @Test
    public void withEndDateTest() {
        ZonedDateTime time = ZonedDateTime.now();
        FinishTestItemRQ event = new FinishEventBuilder()
                .withStatus(Status.PASSED)
                .withEndTime(time, 5)
                .build();
        Date expected = Date.from(time.plus(5, ChronoUnit.MILLIS).toInstant());
        Assert.assertEquals(expected, event.getEndTime());
    }

    @Test(expected = NullPointerException.class)
    public void withNullEndDateTest() {
        new FinishEventBuilder().withStatus(Status.PASSED).build();
    }

    @Test
    public void withIssueTest() {
        Issue issue = new Issue();
        issue.setIssueType("ti001");
        FinishTestItemRQ event = new FinishEventBuilder()
                .withStatus(Status.SKIPPED)
                .withEndTime(ZonedDateTime.now(), 5)
                .withIssue(issue)
                .build();
        Assert.assertSame(issue, event.getIssue());
    }

    @Test
    public void withNullIssueTest() {
        FinishTestItemRQ event = new FinishEventBuilder()
                .withStatus(Status.SKIPPED)
                .withEndTime(ZonedDateTime.now(), 5)
                .withIssue(null)
                .build();
        Assert.assertNull(event.getIssue());
    }
}
