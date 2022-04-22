package com.njdaeger.authenticationhub.database;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public interface ISavedConnection {

    default Map<String, Object> getSavedDataMap() {
        var map = new HashMap<String, Object>();
        try {
            var fields = Arrays.stream(getClass().getDeclaredFields()).filter(field -> field.isAnnotationPresent(SaveData.class)).peek(field -> field.setAccessible(true));
            for (var field : fields.toList()) {
                var annotation = field.getAnnotation(SaveData.class);
                var name = annotation.fieldName().isEmpty() ? field.getName() : annotation.fieldName();
                var val = field.get(this);
                map.put(name, val);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return map;
    }
}
