# MyInstructions

Android app built with Kotlin using Navigation Component for multi-fragment navigation.

## Project Structure

- `app/src/main/java/com/example/myinstructions/` - Kotlin source files
- `app/src/main/res/` - Resources (layouts, navigation, values, drawables)
- `app/src/test/` - Unit tests (JUnit 4)
- `app/src/androidTest/` - Instrumented tests (Espresso)
- `gradle/libs.versions.toml` - Dependency version catalog

## Build & Run

```sh
./gradlew assembleDebug        # Build debug APK
./gradlew test                 # Run unit tests
./gradlew connectedAndroidTest # Run instrumented tests
./gradlew lint                 # Run lint checks
```

## Tech Stack

- **Language**: Kotlin (Java 11 compatibility)
- **Min SDK**: 29 (Android 10) / **Target SDK**: 36
- **Package**: `com.example.myinstructions`
- **View Binding**: Enabled
- **Navigation**: Jetpack Navigation Component
- **UI**: Material Design Components

## Conventions

- Use Kotlin for all new code
- Follow official Kotlin code style
- Dependencies managed via `gradle/libs.versions.toml` version catalog
- Use View Binding for view references (no `findViewById`)
- When the data model is updated, the goal is always to migrate existing data. Never delete existing data unless absolutely necessary
- Update readme.md accordingly when making changes