package cz.nox.skgame.core.region;

import cz.nox.skgame.api.region.Region;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class RegionFactory {

    private static final Map<Class<?>, Function<?, Region>> adapters = new LinkedHashMap<>();

    public static <T> void register(Class<T> sourceClass, Function<T, Region> adapter) {
        adapters.put(sourceClass, adapter);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static Region adapt(Object source) {
        for (Map.Entry<Class<?>, Function<?, Region>> entry : adapters.entrySet()) {
            if (entry.getKey().isInstance(source)) {
                Function<Object, Region> fn = (Function<Object, Region>) entry.getValue();
                return fn.apply(source);
            }
        }
        return null;
    }
}
