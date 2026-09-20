package com.gitpulse.domain.evolution;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Test-only Hibernate StatementInspector to adapt PostgreSQL-specific generate_series
 * into H2-compatible recursive CTE execution during test runs.
 */
public class H2TestStatementInspector implements StatementInspector {

    @Override
    public String inspect(String sql) {
        if (sql == null || !sql.contains("generate_series")) {
            return sql;
        }

        // Replace PostgreSQL generate_series with an exact semantic equivalent in H2
        // while preserving exact parameter placeholder counts (3 placeholders: from, to, to).
        return sql.replaceAll(
                "(?s)WITH months AS \\(\\s*SELECT s\\.bucket_month\\s*FROM generate_series\\(\\s*date_trunc\\('month', CAST\\(\\? AS TIMESTAMP WITH TIME ZONE\\)\\),\\s*date_trunc\\('month', CAST\\(\\? AS TIMESTAMP WITH TIME ZONE\\)\\),\\s*INTERVAL '1' MONTH\\s*\\) AS s\\(bucket_month\\)\\s*WHERE s\\.bucket_month < CAST\\(\\? AS TIMESTAMP WITH TIME ZONE\\)\\s*\\)",
                "WITH RECURSIVE months(bucket_month) AS ( " +
                        "SELECT CAST(date_trunc('month', CAST(? AS TIMESTAMP WITH TIME ZONE)) AS TIMESTAMP WITH TIME ZONE) " +
                        "UNION ALL " +
                        "SELECT CAST(CAST(bucket_month AS TIMESTAMP WITH TIME ZONE) + INTERVAL '1' MONTH AS TIMESTAMP WITH TIME ZONE) " +
                        "FROM months " +
                        "WHERE CAST(date_trunc('month', CAST(? AS TIMESTAMP WITH TIME ZONE)) AS TIMESTAMP WITH TIME ZONE) IS NOT NULL " +
                        "AND CAST(CAST(bucket_month AS TIMESTAMP WITH TIME ZONE) + INTERVAL '1' MONTH AS TIMESTAMP WITH TIME ZONE) < CAST(? AS TIMESTAMP WITH TIME ZONE) )"
        );
    }
}
