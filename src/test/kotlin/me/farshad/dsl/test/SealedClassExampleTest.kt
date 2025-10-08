package me.farshad.dsl.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.farshad.dsl.builder.core.openApi
import me.farshad.dsl.builder.core.toJson
import me.farshad.dsl.builder.utils.toJsonElement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SealedClassExampleTest {

    @Serializable
    enum class CustomerType {
        INDIVIDUAL, ORGANIZATION
    }

    @Serializable
    sealed class CustomerDto {
        @Serializable
        data class IndividualCustomerDto(
            val id: String,
            val firstName: String,
            val lastName: String,
            val email: String,
            val type: CustomerType = CustomerType.INDIVIDUAL
        ) : CustomerDto()

        @Serializable
        data class OrganizationCustomerDto(
            val id: String,
            val organizationName: String,
            val vat: String,
            val email: String,
            val type: CustomerType = CustomerType.ORGANIZATION
        ) : CustomerDto()
    }

    @Serializable
    data class CustomerListDto(
        val customers: List<CustomerDto>
    )

    fun createSampleIndividualCustomer(): CustomerDto.IndividualCustomerDto =
        CustomerDto.IndividualCustomerDto(
            id = "cust-001",
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com"
        )

    fun createSampleOrganizationCustomer(): CustomerDto.OrganizationCustomerDto =
        CustomerDto.OrganizationCustomerDto(
            id = "cust-002",
            organizationName = "TechCorp",
            vat = "123456789",
            email = "admin@techcorp.com"
        )

    fun createSampleCustomerList(): CustomerListDto = CustomerListDto(
        customers = listOf(
            createSampleIndividualCustomer(),
            createSampleOrganizationCustomer()
        )
    )

    @Test
    fun `sealed class example should serialize properly, not as toString`() {
        val sampleCustomer = createSampleIndividualCustomer()
        val result = sampleCustomer.toJsonElement()

        // Check if it failed and print debug info
        if (result is JsonPrimitive && result.content.startsWith("SERIALIZATION_FAILED")) {
            println("DEBUG: Serialization failed - ${result.content}")
            // Continue with assertion to show the failure
        }

        // Should be JsonObject, not JsonPrimitive (toString)
        assertTrue(result is JsonObject, "Sealed class should serialize as JsonObject, not toString. Got: $result")

        val jsonObj = result.jsonObject
        assertEquals("cust-001", jsonObj["id"]?.jsonPrimitive?.content)
        assertEquals("John", jsonObj["firstName"]?.jsonPrimitive?.content)
        assertEquals("Doe", jsonObj["lastName"]?.jsonPrimitive?.content)
        assertEquals("john.doe@example.com", jsonObj["email"]?.jsonPrimitive?.content)
        assertEquals("INDIVIDUAL", jsonObj["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `sealed class list should serialize properly with fix`() {
        val sampleList = createSampleCustomerList()
        val result = sampleList.toJsonElement()

        println("Serialized result: $result")

        // Check if it failed with debug info
        if (result is JsonPrimitive && result.content.startsWith("SERIALIZATION_FAILED")) {
            println("DEBUG: List serialization failed - ${result.content}")
        }

        assertTrue(result is JsonObject, "CustomerListDto should serialize as JsonObject. Got: $result")
        val customersArray = result.jsonObject["customers"]
        assertNotNull(customersArray, "customers field should exist")

        // With the fix, this should now work properly
        assertTrue(customersArray is JsonArray, "customers should be JsonArray. Got: $customersArray")
        assertEquals(2, customersArray.size, "Should have 2 customers")

        // Verify first customer (Individual)
        val firstCustomer = customersArray[0]
        if (firstCustomer is JsonPrimitive && firstCustomer.content.startsWith("PROPERTY_ERROR")) {
            println("DEBUG: First customer error - ${firstCustomer.content}")
        }
        assertTrue(firstCustomer is JsonObject, "First customer should be JsonObject, not string. Got: $firstCustomer")
        val firstCustomerObj = firstCustomer.jsonObject
        assertEquals("cust-001", firstCustomerObj["id"]?.jsonPrimitive?.content)
        assertEquals("John", firstCustomerObj["firstName"]?.jsonPrimitive?.content)

        // Verify second customer (Organization)
        val secondCustomer = customersArray[1]
        if (secondCustomer is JsonPrimitive && secondCustomer.content.startsWith("PROPERTY_ERROR")) {
            println("DEBUG: Second customer error - ${secondCustomer.content}")
        }
        assertTrue(secondCustomer is JsonObject, "Second customer should be JsonObject, not string. Got: $secondCustomer")
        val secondCustomerObj = secondCustomer.jsonObject
        assertEquals("cust-002", secondCustomerObj["id"]?.jsonPrimitive?.content)
        assertEquals("TechCorp", secondCustomerObj["organizationName"]?.jsonPrimitive?.content)
    }

    @Test
    fun `openapi spec with sealed class examples`() {
        val sampleList = createSampleCustomerList()

        val spec = openApi {
            openapi = "3.1.0"
            info {
                title = "Customer API"
                version = "1.0.0"
            }
            paths {
                path("/customers") {
                    get {
                        summary = "List customers"
                        response("200", "Success") {
                            jsonContent(CustomerListDto::class)
                            examples {
                                example("sample") {
                                    summary = "Sample customer list"
                                    value(sampleList)
                                }
                            }
                        }
                    }
                }
            }
            components {
                schema(CustomerListDto::class)
                schema(CustomerDto::class)
            }
        }

        val json = spec.toJson()
        println("OpenAPI spec: $json")

        // Parse and verify the structure
        val jsonElement = Json.parseToJsonElement(json)
        assertTrue(jsonElement is JsonObject)

        // Check if the example is properly serialized
        val paths = jsonElement.jsonObject["paths"]?.jsonObject
        val customersPath = paths?.get("/customers")?.jsonObject
        val getOp = customersPath?.get("get")?.jsonObject
        val responses = getOp?.get("responses")?.jsonObject
        val response200 = responses?.get("200")?.jsonObject
        val content = response200?.get("content")?.jsonObject
        val jsonContent = content?.get("application/json")?.jsonObject
        val examples = jsonContent?.get("examples")?.jsonObject
        val sampleExample = examples?.get("sample")?.jsonObject
        val exampleValue = sampleExample?.get("value")

        assertNotNull(exampleValue, "Example value should exist")
        println("Example value: $exampleValue")

        // Currently this will be a string due to the toString() fallback issue
        // After fix, this should be a proper JsonObject
    }
}