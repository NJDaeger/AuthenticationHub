package com.njdaeger.authenticationhub.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import spark.Request;

/**
 * Web utilities
 */
public final class WebUtils {

    private WebUtils() {
    }

    /**
     * OK Response code
     */
    public static final int OK = 200;
    /**
     * BAD REQUEST response code
     */
    public static final int BAD_REQUEST = 400;
    /**
     * UNAUTHORIZED response code
     */
    public static final int UNAUTHORIZED = 401;
    /**
     * FORBIDDEN response code
     */
    public static final int FORBIDDEN = 403;
    /**
     * SERVER ERROR response code
     */
    public static final int SERVER_ERROR = 500;

    /**
     * Create a Json object with every odd value in the provided array being a key and every even value in the array being a value
     * @param keyValues The array of keys and values that comprise this Json Object
     * @return The generated Json Object.
     */
    public static JsonObject createObject(Object... keyValues) {
        if (keyValues.length % 2 != 0) throw new RuntimeException("Error creating json object. (key value mismatch)");
        JsonObject obj = new JsonObject();
        for (int i = 0; i < keyValues.length; i+=2) {
            String key = keyValues[i].toString();//this really SHOULD be a string- im going to treat it that way.
            Object value = keyValues[i + 1];
            if (value instanceof String s) {
                obj.addProperty(key, s);
            } else if (value instanceof Boolean b) {
                obj.addProperty(key, b);
            } else if (value instanceof Number n) {
                obj.addProperty(key, n);
            } else if (value instanceof Character c) {
                obj.addProperty(key, c);
            } else if (value instanceof JsonElement e) {
                obj.add(key, e);
            } else if (value == null) {
                obj.addProperty(key, (String)null);
            } else {
                obj.addProperty(key, value.toString());
            }
        }
        return obj;
    }

    public static JsonObject parseBody(Request request) {
        return parseBody(request.body());
    }

    public static JsonObject parseBody(String bodyString) {
        JsonElement body = new JsonParser().parse(bodyString);

        //We must be given a json object, if not, fail.
        if (!body.isJsonObject()) throw new RequestException();
        return body.getAsJsonObject();
    }

}
