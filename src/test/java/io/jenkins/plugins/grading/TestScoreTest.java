package io.jenkins.plugins.grading;

import org.junit.jupiter.api.Test;

import hudson.tasks.junit.TestResultAction;

import static io.jenkins.plugins.grading.assertions.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link TestScore}.
 *
 * @author Eva-Maria Zeintl
 * @author Ullrich Hafner
 */
class TestScoreTest {
    @Test
    void shouldCalculate() {
        TestConfiguration testsConfiguration = new TestConfiguration.TestConfigurationBuilder().setMaxScore(25)
                .setSkippedImpact(-1)
                .setFailureImpact(-2)
                .setPassedImpact(1)
                .build();

        TestResultAction action = createAction(8, 1, 1);
        TestScore test = new TestScore(testsConfiguration, action);

        assertThat(test.getTotalImpact()).isEqualTo(3);
    }

    private TestResultAction createAction(final int totalSize, final int failedSize, final int skippedSize) {
        TestResultAction action = mock(TestResultAction.class);
        when(action.getTotalCount()).thenReturn(totalSize);
        when(action.getFailCount()).thenReturn(failedSize);
        when(action.getSkipCount()).thenReturn(skippedSize);
        return action;
    }

    @Test
    void shouldCalculateNegativeResult() {
        TestConfiguration testsConfiguration = new TestConfiguration.TestConfigurationBuilder().setMaxScore(25)
                .setSkippedImpact(-1)
                .setFailureImpact(-2)
                .setPassedImpact(1)
                .build();

        TestScore test = new TestScore(testsConfiguration, createAction(8, 5, 1));

        assertThat(test.getTotalImpact()).isEqualTo(-9);
    }
}
