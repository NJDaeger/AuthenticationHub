package com.njdaeger.authenticationhub.database.impl;

import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.AuthenticationHub;
import com.njdaeger.authenticationhub.database.ISavedResponse;
import com.njdaeger.authenticationhub.database.IDatabase;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    app_id_user_connections:
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
                plugin.getLogger().warning("An error occurred while attempting to create the YML database.");
                e.printStackTrace();
                database = null;
            }
        }
        database = YamlConfiguration.loadConfiguration(dbFile);
    }

    @Override
    public int createApplication(Application application) {
        int id = 0;
        var section = database.getConfigurationSection("applications");
        if (section != null) {
            id = section.getKeys(false).stream().map(Integer::parseInt).mapToInt(i -> i).max().orElse(0) + 1;
        }
        database.set("applications." + id, application.getUniqueName());
        return id;
    }

    @Override
    public int getApplicationId(Application application) {
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
    public int saveUser(UUID userId) {
        int id = 0;
        var section = database.getConfigurationSection("users");
        if (section != null) {
            id = section.getKeys(false).stream().map(Integer::parseInt).mapToInt(i -> i).max().orElse(0) + 1;
        }
        database.set("users." + id, userId.toString());
        return id;
    }

    @Override
    public int getUserId(UUID userId) {
        var section = database.getConfigurationSection("users");
        if (section != null) {
            for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
                String k = entry.getKey();
                String v = (String)entry.getValue();
                if (v.equals(userId.toString())) return Integer.parseInt(k);
            }
        }
        return -1;
    }

    @Override
    public List<Integer> getUserConnections(UUID userId) {
        var id = getUserId(userId);
        if (id == -1) throw new RuntimeException("User " + userId + " does not exist.");
        return database.getIntegerList("user_connections." + id);
    }

    @Override
    public <T extends ISavedResponse> void saveUserConnection(Application<T> application, UUID uuid, T response) {
        var appId = getApplicationId(application);
        if (appId == -1) throw new RuntimeException("Application " + application.getUniqueName() + " does not exist.");
        var userId = getUserId(uuid);
        if (userId == -1) throw new RuntimeException("User " + uuid + " does not exist.");
        var connections = database.getIntegerList("user_connections." + userId);
        if (!connections.contains(appId)) {
            connections.add(appId);
            database.set("user_connections." + userId, connections);
        }
        database.createSection(appId + "_user_connections." + userId, response.getSavedDataMap());
//        database.set(appId + "_user_connections." + userId, token);
    }

    @Override
    public <T extends ISavedResponse> T getUserConnection(Application<T> application, UUID uuid) {
        var appId = getApplicationId(application);
        if (appId == -1) throw new RuntimeException("Application " + application.getUniqueName() + " does not exist.");
        var userId = getUserId(uuid);
        if (userId == -1) throw new RuntimeException("User " + uuid + " does not exist.");
        var userSection = database.getConfigurationSection(appId + "_user_connections." + userId);
        if (userSection == null) throw new RuntimeException("User " + uuid + " does not have a connection with " + application.getUniqueName());

        var savedDataCls = application.getSavedDataClass();
        var fields = application.getSavedDataFieldNames();
        var fieldTypes = application.getSavedDataFieldTypes();
        Constructor<T> ctor = null;

        try {
            ctor = savedDataCls.getDeclaredConstructor(fieldTypes.toArray(new Class<?>[0]));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        if (ctor == null) throw new RuntimeException(savedDataCls.getName() + " does not have a connection with " + application.getUniqueName());

        var args = new ArrayList<>();
        fields.forEach(field -> args.add(userSection.get(field)));
        try {
            return ctor.newInstance(args.toArray(new Object[0]));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Unable to create User Connection Instance. Fields: " + fields);
    }



    @Override
    public boolean removeUserToken(Application application, UUID uuid) {
        return false;
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
