package com.universalremote.domain

import com.universalremote.core.model.QuickAction
import com.universalremote.core.model.QuickActionStep
import com.universalremote.core.model.RemoteCommand
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class QuickActionRunnerTest {

    @Test
    fun runnerExecutesStepsSequentially() = runTest {
        val runner = QuickActionRunner()
        val executedSteps = mutableListOf<RemoteCommand>()

        val steps = listOf(
            QuickActionStep("s1", "act1", 0, RemoteCommand.POWER, 10L),
            QuickActionStep("s2", "act1", 1, RemoteCommand.HDMI, 10L)
        )
        val action = QuickAction("act1", "Encender y HDMI", "d1", steps)

        runner.execute(action) { step ->
            executedSteps.add(step.command)
            true
        }

        assertEquals(2, executedSteps.size)
        assertEquals(RemoteCommand.POWER, executedSteps[0])
        assertEquals(RemoteCommand.HDMI, executedSteps[1])
        assertTrue(runner.state.value is QuickActionExecutionState.Completed)
    }

    @Test
    fun runnerStopsOnStepFailure() = runTest {
        val runner = QuickActionRunner()
        val executedSteps = mutableListOf<RemoteCommand>()

        val steps = listOf(
            QuickActionStep("s1", "act1", 0, RemoteCommand.POWER, 10L),
            QuickActionStep("s2", "act1", 1, RemoteCommand.HDMI, 10L)
        )
        val action = QuickAction("act1", "Fail Test", "d1", steps)

        runner.execute(action) { step ->
            executedSteps.add(step.command)
            false // Return failure
        }

        assertEquals(1, executedSteps.size)
        assertTrue(runner.state.value is QuickActionExecutionState.Failed)
    }
}
