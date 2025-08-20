plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    `maven-publish`
}

group = "me.farshad"
version = "1.0.6-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // YAML support (using kaml which is built on SnakeYAML)
    implementation("com.charleskorn.kaml:kaml:0.66.0")
    
    // Kotlin reflection (needed for annotations)
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            groupId = project.group.toString()
            artifactId = "kotlin-openapi-spec-dsl"
            version = project.version.toString()
            
            pom {
                name.set("Kotlin OpenAPI Spec DSL")
                description.set("A type-safe Kotlin DSL for generating OpenAPI 3.1.0 specifications")
                
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://opensource.org/licenses/Apache-2.0")
                    }
                }
                
                developers {
                    developer {
                        id.set("farshad")
                        name.set("Farshad Akbari")
                    }
                }
            }
        }
    }
    
    repositories {
        // For publishing to local repository
        mavenLocal()

//         For publishing to a custom repository (uncomment and configure as needed)
//         maven {
//             name = "GitHubPackages"
//             url = uri("https://maven.pkg.github.com/your-username/kotlin-openapi-spec-dsl")
//             credentials {
//                 username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
//                 password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
//             }
//         }
//        maven {
//            url = uri("https://pkgs.dev.azure.com/dcsgmbh/_packaging/libs-snapshot/maven/v1")
//            name = "libs-snapshot"
//            credentials {
//                username = project.findProperty("azureDevOpsUsername") as String? ?: ""
//                password = project.findProperty("azureDevOpsPassword") as String? ?: ""
//            }
//        }
    }
}