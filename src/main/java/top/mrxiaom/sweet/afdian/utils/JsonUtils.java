package top.mrxiaom.sweet.afdian.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonUtils {
    public static Integer optInt(@Nullable JsonObject obj, @NotNull String key, Integer def) {
        JsonElement element = obj == null ? null : obj.get(key);
        if (element != null && element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.isNumber() ? Integer.valueOf(primitive.getAsInt()) : def;
        }
        return def;
    }
    public static Long optLong(@Nullable JsonObject obj, @NotNull String key, Long def) {
        JsonElement element = obj == null ? null : obj.get(key);
        if (element != null && element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.isNumber() ? Long.valueOf(primitive.getAsLong()) : def;
        }
        return def;
    }
    public static String optString(@Nullable JsonObject obj, @NotNull String key, String def) {
        JsonElement element = obj == null ? null : obj.get(key);
        if (element != null && element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.isString() ? primitive.getAsString() : def;
        }
        return def;
    }
    public static JsonObject optObject(@Nullable JsonObject obj, @NotNull String key) {
        JsonElement element = obj == null ? null : obj.get(key);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return new JsonObject();
    }
    public static JsonArray optArray(@Nullable JsonObject obj, @NotNull String key) {
        JsonElement element = obj == null ? null : obj.get(key);
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        return new JsonArray();
    }
}
