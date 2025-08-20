package me.farshad.dsl.builder.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.farshad.dsl.spec.Example
import me.farshad.dsl.spec.ParameterLocation
import me.farshad.dsl.spec.PropertyType
import me.farshad.dsl.spec.Schema
import me.farshad.dsl.spec.SchemaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OpenApiBuilderTest {
    @Test
    fun testMinimalOpenApiSpec() {
        val spec =
            openApi {
                info {
                    title = "Test API"
                    version = "1.0.0"
                }
            }

        assertEquals("3.1.0", spec.openapi)
        assertEquals("Test API", spec.info.title)
        assertEquals("1.0.0", spec.info.version)
        assertEquals(0, spec.servers.size)
        assertEquals(0, spec.paths.size)
        assertNull(spec.components)
    }

    @Test
    fun testCustomOpenApiVersion() {
        val spec =
            openApi {
                openapi = "3.0.0"
                info {
                    title = "Test API"
                    version = "1.0.0"
                }
            }

        assertEquals("3.0.0", spec.openapi)
    }

    @Test
    fun testOpenApiWithServers() {
        val spec =
            openApi {
                info {
                    title = "Test API"
                    version = "1.0.0"
                }
                server("https://api.example.com")
                server("https://staging.example.com") {
                    description = "Staging server"
                }
            }

        assertEquals(2, spec.servers.size)
        assertEquals("https://api.example.com", spec.servers[0].url)
        assertNull(spec.servers[0].description)
        assertEquals("https://staging.example.com", spec.servers[1].url)
        assertEquals("Staging server", spec.servers[1].description)
    }

    @Test
    fun testOpenApiWithPaths() {
        val spec =
            openApi {
                info {
                    title = "Test API"
                    version = "1.0.0"
                }
                paths {
                    path("/users") {
                        get {
                            summary = "List users"
                        }
                    }
                    path("/users/{id}") {
                        get {
                            summary = "Get user by ID"
                        }
                        put {
                            summary = "Update user"
                        }
                    }
                }
            }

        assertEquals(2, spec.paths.size)
        assertNotNull(spec.paths["/users"])
        assertNotNull(spec.paths["/users"]?.get)
        assertEquals("List users", spec.paths["/users"]?.get?.summary)

        assertNotNull(spec.paths["/users/{id}"])
        assertNotNull(spec.paths["/users/{id}"]?.get)
        assertEquals("Get user by ID", spec.paths["/users/{id}"]?.get?.summary)
        assertNotNull(spec.paths["/users/{id}"]?.put)
        assertEquals("Update user", spec.paths["/users/{id}"]?.put?.summary)
    }

    @Test
    fun testOpenApiWithComponents() {
        val spec =
            openApi {
                info {
                    title = "Test API"
                    version = "1.0.0"
                }
                components {
                    schema("User") {
                        type = SchemaType.OBJECT
                        property("id", PropertyType.INTEGER, true)
                        property("name", PropertyType.STRING, true)
                    }
                    securityScheme("bearerAuth", "http", "bearer", "JWT")
                }
            }

        assertNotNull(spec.components)
        assertNotNull(spec.components?.schemas)
        assertEquals(1, spec.components?.schemas?.size)
        assertNotNull(spec.components?.schemas?.get("User"))
        assertNotNull(spec.components?.securitySchemes)
        assertEquals(1, spec.components?.securitySchemes?.size)
    }

    @Test
    fun testCompleteOpenApiSpec() {
        val spec =
            openApi {
                openapi = "3.1.0"
                info {
                    title = "Complete API"
                    version = "2.0.0"
                    description = "A complete API example"
                    termsOfService = "https://example.com/terms"
                    contact {
                        name = "API Support"
                        email = "support@example.com"
                    }
                    license("MIT", "https://opensource.org/licenses/MIT")
                }
                server("https://api.example.com") {
                    description = "Production server"
                }
                paths {
                    path("/users") {
                        get {
                            summary = "List all users"
                            tags("users")
                            response("200", "Successful response") {
                                jsonContent("UserList")
                            }
                        }
                        post {
                            summary = "Create a new user"
                            tags("users")
                            requestBody {
                                required = true
                                jsonContent("User")
                            }
                            response("201", "User created")
                        }
                    }
                }
                components {
                    schema("User") {
                        type = SchemaType.OBJECT
                        property("id", PropertyType.INTEGER, true)
                        property("name", PropertyType.STRING, true)
                        property("email", PropertyType.STRING, false)
                    }
                    schema("UserList") {
                        type = SchemaType.ARRAY
                        items {
                            type = SchemaType.OBJECT
                        }
                    }
                }
            }

        assertEquals("3.1.0", spec.openapi)
        assertEquals("Complete API", spec.info.title)
        assertEquals("2.0.0", spec.info.version)
        assertEquals("A complete API example", spec.info.description)
        assertEquals("https://example.com/terms", spec.info.termsOfService)
        assertNotNull(spec.info.contact)
        assertEquals("API Support", spec.info.contact?.name)
        assertNotNull(spec.info.license)
        assertEquals("MIT", spec.info.license?.name)

        assertEquals(1, spec.servers.size)
        assertEquals("https://api.example.com", spec.servers[0].url)

        assertEquals(1, spec.paths.size)
        assertNotNull(spec.paths["/users"])
        assertNotNull(spec.paths["/users"]?.get)
        assertNotNull(spec.paths["/users"]?.post)

        assertNotNull(spec.components)
        assertEquals(2, spec.components?.schemas?.size)
    }

    @Test
    fun testOpenApiJsonSerialization() {
        val spec =
            openApi {
                info {
                    title = "JSON Test API"
                    version = "1.0.0"
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
        val jsonObject = Json.parseToJsonElement(json).jsonObject

        assertEquals("3.1.0", jsonObject["openapi"]?.jsonPrimitive?.content)
        assertEquals(
            "JSON Test API",
            jsonObject["info"]
                ?.jsonObject
                ?.get("title")
                ?.jsonPrimitive
                ?.content,
        )
        assertNotNull(jsonObject["paths"]?.jsonObject?.get("/test"))
    }

    @Test
    fun testOpenApiYamlSerialization() {
        val spec =
            openApi {
                info {
                    title = "YAML Test API"
                    version = "1.0.0"
                    description = "Testing YAML serialization"
                }
            }

        val yaml = spec.toYaml()

        assert(yaml.contains("openapi: \"3.1.0\""))
        assert(yaml.contains("title: \"YAML Test API\""))
        assert(yaml.contains("version: \"1.0.0\""))
        assert(yaml.contains("description: \"Testing YAML serialization\""))
    }

    @Test
    fun testEmptyPathsAndComponents() {
        val spec =
            openApi {
                info {
                    title = "Empty API"
                    version = "1.0.0"
                }
                paths {
                    // Empty paths block
                }
                components {
                    // Empty components block
                }
            }

        assertEquals(0, spec.paths.size)
        assertNotNull(spec.components)
        assertNull(spec.components?.schemas)
        assertNull(spec.components?.securitySchemes)
        assertNull(spec.components?.examples)
    }

    @Test
    fun testMultipleServersWithoutDescription() {
        val spec =
            openApi {
                info {
                    title = "Multi-Server API"
                    version = "1.0.0"
                }
                server("https://api1.example.com")
                server("https://api2.example.com")
                server("https://api3.example.com")
            }

        assertEquals(3, spec.servers.size)
        spec.servers.forEach { server ->
            assertNull(server.description)
        }
    }

    @Test
    fun testParameterExamplesInJsonOutput() {
        val spec = openApi {
            info {
                title = "API with Parameter Examples"
                version = "1.0.0"
            }
            paths {
                path("/users/{userId}") {
                    get {
                        summary = "Get user by ID"
                        parameter(
                            name = "userId",
                            location = ParameterLocation.PATH,
                            type = PropertyType.STRING,
                            required = true,
                            description = "The user identifier",
                            examples = mapOf(
                                "normalUser" to Example(
                                    summary = "Regular user ID",
                                    value = JsonPrimitive("user-123")
                                ),
                                "adminUser" to Example(
                                    summary = "Admin user ID",
                                    description = "ID for admin users",
                                    value = JsonPrimitive("admin-456")
                                )
                            )
                        )
                        response("200", "Success")
                    }
                }
            }
        }

        val json = spec.toJson()
        val jsonObject = Json.parseToJsonElement(json).jsonObject

        // Navigate to the parameter examples
        val pathItem = jsonObject["paths"]?.jsonObject?.get("/users/{userId}")?.jsonObject
        val getOperation = pathItem?.get("get")?.jsonObject
        val parameters = getOperation?.get("parameters")?.jsonArray

        assertNotNull(parameters)
        assertEquals(1, parameters.size)

        val parameter = parameters[0].jsonObject
        assertEquals("userId", parameter["name"]?.jsonPrimitive?.content)
        assertEquals("path", parameter["in"]?.jsonPrimitive?.content)
        assertEquals(true, parameter["required"]?.jsonPrimitive?.boolean)

        // Check examples
        val examples = parameter["examples"]?.jsonObject
        assertNotNull(examples)
        assertEquals(2, examples.size)

        // Check normalUser example
        val normalUser = examples["normalUser"]?.jsonObject
        assertNotNull(normalUser)
        assertEquals("Regular user ID", normalUser["summary"]?.jsonPrimitive?.content)
        assertEquals("user-123", normalUser["value"]?.jsonPrimitive?.content)

        // Check adminUser example
        val adminUser = examples["adminUser"]?.jsonObject
        assertNotNull(adminUser)
        assertEquals("Admin user ID", adminUser["summary"]?.jsonPrimitive?.content)
        assertEquals("ID for admin users", adminUser["description"]?.jsonPrimitive?.content)
        assertEquals("admin-456", adminUser["value"]?.jsonPrimitive?.content)
    }

    @Test
    fun testParameterExamplesInYamlOutput() {
        val spec = openApi {
            info {
                title = "YAML API with Examples"
                version = "1.0.0"
            }
            paths {
                path("/items") {
                    get {
                        summary = "List items"
                        parameter(
                            name = "category",
                            location = ParameterLocation.QUERY,
                            type = PropertyType.STRING,
                            description = "Filter by category",
                            examples = mapOf(
                                "electronics" to Example(
                                    value = JsonPrimitive("electronics"),
                                    summary = "Electronics category"
                                ),
                                "books" to Example(
                                    value = JsonPrimitive("books"),
                                    summary = "Books category"
                                )
                            )
                        )
                        response("200", "Items retrieved")
                    }
                }
            }
        }

        // First verify JSON output works correctly
        val json = spec.toJson()
        val jsonObject = Json.parseToJsonElement(json).jsonObject
        val params =
            jsonObject["paths"]?.jsonObject?.get("/items")?.jsonObject?.get("get")?.jsonObject?.get("parameters")?.jsonArray
        assertNotNull(params, "Parameters should not be null in JSON")
        assertEquals(1, params.size, "Should have 1 parameter in JSON")

        val yaml = spec.toYaml()

        // Verify YAML contains the examples structure
        // Note: YAML uses list format for parameters, not map format
        assert(yaml.contains("parameters:"))
        assert(yaml.contains("- name: \"category\""))
        assert(yaml.contains("in: \"query\""))
        assert(yaml.contains("examples:"))
        assert(yaml.contains("\"electronics\":"))
        assert(yaml.contains("summary: \"Electronics category\""))
        assert(yaml.contains("value: \"electronics\""))
        assert(yaml.contains("\"books\":"))
        assert(yaml.contains("summary: \"Books category\""))
        assert(yaml.contains("value: \"books\""))
    }

    @Test
    fun testCompleteApiWithParameterExamples() {
        val spec = openApi {
            info {
                title = "Complete API with Examples"
                version = "2.0.0"
                description = "API demonstrating parameter examples"
            }
            server("https://api.example.com")
            paths {
                path("/users/{userId}/posts") {
                    get {
                        summary = "Get user posts"
                        operationId = "getUserPosts"
                        tags("users", "posts")

                        // Path parameter with examples
                        parameter(
                            name = "userId",
                            location = ParameterLocation.PATH,
                            type = PropertyType.STRING,
                            required = true,
                            description = "User ID",
                            examples = mapOf(
                                "user1" to Example(
                                    value = JsonPrimitive("550e8400-e29b-41d4-a716-446655440000"),
                                    summary = "UUID example"
                                ),
                                "user2" to Example(
                                    value = JsonPrimitive("user-12345"),
                                    summary = "Simple ID example"
                                )
                            )
                        )

                        // Query parameters with examples
                        parameter(
                            name = "status",
                            location = ParameterLocation.QUERY,
                            type = PropertyType.STRING,
                            description = "Filter by post status",
                            examples = mapOf(
                                "published" to Example(
                                    value = JsonPrimitive("published"),
                                    summary = "Published posts"
                                ),
                                "draft" to Example(
                                    value = JsonPrimitive("draft"),
                                    summary = "Draft posts"
                                ),
                                "archived" to Example(
                                    value = JsonPrimitive("archived"),
                                    summary = "Archived posts"
                                )
                            )
                        )

                        parameter(
                            name = "limit",
                            location = ParameterLocation.QUERY,
                            type = PropertyType.INTEGER,
                            description = "Number of posts to return",
                            examples = mapOf(
                                "small" to Example(
                                    value = JsonPrimitive(10),
                                    summary = "Small result set"
                                ),
                                "medium" to Example(
                                    value = JsonPrimitive(50),
                                    summary = "Medium result set"
                                ),
                                "large" to Example(
                                    value = JsonPrimitive(100),
                                    summary = "Large result set"
                                )
                            )
                        )

                        response("200", "Success") {
                            jsonContent {
                                type = SchemaType.ARRAY
                                items = Schema(ref = "#/components/schemas/Post")
                            }
                        }
                    }
                }
            }
            components {
                schema("Post") {
                    type = SchemaType.OBJECT
                    property("id", PropertyType.STRING, true)
                    property("title", PropertyType.STRING, true)
                    property("content", PropertyType.STRING, true)
                    property("status", PropertyType.STRING, true)
                }
            }
        }

        // Verify JSON output
        val json = spec.toJson()
        val jsonObject = Json.parseToJsonElement(json).jsonObject

        val getOperation = jsonObject["paths"]?.jsonObject
            ?.get("/users/{userId}/posts")?.jsonObject
            ?.get("get")?.jsonObject

        assertNotNull(getOperation)
        assertEquals("getUserPosts", getOperation["operationId"]?.jsonPrimitive?.content)

        val parameters = getOperation["parameters"]?.jsonArray
        assertNotNull(parameters)
        assertEquals(3, parameters.size)

        // Verify each parameter has examples
        parameters.forEach { param ->
            val paramObj = param.jsonObject
            val paramName = paramObj["name"]?.jsonPrimitive?.content
            assertNotNull(paramName)

            when (paramName) {
                "userId" -> {
                    val examples = paramObj["examples"]?.jsonObject
                    assertNotNull(examples)
                    assertEquals(2, examples.size)
                    assert(examples.containsKey("user1"))
                    assert(examples.containsKey("user2"))
                }

                "status" -> {
                    val examples = paramObj["examples"]?.jsonObject
                    assertNotNull(examples)
                    assertEquals(3, examples.size)
                    assert(examples.containsKey("published"))
                    assert(examples.containsKey("draft"))
                    assert(examples.containsKey("archived"))
                }

                "limit" -> {
                    val examples = paramObj["examples"]?.jsonObject
                    assertNotNull(examples)
                    assertEquals(3, examples.size)
                    assert(examples.containsKey("small"))
                    assert(examples.containsKey("medium"))
                    assert(examples.containsKey("large"))

                    // Verify integer values
                    assertEquals(10, examples["small"]?.jsonObject?.get("value")?.jsonPrimitive?.int)
                    assertEquals(50, examples["medium"]?.jsonObject?.get("value")?.jsonPrimitive?.int)
                    assertEquals(100, examples["large"]?.jsonObject?.get("value")?.jsonPrimitive?.int)
                }
            }
        }

        // Verify YAML output contains examples
        val yaml = spec.toYaml()
        assert(yaml.contains("examples:"))
        assert(yaml.contains("UUID example"))
        assert(yaml.contains("Published posts"))
        assert(yaml.contains("Small result set"))
    }

    @Test
    fun testResponseWithHeadersInJsonOutput() {
        val spec = openApi {
            info {
                title = "API with Response Headers"
                version = "1.0.0"
            }
            paths {
                path("/users") {
                    get {
                        summary = "List users"
                        response("200", "Success") {
                            jsonContent("UserList")
                            header("X-Total-Count", "Total number of users", SchemaType.INTEGER)
                            header("X-Page-Size", "Number of users per page", SchemaType.INTEGER)
                        }
                    }
                    post {
                        summary = "Create user"
                        response("201", "Created") {
                            header("Location", "URL of created resource", SchemaType.STRING, true)
                        }
                    }
                }
            }
            components {
                schema("UserList") {
                    type = SchemaType.ARRAY
                    items = Schema(ref = "#/components/schemas/User")
                }
                schema("User") {
                    type = SchemaType.OBJECT
                    property("id", PropertyType.STRING, true)
                    property("name", PropertyType.STRING, true)
                }
            }
        }

        val json = spec.toJson()
        val jsonObject = Json.parseToJsonElement(json).jsonObject

        // Check GET response headers
        val getResponse = jsonObject["paths"]?.jsonObject
            ?.get("/users")?.jsonObject
            ?.get("get")?.jsonObject
            ?.get("responses")?.jsonObject
            ?.get("200")?.jsonObject

        assertNotNull(getResponse)
        val getHeaders = getResponse["headers"]?.jsonObject
        assertNotNull(getHeaders)
        assertEquals(2, getHeaders.size)

        // Check X-Total-Count header
        val totalCountHeader = getHeaders["X-Total-Count"]?.jsonObject
        assertNotNull(totalCountHeader)
        assertEquals("Total number of users", totalCountHeader["description"]?.jsonPrimitive?.content)
        assertEquals("integer", totalCountHeader["schema"]?.jsonObject?.get("type")?.jsonPrimitive?.content)

        // Check X-Page-Size header
        val pageSizeHeader = getHeaders["X-Page-Size"]?.jsonObject
        assertNotNull(pageSizeHeader)
        assertEquals("Number of users per page", pageSizeHeader["description"]?.jsonPrimitive?.content)
        assertEquals("integer", pageSizeHeader["schema"]?.jsonObject?.get("type")?.jsonPrimitive?.content)

        // Check POST response headers
        val postResponse = jsonObject["paths"]?.jsonObject
            ?.get("/users")?.jsonObject
            ?.get("post")?.jsonObject
            ?.get("responses")?.jsonObject
            ?.get("201")?.jsonObject

        assertNotNull(postResponse)
        val postHeaders = postResponse["headers"]?.jsonObject
        assertNotNull(postHeaders)
        assertEquals(1, postHeaders.size)

        // Check Location header
        val locationHeader = postHeaders["Location"]?.jsonObject
        assertNotNull(locationHeader)
        assertEquals("URL of created resource", locationHeader["description"]?.jsonPrimitive?.content)
        assertEquals(true, locationHeader["required"]?.jsonPrimitive?.boolean)
        assertEquals("string", locationHeader["schema"]?.jsonObject?.get("type")?.jsonPrimitive?.content)
    }

    @Test
    fun testResponseWithHeadersInYamlOutput() {
        val spec = openApi {
            info {
                title = "YAML API with Headers"
                version = "1.0.0"
            }
            paths {
                path("/items/{id}") {
                    get {
                        summary = "Get item by ID"
                        response("200", "Success") {
                            jsonContent {
                                type = SchemaType.OBJECT
                            }
                            header("X-Rate-Limit", "Requests per hour", SchemaType.INTEGER)
                            header("X-Rate-Remaining", "Remaining requests", SchemaType.INTEGER)
                        }
                        response("429", "Too Many Requests") {
                            header("Retry-After", "Seconds until next request allowed", SchemaType.INTEGER, true)
                        }
                    }
                }
            }
        }

        val yaml = spec.toYaml()

        // Verify headers are present in YAML output
        assert(yaml.contains("headers:"))
        assert(yaml.contains("\"X-Rate-Limit\":"))
        assert(yaml.contains("description: \"Requests per hour\""))
        assert(yaml.contains("\"X-Rate-Remaining\":"))
        assert(yaml.contains("description: \"Remaining requests\""))
        assert(yaml.contains("\"Retry-After\":"))
        assert(yaml.contains("description: \"Seconds until next request allowed\""))
        assert(yaml.contains("required: true"))
        assert(yaml.contains("type: \"integer\""))
    }

    @Test
    fun testResponseWithComplexHeaders() {
        val spec = openApi {
            info {
                title = "API with Complex Headers"
                version = "1.0.0"
            }
            paths {
                path("/data") {
                    get {
                        summary = "Get data with complex headers"
                        response("200", "Success") {
                            jsonContent("DataResponse")
                            header("X-Custom-Header") {
                                description = "A custom header with examples"
                                required = true
                                deprecated = true
                                schema {
                                    type = SchemaType.STRING
                                    format = me.farshad.dsl.spec.SchemaFormat.EMAIL
                                }
                                example("user@example.com")
                            }
                            header("X-Api-Version", "ApiVersion", "The API version used", false)
                        }
                    }
                }
            }
            components {
                schema("DataResponse") {
                    type = SchemaType.OBJECT
                    property("data", PropertyType.STRING, true)
                }
                schema("ApiVersion") {
                    type = SchemaType.STRING
                    description = "API version (v1, v2, or v3)"
                }
            }
        }

        val json = spec.toJson()
        val jsonObject = Json.parseToJsonElement(json).jsonObject

        val response = jsonObject["paths"]?.jsonObject
            ?.get("/data")?.jsonObject
            ?.get("get")?.jsonObject
            ?.get("responses")?.jsonObject
            ?.get("200")?.jsonObject

        assertNotNull(response)
        val headers = response["headers"]?.jsonObject
        assertNotNull(headers)
        assertEquals(2, headers.size)

        // Check X-Custom-Header
        val customHeader = headers["X-Custom-Header"]?.jsonObject
        assertNotNull(customHeader)
        assertEquals("A custom header with examples", customHeader["description"]?.jsonPrimitive?.content)
        assertEquals(true, customHeader["required"]?.jsonPrimitive?.boolean)
        assertEquals(true, customHeader["deprecated"]?.jsonPrimitive?.boolean)
        assertEquals("string", customHeader["schema"]?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals("email", customHeader["schema"]?.jsonObject?.get("format")?.jsonPrimitive?.content)
        assertEquals("user@example.com", customHeader["example"]?.jsonPrimitive?.content)

        // Check X-Api-Version header with ref
        val apiVersionHeader = headers["X-Api-Version"]?.jsonObject
        assertNotNull(apiVersionHeader)
        assertEquals("The API version used", apiVersionHeader["description"]?.jsonPrimitive?.content)
        assertEquals("#/components/schemas/ApiVersion", apiVersionHeader["schema"]?.jsonObject?.get("\$ref")?.jsonPrimitive?.content)
    }

    @Test
    fun testResponseWithHeaderExamples() {
        val spec = openApi {
            info {
                title = "API with Header Examples"
                version = "1.0.0"
            }
            paths {
                path("/auth/login") {
                    post {
                        summary = "User login"
                        response("200", "Login successful") {
                            header("Authorization") {
                                description = "Bearer token for authentication"
                                schema {
                                    type = SchemaType.STRING
                                }
                                examples {
                                    example("bearer") {
                                        summary = "Bearer token example"
                                        value("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                                    }
                                    example("expired") {
                                        summary = "Expired token example"
                                        description = "This token has expired"
                                        value("Bearer expired-token-example")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val json = spec.toJson()
        val jsonObject = Json.parseToJsonElement(json).jsonObject

        val authHeader = jsonObject["paths"]?.jsonObject
            ?.get("/auth/login")?.jsonObject
            ?.get("post")?.jsonObject
            ?.get("responses")?.jsonObject
            ?.get("200")?.jsonObject
            ?.get("headers")?.jsonObject
            ?.get("Authorization")?.jsonObject

        assertNotNull(authHeader)
        val examples = authHeader["examples"]?.jsonObject
        assertNotNull(examples)
        assertEquals(2, examples.size)

        // Check bearer example
        val bearerExample = examples["bearer"]?.jsonObject
        assertNotNull(bearerExample)
        assertEquals("Bearer token example", bearerExample["summary"]?.jsonPrimitive?.content)
        assert(bearerExample["value"]?.jsonPrimitive?.content?.startsWith("Bearer eyJ") ?: false)

        // Check expired example
        val expiredExample = examples["expired"]?.jsonObject
        assertNotNull(expiredExample)
        assertEquals("Expired token example", expiredExample["summary"]?.jsonPrimitive?.content)
        assertEquals("This token has expired", expiredExample["description"]?.jsonPrimitive?.content)
    }


//    ----------------


    @Test
    fun testResponseWithComplexHeaders2() {
        val spec = openApi {
            info {
                title = "API with Complex Headers"
                version = "1.0.0"
            }
            paths {
                path("/data") {
                    get {
                        summary = "Get data with complex headers"
                        response("200", "Success") {
                            header("X-Custom-Header") {
                                description = "A custom header with examples"
                                required = true
                                deprecated = true
                                schema {
                                    type = SchemaType.STRING
                                    format = me.farshad.dsl.spec.SchemaFormat.EMAIL
                                }
                                example("user@example.com")
                            }
                            header("X-Api-Version", "ApiVersion", "The API version used", false)
                        }
                    }
                }
            }
            components {
                schema("DataResponse") {
                    type = SchemaType.OBJECT
                    property("data", PropertyType.STRING, true)
                }
                schema("ApiVersion") {
                    type = SchemaType.STRING
                    description = "API version (v1, v2, or v3)"
                }
            }
        }

        val json = spec.toJson()
        val jsonObject = Json.parseToJsonElement(json).jsonObject

        val response = jsonObject["paths"]?.jsonObject
            ?.get("/data")?.jsonObject
            ?.get("get")?.jsonObject
            ?.get("responses")?.jsonObject
            ?.get("200")?.jsonObject

        assertNotNull(response)
        val headers = response["headers"]?.jsonObject
        assertNotNull(headers)
        assertEquals(2, headers.size)

        // Check X-Custom-Header
        val customHeader = headers["X-Custom-Header"]?.jsonObject
        assertNotNull(customHeader)
        assertEquals("A custom header with examples", customHeader["description"]?.jsonPrimitive?.content)
        assertEquals(true, customHeader["required"]?.jsonPrimitive?.boolean)
        assertEquals(true, customHeader["deprecated"]?.jsonPrimitive?.boolean)
        assertEquals("string", customHeader["schema"]?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals("email", customHeader["schema"]?.jsonObject?.get("format")?.jsonPrimitive?.content)
        assertEquals("user@example.com", customHeader["example"]?.jsonPrimitive?.content)

        // Check X-Api-Version header with ref
        val apiVersionHeader = headers["X-Api-Version"]?.jsonObject
        assertNotNull(apiVersionHeader)
        assertEquals("The API version used", apiVersionHeader["description"]?.jsonPrimitive?.content)
        assertEquals("#/components/schemas/ApiVersion", apiVersionHeader["schema"]?.jsonObject?.get("\$ref")?.jsonPrimitive?.content)
    }

}
