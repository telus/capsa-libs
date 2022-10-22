package digital.capsa.it.json

import assertk.assertThat
import assertk.assertions.matches
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import digital.capsa.it.validation.OpType
import digital.capsa.it.validation.ValidationRule
import net.minidev.json.JSONArray
import org.opentest4j.AssertionFailedError
import kotlin.test.assertEquals

object JsonPathValidator {

    fun assertJson(json: String, rules: List<ValidationRule>) {
        val document = Configuration.defaultConfiguration().jsonProvider().parse(json)

        for (rule in rules) {
            val values: List<Any?> = if (rule.value is List<*>) {
                rule.value
            } else {
                listOf(rule.value)
            }
            var jsonPath: Any?
            try {
                jsonPath = JsonPath.read(document, rule.path)
            } catch (e: Exception) {
                throw AssertionFailedError("Path not found, document: $document, path: ${rule.path}", e)
            }
            if (jsonPath == null) {
                assertEquals(
                    values[0], null, "json path ${rule.path} validation failed, document: $document"
                )
            } else
                when (jsonPath) {
                    is JSONArray -> {
                        when (rule.op) {
                            OpType.Equal -> {
                                if (jsonPath.size == 0) {
                                    assertEquals(
                                        values.size,
                                        0,
                                        "Json path ${rule.path} validation failed. Document: $document"
                                    )
                                } else {
                                    assertEquals(
                                        values.toSet(),
                                        jsonPath.toSet(),
                                        "Json path ${rule.path} validation failed. Document: $document"
                                    )
                                }
                            }
                            OpType.Size -> {
                                assertEquals(
                                    values[0],
                                    jsonPath.size,
                                    "Json path ${rule.path} validation failed. Document: $document"
                                )
                            }
                            else -> throw AssertionFailedError(
                                throw AssertionFailedError("'${rule.op}' op is not supported for JSONArray result. Use 'equal' op")
                            )
                        }
                    }
                    is Number -> {
                        if (rule.op != OpType.Equal) {
                            throw AssertionFailedError("${rule.op} op is not supported for Number result. Use 'equal' op")
                        }
                        assertEquals(
                            values[0], jsonPath, "Json path ${rule.path} validation failed. Document: $document"
                        )
                    }
                    is Boolean -> {
                        if (rule.op != OpType.Equal) {
                            throw AssertionFailedError("${rule.op} op is not supported for Boolean result. Use 'equal' op")
                        }
                        assertEquals(
                            values[0], jsonPath, "Json path ${rule.path} validation failed. Document: $document"
                        )
                    }
                    is String ->
                        when (rule.op) {
                            OpType.Regex ->
                                assertThat(
                                    jsonPath,
                                    "json path ${rule.path} validation failed. Document: $document"
                                ).matches(Regex(values[0].toString(), RegexOption.DOT_MATCHES_ALL))
                            OpType.Equal ->
                                assertEquals(
                                    values[0],
                                    jsonPath,
                                    "Json path ${rule.path} validation failed. Document: $document"
                                )
                            OpType.Like ->
                                assertThat(
                                    jsonPath,
                                    "Json path ${rule.path} validation failed. Document: $document"
                                ).matches(Regex(".*${values[0]}.*", RegexOption.DOT_MATCHES_ALL))
                            OpType.Size ->
                                throw AssertionFailedError(
                                    "'Size' op is not supported for String result."
                                )
                        }
                    else -> throw AssertionFailedError("Expected JSONArray or Number or String, received ${jsonPath.let { it::class }}")
                }
        }
    }
}

fun <T> assertk.Assert<T>.isJsonWhere(vararg validations: ValidationRule) = given { actual ->
    JsonPathValidator.assertJson(actual.toString(), validations.asList())
}
