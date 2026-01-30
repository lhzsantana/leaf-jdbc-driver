plugins {
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.leaf"
version = (findProperty("artifactVersion") as String?) ?: "0.3.7"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    val calciteVersion = "1.35.0"
    val okhttpVersion = "4.12.0"
    val jacksonVersion = "2.16.2"
    val slf4jVersion = "2.0.13"

    api("org.apache.calcite:calcite-core:$calciteVersion")
    api("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

spotless {
    java {
        googleJavaFormat()
        target("src/**/*.java")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports", "java.sql.rowset/com.sun.rowset=ALL-UNNAMED"
        )
    )
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("Leaf JDBC Driver")
                description.set("JDBC driver for Leaf Agriculture using Apache Calcite")
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            val repo = System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"
            url = uri("https://maven.pkg.github.com/$repo")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: (findProperty("gpr.user") as String? ?: "")
                password = System.getenv("GITHUB_TOKEN") ?: (findProperty("gpr.key") as String? ?: "")
            }
        }
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("all")
    mergeServiceFiles()
    minimize()
}

