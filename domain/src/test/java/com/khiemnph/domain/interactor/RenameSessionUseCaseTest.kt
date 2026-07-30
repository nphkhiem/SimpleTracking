package com.khiemnph.domain.interactor

import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.SessionSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RenameSessionUseCaseTest {

    private val repository = MockedSessionRepository().apply {
        seedSummaries(listOf(SessionSummary("s1", 1_000.0, 60_000L, 3f, null, 1_700_000_000_000L)))
    }
    private val rename = RenameSessionUseCase(repository)

    private suspend fun titleOf(id: String) = repository.observeSessionSummary(id).first()?.title

    @Test
    fun `stores the name it is given`() = runTest {
        rename("s1", "Morning loop")

        assertEquals("Morning loop", titleOf("s1"))
    }

    @Test
    fun `trims surrounding whitespace`() = runTest {
        rename("s1", "  Morning loop  ")

        assertEquals("Morning loop", titleOf("s1"))
    }

    @Test
    fun `a blank name clears the title rather than storing an empty one`() = runTest {
        // A run titled "" would render as an empty heading with no way to tell it from a bug.
        rename("s1", "Morning loop")

        rename("s1", "   ")

        assertNull(titleOf("s1"))
    }

    @Test
    fun `an empty name clears the title`() = runTest {
        rename("s1", "Morning loop")

        rename("s1", "")

        assertNull(titleOf("s1"))
    }
}
