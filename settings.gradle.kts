pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "NegahCoach"
include(":app", ":core:common", ":core:designsystem", ":domain:core", ":data:local", ":feature:home", ":feature:members")
