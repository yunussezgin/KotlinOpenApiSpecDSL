package me.farshad.dsl.test

import me.farshad.dsl.builder.core.openApi
import me.farshad.dsl.builder.core.toYaml
import me.farshad.dsl.builder.core.toJson
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray

// Test enums for schema generation
enum class UserStatus {
    ACTIVE,
    INACTIVE,
    PENDING,
    SUSPENDED
}

enum class Priority(val displayName: String) {
    LOW("Low Priority"),
    MEDIUM("Medium Priority"), 
    HIGH("High Priority"),
    CRITICAL("Critical Priority")
}

// Test data classes using enums
data class User(
    val name: String,
    val email: String,
    val status: UserStatus,
    val priority: Priority?
)

data class Task(
    val title: String,
    val priority: Priority,
    val assignedUser: User?
)

class EnumSupportTest {

    @Test
    fun `test enum properties generate proper enum values in schema`() {
        val openApiSpec = openApi {
            info {
                title = "Enum Support Test API"
                version = "1.0.0"
                description = "Testing enum value generation in schemas"
            }
            
            components {
                schema(UserStatus::class)
                schema(Priority::class)
                schema(User::class)
                schema(Task::class)
            }
        }

        val yamlOutput = openApiSpec.toYaml()
        println("Generated YAML:")
        println(yamlOutput)
        
        // Verify UserStatus enum has proper values
        assertTrue(yamlOutput.contains("\"UserStatus\":"), "Should contain UserStatus schema")
        assertTrue(yamlOutput.contains("enum:"), "Should contain enum property")
        assertTrue(yamlOutput.contains("- \"ACTIVE\""), "Should contain ACTIVE enum value")
        assertTrue(yamlOutput.contains("- \"INACTIVE\""), "Should contain INACTIVE enum value")
        assertTrue(yamlOutput.contains("- \"PENDING\""), "Should contain PENDING enum value")
        assertTrue(yamlOutput.contains("- \"SUSPENDED\""), "Should contain SUSPENDED enum value")
        
        // Verify Priority enum has proper values
        assertTrue(yamlOutput.contains("\"Priority\":"), "Should contain Priority schema")
        assertTrue(yamlOutput.contains("- \"LOW\""), "Should contain LOW enum value")
        assertTrue(yamlOutput.contains("- \"MEDIUM\""), "Should contain MEDIUM enum value")
        assertTrue(yamlOutput.contains("- \"HIGH\""), "Should contain HIGH enum value")
        assertTrue(yamlOutput.contains("- \"CRITICAL\""), "Should contain CRITICAL enum value")
        
        // Verify User schema references the enum schemas
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/UserStatus\""), "User should reference UserStatus enum")
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/Priority\""), "User should reference Priority enum")
    }
    
    @Test 
    fun `test enum properties in JSON output have proper enum arrays`() {
        val openApiSpec = openApi {
            info {
                title = "Enum JSON Test API"
                version = "1.0.0"
            }
            
            components {
                schema(UserStatus::class)
                schema(Priority::class)
                schema(User::class)
            }
        }

        val json = openApiSpec.toJson()
        val jsonObject = Json.parseToJsonElement(json).jsonObject
        
        // Check UserStatus enum in components
        val userStatusSchema = jsonObject["components"]?.jsonObject
            ?.get("schemas")?.jsonObject
            ?.get("UserStatus")?.jsonObject
            
        assertTrue(userStatusSchema != null, "UserStatus schema should exist")
        
        val userStatusEnum = userStatusSchema?.get("enum")?.jsonArray
        assertTrue(userStatusEnum != null, "UserStatus should have enum array")
        assertTrue(userStatusEnum?.size == 4, "UserStatus should have 4 enum values")
        
        // Check Priority enum in components  
        val prioritySchema = jsonObject["components"]?.jsonObject
            ?.get("schemas")?.jsonObject
            ?.get("Priority")?.jsonObject
            
        assertTrue(prioritySchema != null, "Priority schema should exist")
        
        val priorityEnum = prioritySchema?.get("enum")?.jsonArray
        assertTrue(priorityEnum != null, "Priority should have enum array")
        assertTrue(priorityEnum?.size == 4, "Priority should have 4 enum values")
    }
    
    @Test
    fun `test nested enums are properly referenced and defined`() {
        val openApiSpec = openApi {
            info {
                title = "Nested Enum Test"
                version = "1.0.0"
            }
            
            components {
                schema(Task::class)  // This should auto-register Priority and User enums
            }
        }

        val yamlOutput = openApiSpec.toYaml()
        
        // Verify all required schemas were auto-registered
        assertTrue(yamlOutput.contains("\"Task\":"), "Should contain Task schema")
        assertTrue(yamlOutput.contains("\"Priority\":"), "Should auto-register Priority enum schema")
        assertTrue(yamlOutput.contains("\"User\":"), "Should auto-register User schema")
        assertTrue(yamlOutput.contains("\"UserStatus\":"), "Should auto-register UserStatus enum schema")
        
        // Verify enum values are present
        assertTrue(yamlOutput.contains("enum:"), "Should contain enum properties")
        assertTrue(yamlOutput.contains("- \"LOW\""), "Should contain Priority enum values")
        assertTrue(yamlOutput.contains("- \"ACTIVE\""), "Should contain UserStatus enum values")
    }
}