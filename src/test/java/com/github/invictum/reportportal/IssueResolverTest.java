package com.github.invictum.reportportal;

import com.epam.ta.reportportal.ws.model.issue.Issue;
import net.thucydides.model.domain.TestOutcome;
import net.thucydides.model.domain.TestResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Optional;

@RunWith(JUnit4.class)
public class IssueResolverTest {

    @After
    public void resetConfig() {
        ReportIntegrationConfig.get()
                .compromisedDefectLocator(null)
                .compromisedJiraDefectLocator(null)
                .compromisedJiraComment("Blocked by Jira");
    }

    private TestOutcome compromisedOutcome(String failureMessage) {
        TestOutcome out = Mockito.mock(TestOutcome.class);
        Mockito.when(out.getResult()).thenReturn(TestResult.COMPROMISED);
        Mockito.when(out.getTestFailureMessage()).thenReturn(failureMessage);
        return out;
    }

    @Test
    public void notCompromisedReturnsEmpty() {
        TestOutcome out = Mockito.mock(TestOutcome.class);
        Mockito.when(out.getResult()).thenReturn(TestResult.FAILURE);
        ReportIntegrationConfig.get().compromisedDefectLocator("ti001");
        Assert.assertFalse(IssueResolver.forCompromised(out).isPresent());
    }

    @Test
    public void compromisedWithoutConfiguredLocatorReturnsEmpty() {
        TestOutcome out = compromisedOutcome("Blocked by JIRA-1");
        Assert.assertFalse(IssueResolver.forCompromised(out).isPresent());
    }

    @Test
    public void compromisedWithoutJiraKeysUsesDefaultLocator() {
        ReportIntegrationConfig.get().compromisedDefectLocator("ti001");
        TestOutcome out = compromisedOutcome("Environment was unavailable");
        Optional<Issue> issue = IssueResolver.forCompromised(out);
        Assert.assertTrue(issue.isPresent());
        Assert.assertEquals("ti001", issue.get().getIssueType());
        Assert.assertEquals("Environment was unavailable", issue.get().getComment());
    }

    @Test
    public void compromisedWithJiraKeysUsesJiraLocatorAndComment() {
        ReportIntegrationConfig.get()
                .compromisedDefectLocator("ti001")
                .compromisedJiraDefectLocator("ab_vbem6dawej3n");
        TestOutcome out = compromisedOutcome("Skipped because of WF-100 and WF-200");
        Optional<Issue> issue = IssueResolver.forCompromised(out);
        Assert.assertTrue(issue.isPresent());
        Assert.assertEquals("ab_vbem6dawej3n", issue.get().getIssueType());
        Assert.assertEquals("Blocked by Jira: WF-100, WF-200", issue.get().getComment());
    }

    @Test
    public void compromisedWithJiraKeysFallsBackToDefaultLocatorWhenJiraLocatorUnset() {
        ReportIntegrationConfig.get().compromisedDefectLocator("ti001");
        TestOutcome out = compromisedOutcome("Skipped because of WF-100");
        Optional<Issue> issue = IssueResolver.forCompromised(out);
        Assert.assertTrue(issue.isPresent());
        Assert.assertEquals("ti001", issue.get().getIssueType());
        Assert.assertEquals("Blocked by Jira: WF-100", issue.get().getComment());
    }

    @Test
    @Test
    public void extractJiraKeysDedupesPreservingOrder() {
        Assert.assertEquals(
                java.util.Arrays.asList("WF-200", "WF-100"),
                new ArrayList<>(IssueResolver.extractJiraKeys("WF-200, WF-100, WF-200")));
    }

    @Test
    public void extractJiraKeysHandlesNullMessage() {
        Assert.assertTrue(IssueResolver.extractJiraKeys(null).isEmpty());
    }

    @Test
    public void extractJiraKeysIgnoresNonKeyText() {
        Assert.assertTrue(IssueResolver.extractJiraKeys("lowercase-1 and Word-without-number").isEmpty());
    }
}