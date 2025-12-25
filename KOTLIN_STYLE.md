## Kotlin Code Style

This project enforces Kotlin code style using **ktlint**, executed via the **Spotless Maven Plugin** to ensure consistent formatting across local development and CI environments.

---

## Usage

### Check code style

```bash
./mvnw spotless:check
```

Fails the build if any formatting violations are found.

---

### Format code automatically

```bash
./mvnw spotless:apply
```

Reformats Kotlin source files to comply with the configured style rules.

---

### Build with code style check

Code style is verified automatically during the build.
The build will fail if formatting violations are detected.

---

## IDE Configuration

An `.editorconfig` file is included in the repository to ensure consistent formatting across different editors and operating systems.

### IntelliJ IDEA

1. Enable **EditorConfig support**
   *Settings → Editor → Code Style → Enable EditorConfig support*
2. (Optional) Install the **ktlint** plugin for inline editor feedback

### VS Code

1. Install the **EditorConfig** extension
2. Install the **Kotlin** extension

> IDE formatting should follow `.editorconfig`.
> Spotless is the source of truth for CI enforcement.

---

## Line Endings

Line endings are normalized using `.gitattributes` to avoid formatting differences between local machines and CI:

- All text files use **LF**
- Windows `.cmd` files use **CRLF**

---

## Code Style Rules

The project follows the **official Kotlin coding conventions**:

- 4 spaces for indentation (no tabs)
- Maximum line length: **120 characters**
- Trailing commas allowed in declarations and function calls
- Final newline required
- No consecutive blank lines
- No blank lines before closing braces

---

## Notes

- Do **not** run `ktlint` directly via Maven plugins
- Always use `spotless:check` or `spotless:apply`
- Spotless ensures deterministic formatting in both local development and GitHub CI

