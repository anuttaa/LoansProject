package config;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.hibernate.proxy.HibernateProxy;

import java.io.IOException;
import java.lang.reflect.Type;

public class HibernateProxyTypeAdapter  implements JsonSerializer<HibernateProxy> {
    @Override
    public JsonElement serialize(HibernateProxy proxy, Type type, JsonSerializationContext context) {
        if (proxy == null) {
            return JsonNull.INSTANCE;
        }
        // Получаем реальный объект из прокси
        Object original = proxy.getHibernateLazyInitializer().getImplementation();
        return context.serialize(original);
    }
}
