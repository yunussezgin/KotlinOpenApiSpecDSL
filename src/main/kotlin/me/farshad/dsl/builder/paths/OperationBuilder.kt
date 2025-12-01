package me.farshad.dsl.builder.paths

import me.farshad.dsl.builder.request.RequestBodyBuilder
import me.farshad.dsl.builder.response.ResponseBuilder
import me.farshad.dsl.spec.Example
import me.farshad.dsl.spec.Operation
import me.farshad.dsl.spec.Parameter
import me.farshad.dsl.spec.ParameterLocation
import me.farshad.dsl.spec.PropertyType
import me.farshad.dsl.spec.RequestBody
import me.farshad.dsl.spec.Response
import me.farshad.dsl.spec.Schema
import me.farshad.dsl.spec.SchemaFormat
import me.farshad.dsl.spec.SchemaType

class OperationBuilder {
    var summary: String? = null
    var description: String? = null
    var operationId: String? = null
    private val tags = mutableListOf<String>()
    private val parameters = mutableListOf<Parameter>()
    private var requestBody: RequestBody? = null
    private val responses = mutableMapOf<String, Response>()
    private val security = mutableListOf<Map<String, List<String>>>()

    fun tags(vararg tagNames: String) {
        tags.addAll(tagNames)
    }

    fun parameter(
        name: String,
        location: ParameterLocation,
        type: PropertyType,
        required: Boolean = false,
        description: String? = null,
        format: SchemaFormat? = null,
        examples: Map<String, Example>? = null,
        items: Schema? = null,
    ) {
        parameters.add(
            Parameter(
                name = name,
                location = location,
                required = required,
                description = description,
                schema =
                    Schema(
                        type =
                            when (type) {
                                PropertyType.STRING -> SchemaType.STRING
                                PropertyType.NUMBER -> SchemaType.NUMBER
                                PropertyType.INTEGER -> SchemaType.INTEGER
                                PropertyType.BOOLEAN -> SchemaType.BOOLEAN
                                PropertyType.ARRAY -> SchemaType.ARRAY
                                PropertyType.OBJECT -> SchemaType.OBJECT
                                PropertyType.NULL -> SchemaType.NULL
                            },
                        format = format,
                        items = items,
                    ),
                examples = examples,
            ),
        )
    }

    fun requestBody(block: RequestBodyBuilder.() -> Unit) {
        requestBody = RequestBodyBuilder().apply(block).build()
    }

    fun response(
        code: String,
        description: String,
        block: ResponseBuilder.() -> Unit = {},
    ) {
        responses[code] = ResponseBuilder(description).apply(block).build()
    }

    fun security(
        scheme: String,
        vararg scopes: String,
    ) {
        security.add(mapOf(scheme to scopes.toList()))
    }

    fun build() =
        Operation(
            tags = tags.takeIf { it.isNotEmpty() },
            summary = summary,
            description = description,
            operationId = operationId,
            parameters = parameters.takeIf { it.isNotEmpty() },
            requestBody = requestBody,
            responses = responses,
            security = security.takeIf { it.isNotEmpty() },
        )
}
