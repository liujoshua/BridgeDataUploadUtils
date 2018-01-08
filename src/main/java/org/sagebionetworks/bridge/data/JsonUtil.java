package org.sagebionetworks.bridge.data;

import java.lang.reflect.Type;
import java.math.BigDecimal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import org.sagebionetworks.bridge.rest.gson.ByteArrayToBase64TypeAdapter;
import org.sagebionetworks.bridge.rest.gson.DateTimeTypeAdapter;
import org.sagebionetworks.bridge.rest.gson.LocalDateTypeAdapter;

public class JsonUtil {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .registerTypeAdapter(DateTime.class, new DateTimeTypeAdapter())
            .registerTypeAdapter(Double.class, new DoubleTypeAdapter())
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'z'")
            .setPrettyPrinting()
            .create();

    public static class DoubleTypeAdapter implements JsonSerializer<Double> {
        @Override
        public JsonElement serialize(final Double src, final Type typeOfSrc, final JsonSerializationContext context) {
            BigDecimal value = BigDecimal.valueOf(src);

            return new JsonPrimitive(value);
        }
    }
}
