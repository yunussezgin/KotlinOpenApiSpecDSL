package me.farshad.dsl.test

import me.farshad.dsl.builder.components.ComponentsBuilder
import me.farshad.dsl.builder.core.openApi
import me.farshad.dsl.builder.core.toYaml
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

// Test data classes
data class ValidationDetail(
    val field: String,
    val message: String,
    val code: String
)

data class ErrorResponse(
    val message: String,
    val details: List<ValidationDetail>
)

data class TestAllArrayTypes(
    val stringList: List<String>,
    val intList: List<Int>,
    val doubleList: List<Double>,
    val booleanList: List<Boolean>,
    val objectList: List<ValidationDetail>
)

class ArrayItemsGenerationTest {

    @Test
    fun `test array items generation for custom objects`() {
        val openApiSpec = openApi {
            info {
                title = "Test API"
                version = "1.0.0"
                description = "Testing array items generation"
            }

            components {
                schema(ValidationDetail::class)
                schema(ErrorResponse::class)
            }
        }

        val yamlOutput = openApiSpec.toYaml()

        // Verify that the ErrorResponse schema includes proper items reference
        assertTrue(yamlOutput.contains("\"details\":"))
        assertTrue(yamlOutput.contains("type: \"array\""))
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/ValidationDetail\""))
    }

    @Test
    fun `test array items generation for all primitive types`() {
        val openApiSpec = openApi {
            info {
                title = "Test API"
                version = "1.0.0"
            }

            components {
                schema(ValidationDetail::class)
                schema(TestAllArrayTypes::class)
            }
        }

        val yamlOutput = openApiSpec.toYaml()

        // Verify primitive array types
        assertTrue(yamlOutput.contains("\"stringList\":"))
        assertTrue(yamlOutput.contains("type: \"string\""))
        assertTrue(yamlOutput.contains("\"intList\":"))
        assertTrue(yamlOutput.contains("type: \"integer\""))
        assertTrue(yamlOutput.contains("\"doubleList\":"))
        assertTrue(yamlOutput.contains("type: \"number\""))
        assertTrue(yamlOutput.contains("\"booleanList\":"))
        assertTrue(yamlOutput.contains("type: \"boolean\""))

        // Verify object array type
        assertTrue(yamlOutput.contains("\"objectList\":"))
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/ValidationDetail\""))
    }
}