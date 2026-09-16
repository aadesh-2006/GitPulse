package com.gitpulse.domain.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilePathParserTest {

    @Test
    @DisplayName("Should parse nested Java file path")
    void parseNestedJavaFilePath() {
        FilePathParser.ParsedPath parsed = FilePathParser.parse("src/main/java/com/gitpulse/App.java");

        assertThat(parsed.fileName()).isEqualTo("App.java");
        assertThat(parsed.extension()).isEqualTo("java");
        assertThat(parsed.directoryPath()).isEqualTo("src/main/java/com/gitpulse");
    }

    @Test
    @DisplayName("Should parse root-level README.md")
    void parseRootReadme() {
        FilePathParser.ParsedPath parsed = FilePathParser.parse("README.md");

        assertThat(parsed.fileName()).isEqualTo("README.md");
        assertThat(parsed.extension()).isEqualTo("md");
        assertThat(parsed.directoryPath()).isNull();
    }

    @Test
    @DisplayName("Should parse extensionless file (Dockerfile)")
    void parseExtensionlessFile() {
        FilePathParser.ParsedPath parsed = FilePathParser.parse("Dockerfile");

        assertThat(parsed.fileName()).isEqualTo("Dockerfile");
        assertThat(parsed.extension()).isNull();
        assertThat(parsed.directoryPath()).isNull();
    }

    @Test
    @DisplayName("Should parse hidden dotfile (.gitignore)")
    void parseHiddenDotfile() {
        FilePathParser.ParsedPath parsed = FilePathParser.parse(".gitignore");

        assertThat(parsed.fileName()).isEqualTo(".gitignore");
        assertThat(parsed.extension()).isNull();
        assertThat(parsed.directoryPath()).isNull();
    }

    @Test
    @DisplayName("Should parse nested hidden dotfile (.github/workflows/ci.yml)")
    void parseNestedWorkflowFile() {
        FilePathParser.ParsedPath parsed = FilePathParser.parse(".github/workflows/ci.yml");

        assertThat(parsed.fileName()).isEqualTo("ci.yml");
        assertThat(parsed.extension()).isEqualTo("yml");
        assertThat(parsed.directoryPath()).isEqualTo(".github/workflows");
    }

    @Test
    @DisplayName("Should parse unusual path with backslashes and leading/trailing slashes")
    void parseUnusualPathWithBackslashes() {
        FilePathParser.ParsedPath parsed = FilePathParser.parse("/deep\\nested\\file.tar.gz/");

        assertThat(parsed.fileName()).isEqualTo("file.tar.gz");
        assertThat(parsed.extension()).isEqualTo("gz");
        assertThat(parsed.directoryPath()).isEqualTo("deep/nested");
    }

    @Test
    @DisplayName("Should handle null and empty paths gracefully")
    void handleNullAndEmptyPaths() {
        assertThat(FilePathParser.parse(null).fileName()).isEmpty();
        assertThat(FilePathParser.parse("   ").fileName()).isEmpty();
    }
}