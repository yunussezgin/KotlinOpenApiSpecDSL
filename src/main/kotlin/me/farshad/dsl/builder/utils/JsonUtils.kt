package me.farshad.dsl.builder.utils

import kotlinx.serialization.json.Json
import mu.KotlinLogging
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.reflect.KProperty1

private val logger = KotlinLogging.logger {}

// Helper function to create JsonElement from any value
fun Any.toJsonElement(): JsonElement =
    when (this) {
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> ->
            JsonObject(
                this
                    .mapKeys { it.key.toString() }
                    .mapValues { it.value?.toJsonElement() ?: JsonNull },
            )

        is List<*> -> JsonArray(this.map { it?.toJsonElement() ?: JsonNull })
        is Enum<*> -> JsonPrimitive(this.name)
        else -> {
            // Try standard kotlinx.serialization first
            try {
                createPermissiveJson().encodeToJsonElement(this)
            } catch (e: Exception) {
                // Fall back to reflection-based serialization for complex objects
                try {
                    reflectionBasedSerialization(this)
                } catch (reflectionException: Exception) {
                    // Final fallback with error details
                    JsonPrimitive("SERIALIZATION_FAILED[${this::class.simpleName}]: ${e.message}")
                }
            }
        }
    }

/**
 * Creates a Json configuration optimized for sealed classes and DTOs
 */
private fun createPermissiveJson() = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    coerceInputValues = true
    classDiscriminator = "type"
    useArrayPolymorphism = false
}

/**
 * Manually constructs JSON using reflection when kotlinx.serialization fails.
 * This is particularly useful for sealed classes in complex object graphs.
 */
private fun reflectionBasedSerialization(obj: Any): JsonElement {
    val properties = obj::class.members.filterIsInstance<KProperty1<Any, *>>()
    val jsonMap = mutableMapOf<String, JsonElement>()

    for (property in properties) {
        try {
            val value = property.get(obj)
            jsonMap[property.name] = convertValueToJsonElement(value)
        } catch (e: Exception) {
            // Log inaccessible properties for debugging
            logger.debug { "Unable to access property '${property.name}' on ${obj::class.simpleName}: ${e.message}" }
            continue
        }
    }

    return JsonObject(jsonMap)
}

/**
 * Converts a property value to JsonElement, handling common types
 */
private fun convertValueToJsonElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is String -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Enum<*> -> JsonPrimitive(value.name)
    else -> value.toJsonElement() // Recursive call for nested objects
}

// Extension function to convert @Serializable objects to JsonElement
inline fun <reified T> T.toSerializableJsonElement(): JsonElement where T : Any {
    val json =
        Json {
            prettyPrint = false
            encodeDefaults = false
            explicitNulls = false
        }
    return json.encodeToJsonElement(this)
}
