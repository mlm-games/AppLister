# AppLister

![Banner](fastlane/metadata/android/en-US/images/banner.svg)

A simple Android app for listing installed applications. Export your app list in Markdown, Plain Text, JSON, or HTML formats. Backup and restore functionality included.

[//]: # ([<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="80" alt="Get it at IzzyOnDroid">]&#40;https://apt.izzysoft.de/packages/app.applister&#41;)

## Features

- Browse all installed apps (user and system)
- Filter: All apps, User apps only, System apps only
- Sort by name, install date, update date, size, or package name
- Export app list in 4 formats: Markdown, Plain Text, JSON, HTML
- Batch selection for selective exports
- Backup app lists with automatic backup option
- Restore from JSON backups
- View detailed app information (version, install date, APK size)
- Launch apps, open Play Store, share, or uninstall directly
- Material 3 design with light/dark theme support

## Installation

**Option 1:** Download the APK from [GitHub Releases](https://github.com/ymsr/AppLister/releases)

[//]: # (**Option 2:** Install from IzzyOnDroid: https://apt.izzysoft.de/packages/app.applister)

## Building

```bash
./gradlew assembleDebug   # Debug build
./gradlew assembleRelease  # Release build (requires signing)
```

## Contributing

Issues and pull requests are welcome. To set up the development environment:

1. Clone the repository
2. Open in Android Studio (or use command line)
3. Build with `./gradlew`

## License

See [LICENSE](LICENSE) file for details.
