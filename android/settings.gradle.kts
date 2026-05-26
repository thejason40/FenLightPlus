pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FenLightCompanion"
include(":app")

// Redirect build output to the user home directory so OneDrive never touches
// intermediate build files. Without this, OneDrive holds file locks on .dex /
// .class / resource intermediates and Gradle cannot delete them between tasks.
gradle.allprojects {
    layout.buildDirectory.set(
        File(System.getProperty("user.home"), ".gradle-builds/FenLightCompanion/${project.name}")
    )
}

