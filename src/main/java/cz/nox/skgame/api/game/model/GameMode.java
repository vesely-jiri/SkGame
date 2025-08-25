package cz.nox.skgame.api.game.model;

import org.skriptlang.skript.lang.script.Script;

import java.util.HashMap;
import java.util.Map;

public class GameMode {
    private boolean enabled;
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

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean b) {
        this.enabled = b;
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
    public void setInfo(Map<String, Object> values) {
        this.values = values;
    }
}
