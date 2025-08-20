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
    
    // Memoization: Track classes currently being processed to avoid infinite recursion
    private val schemasInProgress = mutableSetOf<KClass<*>>()
    
    // Configuration options for automatic schema generation
    var autoGenerateArrayItems: Boolean = true
    var autoGenerateEnumValues: Boolean = true
    var defaultDiscriminatorProperty: String = "type"

    fun schema(
        name: String,
        block: SchemaBuilder.() -> Unit,
    ) {
        schemas[name] = SchemaBuilder().apply(block).build()
    }

    fun schema(kClass: KClass<*>, customDiscriminator: String? = null) {
        // Check if schema already exists (memoization)
        val schemaName = kClass.simpleName ?: return
        if (schemas.containsKey(schemaName)) {
            return
        }
        
        // Check for circular dependencies
        if (schemasInProgress.contains(kClass)) {
            // Create a reference placeholder for circular dependencies
            schemas[schemaName] = Schema(ref = "#/components/schemas/$schemaName")
            return
        }
        
        // Mark this class as being processed
        schemasInProgress.add(kClass)
        
        try {
            generateSchema(kClass, customDiscriminator)
        } finally {
            // Always remove from in-progress set
            schemasInProgress.remove(kClass)
        }
    }
    
    private fun generateSchema(kClass: KClass<*>, customDiscriminator: String?) {
        // Handle sealed classes
        if (kClass.isSealed) {
            if (generateSealedClassSchema(kClass, customDiscriminator)) {
                return
            }
        }

        // Handle enum classes
        if (!kClass.isSealed && kClass.java.isEnum) {
            generateEnumSchema(kClass)
            return
        }

        // Handle regular object classes
        generateObjectSchema(kClass)
    }
    
    private fun generateSealedClassSchema(kClass: KClass<*>, customDiscriminator: String?): Boolean {
        val sealedSubclasses = getSealedSubclasses(kClass)
        
        if (sealedSubclasses.isEmpty()) {
            return false
        }
        
        val schemaBuilder = SchemaBuilder()
        
        // Register all subclass schemas first
        registerSubclassSchemas(sealedSubclasses)
        
        // Create discriminated union
        schemaBuilder.oneOf(*sealedSubclasses.toTypedArray())
        
        // Set up discriminator
        val discriminatorProperty = customDiscriminator ?: defaultDiscriminatorProperty
        schemaBuilder.discriminator(discriminatorProperty) {
            sealedSubclasses.forEach { subclass ->
                subclass.simpleName?.let { subclassName ->
                    mapping(subclassName, subclass)
                }
            }
        }
        
        // Add description if available
        applySchemaDescription(kClass, schemaBuilder)
        
        schemas[kClass.simpleName!!] = schemaBuilder.build()
        return true
    }
    
    private fun getSealedSubclasses(kClass: KClass<*>): List<KClass<*>> {
        return try {
            // Try to access sealedSubclasses property using different approaches
            val sealedSubclassesProperty = kClass::class.members.find { it.name == "sealedSubclasses" }
            if (sealedSubclassesProperty != null) {
                @Suppress("UNCHECKED_CAST")
                (sealedSubclassesProperty.call(kClass) as? Collection<KClass<*>>)?.toList() ?: emptyList()
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
            kClass.nestedClasses.toList()
        }
    }
    
    private fun registerSubclassSchemas(subclasses: List<KClass<*>>) {
        subclasses.forEach { subclass ->
            subclass.simpleName?.let { subclassName ->
                if (!schemas.containsKey(subclassName)) {
                    schema(subclass)
                }
            }
        }
    }
    
    private fun generateEnumSchema(kClass: KClass<*>) {
        val schemaBuilder = SchemaBuilder()
        schemaBuilder.type = SchemaType.STRING

        // Extract enum values if auto-generation is enabled
        if (autoGenerateEnumValues) {
            val enumValues = kClass.java.enumConstants.map { it.toString() }
            schemaBuilder.enumValues = enumValues
        }

        // Add description if available
        applySchemaDescription(kClass, schemaBuilder)

        schemas[kClass.simpleName!!] = schemaBuilder.build()
    }
    
    private fun generateObjectSchema(kClass: KClass<*>) {
        val schemaBuilder = SchemaBuilder()
        schemaBuilder.type = SchemaType.OBJECT
        val tempRequiredFields = mutableListOf<String>()

        // Add description if available
        applySchemaDescription(kClass, schemaBuilder)

        // Process each property
        kClass.declaredMemberProperties.forEach { prop ->
            processProperty(prop, schemaBuilder, tempRequiredFields)
        }

        // Build final schema with required fields
        val finalSchema = buildFinalSchema(schemaBuilder, tempRequiredFields)
        schemas[kClass.simpleName!!] = finalSchema
    }
    
    private fun processProperty(
        prop: kotlin.reflect.KProperty1<*, *>,
        schemaBuilder: SchemaBuilder,
        tempRequiredFields: MutableList<String>
    ) {
        val propClassifier = prop.returnType.classifier as? KClass<*>
        val propType = determinePropertyType(prop.returnType.classifier)
        val propertyDescription = getPropertyDescription(prop)

        if (shouldCreateReference(propType, propClassifier)) {
            processReferenceProperty(prop, propClassifier!!, schemaBuilder, tempRequiredFields)
        } else {
            processInlineProperty(prop, propType, propertyDescription, schemaBuilder)
        }
    }
    
    private fun determinePropertyType(classifier: Any?): PropertyType {
        return when (classifier) {
            List::class -> PropertyType.ARRAY
            String::class -> PropertyType.STRING
            Int::class, Long::class -> PropertyType.INTEGER
            Double::class, Float::class -> PropertyType.NUMBER
            Boolean::class -> PropertyType.BOOLEAN
            else -> PropertyType.OBJECT
        }
    }
    
    private fun shouldCreateReference(propType: PropertyType, propClassifier: KClass<*>?): Boolean {
        if (propType != PropertyType.OBJECT || propClassifier == null) {
            return false
        }
        
        val primitiveTypes = listOf(
            String::class, Int::class, Long::class, Double::class, Float::class, Boolean::class,
            List::class, Map::class, MutableMap::class, HashMap::class, LinkedHashMap::class,
            Collection::class, MutableCollection::class, MutableList::class,
            Set::class, MutableSet::class, Any::class
        )
        
        return propClassifier !in primitiveTypes
    }
    
    private fun processReferenceProperty(
        prop: kotlin.reflect.KProperty1<*, *>,
        propClassifier: KClass<*>,
        schemaBuilder: SchemaBuilder,
        tempRequiredFields: MutableList<String>
    ) {
        propClassifier.simpleName?.let { nestedSchemaName ->
            if (!schemas.containsKey(nestedSchemaName)) {
                schema(propClassifier)
            }

            schemaBuilder.properties[prop.name] = Schema(ref = "#/components/schemas/$nestedSchemaName")

            if (!prop.returnType.isMarkedNullable) {
                tempRequiredFields.add(prop.name)
            }
        }
    }
    
    private fun processInlineProperty(
        prop: kotlin.reflect.KProperty1<*, *>,
        propType: PropertyType,
        propertyDescription: String?,
        schemaBuilder: SchemaBuilder
    ) {
        schemaBuilder.property(prop.name, propType, !prop.returnType.isMarkedNullable) {
            propertyDescription?.let { this.description = it }

            if (propType == PropertyType.ARRAY && autoGenerateArrayItems) {
                processArrayItems(prop, this)
            }
        }
    }
    
    private fun processArrayItems(
        prop: kotlin.reflect.KProperty1<*, *>,
        propertyBuilder: SchemaBuilder
    ) {
        val typeArguments = prop.returnType.arguments
        if (typeArguments.isEmpty()) return
        
        val itemType = typeArguments.first().type?.jvmErasure ?: return
        
        propertyBuilder.items = when (itemType) {
            String::class -> Schema(type = SchemaType.STRING)
            Int::class, Long::class -> Schema(type = SchemaType.INTEGER)
            Double::class, Float::class -> Schema(type = SchemaType.NUMBER)
            Boolean::class -> Schema(type = SchemaType.BOOLEAN)
            List::class, MutableList::class, Collection::class, Set::class -> {
                processNestedArray(typeArguments)
            }
            else -> processCustomArrayItem(itemType)
        }
    }
    
    private fun processNestedArray(typeArguments: List<kotlin.reflect.KTypeProjection>): Schema {
        val nestedTypeArgs = typeArguments.first().type?.arguments
        if (nestedTypeArgs?.isNotEmpty() == true) {
            val nestedItemType = nestedTypeArgs.first().type?.jvmErasure
            return Schema(
                type = SchemaType.ARRAY,
                items = when (nestedItemType) {
                    String::class -> Schema(type = SchemaType.STRING)
                    Int::class, Long::class -> Schema(type = SchemaType.INTEGER)
                    Double::class, Float::class -> Schema(type = SchemaType.NUMBER)
                    Boolean::class -> Schema(type = SchemaType.BOOLEAN)
                    else -> nestedItemType?.simpleName?.let { typeName ->
                        if (!schemas.containsKey(typeName)) {
                            schema(nestedItemType)
                        }
                        Schema(ref = "#/components/schemas/$typeName")
                    }
                }
            )
        }
        return Schema(type = SchemaType.ARRAY)
    }
    
    private fun processCustomArrayItem(itemType: KClass<*>): Schema? {
        return itemType.simpleName?.let { itemTypeName ->
            if (!schemas.containsKey(itemTypeName)) {
                schema(itemType)
            }
            Schema(ref = "#/components/schemas/$itemTypeName")
        }
    }
    
    private fun getPropertyDescription(prop: kotlin.reflect.KProperty1<*, *>): String? {
        return prop.annotations.find { it is PropertyDescription }?.let { annotation ->
            (annotation as PropertyDescription).value
        }
    }
    
    private fun applySchemaDescription(kClass: KClass<*>, schemaBuilder: SchemaBuilder) {
        kClass.annotations.find { it is SchemaDescription }?.let { annotation ->
            schemaBuilder.description = (annotation as SchemaDescription).value
        }
    }
    
    private fun buildFinalSchema(
        schemaBuilder: SchemaBuilder,
        tempRequiredFields: List<String>
    ): Schema {
        val builtSchema = schemaBuilder.build()
        
        return if (tempRequiredFields.isNotEmpty()) {
            val existingRequired = builtSchema.required ?: emptyList()
            val allRequired = (existingRequired + tempRequiredFields).distinct()
            builtSchema.copy(required = allRequired.takeIf { it.isNotEmpty() })
        } else {
            builtSchema
        }
    }

    // Convenience method for configuring automatic schema generation
    fun configureAutoGeneration(block: ComponentsBuilder.() -> Unit) {
        this.block()
    }
    
    // Builder method for sealed class with custom discriminator
    fun sealedClassSchema(kClass: KClass<*>, discriminator: String) {
        schema(kClass, discriminator)
    }
    
    // Builder method to disable specific auto-generation features
    fun schemaWithoutAutoItems(kClass: KClass<*>) {
        val previousSetting = autoGenerateArrayItems
        try {
            autoGenerateArrayItems = false
            schema(kClass)
        } finally {
            autoGenerateArrayItems = previousSetting
        }
    }
    
    fun schemaWithoutEnumValues(kClass: KClass<*>) {
        val previousSetting = autoGenerateEnumValues
        try {
            autoGenerateEnumValues = false
            schema(kClass)
        } finally {
            autoGenerateEnumValues = previousSetting
        }
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
