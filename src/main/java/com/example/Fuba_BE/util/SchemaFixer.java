package com.example.Fuba_BE.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SchemaFixer {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:5432/postgres";
        String username = "postgres.iycidwzfumlfatntyhdd";
        String password = "L.Dktpmuit2005";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Connected to database...");
            
            // Fix auth_audit_logs
            System.out.println("Fixing auth_audit_logs.logId...");
            stmt.execute("ALTER TABLE auth_audit_logs ALTER COLUMN logId TYPE BIGINT");
            System.out.println("✓ auth_audit_logs.logId fixed");
            
            // Fix refresh_tokens
            System.out.println("Fixing refresh_tokens.tokenId...");
            stmt.execute("ALTER TABLE refresh_tokens ALTER COLUMN tokenId TYPE BIGINT");
            System.out.println("✓ refresh_tokens.tokenId fixed");
            
            System.out.println("\n✓ Schema fixed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error fixing schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
