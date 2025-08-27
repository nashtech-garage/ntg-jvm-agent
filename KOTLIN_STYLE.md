# Kotlin Code Style

This project uses [ktlint](https://ktlint.github.io/) for Kotlin code style enforcement.

## Usage

### Check code style
```bash
./mvnw ktlint:check
```

### Format code automatically
```bash
./mvnw ktlint:format
```

### Build with code style check
The ktlint check is automatically executed during the build process. The build will fail if there are any code style violations.

## IDE Configuration

An `.editorconfig` file is included in the project root to ensure consistent formatting across different IDEs.

### IntelliJ IDEA
1. Install the ktlint plugin (optional)
2. Enable EditorConfig support in Settings > Editor > Code Style > Enable EditorConfig support

### VS Code
1. Install the EditorConfig extension
2. Install the Kotlin extension

## Code Style Rules

The project follows the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) with the following key points:

- 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Trailing commas allowed in function calls and declarations
- Final newline required
- No consecutive blank lines
- No blank lines before closing braces