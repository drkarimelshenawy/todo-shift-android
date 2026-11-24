pluginManagement {
    repositories {
        google()
        // Google Cloud's hosted Maven Central mirror avoids shared-runner 429s.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
    }
}

rootProject.name = "ToDoShift"
include(":app", ":cashbook-core", ":cashbook-feature")
