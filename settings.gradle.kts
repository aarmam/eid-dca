pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven(url="https://androidx.dev/snapshots/builds/13489809/artifacts/repository")
        maven("https://maven.waltid.dev/releases")
        maven("https://maven.waltid.dev/snapshots")
    }
}

rootProject.name = "eID with Digital Credentials API"
include(":app")
include(":id-card-lib")
include(":id-card-lib:id-lib")
include(":id-card-lib:smart-lib")
include(":mylibrary")
