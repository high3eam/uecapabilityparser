/*
 * This file was generated by the Gradle 'init' task.
 */
rootProject.name = "uecapabilityparser"

// Check if we're in CI
val isCiServer = System.getenv().containsKey("CI")

// Cache build artifacts only in CI
buildCache { local { isEnabled = isCiServer } }
