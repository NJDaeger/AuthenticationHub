package com.njdaeger.authenticationhub.test;

import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.RequestException;
import org.bukkit.plugin.Plugin;
import spark.Request;

import java.io.IOException;
import java.util.UUID;

public class TestApplication extends Application<TestAppUser> {

    public TestApplication(Plugin plugin) {
        super(plugin);
    }

    @Override
    public String getApplicationName() {
        return "TestApp";
    }

    @Override
    public String getUniqueName() {
        return "testapp";
    }

    @Override
    public String getConnectionUrl(AuthSession session) {
        return "/callback?state=" + session.getEncodedState(this);
    }

    @Override
    public String getDisconnectUrl(AuthSession session) {
        return "/disconnect?state=" + session.getEncodedState(this);
    }

    @Override
    public void handleConnectCallback(Request req, UUID userId, AuthSession session) throws RequestException, IOException, InterruptedException {
        database.saveUserConnection(this, userId, new TestAppUser(session.getUserId().toString()));
    }

    @Override
    public void handleDisconnectCallback(Request req, UUID userId, AuthSession session) throws RequestException, IOException, InterruptedException {
        database.removeUserConnection(this, userId);
    }

    @Override
    public TestAppUser getConnection(UUID user) {
        if (!hasConnection(user)) return null;
        return database.getUserConnection(this, user);
    }

    @Override
    public Class<TestAppUser> getSavedDataClass() {
        return TestAppUser.class;
    }
}
