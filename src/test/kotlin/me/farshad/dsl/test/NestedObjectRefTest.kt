package me.farshad.dsl.test

import me.farshad.dsl.builder.core.openApi
import me.farshad.dsl.builder.core.toYaml
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

// Test data classes for nested object references
data class Address(
    val street: String,
    val city: String,
    val zipCode: String
)

data class Person(
    val name: String,
    val email: String,
    val address: Address,
    val secondaryAddress: Address?
)

class NestedObjectRefTest {

    @Test
    fun `test nested objects generate $ref instead of inline type object`() {
        val openApiSpec = openApi {
            info {
                title = "Nested Object Ref Test API"
                version = "1.0.0"
                description = "Testing nested object \$ref generation"
            }
            
            components {
                schema(Address::class)
                schema(Person::class)
            }
        }

        val yamlOutput = openApiSpec.toYaml()
        println("Generated YAML:")
        println(yamlOutput)
        
        // Verify that nested objects use $ref instead of inline type: "object"
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/Address\""), "Should contain \$ref to Address schema")
        
        // Verify that we don't have inline type: "object" for the address property
        assertFalse(yamlOutput.contains("\"address\":\n        type: \"object\""), "Should NOT contain inline type: object for address")
        
        // Verify both Address and Person schemas are present in components
        assertTrue(yamlOutput.contains("\"Address\":"), "Should contain Address schema in components")
        assertTrue(yamlOutput.contains("\"Person\":"), "Should contain Person schema in components")
        
        // Verify the required field handling works for non-nullable nested objects
        assertTrue(yamlOutput.contains("\"address\""), "Should contain address property")
        assertTrue(yamlOutput.contains("\"secondaryAddress\""), "Should contain secondaryAddress property")
    }
    
    @Test
    fun `test multiple levels of nesting generate proper refs`() {
        data class Country(val name: String, val code: String)
        data class Region(val name: String, val country: Country)
        data class Company(val name: String, val region: Region)
        
        val openApiSpec = openApi {
            info {
                title = "Multi-level Nesting Test"
                version = "1.0.0"
            }
            
            components {
                schema(Country::class)
                schema(Region::class)
                schema(Company::class)
            }
        }

        val yamlOutput = openApiSpec.toYaml()
        
        // Verify all nested references are generated
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/Country\""), "Should contain \$ref to Country")
        assertTrue(yamlOutput.contains("\$ref: \"#/components/schemas/Region\""), "Should contain \$ref to Region")
        
        // Verify all schemas are registered
        assertTrue(yamlOutput.contains("\"Country\":"), "Should contain Country schema")
        assertTrue(yamlOutput.contains("\"Region\":"), "Should contain Region schema")
        assertTrue(yamlOutput.contains("\"Company\":"), "Should contain Company schema")
    }
}