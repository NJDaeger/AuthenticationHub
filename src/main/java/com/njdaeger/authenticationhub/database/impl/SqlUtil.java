package com.njdaeger.authenticationhub.database.impl;

import com.njdaeger.authenticationhub.AuthenticationHub;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class SqlUtil {

    public static final SqlTable USERS = new SqlTable("users");
    public static final SqlTable APPLICATIONS = new SqlTable("applications");
    public static final SqlTable USER_CONNECTIONS = new SqlTable("user_connections");

    public static SqlTable getApplication(int appId) {
        return new SqlTable(appId + "_user_connections");
    }

    public static class SqlTable {

        private final String tableName;

        public SqlTable(String tableName) {
            this.tableName = AuthenticationHub.getInstance().getAuthHubConfig().getDatabasePrefix() + tableName;
        }

        public String tableName() {
            return tableName;
        }

        public void create(Statement statement, String sqlColumns) throws SQLException {
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + sqlColumns + ")";
//            System.out.println(sql);
            statement.execute(sql);
        }

        public void insert(Statement statement, String sqlValues) throws SQLException {
            String sql = "INSERT INTO " + tableName + " VALUES (" + sqlValues + ")";
//            System.out.println(sql);
            statement.execute(sql);
        }

        public void insert(Statement statement, Map<String, Object> values) throws SQLException {
            StringBuilder query = new StringBuilder("INSERT INTO " + tableName + " (");
            values.keySet().forEach(key -> query.append(key).append(","));
            query.deleteCharAt(query.length() - 1);
            query.append(") VALUES (");
            values.values().forEach(val -> {
                if (val instanceof Number) query.append(val).append(",");
                else query.append("'").append(val).append("',");
            });
            query.deleteCharAt(query.length() - 1);
            query.append(")");
//            System.out.println(query);
            statement.execute(query.toString());
        }

        public ResultSet select(Statement statement, String sqlColumns, String sqlWhere) throws SQLException {
            String sql = "SELECT " + sqlColumns + " FROM " + tableName + " WHERE " + sqlWhere;
//            System.out.println(sql);
            return statement.executeQuery(sql);
        }

        public ResultSet select(Statement statement, String sqlColumns) throws SQLException {
            String sql = "SELECT " + sqlColumns + " FROM " + tableName;
//            System.out.println(sql);
            return statement.executeQuery(sql);
        }

        public int update(Statement statement, String sqlWhere, String sqlInsert) throws SQLException {
            String sql = "UPDATE " + tableName + " SET " + sqlInsert + " WHERE " + sqlWhere;
//            System.out.println(sql);
            return statement.executeUpdate(sql);
        }

        public int update(Statement statement, String sqlWhere, Map<String, Object> values) throws SQLException {
            StringBuilder query = new StringBuilder("UPDATE " + tableName + " SET ");
            values.forEach((key, value) -> query.append(key).append(" = ").append(value).append(","));
            query.deleteCharAt(query.length() - 1);
            query.append(" WHERE ").append(sqlWhere);
//            System.out.println(query);
            return statement.executeUpdate(query.toString());
        }

        public void delete(Statement statement, String sqlWhere) throws SQLException {
            String sql = "DELETE FROM " + tableName + " WHERE " + sqlWhere;
//            System.out.println(sql);
            statement.executeUpdate(sql);
        }

    }

}
