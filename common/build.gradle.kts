plugins {
    `java-library`
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    // keep this small - DTOs, domain primitives only
    implementation(platform("org.junit:junit-bom:5.10.2")) // optional for tests
    testImplementation("org.junit.jupiter:junit-jupiter")
}