package com.njdaeger.authenticationhub;

import com.njdaeger.authenticationhub.database.ISavedConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * The Application Registry is where all authorized applications are stored.
 */
public final class ApplicationRegistry {

    private final List<Application<?>> applications;
    private final AuthenticationHub plugin;

    ApplicationRegistry(AuthenticationHub plugin) {
        this.applications = new ArrayList<>();
        this.plugin = plugin;
    }

    /**
     * Get a list of applications currently registered to the registry
     * @return A list of registered applications
     */
    public List<Application<?>> getApplications() {
        return applications;
    }

    /**
     * Add an application to the application registry
     * @param application The application to add
     * @return True if the application was added, false if the application is already registered or can't be registered.
     */
    public boolean addApplication(Application<?> application) {
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
    public boolean removeApplication(Application<?> application) {
        if (application == null || !application.canBeLoaded) return false;
        return applications.removeIf(app -> app.getUniqueName().equalsIgnoreCase(application.getUniqueName()));
    }

    /**
     * Remove an application from the application registry (based on the route name rather than the application object)
     * @param route The route of the application to remove
     * @return False if the application route was null or doesn't exist in the registry or couldn't have been loaded. True if it was removed.
     */
    public boolean removeApplication(String route) {
        return removeApplication(getApplication(route));
    }

    /**
     * Find an application by unique name
     * @param route The unique name of the application
     * @return The application, or null if the application is not registered.
     */
    public Application<?> getApplication(String route) {
        if (route == null) return null;
        return applications.stream().filter(app -> app.getUniqueName().equalsIgnoreCase(route)).findFirst().orElse(null);
    }

    /**
     * Find an application by Class
     * @param applicationClass The registered application class
     * @param <T> The type of SavedConnection the Application uses
     * @param <A> The application type
     * @return The application, or null if the application is not registered.
     */
    public <T extends ISavedConnection, A extends Application<T>> A getApplication(Class<A> applicationClass) {
        var optionalApp = applications.stream().filter(app -> app.getClass().equals(applicationClass)).findFirst();
        return optionalApp.map(applicationClass::cast).orElse(null);
    }

}
