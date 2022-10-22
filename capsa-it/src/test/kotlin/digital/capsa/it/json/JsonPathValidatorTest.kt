package digital.capsa.it.json

import digital.capsa.it.gherkin.given
import digital.capsa.it.validation.OpType
import digital.capsa.it.validation.ValidationRule
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Suppress("MaxLineLength")
@Tag("unit")
class JsonPathValidatorTest {

    @Test
    fun testValidator_happyPath() {
        given {
            """
                [{
                  "id": "12345",
                  "data": "abcd",
                  "num": 12345
                }, {
                  "id": "23456",
                  "data": "bcde",
                  "num": 23456
                }]
            """
        }.on {
            it.trimIndent()
        }.then {
            JsonPathValidator.assertJson(
                it, listOf(
                    ValidationRule("$.*.id", OpType.Equal, listOf("12345", "23456")),
                    ValidationRule("@[?(@.id == '12345')].data", OpType.Equal, "abcd"),
                    ValidationRule("@[?(@.id == '23456')].num", OpType.Equal, 23456)
                )
            )
        }
    }

    @Test
    fun testValidator_empty() {
        JsonPathValidator.assertJson(
            """
            []
        """.trimIndent(), listOf(
                ValidationRule("$.*.id", OpType.Equal, emptyList<String>())
            )
        )
    }

    @Test
    fun testValidator_empty_negative() {
        var exception: AssertionError? = null
        try {
            JsonPathValidator.assertJson(
                """
            [{
              "id": "12345"
            }, {
              "id": "23456"
            }]
        """.trimIndent(), listOf(
                    ValidationRule("$.*.id", OpType.Equal, "")
                )
            )
        } catch (e: AssertionError) {
            exception = e
        }
        assertTrue(exception is AssertionError)
    }

    @Test
    fun testValidator_regex_positive() {
        JsonPathValidator.assertJson(
            """
            {
              "id": "12345"
            }
        """.trimIndent(), listOf(
                ValidationRule("$.id", OpType.Regex, ".*")
            )
        )
    }

    @Test
    fun testValidator_like_positive() {
        JsonPathValidator.assertJson(
            """
            {
              "cause": "Cannot deserialize value of type `java.util.UUID` from String \"a2f674455-e5f4-4946-a19a-xdace6e1a598\": UUID has to be represented by standard 36-char representation\n at [Source: (String)\"{\"region\":\"qc\",\"listOfId\":[\"a2f674455-e5f4-4946-a19a-xdace6e1a598\"]}\"; line: 1, column: 28]"
            }
        """.trimIndent(), listOf(
                ValidationRule("$.cause", OpType.Like, "Cannot deserialize value of type"),
                ValidationRule("$.cause", OpType.Like, "UUID has to be represented by standard 36-char representation")
            )
        )
    }

    @Test
    @Suppress("FunctionNaming")
    fun testValidator_size() {
        JsonPathValidator.assertJson(
            """
            {
              "array1": ["123", "234", "345"],
              "array2": ["123", "123"],
              "array3": []
            }
        """.trimIndent(), listOf(
                ValidationRule("$.array1", OpType.Size, 3),
                ValidationRule("$.array2", OpType.Size, 2),
                ValidationRule("$.array3", OpType.Size, 0)
            )
        )
    }
}