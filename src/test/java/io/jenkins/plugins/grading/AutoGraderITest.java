package io.jenkins.plugins.grading;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import hudson.model.Result;
import hudson.model.Run;

import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static io.jenkins.plugins.grading.assertions.Assertions.*;

/**
 * Integration tests for the {@link AutoGrader} step.
 *
 * @author Ullrich Hafner
 */
public class AutoGraderITest extends IntegrationTestWithJenkinsPerSuite {
    @Test
    public void shouldSkipGradingSinceConfigurationIsEmpty() {
        WorkflowJob job = createPipelineWithWorkspaceFiles("checkstyle.xml");

        configureScanner(job, "checkstyle", "{}");
        Run<?, ?> baseline = buildSuccessfully(job);

        assertThat(getConsoleLog(baseline)).contains("[Autograding] Skipping static analysis results");
    }

    @Test
    public void shouldAbortBuildSinceNoTestActionHasBeenRegistered() {
        WorkflowJob job = createPipelineWithWorkspaceFiles("checkstyle.xml");

        configureScanner(job, "checkstyle", "{\"analysis\":{\"maxScore\":100,\"errorImpact\":-10,\"highImpact\":-5,\"normalImpact\":-2,\"lowImpact\":-1}, \"tests\":{\"maxScore\":100,\"passedImpact\":1,\"failureImpact\":-5,\"skippedImpact\":-1}, \"coverage\":{\"maxScore\":100,\"coveredImpact\":1,\"missedImpact\":-1}, \"pit\":{\"maxScore\":100,\"detectedImpact\":1,\"undetectedImpact\":-1,\"ratioImpact\":0}}");
        Run<?, ?> baseline = buildWithResult(job, Result.FAILURE);

        assertThat(getConsoleLog(baseline)).contains("java.lang.IllegalArgumentException: Test scoring has been enabled, but no test results have been found.");
    }

    @Test
    public void shouldCountCheckStyleWarnings() {
        WorkflowJob job = createPipelineWithWorkspaceFiles("checkstyle.xml");

        configureScanner(job, "checkstyle", "{\"analysis\":{\"maxScore\":100,\"errorImpact\":-10,\"highImpact\":-5,\"normalImpact\":-2,\"lowImpact\":-1}}");
        Run<?, ?> baseline = buildSuccessfully(job);

        assertThat(getConsoleLog(baseline)).contains("[Autograding] Grading static analysis results for CheckStyle");
        assertThat(getConsoleLog(baseline)).contains("[Autograding] -> Score -60 (warnings distribution err:6, high:0, normal:0, low:0)");
        assertThat(getConsoleLog(baseline)).contains("[Autograding] Total score for static analysis results: 40");

        List<AutoGradingBuildAction> actions = baseline.getActions(AutoGradingBuildAction.class);
        assertThat(actions).hasSize(1);
        Score score = actions.get(0).getResult();

        assertThat(score).hasAchieved(40);
    }

    /**
     * Returns the console log as a String.
     *
     * @param build
     *         the build to get the log for
     *
     * @return the console log
     */
    protected String getConsoleLog(final Run<?, ?> build) {
        try {
            return JenkinsRule.getLog(build);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private void configureScanner(final WorkflowJob job, final String fileName, final String configuration) {
        job.setDefinition(new CpsFlowDefinition("node {\n"
                + "  stage ('Integration Test') {\n"
                + "         recordIssues tool: checkStyle(pattern: '**/" + fileName + "*')\n"
                + "         autoGrade('" + configuration + "')\n"
                + "  }\n"
                + "}", true));
    }

}
