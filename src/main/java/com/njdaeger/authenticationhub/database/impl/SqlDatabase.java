package com.njdaeger.authenticationhub.database.impl;

import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.AuthenticationHub;
import com.njdaeger.authenticationhub.database.IDatabase;
import com.njdaeger.authenticationhub.database.ISavedConnection;
import com.njdaeger.authenticationhub.database.SaveData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SqlDatabase implements IDatabase {

    private final String prefix;
    private Connection database;
    private final AuthenticationHub plugin;

    public SqlDatabase(AuthenticationHub plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getAuthHubConfig().getDatabasePrefix();
    }

    public Connection getConnection() {
        var config = plugin.getAuthHubConfig();
        try {
            if (this.database == null || this.database.isClosed()) {
                return DriverManager.getConnection("jdbc:mysql://" + config.getDatabaseHost() + ":" + config.getDatabasePort() + "/" + config.getDatabaseName() + "?allowReconnect=true&autoReconnect=true", config.getDatabaseUsername(), config.getDatabasePassword());
            } else return this.database;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Unable to connect to database.");
    }

    @Override
    public void createDatabase() {
        if (database != null) return;
        try {

            Statement statement = getConnection().createStatement();
            SqlUtil.USERS.create(statement, "id int NOT NULL AUTO_INCREMENT, uuid char(36) NOT NULL, CONSTRAINT PK_User PRIMARY KEY (id)");
            SqlUtil.APPLICATIONS.create(statement, "app_id int NOT NULL AUTO_INCREMENT, app_name varchar(64) NOT NULL, CONSTRAINT PK_App PRIMARY KEY (app_id)");
            SqlUtil.USER_CONNECTIONS.create(statement, "user_id int NOT NULL, app_id int NOT NULL, CONSTRAINT FK_User_id FOREIGN KEY (user_id) REFERENCES " + SqlUtil.USERS.tableName() + "(id), CONSTRAINT FK_App_id FOREIGN KEY (app_id) REFERENCES " + SqlUtil.APPLICATIONS.tableName() + "(app_id)");
       } catch (SQLException e) {
            plugin.getLogger().severe("An error [ " + e.getErrorCode() + " ] occurred while attempting to create the SQL database. " + e.getMessage());
            e.printStackTrace();
            this.database = null;
        }
    }

    @Override
    public int createApplication(Application<?> application) {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        try {
            Statement statement = getConnection().createStatement();
            var res = SqlUtil.APPLICATIONS.select(statement, "app_id", "app_name = '" + application.getUniqueName().toLowerCase() + "'");
            if (!res.next()) {

                //I want to add it to the applications table AFTER i create the table for the new application.
                var newAppIdRes = SqlUtil.APPLICATIONS.select(statement, "last_insert_id()");

                var app_id = newAppIdRes.next() ? newAppIdRes.getInt("last_insert_id()") : 1;

                StringBuilder columns = new StringBuilder("user_id int NOT NULL,");
                for (Field field : application.getSavedDataFields()) {
                    var anno = field.getAnnotation(SaveData.class);
                    var colName = anno.fieldName().isEmpty() ? field.getName() : anno.fieldName();
                    columns.append(colName).append(" ").append(anno.columnType()).append(",");
                }
                columns.append("CONSTRAINT FK_User_id_").append(app_id).append(" FOREIGN KEY (user_id) REFERENCES ").append(prefix).append("users(id)");
                SqlUtil.getApplication(app_id).create(statement, columns.toString());

                SqlUtil.APPLICATIONS.insert(statement, "last_insert_id(), '" + application.getUniqueName().toLowerCase() + "'");
            }
            else return res.getInt("app_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public int getApplicationId(Application<?> application) {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        try {
            Statement statement = getConnection().createStatement();
            var res = SqlUtil.APPLICATIONS.select(statement, "app_id", "app_name = '" + application.getUniqueName().toLowerCase() + "'");
            if (!res.next()) return -1;
            else return res.getInt("app_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int createUser(UUID userId) {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        try {
            Statement statement = getConnection().createStatement();

            var res = SqlUtil.USERS.select(statement, "id", "uuid = '" + userId.toString() + "'");
            if (!res.next()) {
                SqlUtil.USERS.insert(statement, "last_insert_id(), '" + userId + "'");
                var set = SqlUtil.USERS.select(statement, "id", "uuid = '" + userId + "'");
                set.next();
                return set.getInt("id");
            }
            else return res.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int getUserId(UUID userId) {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        try {
            Statement statement = getConnection().createStatement();

            var res = SqlUtil.USERS.select(statement, "id", "uuid = '" + userId + "'");
            if (!res.next()) return -1;
            else return res.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public List<Integer> getUserConnections(UUID userId) {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        var id = getUserId(userId);
        if (id == -1) throw new RuntimeException("User " + userId + " is not registered.");
        var connections = new ArrayList<Integer>();
        try {
            Statement statement = getConnection().createStatement();

            var res = SqlUtil.USER_CONNECTIONS.select(statement, "app_id", "user_id = '" + id + "'");
            if (!res.next()) return connections;
            do {
                connections.add(res.getInt("app_id"));
            } while (res.next());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connections;
    }

    @Override
    public <T extends ISavedConnection> void saveUserConnection(Application<T> application, UUID uuid, T response) {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        var appId = getApplicationId(application);
        if (appId == -1) throw new RuntimeException("Application " + application.getUniqueName() + " is not registered.");
        var userId = getUserId(uuid);
        if (userId == -1) throw new RuntimeException("User " + uuid + " is not registered.");
        var connections = getUserConnections(uuid);
        try {
            Statement statement = getConnection().createStatement();
            var app = SqlUtil.getApplication(appId);
            var responseMap = response.getSavedDataMap();
            responseMap.put("user_id", userId);
            if (!connections.contains(appId)) {
                app.insert(statement, responseMap);
                SqlUtil.USER_CONNECTIONS.insert(statement, userId + ", " + appId);
            } else {
                app.update(statement, "user_id = " + userId, responseMap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public <T extends ISavedConnection> T getUserConnection(Application<T> application, UUID uuid) {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        var appId = getApplicationId(application);
        if (appId == -1) throw new RuntimeException("Application " + application.getUniqueName() + " is not registered.");
        var userId = getUserId(uuid);
        if (userId == -1) throw new RuntimeException("User " + uuid + " is not registered.");
        var connections = getUserConnections(uuid);
        if (!connections.contains(appId)) throw new RuntimeException("User " + uuid + " does not have a connection with " + application.getUniqueName());

        var savedDataCls = application.getSavedDataClass();
        var fields = application.getSavedDataFieldNames();
        var fieldTypes = application.getSavedDataFieldTypes();

        try {
            Constructor<T> ctor;
            ctor = savedDataCls.getDeclaredConstructor(fieldTypes.toArray(new Class<?>[0]));
            ctor.setAccessible(true);

            Statement statement = getConnection().createStatement();
            var res = SqlUtil.getApplication(appId).select(statement, String.join(", ", fields), " user_id = " + userId);
            if (!res.next()) throw new RuntimeException("User " + uuid + " does not have a connection with " + application.getUniqueName());
            var args = new ArrayList<>();
            fields.forEach(field -> {
                try {
                    args.add(res.getObject(field));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            return ctor.newInstance(args.toArray(new Object[0]));
        } catch (SQLException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Unable to create User Connection Instance. Fields: " + fields);
    }

    @Override
    public boolean removeUserConnection(Application<?> application, UUID uuid) {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        var appId = getApplicationId(application);
        if (appId == -1) throw new RuntimeException("Application " + application.getUniqueName() + " is not registered.");
        var userId = getUserId(uuid);
        if (userId == -1) throw new RuntimeException("User " + uuid + " is not registered.");
        var connections = getUserConnections(uuid);
        if (!connections.contains(appId)) return false;
        else {
            try {
                Statement statement = getConnection().createStatement();
                SqlUtil.getApplication(appId).delete(statement, "user_id = " + userId);
                SqlUtil.USER_CONNECTIONS.delete(statement, "user_id = " + userId + " AND app_id = " + appId);
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        try {
            database.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save() {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        try {
            if (!getConnection().getAutoCommit()) database.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
