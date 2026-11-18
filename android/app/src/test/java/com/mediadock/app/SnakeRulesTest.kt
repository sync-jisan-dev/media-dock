package com.mediadock.app
import com.mediadock.app.feature.media.*
import org.junit.Test
import org.junit.Assert.*

class SnakeRulesTest {
    @Test fun rejectsReverseMoves() {
        val state = SnakeRules.step(SnakeState(), SnakeDirection.LEFT)
        assertEquals(SnakeCell(7,7), state.body.first())
        assertEquals(3, state.body.size)
    }
    @Test fun foodGrowsSnakeAndNeverSpawnsInsideIt() {
        val next = SnakeRules.step(SnakeState(food = SnakeCell(7,7)), choose = { it.first() })
        assertEquals(4, next.body.size); assertEquals(1, next.score)
        assertFalse(next.food in next.body)
    }
    @Test fun wallAndBodyCollisionsEndTheGame() {
        assertTrue(SnakeRules.step(SnakeState(body = listOf(SnakeCell(13,7)))).over)
        val body = listOf(SnakeCell(2,2),SnakeCell(2,3),SnakeCell(3,3),SnakeCell(3,2),SnakeCell(4,2))
        assertTrue(SnakeRules.step(SnakeState(body = body)).over)
    }
    @Test fun movingIntoTheVacatedTailIsAllowed() {
        val body = listOf(SnakeCell(2,2),SnakeCell(2,3),SnakeCell(3,3),SnakeCell(3,2))
        assertFalse(SnakeRules.step(SnakeState(body = body)).over)
    }
}
