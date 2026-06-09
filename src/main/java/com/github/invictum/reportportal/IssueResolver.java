package com.github.invictum.reportportal;

import com.epam.ta.reportportal.ws.model.issue.Issue;
import net.thucydides.model.domain.TestOutcome;
import net.thucydides.model.domain.TestResult;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a Report Portal {@link Issue} (defect type + comment) for compromised tests so it can be
 * attached to the finish-item request directly, instead of correcting the item afterwards over REST.
 * <p>
 * The listener never receives the live exception — Serenity only hands over a serialized
 * {@link TestOutcome}. Jira keys are therefore extracted from {@link TestOutcome#getTestFailureMessage()}
 * rather than from a {@code TestCompromisedException} instance.
 * <p>
 * Defect type locators are project specific in Report Portal and are configured through
 * {@link ReportIntegrationConfig}. When no locator is configured no issue is produced, which keeps
 * the default behaviour for projects that do not use this feature.
 */
public final class IssueResolver {

    /**
     * Matches Jira issue keys such as {@code ABC-123}: an uppercase project key followed by a number.
     */
    private static final Pattern JIRA_KEY = Pattern.compile("[A-Z][A-Z0-9]+-\\d+");

    private IssueResolver() {
    }

    /**
     * Builds an {@link Issue} for a compromised {@link TestOutcome}, or empty when the outcome is not
     * compromised or no defect type locator is configured.
     */
    public static Optional<Issue> forCompromised(TestOutcome out) {
        if (!TestResult.COMPROMISED.equals(out.getResult())) {
            return Optional.empty();
        }
        ReportIntegrationConfig config = ReportIntegrationConfig.get();
        Set<String> jiraKeys = extractJiraKeys(out.getTestFailureMessage());
        String locator;
        String comment;
        if (!jiraKeys.isEmpty()) {
            locator = config.compromisedJiraDefectLocator() != null
                    ? config.compromisedJiraDefectLocator()
                    : config.compromisedDefectLocator();
            comment = config.compromisedJiraComment() + ": " + String.join(", ", jiraKeys);
        } else {
            locator = config.compromisedDefectLocator();
            comment = out.getTestFailureMessage();
        }
        if (locator == null) {
            return Optional.empty();
        }
        Issue issue = new Issue();
        issue.setIssueType(locator);
        if (comment != null && !comment.isEmpty()) {
            issue.setComment(comment);
        }
        return Optional.of(issue);
    }

    /**
     * Extracts the distinct Jira keys referenced in a failure message, preserving their order.
     */
    static Set<String> extractJiraKeys(String message) {
        Set<String> keys = new LinkedHashSet<>();
        if (message != null) {
            Matcher matcher = JIRA_KEY.matcher(message);
            while (matcher.find()) {
                keys.add(matcher.group());
            }
        }
        return keys;
    }
}