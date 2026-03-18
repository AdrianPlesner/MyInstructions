# My Instructions

An Android app for managing tasks with step-by-step instructions. Organize your tasks into categories, attach photos to instructions, and search across everything.

## Features

- **Task Management** — Create, edit, and delete tasks with any number of instructions
- **Rich Instructions** — Each instruction supports text and an optional image (from gallery or camera, with cropping)
- **Categories** — Assign tasks to multiple categories; create categories inline or via a dedicated management screen
- **Expandable Category View** — Main screen groups tasks under collapsible category headers, with uncategorized tasks shown separately at the bottom
- **Sort Modes** — Toggle between sorting by category (alphabetical) and by most recently viewed
- **Search** — Filter tasks by title or instruction text with relevance ranking; matching instructions are displayed with highlighted search terms
- **Drag & Drop** — Reorder instructions by dragging the handle on each card
- **Local Persistence** — All data stored locally using Room database
- **Image Cropping** — Free-style crop images before attaching them to instructions (powered by UCrop)
- **QR Code Sharing** — Share tasks between devices via QR codes; select individual tasks, a whole category, or any combination. Scan incoming QR codes and assign categories to each imported task via a step-by-step wizard (note: images are not included in QR exports)

## Screenshots

_Coming soon_

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| Min SDK | 29 (Android 10) |
| Target SDK | 36 |
| UI | Material Design 3 |
| Navigation | Jetpack Navigation Component |
| Database | Room (v2.7.0) with KSP |
| Image Loading | Coil |
| Image Cropping | UCrop |
| QR Code | ZXing Android Embedded (4.3.0) |
| View References | View Binding |
| Async | Kotlin Coroutines + Flow |
| Architecture | AndroidViewModel + Repository pattern |
| CI/CD | GitHub Actions (lint, build, signed release) |

## Build & Run

```sh
./gradlew assembleDebug        # Build debug APK
./gradlew test                 # Run unit tests
./gradlew connectedAndroidTest # Run instrumented tests
./gradlew lint                 # Run lint checks
```

## Releases

Releases are built automatically via GitHub Actions when a version tag is pushed:

```sh
git tag v2.0.0
git push origin v2.0.0
```

The workflow builds a signed release APK and publishes it to [GitHub Releases](https://github.com/AdrianPlesner/MyInstructions/releases).

## Project Structure

```
app/src/main/java/com/example/myinstructions/
├── data/
│   ├── dao/              # Room DAOs (TaskDao, InstructionDao, CategoryDao)
│   ├── entity/           # Room entities (Task, Instruction, Category, CrossRef)
│   ├── relation/         # Room relations (TaskWithInstructions, CategoryWithTasks)
│   ├── repository/       # Repository layer (TaskRepository, CategoryRepository)
│   └── AppDatabase.kt    # Room database singleton
├── ui/
│   ├── tasklist/         # Main screen (grouped list, search, sort)
│   ├── taskdetail/       # Task detail view (read-only instructions)
│   ├── taskcreate/       # Task create/edit (instructions editor, image picker)
│   ├── category/         # Category management screen
│   ├── share/            # QR sharing (ShareSelectionFragment, QrDisplayFragment)
│   └── scan/             # QR scanning and import wizard
├── util/                 # Helpers (ImageStorageHelper, HighlightHelper, QrCodeHelper)
└── MainActivity.kt       # Single-activity host
```

## License

This project is for personal use.
