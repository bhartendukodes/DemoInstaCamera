pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven{ url = uri("https://jitpack.io") }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public/")  }
        google()
        mavenCentral()
        maven{ url = uri("https://jitpack.io") }
        maven {  url = uri("https://repo1.maven.org/maven2/")  }
        maven {  url = uri("file:///usr/local/apache-maven-3.8.7/repository")  }
        maven {
            url = uri ("http://nexus.arashivision.com:9999/repository/maven-public/")
            isAllowInsecureProtocol = true
            credentials {
                username = "deployment"
                password = "test123"
            }
        }
        maven {
            url = uri("http://nexus.arashivision.com:9999/repository/maven-releases2/")
            isAllowInsecureProtocol = true
            credentials {
                username = "insta360dev"
                password = "50lan123"
            }
        }
    }
}

rootProject.name = "DemoInstaCamera"
include(":app")
