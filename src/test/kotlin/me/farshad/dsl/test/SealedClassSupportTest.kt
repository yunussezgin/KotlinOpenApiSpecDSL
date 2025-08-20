package me.farshad.dsl.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.farshad.dsl.annotation.SchemaDescription
import me.farshad.dsl.builder.core.openApi
import me.farshad.dsl.builder.utils.toJson
import me.farshad.dsl.spec.SchemaType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SealedClassSupportTest {

    @SchemaDescription("A shape that can be either a circle or rectangle")
    @Serializable
    sealed class Shape {
        @Serializable
        data class Circle(val radius: Double, val type: String = "Circle") : Shape()
        
        @Serializable
        data class Rectangle(val width: Double, val height: Double, val type: String = "Rectangle") : Shape()
    }

    @Serializable
    sealed class PaymentMethod {
        @Serializable
        data class CreditCard(val number: String, val expiry: String, val type: String = "CreditCard") : PaymentMethod()
        
        @Serializable
        data class BankTransfer(val accountNumber: String, val routingNumber: String, val type: String = "BankTransfer") : PaymentMethod()
        
        @Serializable
        data class PayPal(val email: String, val type: String = "PayPal") : PaymentMethod()
    }

    @Test
    fun `should automatically generate discriminated union for sealed class`() {
        val spec = openApi {
            openapi = "3.1.0"
            info {
                title = "Shape API"
                version = "1.0.0"
            }
            components {
                schema(Shape::class)
            }
        }

        val json = spec.toJson()
        val jsonObj = Json.parseToJsonElement(json)
        
        // Get the Shape schema
        val shapeSchema = jsonObj.jsonObject["components"]?.jsonObject?.get("schemas")?.jsonObject?.get("Shape")
        assertNotNull(shapeSchema, "Shape schema should exist")
        
        val shapeObj = shapeSchema.jsonObject
        
        // Check that it has oneOf
        assertTrue(shapeObj.containsKey("oneOf"), "Shape schema should have oneOf")
        val oneOf = shapeObj["oneOf"]?.jsonArray
        assertNotNull(oneOf, "oneOf should not be null")
        assertEquals(2, oneOf.size, "Shape should have 2 oneOf options")
        
        // Check discriminator
        assertTrue(shapeObj.containsKey("discriminator"), "Shape schema should have discriminator")
        val discriminator = shapeObj["discriminator"]?.jsonObject
        assertNotNull(discriminator, "discriminator should not be null")
        assertEquals("type", discriminator["propertyName"]?.jsonPrimitive?.content, "discriminator property should be 'type'")
        
        // Check mapping
        assertTrue(discriminator.containsKey("mapping"), "discriminator should have mapping")
        val mapping = discriminator["mapping"]?.jsonObject
        assertNotNull(mapping, "mapping should not be null")
        assertTrue(mapping.containsKey("Circle"), "mapping should contain Circle")
        assertTrue(mapping.containsKey("Rectangle"), "mapping should contain Rectangle")
        
        // Check description
        assertEquals("A shape that can be either a circle or rectangle", 
                    shapeObj["description"]?.jsonPrimitive?.content, 
                    "Shape should have correct description")
    }

    @Test
    fun `should generate schemas for all sealed subclasses`() {
        val spec = openApi {
            openapi = "3.1.0"
            info {
                title = "Shape API"
                version = "1.0.0"
            }
            components {
                schema(Shape::class)
            }
        }

        val json = spec.toJson()
        val jsonObj = Json.parseToJsonElement(json)
        
        val schemas = jsonObj.jsonObject["components"]?.jsonObject?.get("schemas")?.jsonObject
        assertNotNull(schemas, "schemas should exist")
        
        // Check that Circle and Rectangle schemas exist
        assertTrue(schemas.containsKey("Circle"), "Circle schema should exist")
        assertTrue(schemas.containsKey("Rectangle"), "Rectangle schema should exist")
        
        // Check Circle schema properties
        val circleSchema = schemas["Circle"]?.jsonObject
        assertNotNull(circleSchema, "Circle schema should not be null")
        assertEquals("object", circleSchema["type"]?.jsonPrimitive?.content, "Circle should be object type")
        
        val circleProps = circleSchema["properties"]?.jsonObject
        assertNotNull(circleProps, "Circle should have properties")
        assertTrue(circleProps.containsKey("radius"), "Circle should have radius property")
        assertTrue(circleProps.containsKey("type"), "Circle should have type property")
        
        // Check Rectangle schema properties
        val rectangleSchema = schemas["Rectangle"]?.jsonObject
        assertNotNull(rectangleSchema, "Rectangle schema should not be null")
        assertEquals("object", rectangleSchema["type"]?.jsonPrimitive?.content, "Rectangle should be object type")
        
        val rectangleProps = rectangleSchema["properties"]?.jsonObject
        assertNotNull(rectangleProps, "Rectangle should have properties")
        assertTrue(rectangleProps.containsKey("width"), "Rectangle should have width property")
        assertTrue(rectangleProps.containsKey("height"), "Rectangle should have height property")
        assertTrue(rectangleProps.containsKey("type"), "Rectangle should have type property")
    }

    @Test
    fun `should handle sealed classes with multiple subclasses`() {
        val spec = openApi {
            openapi = "3.1.0"
            info {
                title = "Payment API"
                version = "1.0.0"
            }
            components {
                schema(PaymentMethod::class)
            }
        }

        val json = spec.toJson()
        val jsonObj = Json.parseToJsonElement(json)
        
        // Get the PaymentMethod schema
        val paymentSchema = jsonObj.jsonObject["components"]?.jsonObject?.get("schemas")?.jsonObject?.get("PaymentMethod")
        assertNotNull(paymentSchema, "PaymentMethod schema should exist")
        
        val paymentObj = paymentSchema.jsonObject
        
        // Check that it has oneOf with 3 items
        val oneOf = paymentObj["oneOf"]?.jsonArray
        assertNotNull(oneOf, "oneOf should not be null")
        assertEquals(3, oneOf.size, "PaymentMethod should have 3 oneOf options")
        
        // Check discriminator mapping has all 3 payment types
        val discriminator = paymentObj["discriminator"]?.jsonObject
        assertNotNull(discriminator, "discriminator should not be null")
        val mapping = discriminator["mapping"]?.jsonObject
        assertNotNull(mapping, "mapping should not be null")
        assertTrue(mapping.containsKey("CreditCard"), "mapping should contain CreditCard")
        assertTrue(mapping.containsKey("BankTransfer"), "mapping should contain BankTransfer")
        assertTrue(mapping.containsKey("PayPal"), "mapping should contain PayPal")
        
        // Check that all subclass schemas exist
        val schemas = jsonObj.jsonObject["components"]?.jsonObject?.get("schemas")?.jsonObject
        assertNotNull(schemas, "schemas should exist")
        assertTrue(schemas.containsKey("CreditCard"), "CreditCard schema should exist")
        assertTrue(schemas.containsKey("BankTransfer"), "BankTransfer schema should exist")
        assertTrue(schemas.containsKey("PayPal"), "PayPal schema should exist")
    }

    @Test
    fun `should handle sealed class used in other schemas`() {
        @Serializable
        data class Order(val id: String, val paymentMethod: PaymentMethod)
        
        val spec = openApi {
            openapi = "3.1.0"
            info {
                title = "Order API"
                version = "1.0.0"
            }
            components {
                schema(Order::class)
            }
        }

        val json = spec.toJson()
        val jsonObj = Json.parseToJsonElement(json)
        
        val schemas = jsonObj.jsonObject["components"]?.jsonObject?.get("schemas")?.jsonObject
        assertNotNull(schemas, "schemas should exist")
        
        // Check Order schema has reference to PaymentMethod
        val orderSchema = schemas["Order"]?.jsonObject
        assertNotNull(orderSchema, "Order schema should exist")
        val orderProps = orderSchema["properties"]?.jsonObject
        assertNotNull(orderProps, "Order should have properties")
        
        val paymentProp = orderProps["paymentMethod"]?.jsonObject
        assertNotNull(paymentProp, "Order should have paymentMethod property")
        assertEquals("#/components/schemas/PaymentMethod", 
                    paymentProp["\$ref"]?.jsonPrimitive?.content,
                    "paymentMethod should reference PaymentMethod schema")
        
        // Check that PaymentMethod schema exists and is discriminated union
        assertTrue(schemas.containsKey("PaymentMethod"), "PaymentMethod schema should exist")
        val paymentSchema = schemas["PaymentMethod"]?.jsonObject
        assertNotNull(paymentSchema, "PaymentMethod schema should not be null")
        assertTrue(paymentSchema.containsKey("oneOf"), "PaymentMethod should have oneOf")
        assertTrue(paymentSchema.containsKey("discriminator"), "PaymentMethod should have discriminator")
    }
}