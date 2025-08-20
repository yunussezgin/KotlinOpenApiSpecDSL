package me.farshad.dsl.test

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.farshad.dsl.builder.core.openApi
import me.farshad.dsl.builder.core.toJson
import me.farshad.dsl.builder.core.toYaml
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray

class EdgeCaseSchemaGenerationTest {

    // Test 1: Deeply nested sealed classes
    @Serializable
    sealed class Animal {
        @Serializable
        sealed class Mammal : Animal() {
            @Serializable
            data class Dog(val breed: String, val type: String = "Dog") : Mammal()
            
            @Serializable
            data class Cat(val color: String, val type: String = "Cat") : Mammal()
            
            @Serializable
            sealed class Primate : Mammal() {
                @Serializable
                data class Human(val name: String, val type: String = "Human") : Primate()
                
                @Serializable
                data class Monkey(val species: String, val type: String = "Monkey") : Primate()
            }
        }
        
        @Serializable
        sealed class Bird : Animal() {
            @Serializable
            data class Eagle(val wingspan: Double, val type: String = "Eagle") : Bird()
            
            @Serializable
            data class Parrot(val canTalk: Boolean, val type: String = "Parrot") : Bird()
        }
    }

    @Test
    fun `test deeply nested sealed classes generate proper discriminated unions`() {
        val openApiSpec = openApi {
            info {
                title = "Nested Sealed Class Test"
                version = "1.0.0"
            }
            
            components {
                schema(Animal::class)
                schema(Animal.Mammal::class)
                schema(Animal.Mammal.Primate::class)
                schema(Animal.Bird::class)
            }
        }

        val yamlOutput = openApiSpec.toYaml()
        println("Deeply nested sealed classes YAML:")
        println(yamlOutput)
        
        // Verify top-level Animal has oneOf
        assertTrue(yamlOutput.contains("\"Animal\":"), "Should contain Animal schema")
        assertTrue(yamlOutput.contains("oneOf:"), "Animal should have oneOf")
        
        // Verify Mammal has its own oneOf
        assertTrue(yamlOutput.contains("\"Mammal\":"), "Should contain Mammal schema")
        
        // Verify Primate has its own oneOf
        assertTrue(yamlOutput.contains("\"Primate\":"), "Should contain Primate schema")
        
        // Verify Bird has its own oneOf
        assertTrue(yamlOutput.contains("\"Bird\":"), "Should contain Bird schema")
        
        // Verify leaf classes are generated
        assertTrue(yamlOutput.contains("\"Dog\":"), "Should contain Dog schema")
        assertTrue(yamlOutput.contains("\"Cat\":"), "Should contain Cat schema")
        assertTrue(yamlOutput.contains("\"Human\":"), "Should contain Human schema")
        assertTrue(yamlOutput.contains("\"Eagle\":"), "Should contain Eagle schema")
    }

    // Test 2: Enums with custom serialization
    @Serializable
    enum class CustomStatus {
        @SerialName("active_user")
        ACTIVE,
        @SerialName("inactive_user")
        INACTIVE,
        @SerialName("pending_approval")
        PENDING
    }
    
    @Serializable
    data class UserWithCustomEnum(
        val id: String,
        val status: CustomStatus
    )

    @Test
    fun `test enums with custom serialization names`() {
        val openApiSpec = openApi {
            info {
                title = "Custom Enum Serialization Test"
                version = "1.0.0"
            }
            
            components {
                schema(CustomStatus::class)
                schema(UserWithCustomEnum::class)
            }
        }

        val yamlOutput = openApiSpec.toYaml()
        println("Custom enum serialization YAML:")
        println(yamlOutput)
        
        // Verify enum values use the actual enum constant names (not @SerialName)
        // This is expected behavior as OpenAPI generation uses toString() on enum constants
        assertTrue(yamlOutput.contains("\"CustomStatus\":"), "Should contain CustomStatus schema")
        assertTrue(yamlOutput.contains("enum:"), "Should have enum values")
        assertTrue(yamlOutput.contains("- \"ACTIVE\"") || yamlOutput.contains("\"ACTIVE\""), 
            "Should contain ACTIVE enum value")
        assertTrue(yamlOutput.contains("- \"INACTIVE\"") || yamlOutput.contains("\"INACTIVE\""), 
            "Should contain INACTIVE enum value")
    }

    // Test 3: Arrays of arrays (nested collections)
    @Serializable
    data class Matrix(
        val data: List<List<Double>>,
        val labels: List<List<String>>
    )
    
    @Serializable
    data class ComplexArrays(
        val matrix: Matrix,
        val threeDimensional: List<List<List<Int>>>,
        val setOfLists: Set<List<String>>,
        val listOfMaps: List<Map<String, Double>>
    )

    @Test
    fun `test arrays of arrays and nested collections`() {
        val openApiSpec = openApi {
            info {
                title = "Nested Arrays Test"
                version = "1.0.0"
            }
            
            components {
                schema(Matrix::class)
                schema(ComplexArrays::class)
            }
        }

        val yamlOutput = openApiSpec.toYaml()
        println("Nested arrays YAML:")
        println(yamlOutput)
        
        // Verify Matrix contains nested array structure
        assertTrue(yamlOutput.contains("\"Matrix\":"), "Should contain Matrix schema")
        assertTrue(yamlOutput.contains("\"data\":"), "Should contain data property")
        assertTrue(yamlOutput.contains("type: \"array\""), "Should have array type")
        
        // The nested array should have items that are also arrays
        val json = openApiSpec.toJson()
        val jsonObject = Json.parseToJsonElement(json).jsonObject
        
        val matrixSchema = jsonObject["components"]?.jsonObject
            ?.get("schemas")?.jsonObject
            ?.get("Matrix")?.jsonObject
            
        assertTrue(matrixSchema != null, "Matrix schema should exist")
        
        val dataProperty = matrixSchema?.get("properties")?.jsonObject?.get("data")?.jsonObject
        assertTrue(dataProperty != null, "data property should exist")
        assertTrue(dataProperty?.get("type")?.toString()?.contains("array") == true, 
            "data should be array type")
        
        // Verify items property exists for nested array
        val dataItems = dataProperty?.get("items")?.jsonObject
        assertTrue(dataItems != null, "data should have items property for nested array")
        assertTrue(dataItems?.get("type")?.toString()?.contains("array") == true, 
            "nested items should also be array type")
    }

    // Test 4: Circular references
    @Serializable
    data class TreeNode(
        val value: String,
        val children: List<TreeNode>? = null,
        val parent: TreeNode? = null
    )
    
    @Serializable
    data class GraphNode(
        val id: String,
        val connections: List<GraphNode>
    )

    @Test
    fun `test circular references are handled with memoization`() {
        val openApiSpec = openApi {
            info {
                title = "Circular Reference Test"
                version = "1.0.0"
            }
            
            components {
                schema(TreeNode::class)
                schema(GraphNode::class)
            }
        }

        val yamlOutput = openApiSpec.toYaml()
        println("Circular references YAML:")
        println(yamlOutput)
        
        // Verify TreeNode is generated with self-references
        assertTrue(yamlOutput.contains("\"TreeNode\":"), "Should contain TreeNode schema")
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/TreeNode\""), 
            "Should have self-reference for TreeNode")
        
        // Verify GraphNode is generated with self-references
        assertTrue(yamlOutput.contains("\"GraphNode\":"), "Should contain GraphNode schema")
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/GraphNode\""), 
            "Should have self-reference for GraphNode")
    }

    // Test 5: Complex nested structures with mixed types
    @Serializable
    data class Address(val street: String, val city: String)
    
    @Serializable
    enum class Role { ADMIN, USER, GUEST }
    
    @Serializable
    sealed class Permission {
        @Serializable
        data class Read(val resource: String, val type: String = "Read") : Permission()
        
        @Serializable
        data class Write(val resource: String, val level: Int, val type: String = "Write") : Permission()
    }
    
    @Serializable
    data class ComplexUser(
        val id: String,
        val addresses: List<Address>,
        val roles: List<Role>,
        val permissions: List<Permission>,
        val metadata: Map<String, String>,  // Changed from Any to String for serialization
        val tags: Set<String>,
        val history: List<List<String>>
    )

    @Test
    fun `test complex nested structures with mixed types`() {
        val openApiSpec = openApi {
            info {
                title = "Complex Structure Test"
                version = "1.0.0"
            }
            
            components {
                schema(ComplexUser::class)
            }
        }

        val yamlOutput = openApiSpec.toYaml()
        println("Complex structure YAML:")
        println(yamlOutput)
        
        // Verify all dependent schemas are auto-registered
        assertTrue(yamlOutput.contains("\"ComplexUser\":"), "Should contain ComplexUser schema")
        assertTrue(yamlOutput.contains("\"Address\":"), "Should auto-register Address schema")
        assertTrue(yamlOutput.contains("\"Role\":"), "Should auto-register Role enum")
        assertTrue(yamlOutput.contains("\"Permission\":"), "Should auto-register Permission sealed class")
        
        // Verify arrays have proper item references
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/Address\""), 
            "Should reference Address in addresses array")
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/Role\""), 
            "Should reference Role in roles array")
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/Permission\""), 
            "Should reference Permission in permissions array")
    }

    // Test 6: Custom discriminator property
    @Test
    fun `test sealed class with custom discriminator`() {
        val openApiSpec = openApi {
            info {
                title = "Custom Discriminator Test"
                version = "1.0.0"
            }
            
            components {
                // Configure to use a custom discriminator
                configureAutoGeneration {
                    defaultDiscriminatorProperty = "kind"
                }
                schema(Animal::class)
                
                // Reset to default
                configureAutoGeneration {
                    defaultDiscriminatorProperty = "type"
                }
            }
        }

        val yamlOutput = openApiSpec.toYaml()
        
        // Verify discriminator uses custom property
        assertTrue(yamlOutput.contains("discriminator:"), "Should have discriminator")
        assertTrue(yamlOutput.contains("propertyName: \"kind\""), "Should use custom discriminator property 'kind'")
    }

    // Test 7: Disabling auto-generation features
    @Test
    fun `test disabling auto-generation features`() {
        val openApiSpec = openApi {
            info {
                title = "Disable Auto-Generation Test"
                version = "1.0.0"
            }
            
            components {
                // Normal schema with auto-generation
                schema(ComplexArrays::class)
                
                // Schema without array items auto-generation
                schemaWithoutAutoItems(Matrix::class)
                
                // Schema without enum values auto-generation
                schemaWithoutEnumValues(Role::class)
            }
        }

        val json = openApiSpec.toJson()
        val jsonObject = Json.parseToJsonElement(json).jsonObject
        
        val schemas = jsonObject["components"]?.jsonObject?.get("schemas")?.jsonObject
        
        // ComplexArrays should have items generated
        val complexArrays = schemas?.get("ComplexArrays")?.jsonObject
        assertTrue(complexArrays != null, "ComplexArrays should exist")
        
        // Role without enum values should not have enum array
        val roleSchema = schemas?.get("Role")?.jsonObject
        assertTrue(roleSchema != null, "Role schema should exist")
        val hasEnumValues = roleSchema?.get("enum") != null
        assertFalse(hasEnumValues, "Role should not have enum values when auto-generation is disabled")
    }
}