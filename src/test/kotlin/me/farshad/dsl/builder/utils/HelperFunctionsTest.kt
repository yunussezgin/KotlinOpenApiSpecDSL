package me.farshad.dsl.builder.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import me.farshad.dsl.builder.core.openApi
import me.farshad.dsl.builder.core.toJson
import me.farshad.dsl.builder.core.toYaml
import me.farshad.dsl.builder.utils.toJsonElement
import me.farshad.dsl.builder.utils.toSerializableJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HelperFunctionsTest {
    // toJsonElement extension function tests
    @Test
    fun testToJsonElementWithString() {
        val result = "Hello World".toJsonElement()
        assertTrue(result is JsonPrimitive)
        assertEquals("Hello World", result.content)
    }

    @Test
    fun testToJsonElementWithNumbers() {
        val intResult = 42.toJsonElement()
        assertTrue(intResult is JsonPrimitive)
        assertEquals(42, intResult.int)

        val doubleResult = 3.14.toJsonElement()
        assertTrue(doubleResult is JsonPrimitive)
        assertEquals(3.14, doubleResult.double)

        val longResult = 9999999999L.toJsonElement()
        assertTrue(longResult is JsonPrimitive)
        assertEquals(9999999999L, longResult.long)
    }

    @Test
    fun testToJsonElementWithBoolean() {
        val trueResult = true.toJsonElement()
        assertTrue(trueResult is JsonPrimitive)
        assertEquals(true, trueResult.boolean)

        val falseResult = false.toJsonElement()
        assertTrue(falseResult is JsonPrimitive)
        assertEquals(false, falseResult.boolean)
    }

    @Test
    fun testToJsonElementWithMap() {
        val map =
            mapOf(
                "name" to "John",
                "age" to 30,
                "active" to true,
            )

        val result = map.toJsonElement()
        assertTrue(result is JsonObject)
        assertEquals("John", result["name"]?.jsonPrimitive?.content)
        assertEquals(30, result["age"]?.jsonPrimitive?.int)
        assertEquals(true, result["active"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun testToJsonElementWithList() {
        val list = listOf("apple", "banana", "orange")

        val result = list.toJsonElement()
        assertTrue(result is JsonArray)
        assertEquals(3, result.size)
        assertEquals("apple", result[0].jsonPrimitive.content)
        assertEquals("banana", result[1].jsonPrimitive.content)
        assertEquals("orange", result[2].jsonPrimitive.content)
    }

    @Test
    fun testToJsonElementWithNestedStructures() {
        val nested =
            mapOf(
                "user" to
                    mapOf(
                        "id" to 1,
                        "profile" to
                            mapOf(
                                "name" to "John",
                                "tags" to listOf("admin", "user"),
                            ),
                    ),
                "settings" to
                    listOf(
                        mapOf("key" to "theme", "value" to "dark"),
                        mapOf("key" to "notifications", "value" to true),
                    ),
            )

        val result = nested.toJsonElement()
        assertTrue(result is JsonObject)

        val user = result["user"] as JsonObject
        assertEquals(1, user["id"]?.jsonPrimitive?.int)

        val profile = user["profile"] as JsonObject
        assertEquals("John", profile["name"]?.jsonPrimitive?.content)

        val tags = profile["tags"] as JsonArray
        assertEquals(2, tags.size)

        val settings = result["settings"] as JsonArray
        assertEquals(2, settings.size)
    }

    @Test
    fun testToJsonElementWithNull() {
        val nullValue: String? = null
        val result = nullValue?.toJsonElement() ?: JsonNull
        assertEquals(JsonNull, result)
    }

    @Test
    fun testToJsonElementWithMapContainingNulls() {
        val map =
            mapOf(
                "name" to "John",
                "email" to null,
                "age" to 30,
            )

        val result = map.toJsonElement()
        assertTrue(result is JsonObject)
        assertEquals("John", result["name"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, result["email"])
        assertEquals(30, result["age"]?.jsonPrimitive?.int)
    }

    @Test
    fun testToJsonElementWithSerializableObject() {
        val obj = TestSerializableObject("123", "Test", true)
        val result = obj.toJsonElement()

        assertTrue(result is JsonObject)
        assertEquals("123", result["id"]?.jsonPrimitive?.content)
        assertEquals("Test", result["name"]?.jsonPrimitive?.content)
        assertEquals(true, result["active"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun testToJsonElementWithNonSerializableObject() {
        val obj = NonSerializableObject("test")
        val result = obj.toJsonElement()

        assertTrue(result is JsonObject)
        // The result is an empty JsonObject since the property is private
        assertEquals(0, (result as JsonObject).size)
    }

    // toSerializableJsonElement tests
    @Test
    fun testToSerializableJsonElement() {
        val obj = TestSerializableObject("456", "Serializable Test", false)
        val result = obj.toSerializableJsonElement()

        assertTrue(result is JsonObject)
        assertEquals("456", result["id"]?.jsonPrimitive?.content)
        assertEquals("Serializable Test", result["name"]?.jsonPrimitive?.content)
        assertEquals(false, result["active"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun testToSerializableJsonElementWithNullableFields() {
        val obj = TestSerializableWithNulls("789", null, null)
        val result = obj.toSerializableJsonElement()

        assertTrue(result is JsonObject)
        assertEquals("789", result["id"]?.jsonPrimitive?.content)
        // Nulls should not be included due to explicitNulls = false
        assertTrue(!result.containsKey("name") || result["name"] == JsonNull)
        assertTrue(!result.containsKey("description") || result["description"] == JsonNull)
    }

    // OpenApiSpec extension function tests
    @Test
    fun testOpenApiSpecToJson() {
        val spec =
            openApi {
                info {
                    title = "Test API"
                    version = "1.0.0"
                    description = "JSON serialization test"
                }
                paths {
                    path("/test") {
                        get {
                            summary = "Test endpoint"
                            response("200", "Success")
                        }
                    }
                }
            }

        val json = spec.toJson()

        assertNotNull(json)
        assertTrue(json.contains("\"openapi\""))
        assertTrue(json.contains("\"3.1.0\""))
        assertTrue(json.contains("\"Test API\""))
        assertTrue(json.contains("\"Test endpoint\""))

        // Verify it's valid JSON
        val parsed = Json.parseToJsonElement(json)
        assertTrue(parsed is JsonObject)
    }

    @Test
    fun testOpenApiSpecToYaml() {
        val spec =
            openApi {
                info {
                    title = "YAML Test API"
                    version = "2.0.0"
                    description = "YAML serialization test"
                }
                server("https://api.example.com") {
                    description = "Production server"
                }
            }

        val yaml = spec.toYaml()

        assertNotNull(yaml)
        assertTrue(yaml.contains("openapi: \"3.1.0\""))
        assertTrue(yaml.contains("title: \"YAML Test API\""))
        assertTrue(yaml.contains("version: \"2.0.0\""))
        assertTrue(yaml.contains("url: \"https://api.example.com\""))
        assertTrue(yaml.contains("description: \"Production server\""))
    }

    @Test
    fun testToJsonElementWithEmptyCollections() {
        val emptyMap = emptyMap<String, Any>()
        val emptyMapResult = emptyMap.toJsonElement()
        assertTrue(emptyMapResult is JsonObject)
        assertEquals(0, emptyMapResult.size)

        val emptyList = emptyList<Any>()
        val emptyListResult = emptyList.toJsonElement()
        assertTrue(emptyListResult is JsonArray)
        assertEquals(0, emptyListResult.size)
    }

    @Test
    fun testToJsonElementWithMixedTypes() {
        val mixed =
            listOf(
                "string",
                123,
                true,
                mapOf("key" to "value"),
                listOf(1, 2, 3),
                null,
            )

        val result = mixed.toJsonElement()
        assertTrue(result is JsonArray)
        assertEquals(6, result.size)

        assertTrue(result[0] is JsonPrimitive)
        assertTrue(result[1] is JsonPrimitive)
        assertTrue(result[2] is JsonPrimitive)
        assertTrue(result[3] is JsonObject)
        assertTrue(result[4] is JsonArray)
        assertEquals(JsonNull, result[5])
    }

    @Test
    fun testToJsonElementWithSpecialStrings() {
        val specialStrings =
            mapOf(
                "empty" to "",
                "spaces" to "   ",
                "newline" to "line1\nline2",
                "tab" to "col1\tcol2",
                "unicode" to "Hello 世界",
                "emoji" to "🚀 🎉 ✨",
                "quotes" to "He said \"Hello\"",
                "backslash" to "C:\\path\\to\\file",
            )

        val result = specialStrings.toJsonElement()
        assertTrue(result is JsonObject)

        assertEquals("", result["empty"]?.jsonPrimitive?.content)
        assertEquals("   ", result["spaces"]?.jsonPrimitive?.content)
        assertEquals("line1\nline2", result["newline"]?.jsonPrimitive?.content)
        assertEquals("col1\tcol2", result["tab"]?.jsonPrimitive?.content)
        assertEquals("Hello 世界", result["unicode"]?.jsonPrimitive?.content)
        assertEquals("🚀 🎉 ✨", result["emoji"]?.jsonPrimitive?.content)
        assertEquals("He said \"Hello\"", result["quotes"]?.jsonPrimitive?.content)
        assertEquals("C:\\path\\to\\file", result["backslash"]?.jsonPrimitive?.content)
    }
}

@Serializable
data class TestSerializableObject(
    val id: String,
    val name: String,
    val active: Boolean,
)

@Serializable
data class TestSerializableWithNulls(
    val id: String,
    val name: String? = null,
    val description: String? = null,
)

class NonSerializableObject(
    private val value: String,
) {
    override fun toString(): String = "NonSerializableObject: $value"
}
