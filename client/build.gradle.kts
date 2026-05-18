plugins {
    application
    java
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation(project(":common"))
    implementation("org.openjfx:javafx-controls:21")
    implementation("org.openjfx:javafx-fxml:21")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // WebSocket client (Java-WebSocket library)
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

application {
    mainClass.set("poker.Main")
}
