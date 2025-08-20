# Kotlin OpenAPI DSL - Builder Classes Reference

This directory contains comprehensive documentation for all builder classes in the Kotlin OpenAPI DSL library. Each builder is documented in its own file with detailed explanations, properties, methods, and usage examples.

## Builder Classes by Category

### Core Builders
- [**OpenApiBuilder**](src/main/kotlin/me/farshad/dsl/builder/core/OpenApiBuilder.md) - Main entry point for creating OpenAPI specifications

### Info Builders
- [**InfoBuilder**](src/main/kotlin/me/farshad/dsl/builder/info/InfoBuilder.md) - Builds API information metadata
- [**ContactBuilder**](src/main/kotlin/me/farshad/dsl/builder/info/ContactBuilder.md) - Builds contact information
- [**ServerBuilder**](src/main/kotlin/me/farshad/dsl/builder/info/ServerBuilder.md) - Builds server configurations with variables

### Path Builders
- [**PathsBuilder**](src/main/kotlin/me/farshad/dsl/builder/paths/PathsBuilder.md) - Manages API paths and endpoints
- [**PathItemBuilder**](src/main/kotlin/me/farshad/dsl/builder/paths/PathItemBuilder.md) - Builds individual path items with operations
- [**OperationBuilder**](src/main/kotlin/me/farshad/dsl/builder/paths/OperationBuilder.md) - Builds API operations (GET, POST, etc.)

### Schema Builders
- [**SchemaBuilder**](src/main/kotlin/me/farshad/dsl/builder/schema/SchemaBuilder.md) - Comprehensive schema definition builder
- [**OneOfBuilder**](src/main/kotlin/me/farshad/dsl/builder/schema/OneOfBuilder.md) - Builds oneOf schema compositions
- [**AllOfBuilder**](src/main/kotlin/me/farshad/dsl/builder/schema/AllOfBuilder.md) - Builds allOf schema compositions
- [**AnyOfBuilder**](src/main/kotlin/me/farshad/dsl/builder/schema/AnyOfBuilder.md) - Builds anyOf schema compositions
- [**DiscriminatorBuilder**](src/main/kotlin/me/farshad/dsl/builder/schema/DiscriminatorBuilder.md) - Configures discriminators for polymorphic types

### Request/Response Builders
- [**RequestBodyBuilder**](src/main/kotlin/me/farshad/dsl/builder/request/RequestBodyBuilder.md) - Builds request body configurations
- [**ResponseBuilder**](src/main/kotlin/me/farshad/dsl/builder/response/ResponseBuilder.md) - Builds response configurations

### Component Builders
- [**ComponentsBuilder**](src/main/kotlin/me/farshad/dsl/builder/components/ComponentsBuilder.md) - Manages reusable components (schemas, examples, etc.)

### Example Builders
- [**ExampleBuilder**](src/main/kotlin/me/farshad/dsl/builder/example/ExampleBuilder.md) - Builds individual examples
- [**ExamplesBuilder**](src/main/kotlin/me/farshad/dsl/builder/example/ExamplesBuilder.md) - Manages collections of examples

## Quick Reference

| Builder | Purpose | Key Features |
|---------|---------|--------------|
| **OpenApiBuilder** | Main specification builder | Orchestrates all builders, JSON/YAML export |
| **InfoBuilder** | API metadata | Title, version, description, contact, license |
| **ContactBuilder** | Contact details | Name, email, URL |
| **ServerBuilder** | Server configuration | URL templates, variables, descriptions |
| **PathsBuilder** | Path management | HTTP endpoints and operations |
| **PathItemBuilder** | Single path configuration | All HTTP methods, shared parameters |
| **OperationBuilder** | Operation details | Parameters, request/response, security |
| **SchemaBuilder** | Schema definitions | Types, validation, composition, references |
| **OneOfBuilder** | Exclusive choice schemas | Exactly one schema must match |
| **AllOfBuilder** | Schema intersection | All schemas must match (inheritance) |
| **AnyOfBuilder** | Schema union | One or more schemas must match |
| **DiscriminatorBuilder** | Polymorphism support | Type discrimination for oneOf/anyOf |
| **RequestBodyBuilder** | Request configuration | Content types, schemas, examples |
| **ResponseBuilder** | Response configuration | Status codes, headers, content |
| **ComponentsBuilder** | Reusable definitions | Schemas, examples, parameters, etc. |
| **ExampleBuilder** | Single example | Value, summary, description |
| **ExamplesBuilder** | Multiple examples | Named examples for various scenarios |

## Usage Example

Here's a quick example showing how these builders work together:

```kotlin
val spec = openApi {
    openapi = "3.1.0"
    
    info {
        title = "My API"
        version = "1.0.0"
        contact {
            name = "API Support"
            email = "support@example.com"
        }
    }
    
    servers {
        server("https://api.example.com") {
            description = "Production server"
        }
    }
    
    paths {
        path("/users") {
            get {
                summary = "List users"
                response("200", "Success") {
                    jsonContent {
                        type = "array"
                        items { ref("User") }
                    }
                }
            }
            
            post {
                summary = "Create user"
                requestBody {
                    required = true
                    jsonContent(CreateUserRequest::class)
                }
                response("201", "Created") {
                    jsonContent(User::class)
                }
            }
        }
    }
    
    components {
        schema<User>()
        schema<CreateUserRequest>()
    }
}

// Export as JSON or YAML
val json = spec.toJson()
val yaml = spec.toYaml()
```

## Automatic Schema Generation

The library provides powerful automatic schema generation features:

### Sealed Class Support

Sealed classes are automatically converted to OpenAPI discriminated unions:

```kotlin
@Serializable
sealed class PaymentMethod {
    @Serializable
    data class CreditCard(val number: String, val type: String = "CreditCard") : PaymentMethod()
    
    @Serializable
    data class BankTransfer(val accountNumber: String, val type: String = "BankTransfer") : PaymentMethod()
    
    @Serializable
    data class PayPal(val email: String, val type: String = "PayPal") : PaymentMethod()
}

val spec = openApi {
    components {
        schema(PaymentMethod::class) // Automatically creates discriminated union
    }
}
```

This generates:
- A `oneOf` schema with references to all subclasses
- Automatic discriminator with "type" property
- Individual schemas for each subclass
- Proper discriminator mapping

### Enum Support

Enum classes are automatically converted to string schemas with enum values:

```kotlin
enum class UserStatus {
    ACTIVE, INACTIVE, PENDING
}

// Automatically generates schema with enum: ["ACTIVE", "INACTIVE", "PENDING"]
```

### Array Item Detection

Generic array types are automatically handled:

```kotlin
@Serializable
data class UserList(val users: List<User>) // Automatically detects User as array items
```

## Annotation Classes

In addition to builders, the library provides annotations for enhancing schema generation:

- [**SchemaDescription**](src/main/kotlin/me/farshad/dsl/annotation/SchemaDescription.md) - Class-level descriptions for schemas  
- [**PropertyDescription**](src/main/kotlin/me/farshad/dsl/annotation/PropertyDescription.md) - Property-level descriptions for schema fields

## Package Structure

```
me.farshad.dsl.
├── annotation/          # Annotation classes
│   └── Annotations.kt
├── builder/            # All builder classes
│   ├── components/     # ComponentsBuilder
│   ├── core/          # OpenApiBuilder and extensions
│   ├── example/       # Example builders
│   ├── info/          # Info-related builders
│   ├── paths/         # Path and operation builders
│   ├── request/       # Request body builder
│   ├── response/      # Response builder
│   └── schema/        # Schema and composition builders
├── serializer/        # Custom serializers
└── spec/             # Data model specifications
```

## Getting Started

1. Start with [OpenApiBuilder](src/main/kotlin/me/farshad/dsl/builder/core/OpenApiBuilder.md) to understand the main entry point
2. Learn about [SchemaBuilder](src/main/kotlin/me/farshad/dsl/builder/schema/SchemaBuilder.md) for defining data models
3. Explore [PathsBuilder](src/main/kotlin/me/farshad/dsl/builder/paths/PathsBuilder.md) and [OperationBuilder](src/main/kotlin/me/farshad/dsl/builder/paths/OperationBuilder.md) for API endpoints
4. Use [ComponentsBuilder](src/main/kotlin/me/farshad/dsl/builder/components/ComponentsBuilder.md) for reusable definitions
5. Refer to specific builders as needed for detailed configurations

## Additional Resources

- For schema composition patterns, see the composition builders (OneOf, AllOf, AnyOf)
- For API examples, check [ExampleBuilder](src/main/kotlin/me/farshad/dsl/builder/example/ExampleBuilder.md) and [ExamplesBuilder](src/main/kotlin/me/farshad/dsl/builder/example/ExamplesBuilder.md)
- For request/response configuration, see [RequestBodyBuilder](src/main/kotlin/me/farshad/dsl/builder/request/RequestBodyBuilder.md) and [ResponseBuilder](src/main/kotlin/me/farshad/dsl/builder/response/ResponseBuilder.md)