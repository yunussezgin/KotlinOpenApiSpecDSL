package me.farshad.dsl.spec

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import me.farshad.dsl.annotation.PropertyDescription
import me.farshad.dsl.annotation.SchemaDescription
import me.farshad.dsl.builder.core.openApi
import me.farshad.dsl.builder.core.toJson
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsonContentExamplesTest {
    @Serializable
    @SchemaDescription("User representation")
    data class User(
        @PropertyDescription("User ID")
        val id: Int,
        @PropertyDescription("User's full name")
        val name: String,
        @PropertyDescription("User's email address")
        val email: String,
        @PropertyDescription("User's role in the system")
        val role: String = "user",
    )

    @Test
    fun testSingleExampleWithSchemaClass() {
        val spec =
            openApi {
                openapi = "3.1.0"
                info {
                    title = "User API"
                    version = "1.0.0"
                }
                paths {
                    path("/users/{id}") {
                        get {
                            summary = "Get user by ID"
                            response("200", "Success") {
                                jsonContent(
                                    User::class,
                                    example =
                                        User(
                                            id = 1,
                                            name = "John Doe",
                                            email = "john@example.com",
                                            role = "user",
                                        ),
                                )
                            }
                        }
                    }
                }
                components {
                    schema(User::class)
                }
            }

        val json = spec.toJson()
        val jsonObj = Json.parseToJsonElement(json).jsonObject

        // Verify the response content has example
        val responseContent =
            jsonObj["paths"]
                ?.jsonObject
                ?.get("/users/{id}")
                ?.jsonObject
                ?.get("get")
                ?.jsonObject
                ?.get("responses")
                ?.jsonObject
                ?.get("200")
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("application/json")
                ?.jsonObject

        assertNotNull(responseContent)

        // Check schema reference
        val schema = responseContent["schema"]?.jsonObject
        assertEquals("#/components/schemas/User", schema?.get("\$ref")?.jsonPrimitive?.content)

        // Check example
        val example = responseContent["example"]?.jsonObject
        assertNotNull(example)
        assertEquals(1, example["id"]?.jsonPrimitive?.int)
        assertEquals("John Doe", example["name"]?.jsonPrimitive?.content)
        assertEquals("john@example.com", example["email"]?.jsonPrimitive?.content)
        // role has default value "user" and won't be in JSON due to encodeDefaults=false
    }

    @Test
    fun testMultipleExamplesWithSchemaClass() {
        val spec =
            openApi {
                openapi = "3.1.0"
                info {
                    title = "User API"
                    version = "1.0.0"
                }
                paths {
                    path("/users") {
                        post {
                            summary = "Create user"
                            requestBody {
                                description = "User data"
                                jsonContent(User::class)
                                examples {
                                    example("admin") {
                                        summary = "Admin user example"
                                        description = "Example of creating an admin user"
                                        value(
                                            User(
                                                id = 1,
                                                name = "Admin User",
                                                email = "admin@example.com",
                                                role = "admin",
                                            ),
                                        )
                                    }
                                    example("regular") {
                                        summary = "Regular user example"
                                        value(
                                            User(
                                                id = 2,
                                                name = "John Doe",
                                                email = "john@example.com",
                                                role = "user",
                                            ),
                                        )
                                    }
                                }
                            }
                            response("201", "Created") {
                                jsonContent(User::class)
                            }
                        }
                    }
                }
                components {
                    schema(User::class)
                }
            }

        val json = spec.toJson()
        val jsonObj = Json.parseToJsonElement(json).jsonObject

        // Verify the request body content has examples
        val requestContent =
            jsonObj["paths"]
                ?.jsonObject
                ?.get("/users")
                ?.jsonObject
                ?.get("post")
                ?.jsonObject
                ?.get("requestBody")
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("application/json")
                ?.jsonObject

        assertNotNull(requestContent)

        // Check examples
        val examples = requestContent["examples"]?.jsonObject
        assertNotNull(examples)

        // Verify admin example
        val adminExample = examples["admin"]?.jsonObject
        assertNotNull(adminExample)
        assertEquals("Admin user example", adminExample["summary"]?.jsonPrimitive?.content)
        assertEquals("Example of creating an admin user", adminExample["description"]?.jsonPrimitive?.content)

        val adminValue = adminExample["value"]?.jsonObject
        assertNotNull(adminValue)
        assertEquals(1, adminValue["id"]?.jsonPrimitive?.int)
        assertEquals("Admin User", adminValue["name"]?.jsonPrimitive?.content)
        assertEquals("admin@example.com", adminValue["email"]?.jsonPrimitive?.content)
        assertEquals("admin", adminValue["role"]?.jsonPrimitive?.content)

        // Verify regular example
        val regularExample = examples["regular"]?.jsonObject
        assertNotNull(regularExample)
        assertEquals("Regular user example", regularExample["summary"]?.jsonPrimitive?.content)

        val regularValue = regularExample["value"]?.jsonObject
        assertNotNull(regularValue)
        assertEquals(2, regularValue["id"]?.jsonPrimitive?.int)
        assertEquals("John Doe", regularValue["name"]?.jsonPrimitive?.content)
        assertEquals("john@example.com", regularValue["email"]?.jsonPrimitive?.content)
        // role has default value "user" and won't be in JSON due to encodeDefaults=false
    }

    @Test
    fun testSchemaReferenceWithExamples() {
        val spec =
            openApi {
                openapi = "3.1.0"
                info {
                    title = "User API"
                    version = "1.0.0"
                }
                paths {
                    path("/users/{id}") {
                        patch {
                            summary = "Update user"
                            requestBody {
                                description = "User update data"
                                jsonContent("User")
                                example(
                                    User(
                                        id = 1,
                                        name = "Updated Name",
                                        email = "updated@example.com",
                                        role = "user",
                                    ),
                                )
                                examples {
                                    example("name_update") {
                                        summary = "Update name only"
                                        value(
                                            buildJsonObject {
                                                put("name", "New Name")
                                            },
                                        )
                                    }
                                    example("email_update") {
                                        summary = "Update email only"
                                        value(
                                            buildJsonObject {
                                                put("email", "newemail@example.com")
                                            },
                                        )
                                    }
                                }
                            }
                            response("200", "Success") {
                                jsonContent(User::class)
                            }
                        }
                    }
                }
                components {
                    schema(User::class)
                }
            }

        val json = spec.toJson()
        val jsonObj = Json.parseToJsonElement(json).jsonObject

        // Verify the request body content
        val requestContent =
            jsonObj["paths"]
                ?.jsonObject
                ?.get("/users/{id}")
                ?.jsonObject
                ?.get("patch")
                ?.jsonObject
                ?.get("requestBody")
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("application/json")
                ?.jsonObject

        assertNotNull(requestContent)

        // Check schema reference
        val schema = requestContent["schema"]?.jsonObject
        assertEquals("#/components/schemas/User", schema?.get("\$ref")?.jsonPrimitive?.content)

        // When both example and examples are set, examples takes precedence
        // and example is set to null (see RequestBodyBuilder line 251)

        // Check multiple examples
        val examples = requestContent["examples"]?.jsonObject
        assertNotNull(examples)

        val nameUpdate = examples["name_update"]?.jsonObject
        assertNotNull(nameUpdate)
        assertEquals("Update name only", nameUpdate["summary"]?.jsonPrimitive?.content)
        // Value is properly serialized as a JsonObject
        val nameValue =
            nameUpdate["value"]
                ?.jsonObject
                ?.get("name")
                ?.jsonPrimitive
                ?.content
        assertEquals("New Name", nameValue)

        val emailUpdate = examples["email_update"]?.jsonObject
        assertNotNull(emailUpdate)
        assertEquals("Update email only", emailUpdate["summary"]?.jsonPrimitive?.content)
        // Value is properly serialized as a JsonObject
        val emailValue =
            emailUpdate["value"]
                ?.jsonObject
                ?.get("email")
                ?.jsonPrimitive
                ?.content
        assertEquals("newemail@example.com", emailValue)
    }

    @Test
    fun testComprehensiveIntegrationExample() {
        @Serializable
        data class ErrorResponse(
            val code: String,
            val message: String,
            val details: Map<String, String>? = null,
        )

        val spec =
            openApi {
                openapi = "3.1.0"
                info {
                    title = "Complete User API"
                    version = "1.0.0"
                    description = "API with comprehensive examples"
                }

                paths {
                    path("/users") {
                        get {
                            summary = "List all users"
                            response("200", "Success") {
                                jsonContent {
                                    type = SchemaType.ARRAY
                                    items = Schema(ref = "#/components/schemas/User")
                                }
                                example(
                                    listOf(
                                        User(1, "John Doe", "john@example.com"),
                                        User(2, "Jane Smith", "jane@example.com", "admin"),
                                    ),
                                )
                            }
                        }

                        post {
                            summary = "Create a new user"
                            requestBody {
                                description = "New user data"
                                required = true
                                jsonContent(User::class)
                                examples {
                                    example("minimal") {
                                        summary = "Minimal user data"
                                        value(
                                            mapOf(
                                                "name" to "New User",
                                                "email" to "newuser@example.com",
                                            ),
                                        )
                                    }
                                    example("complete") {
                                        summary = "Complete user data"
                                        value(
                                            User(
                                                id = 0,
                                                name = "Complete User",
                                                email = "complete@example.com",
                                                role = "admin",
                                            ),
                                        )
                                    }
                                }
                            }

                            response("201", "Created") {
                                jsonContent(
                                    User::class,
                                    example =
                                        User(
                                            id = 3,
                                            name = "New User",
                                            email = "newuser@example.com",
                                            role = "user",
                                        ),
                                )
                            }

                            response("400", "Bad Request") {
                                jsonContent(ErrorResponse::class)
                                examples {
                                    example("validation_error") {
                                        summary = "Validation error"
                                        value(
                                            ErrorResponse(
                                                code = "VALIDATION_ERROR",
                                                message = "Invalid input data",
                                                details =
                                                    mapOf(
                                                        "email" to "Invalid email format",
                                                        "name" to "Name is required",
                                                    ),
                                            ),
                                        )
                                    }
                                    example("duplicate_email") {
                                        summary = "Duplicate email"
                                        value(
                                            ErrorResponse(
                                                code = "DUPLICATE_EMAIL",
                                                message = "Email already exists",
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                components {
                    schema(User::class)
                    schema(ErrorResponse::class)

                    // Reusable examples
                    example("UserExample") {
                        summary = "Standard user example"
                        value(User(1, "John Doe", "john@example.com"))
                    }

                    example("AdminExample") {
                        summary = "Admin user example"
                        value(User(2, "Admin User", "admin@example.com", "admin"))
                    }
                }
            }

        val json = spec.toJson()
        val jsonObj = Json.parseToJsonElement(json).jsonObject

        // Verify components examples
        val components = jsonObj["components"]?.jsonObject
        val componentExamples = components?.get("examples")?.jsonObject
        assertNotNull(componentExamples)
        assertTrue(componentExamples.containsKey("UserExample"))
        assertTrue(componentExamples.containsKey("AdminExample"))

        // Verify array response with example
        val listResponse =
            jsonObj["paths"]
                ?.jsonObject
                ?.get("/users")
                ?.jsonObject
                ?.get("get")
                ?.jsonObject
                ?.get("responses")
                ?.jsonObject
                ?.get("200")
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("application/json")
                ?.jsonObject

        assertNotNull(listResponse)
        val arrayExample = listResponse["example"]?.jsonArray
        assertNotNull(arrayExample)
        assertEquals(2, arrayExample.size)

        // Verify error response examples
        val errorResponse =
            jsonObj["paths"]
                ?.jsonObject
                ?.get("/users")
                ?.jsonObject
                ?.get("post")
                ?.jsonObject
                ?.get("responses")
                ?.jsonObject
                ?.get("400")
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("application/json")
                ?.jsonObject
                ?.get("examples")
                ?.jsonObject

        assertNotNull(errorResponse)
        assertTrue(errorResponse.containsKey("validation_error"))
        assertTrue(errorResponse.containsKey("duplicate_email"))
    }
}
