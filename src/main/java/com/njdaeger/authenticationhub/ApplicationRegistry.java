package com.njdaeger.authenticationhub;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The Application Registry is where all authorizable applications are stored.
 */
public final class ApplicationRegistry {

    private final List<Application> applications;
    private final AuthenticationHub plugin;

    ApplicationRegistry(AuthenticationHub plugin) {
        this.applications = new ArrayList<>();
        this.plugin = plugin;
    }

    /**
     * Get a json array containing json objects of the route and application name of all applications that can be authorized.
     * @return A json array of all registered app objects
     */
    public JsonArray getApplicationJsonObject() {
        JsonArray arr = new JsonArray();
        applications.forEach(value -> {
            JsonObject appObject = new JsonObject();
            appObject.addProperty("route", value.getUniqueName());
            appObject.addProperty("application_name", value.getApplicationName());
            arr.add(appObject);
        });
        return arr;
    }

    /**
     * Get a list of applications currently registered to the registry
     * @return A list of registered applications
     */
    public List<Application> getApplications() {
        return applications;
    }

    /**
     * Add an application to the application registry
     * @param application The application to add
     * @return True if the application was added, false if the application is already registered or can't be registered.
     */
    public boolean addApplication(Application application) {
        if (!application.canBeLoaded) {
            plugin.getLogger().warning("Unable to load application " + application.getUniqueName());
            return false;
        }
        if (applications.stream().anyMatch(app -> app.getUniqueName().equalsIgnoreCase(application.getUniqueName()))) return false;
        applications.add(application);
        return true;
    }

    /**
     * Remove an application from the application registry
     * @param application The application to remove
     * @return False if the application was null or doesn't exist in the registry or couldn't have been loaded. True if it was removed.
     */
    public boolean removeApplication(Application application) {
        if (application == null || !application.canBeLoaded) return false;
        return applications.removeIf(app -> app.getUniqueName().equalsIgnoreCase(application.getUniqueName()));
    }

    /**
     * Remove an application from the application registry (based on the route name rather than the application object)
     * @param route The route of the application to remove
     * @return False if the application route was null or doesn't exist in the registry or couldn't have been loaded. True if it was removed.
     */
    public boolean removeApplication(String route) {
        return removeApplication(findApplication(route));
    }

    /**
     * Find an application by route name
     * @param route The name of the application route
     * @return The application that uses the given route name, or null if the application is not registered.
     */
    public Application findApplication(String route) {
        if (route == null) return null;
        return applications.stream().filter(app -> app.getUniqueName().equalsIgnoreCase(route)).findFirst().orElse(null);
    }
}
