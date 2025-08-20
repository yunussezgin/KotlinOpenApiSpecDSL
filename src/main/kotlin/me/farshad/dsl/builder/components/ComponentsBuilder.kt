package me.farshad.dsl.builder.components

import me.farshad.dsl.annotation.PropertyDescription
import me.farshad.dsl.annotation.SchemaDescription
import me.farshad.dsl.builder.example.ExampleBuilder
import me.farshad.dsl.builder.schema.DiscriminatorBuilder
import me.farshad.dsl.builder.schema.SchemaBuilder
import me.farshad.dsl.builder.utils.toJsonElement
import me.farshad.dsl.spec.Components
import me.farshad.dsl.spec.Example
import me.farshad.dsl.spec.PropertyType
import me.farshad.dsl.spec.Schema
import me.farshad.dsl.spec.SchemaType
import me.farshad.dsl.spec.SecurityScheme
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.jvm.jvmErasure

class ComponentsBuilder {
    private val schemas = mutableMapOf<String, Schema>()
    private val securitySchemes = mutableMapOf<String, SecurityScheme>()
    private val examples = mutableMapOf<String, Example>()

    fun schema(
        name: String,
        block: SchemaBuilder.() -> Unit,
    ) {
        schemas[name] = SchemaBuilder().apply(block).build()
    }

    fun schema(kClass: KClass<*>) {
        val schemaBuilder = SchemaBuilder()

        // Check if this is a sealed class
        if (kClass.isSealed) {
            // For sealed classes, we need to use reflection to get sealed subclasses
            val sealedSubclasses = try {
                // Try to access sealedSubclasses property using different approaches
                val sealedSubclassesProperty = kClass::class.members.find { it.name == "sealedSubclasses" }
                if (sealedSubclassesProperty != null) {
                    @Suppress("UNCHECKED_CAST")
                    sealedSubclassesProperty.call(kClass) as? Collection<KClass<*>> ?: emptyList()
                } else {
                    // Alternative: check nested classes that are subclasses
                    kClass.nestedClasses.filter { nested ->
                        nested.supertypes.any { supertype -> 
                            supertype.classifier == kClass 
                        }
                    }
                }
            } catch (e: Exception) {
                // Final fallback: assume all nested classes are subclasses for sealed classes
                kClass.nestedClasses
            }
            
            if (sealedSubclasses.isNotEmpty()) {
                // Register all subclass schemas first
                sealedSubclasses.forEach { subclass ->
                    subclass.simpleName?.let { subclassName ->
                        if (!schemas.containsKey(subclassName)) {
                            schema(subclass)
                        }
                    }
                }
                
                // Create discriminated union using oneOf with a default discriminator
                schemaBuilder.oneOf(*sealedSubclasses.toTypedArray())
                
                // Set up discriminator - use "type" as the default discriminator property
                val discriminatorProperty = "type"
                schemaBuilder.discriminator(discriminatorProperty) {
                    sealedSubclasses.forEach { subclass ->
                        subclass.simpleName?.let { subclassName ->
                            // Map the class simple name to its schema reference
                            mapping(subclassName, subclass)
                        }
                    }
                }
                
                // Check for class-level description annotation
                kClass.annotations.find { it is SchemaDescription }?.let { annotation ->
                    schemaBuilder.description = (annotation as SchemaDescription).value
                }
                
                schemas[kClass.simpleName!!] = schemaBuilder.build()
                return
            }
        }

        // Check if this is an enum class
        if (kClass.isSealed == false && kClass.java.isEnum) {
            schemaBuilder.type = SchemaType.STRING

            // Extract enum values
            val enumValues = kClass.java.enumConstants.map { it.toString() }
            schemaBuilder.enumValues = enumValues

            // Check for class-level ApiDescription annotation
            kClass.annotations.find { it is SchemaDescription }?.let { annotation ->
                schemaBuilder.description = (annotation as SchemaDescription).value
            }

            schemas[kClass.simpleName!!] = schemaBuilder.build()
            return
        }

        schemaBuilder.type = SchemaType.OBJECT
        val tempRequiredFields = mutableListOf<String>()

        // Check for class-level ApiDescription annotation
        kClass.annotations.find { it is SchemaDescription }?.let { annotation ->
            schemaBuilder.description = (annotation as SchemaDescription).value
        }

        kClass.declaredMemberProperties.forEach { prop ->
            val propClassifier = prop.returnType.classifier as? KClass<*>
            val propType =
                when {
                    prop.returnType.classifier == List::class -> PropertyType.ARRAY
                    prop.returnType.classifier == String::class -> PropertyType.STRING
                    prop.returnType.classifier == Int::class || prop.returnType.classifier == Long::class -> PropertyType.INTEGER
                    prop.returnType.classifier == Double::class || prop.returnType.classifier == Float::class -> PropertyType.NUMBER
                    prop.returnType.classifier == Boolean::class -> PropertyType.BOOLEAN
                    else -> PropertyType.OBJECT
                }

            // Check for property-level PropertyDescription annotation
            val propertyDescription =
                prop.annotations.find { it is PropertyDescription }?.let { annotation ->
                    (annotation as PropertyDescription).value
                }

            // For custom objects (non-primitives and enums), create $ref and register the nested schema
            if (propType == PropertyType.OBJECT && propClassifier != null &&
                propClassifier !in listOf(String::class, Int::class, Long::class, Double::class, Float::class, Boolean::class,
                                           List::class, Map::class, MutableMap::class, HashMap::class, LinkedHashMap::class,
                                           Collection::class, MutableCollection::class, MutableList::class,
                                           Set::class, MutableSet::class, Any::class)) {

                // Register the nested schema if not already registered (including enums)
                propClassifier.simpleName?.let { nestedSchemaName ->
                    if (!schemas.containsKey(nestedSchemaName)) {
                        schema(propClassifier)
                    }

                    // Add property as reference instead of inline object
                    schemaBuilder.properties[prop.name] = Schema(ref = "#/components/schemas/$nestedSchemaName")

                    // Handle required fields by manually building the schema with required list
                    if (!prop.returnType.isMarkedNullable) {
                        // We'll handle the required fields in the final build step
                        tempRequiredFields.add(prop.name)
                    }
                }
            } else {
                schemaBuilder.property(prop.name, propType, !prop.returnType.isMarkedNullable) {
                    propertyDescription?.let { this.description = it }

                    // For array types, automatically add items reference to the generic type
                    if (propType == PropertyType.ARRAY) {
                        val typeArguments = prop.returnType.arguments
                        if (typeArguments.isNotEmpty()) {
                            val itemType = typeArguments.first().type?.jvmErasure
                            itemType?.let { kClass ->
                                when (kClass) {
                                    String::class -> {
                                        this.items = Schema(type = SchemaType.STRING)
                                    }
                                    Int::class, Long::class -> {
                                        this.items = Schema(type = SchemaType.INTEGER)
                                    }
                                    Double::class, Float::class -> {
                                        this.items = Schema(type = SchemaType.NUMBER)
                                    }
                                    Boolean::class -> {
                                        this.items = Schema(type = SchemaType.BOOLEAN)
                                    }
                                    else -> {
                                        // For custom/complex types and enums, create a schema reference
                                        kClass.simpleName?.let { itemTypeName ->
                                            // Register the schema if it doesn't exist yet
                                            if (!schemas.containsKey(itemTypeName)) {
                                                schema(kClass)
                                            }
                                            this.items = Schema(ref = "#/components/schemas/$itemTypeName")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Build the base schema
        val builtSchema = schemaBuilder.build()

        // If we have temp required fields for $ref properties, create a new schema with those included
        val finalSchema = if (tempRequiredFields.isNotEmpty()) {
            val existingRequired = builtSchema.required ?: emptyList()
            val allRequired = (existingRequired + tempRequiredFields).distinct()
            builtSchema.copy(required = allRequired.takeIf { it.isNotEmpty() })
        } else {
            builtSchema
        }

        schemas[kClass.simpleName!!] = finalSchema
    }

    fun securityScheme(
        name: String,
        type: String,
        scheme: String? = null,
        bearerFormat: String? = null,
    ) {
        securitySchemes[name] = SecurityScheme(type, scheme, bearerFormat)
    }

    fun example(
        name: String,
        block: ExampleBuilder.() -> Unit,
    ) {
        examples[name] = ExampleBuilder().apply(block).build()
    }

    fun example(
        name: String,
        value: Any,
        summary: String? = null,
        description: String? = null,
    ) {
        examples[name] = Example(summary, description, value.toJsonElement())
    }

    fun build() =
        Components(
            schemas = schemas.takeIf { it.isNotEmpty() },
            securitySchemes = securitySchemes.takeIf { it.isNotEmpty() },
            examples = examples.takeIf { it.isNotEmpty() },
        )
}
