pluginManagement {
    repositories {
        google() // This block sets up the plugin repository for Google's plugins
        mavenCentral() // This block sets up the Maven Central plugin repository
        gradlePluginPortal() // This block sets up the Gradle Plugin Portal repository
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // This line ensures that the build fails if any project-level repositories are defined and cause issues

    repositories {
        google() // This block sets up the Google repository for dependencies
        mavenCentral() // This block sets up the Maven Central repository for dependencies
        maven(url = "https://jitpack.io") // This block sets up the JitPack repository for dependencies
    }
}

rootProject.name = "Bijli Onn" // This line sets the name of the root project
include(":app") // This line includes the 'app' module in the project


