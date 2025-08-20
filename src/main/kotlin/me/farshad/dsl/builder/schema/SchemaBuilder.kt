package me.farshad.dsl.builder.schema

import kotlinx.serialization.json.JsonElement
import me.farshad.dsl.builder.example.ExamplesBuilder
import me.farshad.dsl.builder.utils.toJsonElement
import me.farshad.dsl.spec.Discriminator
import me.farshad.dsl.spec.Example
import me.farshad.dsl.spec.PropertyType
import me.farshad.dsl.spec.Schema
import me.farshad.dsl.spec.SchemaFormat
import me.farshad.dsl.spec.SchemaType
import kotlin.reflect.KClass

class SchemaBuilder {
    var type: SchemaType? = null
    var format: SchemaFormat? = null
    var description: String? = null
    var properties = mutableMapOf<String, Schema>()
    private val required = mutableListOf<String>()
    var items: Schema? = null
    private var oneOfInternal: MutableList<SchemaReference>? = null
    private var allOf: MutableList<SchemaReference>? = null
    private var anyOf: MutableList<SchemaReference>? = null
    private var not: SchemaReference? = null
    private var discriminator: Discriminator? = null
    var example: JsonElement? = null
    private var examples: Map<String, Example>? = null
    var enumValues: List<String>? = null

    // Backward compatibility: allow setting oneOf as List<String>
    var oneOf: List<String>?
        get() =
            oneOfInternal?.mapNotNull {
                when (it) {
                    is SchemaReference.Ref -> it.path
                    else -> null
                }
            }
        set(value) {
            oneOfInternal = value?.map { SchemaReference.Ref(it) }?.toMutableList()
        }

    fun property(
        name: String,
        type: PropertyType,
        required: Boolean = false,
        block: SchemaBuilder.() -> Unit = {},
    ) {
        properties[name] =
            SchemaBuilder()
                .apply {
                    this.type =
                        when (type) {
                            PropertyType.STRING -> SchemaType.STRING
                            PropertyType.NUMBER -> SchemaType.NUMBER
                            PropertyType.INTEGER -> SchemaType.INTEGER
                            PropertyType.BOOLEAN -> SchemaType.BOOLEAN
                            PropertyType.ARRAY -> SchemaType.ARRAY
                            PropertyType.OBJECT -> SchemaType.OBJECT
                            PropertyType.NULL -> SchemaType.NULL
                        }
                    block()
                }.build()
        if (required) {
            this.required.add(name)
        }
    }

    fun items(block: SchemaBuilder.() -> Unit) {
        items = SchemaBuilder().apply(block).build()
    }

    fun example(value: Any) {
        example = value.toJsonElement()
    }

    fun examples(block: ExamplesBuilder.() -> Unit) {
        examples = ExamplesBuilder().apply(block).build()
    }

    // OneOf DSL methods
    fun oneOf(vararg refs: String) {
        if (oneOfInternal == null) oneOfInternal = mutableListOf()
        oneOfInternal?.addAll(
            refs.map { ref ->
                if (ref.startsWith("#/")) {
                    SchemaReference.Ref(ref)
                } else {
                    SchemaReference.Ref("#/components/schemas/$ref")
                }
            },
        )
    }

    fun oneOf(vararg classes: KClass<*>) {
        if (oneOfInternal == null) oneOfInternal = mutableListOf()
        oneOfInternal?.addAll(classes.map { schemaRef(it) })
    }

    fun oneOf(block: OneOfBuilder.() -> Unit) {
        if (oneOfInternal == null) oneOfInternal = mutableListOf()
        oneOfInternal?.addAll(OneOfBuilder().apply(block).build())
    }

    // AllOf DSL methods
    fun allOf(vararg refs: String) {
        if (allOf == null) allOf = mutableListOf()
        allOf?.addAll(
            refs.map { ref ->
                if (ref.startsWith("#/")) {
                    SchemaReference.Ref(ref)
                } else {
                    SchemaReference.Ref("#/components/schemas/$ref")
                }
            },
        )
    }

    fun allOf(vararg classes: KClass<*>) {
        if (allOf == null) allOf = mutableListOf()
        allOf?.addAll(classes.map { schemaRef(it) })
    }

    fun allOf(block: AllOfBuilder.() -> Unit) {
        if (allOf == null) allOf = mutableListOf()
        allOf?.addAll(AllOfBuilder().apply(block).build())
    }

    // AnyOf DSL methods
    fun anyOf(vararg refs: String) {
        if (anyOf == null) anyOf = mutableListOf()
        anyOf?.addAll(
            refs.map { ref ->
                if (ref.startsWith("#/")) {
                    SchemaReference.Ref(ref)
                } else {
                    SchemaReference.Ref("#/components/schemas/$ref")
                }
            },
        )
    }

    fun anyOf(vararg classes: KClass<*>) {
        if (anyOf == null) anyOf = mutableListOf()
        anyOf?.addAll(classes.map { schemaRef(it) })
    }

    fun anyOf(block: AnyOfBuilder.() -> Unit) {
        if (anyOf == null) anyOf = mutableListOf()
        anyOf?.addAll(AnyOfBuilder().apply(block).build())
    }

    // Not DSL methods
    fun not(ref: String) {
        not =
            if (ref.startsWith("#/")) {
                SchemaReference.Ref(ref)
            } else {
                SchemaReference.Ref("#/components/schemas/$ref")
            }
    }

    fun not(clazz: KClass<*>) {
        not = schemaRef(clazz)
    }

    fun not(block: SchemaBuilder.() -> Unit) {
        not = inlineSchema(block)
    }

    // Discriminator DSL
    fun discriminator(
        propertyName: String,
        block: DiscriminatorBuilder.() -> Unit = {},
    ) {
        discriminator = DiscriminatorBuilder(propertyName).apply(block).build()
    }

    fun build() =
        Schema(
            type = type,
            format = format,
            properties = properties.takeIf { it.isNotEmpty() },
            required = required.takeIf { it.isNotEmpty() },
            items = items,
            oneOf = oneOfInternal?.toList(),
            allOf = allOf?.toList(),
            anyOf = anyOf?.toList(),
            not = not,
            discriminator = discriminator,
            description = description,
            example = example,
            examples = examples,
            enumValues = enumValues?.map { it.toJsonElement() },
        )
}
