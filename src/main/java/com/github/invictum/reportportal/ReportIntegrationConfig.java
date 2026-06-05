package com.github.invictum.reportportal;

import java.util.Objects;
import java.util.function.Function;

import net.serenitybdd.annotations.Narrative;

/**
 * Configuration entry point for integration.
 * Allows to redefine configuration for integration module. Configuration should be altered only once, before tests invocation.
 */
public class ReportIntegrationConfig {

    public static final String COMMUNICATION_DIR_KEY = "serenity.rp.communication.dir";
    public static final String MODULES_COUNT_KEY = "serenity.rp.modules.count";
    public static final String FAILSAFE_RERUN_KEY = "failsafe.rerunFailingTestsCount";
    public static final String SUREFIRE_RERUN_KEY = "surefire.rerunFailingTestsCount";

    /**
     * Report Portal defect type locator assigned to compromised tests. When unset no issue is
     * attached. The built-in "To Investigate" type has the locator {@code ti001}.
     */
    public static final String COMPROMISED_DEFECT_LOCATOR_KEY = "serenity.rp.compromised.locator";

    /**
     * Report Portal defect type locator assigned to compromised tests whose failure message
     * references Jira keys. Project specific (e.g. a custom "Blocked by Jira" subtype such as
     * {@code ab_vbem6dawej3n}); when unset the {@link #COMPROMISED_DEFECT_LOCATOR_KEY} value is used.
     */
    public static final String COMPROMISED_JIRA_DEFECT_LOCATOR_KEY = "serenity.rp.compromised.jira.locator";
    /**
     * Comment prefix used when a compromised test is tagged with the Jira defect type. The detected
     * Jira keys are appended to it.
     */
    public static final String COMPROMISED_JIRA_COMMENT_KEY = "serenity.rp.compromised.jira.comment";

    private static volatile ReportIntegrationConfig instance;

    private LogsPreset preset = LogsPreset.DEFAULT;
    private Function<Narrative, String> classNarrativeFormatter = n -> String.join("\n", n.text());
    boolean harvestSeleniumLogs = false;
    boolean truncateNames = false;
    private String compromisedDefectLocator = System.getProperty(COMPROMISED_DEFECT_LOCATOR_KEY);
    private String compromisedJiraDefectLocator = System.getProperty(COMPROMISED_JIRA_DEFECT_LOCATOR_KEY);
    private String compromisedJiraComment = System.getProperty(COMPROMISED_JIRA_COMMENT_KEY, "Blocked by Jira");

    /**
     * Access to shared configuration instance
     */
    public static ReportIntegrationConfig get() {
        if (instance == null) {
            synchronized (ReportIntegrationConfig.class) {
                if (instance == null) {
                    instance = new ReportIntegrationConfig();
                }
            }
        }
        return instance;
    }

    /**
     * Defines {@link LogsPreset} configuration
     */
    public ReportIntegrationConfig usePreset(LogsPreset preset) {
        this.preset = Objects.requireNonNull(preset, "Profile could not be null");
        return this;
    }

    public LogsPreset preset() {
        return preset;
    }

    /**
     * Overrides class level narrative formatter with custom implementation
     */
    public void useClassNarrativeFormatter(Function<Narrative, String> formatter) {
        classNarrativeFormatter = Objects.requireNonNull(formatter, "Formatter must not be null");
    }

    /**
     * Option allows to enable or disable selenium based logs harvesting
     * Designed to be used in conjunction with {@link com.github.invictum.reportportal.log.unit.Selenium} log unit
     * Disabled by default that means selenium logs won't be collected even if selenium log unit was added to preset
     */
    public ReportIntegrationConfig harvestSeleniumLogs(boolean harvestLogs) {
        harvestSeleniumLogs = harvestLogs;
        return this;
    }

    public Function<Narrative, String> formatter() {
        return classNarrativeFormatter;
    }

    /**
     * Option sets names truncation feature, that allows to avoid RP errors with long entities creation
     */
    public ReportIntegrationConfig truncateNames(boolean setting) {
        truncateNames = setting;
        return this;
    }

    public String communicationDirectory() {
        return System.getProperty(COMMUNICATION_DIR_KEY);
    }

    public int retriesCount() {
        String value = System.getProperty(FAILSAFE_RERUN_KEY, System.getProperty(SUREFIRE_RERUN_KEY));
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Wrong retires count", e);
        }
    }

    public int modulesQuantity() {
        String value = System.getProperty(MODULES_COUNT_KEY);
        return value == null ? 0 : Integer.parseInt(value);
    }

    /**
     * Defines the Report Portal defect type locator assigned to compromised tests.
     * Defect type locators are project specific; leaving it unset keeps compromised tests free of
     * any issue. See {@link #COMPROMISED_DEFECT_LOCATOR_KEY}.
     */
    public ReportIntegrationConfig compromisedDefectLocator(String locator) {
        compromisedDefectLocator = locator;
        return this;
    }

    public String compromisedDefectLocator() {
        return compromisedDefectLocator;
    }

    /**
     * Defines the Report Portal defect type locator used for compromised tests that reference Jira
     * keys in their failure message. See {@link #COMPROMISED_JIRA_DEFECT_LOCATOR_KEY}.
     */
    public ReportIntegrationConfig compromisedJiraDefectLocator(String locator) {
        compromisedJiraDefectLocator = locator;
        return this;
    }

    public String compromisedJiraDefectLocator() {
        return compromisedJiraDefectLocator;
    }

    /**
     * Overrides the comment prefix used when a compromised test is tagged with the Jira defect type.
     */
    public ReportIntegrationConfig compromisedJiraComment(String comment) {
        compromisedJiraComment = comment;
        return this;
    }

    public String compromisedJiraComment() {
        return compromisedJiraComment;
    }

}
