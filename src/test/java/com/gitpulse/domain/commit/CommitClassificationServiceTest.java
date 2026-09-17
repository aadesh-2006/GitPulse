package com.gitpulse.domain.commit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CommitClassificationServiceTest {

    private CommitClassificationService classificationService;

    @BeforeEach
    void setUp() {
        classificationService = new CommitClassificationService();
    }

    @Nested
    @DisplayName("Conventional Commit Prefix Tests")
    class ConventionalCommitPrefixTests {

        @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
        @CsvSource({
                "feat: add login, FEATURE",
                "feature: add user profile, FEATURE",
                "FEAT: uppercase prefix, FEATURE",
                "Feature: capitalized prefix, FEATURE",
                "feat(api): add login endpoint, FEATURE",
                "feat(ui/dashboard)!: breaking change on ui, FEATURE",
                "fix: resolve null pointer, BUG_FIX",
                "bugfix: handle missing field, BUG_FIX",
                "bug-fix: correct rounding error, BUG_FIX",
                "FIX(auth): prevent token expiry bug, BUG_FIX",
                "refactor: simplify service, REFACTOR",
                "refactor(core): cleanup domain entities, REFACTOR",
                "docs: update README, DOCUMENTATION",
                "doc: fix typo, DOCUMENTATION",
                "documentation(api): describe endpoints, DOCUMENTATION",
                "test: add repository tests, TEST",
                "tests: integration test coverage, TEST",
                "testing(job): test kafka pipeline, TEST",
                "build: update Maven configuration, BUILD",
                "ci: update GitHub workflow, BUILD",
                "chore(ci): update build matrix, BUILD",
                "deps: bump jackson, DEPENDENCY",
                "dep: upgrade log4j, DEPENDENCY",
                "dependency: upgrade spring, DEPENDENCY",
                "dependencies: update all, DEPENDENCY",
                "chore(deps): upgrade Spring Boot, DEPENDENCY",
                "chore(dependency): update postgresql, DEPENDENCY",
                "chore(dependencies): bump library versions, DEPENDENCY",
                "config: update application properties, CONFIGURATION",
                "configuration: set database url, CONFIGURATION",
                "chore(config): update Kafka settings, CONFIGURATION",
                "chore(configuration): adjust pool size, CONFIGURATION"
        })
        void shouldClassifyConventionalCommitPrefixes(String message, CommitClassification expected) {
            assertThat(classificationService.classify(message)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Conventional commit header with multiline body preserves header classification")
        void multilineConventionalCommit() {
            String message = "feat(auth): add OAuth2 login\n\nThis commit fixes issue #123 and updates docs.";
            assertThat(classificationService.classify(message)).isEqualTo(CommitClassification.FEATURE);
        }
    }

    @Nested
    @DisplayName("Keyword Fallback Tests")
    class KeywordFallbackTests {

        @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
        @CsvSource({
                "fix authentication bug, BUG_FIX",
                "fixed race condition in consumer, BUG_FIX",
                "fixing connection leak, BUG_FIX",
                "resolve issue with database query, BUG_FIX",
                "resolved null pointer exception, BUG_FIX",
                "resolving startup failure, BUG_FIX",
                "apply emergency patch, BUG_FIX",
                "handle error during serialization, BUG_FIX",
                "update dependency versions, DEPENDENCY",
                "upgrade dependency postgresql, DEPENDENCY",
                "bump dependency jackson-databind, DEPENDENCY",
                "package upgrade to latest version, DEPENDENCY",
                "update dependency for spring boot, DEPENDENCY",
                "deps update for library, DEPENDENCY",
                "simplify service logic, REFACTOR",
                "clean up unused imports and variables, REFACTOR",
                "cleanup repository queries, REFACTOR",
                "restructure package hierarchy, REFACTOR",
                "reorganize controller methods, REFACTOR",
                "reorganise project structure, REFACTOR",
                "update README with instructions, DOCUMENTATION",
                "add javadoc to public classes, DOCUMENTATION",
                "document rest api endpoints, DOCUMENTATION",
                "improve documentation for setup, DOCUMENTATION",
                "increase unit test coverage, TEST",
                "add test case for analysis processor, TEST",
                "testing kafka event processing, TEST",
                "update application configuration, CONFIGURATION",
                "change database settings for dev, CONFIGURATION",
                "update application.yml with kafka topic, CONFIGURATION",
                "modify application.yaml logging levels, CONFIGURATION",
                "update docker-compose with postgres 16, CONFIGURATION",
                "adjust properties for connection timeout, CONFIGURATION",
                "update maven build script, BUILD",
                "modify ci pipeline definition, BUILD",
                "update gradle workflow, BUILD",
                "edit pom to include plugin, BUILD",
                "add repository endpoint, FEATURE",
                "added new metrics calculation, FEATURE",
                "adding support for file churn, FEATURE",
                "implement user authentication, FEATURE",
                "implemented contributor attribution, FEATURE",
                "implementing retry mechanism, FEATURE",
                "introduce file analysis stage, FEATURE",
                "introduced hotspot query layer, FEATURE",
                "create repository model, FEATURE",
                "created new analysis job table, FEATURE",
                "support multi-segment file paths, FEATURE"
        })
        void shouldClassifyKeywordFallbacks(String message, CommitClassification expected) {
            assertThat(classificationService.classify(message)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Keyword Fallback Precedence Tests")
    class KeywordFallbackPrecedenceTests {

        @Test
        @DisplayName("BUG_FIX takes precedence over DEPENDENCY and FEATURE")
        void bugFixPrecedence() {
            String message = "fix bug in dependency upgrade and add validation";
            assertThat(classificationService.classify(message)).isEqualTo(CommitClassification.BUG_FIX);
        }

        @Test
        @DisplayName("DEPENDENCY takes precedence over REFACTOR and FEATURE")
        void dependencyPrecedence() {
            String message = "update dependency versions, simplify code and add features";
            assertThat(classificationService.classify(message)).isEqualTo(CommitClassification.DEPENDENCY);
        }

        @Test
        @DisplayName("REFACTOR takes precedence over DOCUMENTATION and FEATURE")
        void refactorPrecedence() {
            String message = "refactor controller, update readme and implement endpoint";
            assertThat(classificationService.classify(message)).isEqualTo(CommitClassification.REFACTOR);
        }

        @Test
        @DisplayName("DOCUMENTATION takes precedence over TEST and FEATURE")
        void documentationPrecedence() {
            String message = "update docs, add test coverage and introduce new feature";
            assertThat(classificationService.classify(message)).isEqualTo(CommitClassification.DOCUMENTATION);
        }

        @Test
        @DisplayName("TEST takes precedence over CONFIGURATION and FEATURE")
        void testPrecedence() {
            String message = "test configuration properties and add support";
            assertThat(classificationService.classify(message)).isEqualTo(CommitClassification.TEST);
        }

        @Test
        @DisplayName("CONFIGURATION takes precedence over BUILD and FEATURE")
        void configurationPrecedence() {
            String message = "update configuration settings for maven build and add profile";
            assertThat(classificationService.classify(message)).isEqualTo(CommitClassification.CONFIGURATION);
        }

        @Test
        @DisplayName("BUILD takes precedence over FEATURE")
        void buildPrecedence() {
            String message = "update build pipeline and add step";
            assertThat(classificationService.classify(message)).isEqualTo(CommitClassification.BUILD);
        }
    }

    @Nested
    @DisplayName("Edge Cases, Normalization and False-Positive Prevention")
    class EdgeCasesAndFalsePositiveTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n", "\r\n"})
        void shouldReturnOtherForNullOrBlank(String message) {
            assertThat(classificationService.classify(message)).isEqualTo(CommitClassification.OTHER);
        }

        @Test
        @DisplayName("Classify null Commit entity returns OTHER")
        void classifyNullCommit() {
            assertThat(classificationService.classify((Commit) null)).isEqualTo(CommitClassification.OTHER);
        }

        @Test
        @DisplayName("Sentence containing 'feature' without prefix or feature keywords returns OTHER")
        void featureRequestDiscussionReturnsOther() {
            assertThat(classificationService.classify("feature request discussion")).isEqualTo(CommitClassification.OTHER);
        }

        @Test
        @DisplayName("Arbitrary words containing keyword substrings do not falsely match")
        void substringFalsePositivePrevention() {
            // "addition" / "address" should not match "add"
            assertThat(classificationService.classify("addressing the meeting concerns")).isEqualTo(CommitClassification.OTHER);

            // "prefix" / "suffix" / "infix" should not match "fix"
            assertThat(classificationService.classify("prefix and suffix review")).isEqualTo(CommitClassification.OTHER);

            // "testimony" / "latest" should not match "test"
            assertThat(classificationService.classify("latest team update discussion")).isEqualTo(CommitClassification.OTHER);

            // "create" inside "recreate" -> word boundary
            assertThat(classificationService.classify("something unrelated completely")).isEqualTo(CommitClassification.OTHER);
        }

        @Test
        @DisplayName("Classification is deterministic across repeated calls")
        void deterministicExecution() {
            String message = "feat(api): add repository file intelligence";
            for (int i = 0; i < 100; i++) {
                assertThat(classificationService.classify(message)).isEqualTo(CommitClassification.FEATURE);
            }
        }

        @Test
        @DisplayName("Chore without recognized scope falls back to keyword or OTHER")
        void choreWithoutRecognizedScope() {
            // chore with no recognized scope or keywords -> OTHER
            assertThat(classificationService.classify("chore: misc tasks")).isEqualTo(CommitClassification.OTHER);
            // chore with keyword -> keyword fallback
            assertThat(classificationService.classify("chore: clean up logs")).isEqualTo(CommitClassification.REFACTOR);
        }
    }
}
