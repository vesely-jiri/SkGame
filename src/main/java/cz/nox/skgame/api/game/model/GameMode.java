package cz.nox.skgame.api.game.model;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.script.Script;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GameMode implements ConfigurationSerializable {
    private String id;
    private String name;
    private Script script;
    private Map<String, Object> values;

    public GameMode(String id, String name, Script script, Map<String, Object> values) {
        this.id = id;
        this.name = name;
        this.script = script;
        this.values = values;
    }
    public GameMode(String id) {
        this(id,null,null,new HashMap<>());
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Script getScript() {
        return script;
    }
    public void setScript(Script script) {
        this.script = script;
    }
    public Map<String, Object> getInfo() {
        return values;
    }
    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> gm = new HashMap<>();
        gm.put("id", this.id);
        gm.put("name", this.name);
        gm.put("script", this.script.nameAndPath());
        gm.put("values", this.values);
        return gm;
    }

    @SuppressWarnings("unchecked")
    public static GameMode deserialize(Map<String, Object> gm) {
        GameMode newGm = new GameMode((String) gm.get("id"));
        newGm.setName((String) gm.get("name"));
        newGm.setValues((Map<String, Object>) gm.get("values"));

        String scriptPath = (String) gm.get("script");
        File scriptFile = new File(Skript.getInstance().getScriptsFolder(), scriptPath + ".sk");
        Script gmScript = ScriptLoader.getScript(scriptFile);
        newGm.setScript(gmScript);

        return newGm;
    }
}
