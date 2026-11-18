pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // MapLibre Native Android — разблокировать когда maven.maplibre.org будет доступен по DNS:
        // maven {
        //     url = uri("https://maven.maplibre.org")
        // }
        // Альтернативный источник (требует аутентификации):
        // maven {
        //     url = uri("https://jitpack.io")
        // }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        flatDir {
            dirs = setOf(rootDir.resolve("app/libs"))
        }
        // MapLibre Native Android — разблокировать когда maven.maplibre.org будет доступен по DNS:
        // maven {
        //     url = uri("https://maven.maplibre.org")
        // }
    }
}

rootProject.name = "road-guide"

include(":app")