package com.gitpulse.domain.commit;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic and stateless commit classification engine.
 * <p>
 * Classifies commit messages into a canonical {@link CommitClassification} based on:
 * <ol>
 *   <li>Conventional commit prefix matching (highest precedence, structurally aware)</li>
 *   <li>Conservative keyword fallback matching with explicit deterministic precedence</li>
 *   <li>Default fallback to {@link CommitClassification#OTHER}</li>
 * </ol>
 */
@Service
public class CommitClassificationService {

    /**
     * Pattern matching conventional commit headers, e.g. "feat: ...", "fix(api): ...", "chore(deps)!: ..."
     */
    private static final Pattern CONVENTIONAL_COMMIT_PATTERN = Pattern.compile(
            "^([a-zA-Z0-9_\\-]+)(?:\\(([^\\)]*)\\))?(!)?:\\s*(.*)$",
            Pattern.DOTALL
    );

    // Keyword fallback patterns with strict word boundary enforcement
    private static final List<Pattern> BUG_FIX_PATTERNS = compilePatterns(
            "fix", "fixed", "fixing", "bug", "bugfix", "issue", "error", "patch", "resolve", "resolved", "resolving"
    );

    private static final List<Pattern> DEPENDENCY_PATTERNS = compilePatterns(
            "dependency", "dependencies", "dependency\\s+upgrade", "dependency\\s+update",
            "upgrade\\s+dependency", "update\\s+dependency", "deps", "bump\\s+dependency", "package\\s+upgrade"
    );

    private static final List<Pattern> REFACTOR_PATTERNS = compilePatterns(
            "refactor", "restructure", "simplify", "cleanup", "clean\\s+up", "reorganize", "reorganise"
    );

    private static final List<Pattern> DOCUMENTATION_PATTERNS = compilePatterns(
            "documentation", "document", "docs", "readme", "javadoc"
    );

    private static final List<Pattern> TEST_PATTERNS = compilePatterns(
            "test", "tests", "testing", "testcase", "test\\s+case", "coverage"
    );

    private static final List<Pattern> CONFIGURATION_PATTERNS = compilePatterns(
            "config", "configuration", "settings", "properties", "yaml", "yml",
            "application\\.ya?ml", "docker-compose", "environment"
    );

    private static final List<Pattern> BUILD_PATTERNS = compilePatterns(
            "build", "ci", "pipeline", "gradle", "maven", "pom", "workflow"
    );

    private static final List<Pattern> FEATURE_PATTERNS = compilePatterns(
            "add", "added", "adding", "implement", "implemented", "implementing",
            "introduce", "introduced", "create", "created", "support"
    );

    /**
     * Classifies a commit by its message.
     *
     * @param message the raw commit message
     * @return the classified {@link CommitClassification}, never null (defaults to {@link CommitClassification#OTHER})
     */
    public CommitClassification classify(String message) {
        if (message == null || message.isBlank()) {
            return CommitClassification.OTHER;
        }

        String trimmed = message.strip();

        // 1. Check conventional commit prefix on the first line / header
        String firstLine = extractFirstLine(trimmed);
        CommitClassification conventionalMatch = matchConventionalCommit(firstLine);
        if (conventionalMatch != null) {
            return conventionalMatch;
        }

        // 2. Keyword fallback with explicit precedence:
        // BUG_FIX -> DEPENDENCY -> REFACTOR -> DOCUMENTATION -> TEST -> CONFIGURATION -> BUILD -> FEATURE -> OTHER
        if (matchesAny(trimmed, BUG_FIX_PATTERNS)) {
            return CommitClassification.BUG_FIX;
        }
        if (matchesAny(trimmed, DEPENDENCY_PATTERNS)) {
            return CommitClassification.DEPENDENCY;
        }
        if (matchesAny(trimmed, REFACTOR_PATTERNS)) {
            return CommitClassification.REFACTOR;
        }
        if (matchesAny(trimmed, DOCUMENTATION_PATTERNS)) {
            return CommitClassification.DOCUMENTATION;
        }
        if (matchesAny(trimmed, TEST_PATTERNS)) {
            return CommitClassification.TEST;
        }
        if (matchesAny(trimmed, CONFIGURATION_PATTERNS)) {
            return CommitClassification.CONFIGURATION;
        }
        if (matchesAny(trimmed, BUILD_PATTERNS)) {
            return CommitClassification.BUILD;
        }
        if (matchesAny(trimmed, FEATURE_PATTERNS)) {
            return CommitClassification.FEATURE;
        }

        return CommitClassification.OTHER;
    }

    /**
     * Overloaded convenience method to classify a Commit entity.
     *
     * @param commit the Commit entity
     * @return the classified {@link CommitClassification}, never null
     */
    public CommitClassification classify(Commit commit) {
        if (commit == null) {
            return CommitClassification.OTHER;
        }
        return classify(commit.getMessage());
    }

    private String extractFirstLine(String text) {
        int newlineIdx = text.indexOf('\n');
        if (newlineIdx == -1) {
            return text;
        }
        return text.substring(0, newlineIdx).strip();
    }

    private CommitClassification matchConventionalCommit(String header) {
        Matcher matcher = CONVENTIONAL_COMMIT_PATTERN.matcher(header);
        if (!matcher.matches()) {
            return null;
        }

        String type = matcher.group(1).toLowerCase();
        String scope = matcher.group(2) != null ? matcher.group(2).toLowerCase().strip() : "";

        return switch (type) {
            case "feat", "feature" -> CommitClassification.FEATURE;
            case "fix", "bugfix", "bug-fix" -> CommitClassification.BUG_FIX;
            case "refactor" -> CommitClassification.REFACTOR;
            case "docs", "doc", "documentation" -> CommitClassification.DOCUMENTATION;
            case "test", "tests", "testing" -> CommitClassification.TEST;
            case "build", "ci" -> CommitClassification.BUILD;
            case "deps", "dep", "dependency", "dependencies" -> CommitClassification.DEPENDENCY;
            case "config", "configuration" -> CommitClassification.CONFIGURATION;
            case "chore" -> switch (scope) {
                case "ci" -> CommitClassification.BUILD;
                case "deps", "dep", "dependency", "dependencies" -> CommitClassification.DEPENDENCY;
                case "config", "configuration" -> CommitClassification.CONFIGURATION;
                default -> null;
            };
            default -> null;
        };
    }

    private static boolean matchesAny(String text, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private static List<Pattern> compilePatterns(String... regexParts) {
        return List.of(regexParts).stream()
                .map(part -> Pattern.compile("\\b" + part + "\\b", Pattern.CASE_INSENSITIVE))
                .toList();
    }
}
