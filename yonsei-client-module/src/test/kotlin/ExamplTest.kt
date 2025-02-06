import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ExamplTest {
    @Test
    fun exampleTest() {
        assertContentEquals(listOf(1, 2, 3), listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3))
    }
}