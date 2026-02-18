package com.zap.procurement.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;


@Configuration
public class DatabaseFixConfig {

    @Bean
    @Order(1) // Run before RBACSeeder
    public CommandLineRunner dropOldUniqueConstraints(JdbcTemplate jdbcTemplate) {
        return args -> {
            System.out.println("[DatabaseFix] Starting aggressive cleanup of legacy unique constraints...");

            // Tables to cleanup
            String[] tables = { "roles", "permissions" };

            for (String table : tables) {
                try {
                    // Find indices that are NOT our new named composite ones and NOT PRIMARY
                    String sql = "SELECT DISTINCT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                            "AND INDEX_NAME != 'PRIMARY' " +
                            "AND INDEX_NAME NOT IN ('uk_roles_name_tenant', 'uk_roles_slug_tenant', " +
                            "'uk_permissions_name_tenant', 'uk_permissions_slug_tenant')";

                    List<String> indexNames = jdbcTemplate.queryForList(sql, String.class, table);

                    for (String indexName : indexNames) {
                        try {
                            System.out.println(
                                    "[DatabaseFix] Dropping legacy index: " + indexName + " from table: " + table);
                            jdbcTemplate.execute("ALTER TABLE " + table + " DROP INDEX " + indexName);
                        } catch (Exception e) {
                            System.out
                                    .println("[DatabaseFix] Could not drop index " + indexName + ": " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out
                            .println("[DatabaseFix] Error querying indices for table " + table + ": " + e.getMessage());
                }
            }

            System.out.println("[DatabaseFix] Legacy cleanup completed.");
        };
    }
}
