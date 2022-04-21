package com.njdaeger.authenticationhub.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import spark.Request;

public final class WebUtils {

    private WebUtils() {
    }

    public static final int OK = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int SERVER_ERROR = 500;

    public static JsonObject createObject(Object... values) {
        if (values.length % 2 != 0) throw new RuntimeException("Error creating json object. (key value mismatch)");
        JsonObject obj = new JsonObject();
        for (int i = 0; i < values.length; i+=2) {
            String key = values[i].toString();//this really SHOULD be a string- im going to treat it that way.
            Object value = values[i + 1];
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
