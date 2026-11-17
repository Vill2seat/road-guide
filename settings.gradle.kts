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
        // MapLibre Native Android — разблокировать когда maven.maplibre.org будет доступен по DNS:
        // maven {
        //     url = uri("https://maven.maplibre.org")
        // }
        // maven {
        //     url = uri("https://jitpack.io")
        // }
    }
}

rootProject.name = "road-guide"

include(":app")