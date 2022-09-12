package com.njdaeger.authenticationhub.database.impl;

import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.AuthenticationHub;
import com.njdaeger.authenticationhub.database.IDatabase;
import com.njdaeger.authenticationhub.database.ISavedConnection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class YmlDatabase implements IDatabase {

    private File dbFile;
    private YamlConfiguration database;
    private final AuthenticationHub plugin;
    /*

    users:
        id: uuid

    applications:
        app_id: app_name

    user_connections:
        user_id:
            - app_id1
            - app_id2
            - app_id3

    [app_id]_user_connections:
        user_id:
            app_defined_key: app value
            app_defined_key2: app value


     */
    public YmlDatabase(AuthenticationHub plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createDatabase() {
        if (database != null) return;
        dbFile = new File(plugin.getDataFolder().getAbsoluteFile() + File.separator + "db.yml");
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("An error occurred while attempting to create the YML database.");
                e.printStackTrace();
                this.database = null;
            }
        }
        this.database = YamlConfiguration.loadConfiguration(dbFile);
    }

    @Override
    public int createApplication(Application<?> application) {
        int id = 0;
        var section = database.getConfigurationSection("applications");

        if (section != null) {
            for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
                Object value = entry.getValue();
                if (value.equals(application.getUniqueName())) return Integer.parseInt(entry.getKey());
                id++;
            }
        }
        database.set("applications." + id, application.getUniqueName());
        return id;
    }

    @Override
    public int getApplicationId(Application<?> application) {
        var section = database.getConfigurationSection("applications");
        if (section != null) {
            for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
                String k = entry.getKey();
                String v = (String)entry.getValue();
                if (v.equalsIgnoreCase(application.getUniqueName())) return Integer.parseInt(k);
            }
        }
        return -1;
    }

    @Override
    public int createUser(UUID uuid) {
        int id = 0;
        var section = database.getConfigurationSection("users");
        if (section != null) {
            for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
                Object value = entry.getValue();
                if (value.equals(uuid.toString())) return Integer.parseInt(entry.getKey());
                id++;
            }
        }
        database.set("users." + id, uuid.toString());
        return id;
    }

    @Override
    public int getUserId(UUID uuid) {
        var section = database.getConfigurationSection("users");
        if (section != null) {
            for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
                String k = entry.getKey();
                String v = (String)entry.getValue();
                if (v.equals(uuid.toString())) return Integer.parseInt(k);
            }
        }
        return -1;
    }

    @Override
    public List<Integer> getUserConnections(UUID uuid) {
        var id = getUserId(uuid);
        if (id == -1) throw new RuntimeException("User " + uuid + " is not registered.");
        return database.getIntegerList("user_connections." + id);
    }

    @Override
    public <T extends ISavedConnection> void saveUserConnection(Application<T> application, UUID uuid, T response) {
        var appId = getApplicationId(application);
        if (appId == -1) throw new RuntimeException("Application " + application.getUniqueName() + " is not registered.");
        var userId = getUserId(uuid);
        if (userId == -1) throw new RuntimeException("User " + uuid + " is not registered.");
        var connections = getUserConnections(uuid);
        if (!connections.contains(appId)) {
            connections.add(appId);
            database.set("user_connections." + userId, connections);
        }
        database.createSection(appId + "_user_connections." + userId, response.getSavedDataMap());
    }

    @Override
    public <T extends ISavedConnection> T getUserConnection(Application<T> application, UUID uuid) {
        var appId = getApplicationId(application);
        if (appId == -1) throw new RuntimeException("Application " + application.getUniqueName() + " is not registered.");
        var userId = getUserId(uuid);
        if (userId == -1) throw new RuntimeException("User " + uuid + " is not registered.");
        var userSection = database.getConfigurationSection(appId + "_user_connections." + userId);
        if (userSection == null) throw new RuntimeException("User " + uuid + " does not have a connection with " + application.getUniqueName());

        var savedDataCls = application.getSavedDataClass();
        var fields = application.getSavedDataFieldNames();
        var fieldTypes = application.getSavedDataFieldTypes();

        try {
            Constructor<T> ctor;
            ctor = savedDataCls.getDeclaredConstructor(fieldTypes.toArray(new Class<?>[0]));
            ctor.setAccessible(true);

            var args = new ArrayList<>();
            fields.forEach(field -> args.add(userSection.get(field)));
            return ctor.newInstance(args.toArray(new Object[0]));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Unable to create User Connection Instance. Fields: " + fields);
    }

    @Override
    public <T extends ISavedConnection> Map<UUID, T> getConnections(Application<T> application) {
        if (database == null) throw new RuntimeException("Database has not been initialized.");
        var appId = getApplicationId(application);
        if (appId == -1) throw new RuntimeException("Application " + application.getUniqueName() + " is not registered.");

        var savedDataCls = application.getSavedDataClass();
        var fields = application.getSavedDataFieldNames();
        var fieldTypes = application.getSavedDataFieldTypes();
        var connections = new HashMap<UUID, T>();

        try {
            Constructor<T> ctor;
            ctor = savedDataCls.getDeclaredConstructor(fieldTypes.toArray(new Class<?>[0]));
            ctor.setAccessible(true);

            var section = database.getConfigurationSection(appId + "_user_connections");
            if (section == null) throw new RuntimeException("Application " + application.getUniqueName() + " is not registered and found in the database.");
            for (String userId : section.getKeys(false)) {
                String stringUuid = database.getString("users." + userId);
                if (stringUuid == null || stringUuid.isEmpty()) continue;
                UUID uuid = UUID.fromString(stringUuid);
                var userSection = database.getConfigurationSection(appId + "_user_connections." + userId);
                var args = new ArrayList<>();
                fields.forEach(field -> args.add(userSection.get(field)));
                connections.put(uuid, ctor.newInstance(args.toArray(new Object[0])));
            }

        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return connections;
    }

    @Override
    public boolean removeUserConnection(Application<?> application, UUID uuid) {
        var appId = getApplicationId(application);
        if (appId == -1) throw new RuntimeException("Application " + application.getUniqueName() + " is not registered.");
        var userId = getUserId(uuid);
        if (userId == -1) throw new RuntimeException("User " + uuid + " is not registered.");
        var userSection = database.getConfigurationSection(appId + "_user_connections." + userId);
        if (userSection == null) return false;
        else {
            database.set(appId + "_user_connections." + userId, null);
            var connections = getUserConnections(uuid);
            connections.remove((Object)appId);
            database.set("user_connections." + userId, connections);
        }
        return true;
    }

    @Override
    public void close() {
        save();
    }

    @Override
    public void save() {
        try {
            plugin.getLogger().info("Saving database...");
            database.save(dbFile);
        } catch (IOException e) {
            plugin.getLogger().severe("There was an error saving the database. Any changes will NOT BE SAVED TO " + dbFile.getName() + ". HOWEVER, an attempt will be made to save it to a backup file.");
            var backup = new File(plugin.getDataFolder().getAbsoluteFile() + File.separator + "db_backup_" + System.currentTimeMillis() + ".yml");
            try {
                backup.createNewFile();
                database.save(backup);
            } catch (IOException ex) {
                plugin.getLogger().severe("There was a critical error saving the backup database. No data will be saved.");
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}
