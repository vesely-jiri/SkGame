package cz.nox.skgame.api.game.model;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.SerializedVariable;
import cz.nox.skgame.api.game.model.type.CancellableEventType;
import cz.nox.skgame.api.game.model.type.TeamAssignmentMode;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MiniGame implements ConfigurationSerializable {
    private String id;
    private Map<String, Object> values;
    private Map<String, CustomValue> gameMapValueDefs = new LinkedHashMap<>();
    private Map<String, CustomValue> sessionValueDefs = new LinkedHashMap<>();
    private Set<MinigameTag> tags = EnumSet.noneOf(MinigameTag.class);
    private Set<CancellableEventType> cancelledEvents = EnumSet.noneOf(CancellableEventType.class);
    private List<TeamEntry> teams = new ArrayList<>();
    private TeamAssignmentMode teamAssignment = TeamAssignmentMode.AUTO;

    public MiniGame(String id,Map<String, Object> values) {
        this.id = id;
        this.values = values;
    }
    public MiniGame(String id) {
        this(id.toLowerCase(), new HashMap<>());
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id.toLowerCase();
    }

    public Object getValue(String key) {
        return values.get(key);
    }
    public String[] getKeys() {
        return values.keySet().toArray(new String[0]);
    }
    public Object[] getValues() {
        return values.values().toArray();
    }
    public void setValue(String key, Object o) {
        values.put(key,o);
    }
    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
    public void removeValue(String key) {
        this.values.remove(key);
    }
    public void removeValues() {
        this.values.clear();
    }

    public Map<String, CustomValue> getGameMapValueDefs() {
        return gameMapValueDefs;
    }
    public @Nullable CustomValue getGameMapValueDef(String key) {
        return gameMapValueDefs.get(key);
    }
    public void setGameMapValueDef(String key, @Nullable CustomValue cv) {
        if (cv == null) gameMapValueDefs.remove(key);
        else gameMapValueDefs.put(key, cv);
    }
    public void setGameMapValueDefs(Map<String, CustomValue> defs) {
        this.gameMapValueDefs = (defs != null) ? defs : new LinkedHashMap<>();
    }

    public Map<String, CustomValue> getSessionValueDefs() {
        return sessionValueDefs;
    }
    public @Nullable CustomValue getSessionValueDef(String key) {
        return sessionValueDefs.get(key);
    }
    public void setSessionValueDef(String key, @Nullable CustomValue cv) {
        if (cv == null) sessionValueDefs.remove(key);
        else sessionValueDefs.put(key, cv);
    }
    public void setSessionValueDefs(Map<String, CustomValue> defs) {
        this.sessionValueDefs = (defs != null) ? defs : new LinkedHashMap<>();
    }

    public Set<MinigameTag> getTags() {
        return tags;
    }
    public void setTags(Set<MinigameTag> tags) {
        this.tags = tags != null ? tags : EnumSet.noneOf(MinigameTag.class);
    }
    public void addTag(MinigameTag tag) {
        if (tag != null) tags.add(tag);
    }
    public void removeTag(MinigameTag tag) {
        tags.remove(tag);
    }

    public Set<CancellableEventType> getCancelledEvents() {
        return cancelledEvents;
    }
    public void setCancelledEvents(Set<CancellableEventType> events) {
        this.cancelledEvents = events != null ? events : EnumSet.noneOf(CancellableEventType.class);
    }

    /** Derived view — returns team ids in declaration order. All existing callers unchanged. */
    public List<String> getTeams() {
        return teams.stream().map(TeamEntry::getId).toList();
    }
    public List<TeamEntry> getTeamEntries() {
        return Collections.unmodifiableList(teams);
    }
    public @Nullable TeamEntry getTeamEntry(String id) {
        for (TeamEntry t : teams) if (t.getId().equals(id)) return t;
        return null;
    }
    public void setTeamEntries(List<TeamEntry> entries) {
        this.teams = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
    }

    public TeamAssignmentMode getTeamAssignment() {
        return teamAssignment;
    }
    public void setTeamAssignment(TeamAssignmentMode mode) {
        this.teamAssignment = mode != null ? mode : TeamAssignmentMode.AUTO;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> gm = new HashMap<>();
        gm.put("id", this.id);

        Map<String, Object> serializedValues = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : this.values.entrySet()) {
            Object v = entry.getValue();
            SerializedVariable.Value serialized = Classes.serialize(v);
            if (serialized != null) {
                Map<String, Object> ser = new HashMap<>();
                ser.put("type", serialized.type);
                ser.put("data", serialized.data);
                serializedValues.put(entry.getKey(), ser);
            } else {
                serializedValues.put(entry.getKey(), v);
            }
        }
        gm.put("values", serializedValues);

        if (!gameMapValueDefs.isEmpty()) {
            Map<String, Object> defs = new LinkedHashMap<>();
            for (Map.Entry<String, CustomValue> e : gameMapValueDefs.entrySet()) {
                defs.put(e.getKey(), e.getValue().serialize());
            }
            gm.put("gamemap-values", defs);
        }

        if (!sessionValueDefs.isEmpty()) {
            Map<String, Object> defs = new LinkedHashMap<>();
            for (Map.Entry<String, CustomValue> e : sessionValueDefs.entrySet()) {
                defs.put(e.getKey(), e.getValue().serialize());
            }
            gm.put("session-values", defs);
        }

        if (!tags.isEmpty()) {
            gm.put("tags", tags.stream().map(MinigameTag::name)
                    .reduce((a, b) -> a + "," + b).orElse(""));
        }

        if (!cancelledEvents.isEmpty()) {
            gm.put("cancel-events", cancelledEvents.stream().map(CancellableEventType::name)
                    .reduce((a, b) -> a + "," + b).orElse(""));
        }

        if (!teams.isEmpty()) {
            Map<String, Object> teamsMap = new LinkedHashMap<>();
            for (TeamEntry te : teams) {
                Map<String, Object> td = new LinkedHashMap<>();
                if (te.getRawDisplayName() != null) td.put("name", te.getRawDisplayName());
                if (te.getIcon() != null) td.put("icon", te.getIcon().serialize());
                teamsMap.put(te.getId(), td);
            }
            gm.put("teams", teamsMap);
        }
        if (teamAssignment != TeamAssignmentMode.AUTO) {
            gm.put("team-assignment", teamAssignment.name());
        }

        return gm;
    }

    @SuppressWarnings("unchecked")
    public static MiniGame deserialize(Map<String, Object> gm) {
        if (gm == null) return null;
        String id = (String) gm.get("id");
        Object rawValues = gm.get("values");
        Map<String, Object> rawMap = new HashMap<>();
        if (rawValues instanceof Map) {
            rawMap = (Map<String, Object>) rawValues;
        } else if (rawValues instanceof MemorySection sec) {
            rawMap = sec.getValues(false);
        }

        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            Object raw = entry.getValue();
            boolean decoded = false;
            if (raw instanceof MemorySection rawSection) {
                Map<String, Object> data = rawSection.getValues(false);
                String typeObj = (String) data.get("type");
                Object rawBytes = data.get("data");
                if (typeObj != null && rawBytes instanceof byte[] bytes) {
                    ClassInfo<?> classInfo = Classes.getClassInfoNoError(typeObj);
                    if (classInfo != null) {
                        Serializer<?> ser = classInfo.getSerializer();
                        if (ser != null) {
                            Object obj = Classes.deserialize(classInfo, bytes);
                            if (obj != null) {
                                values.put(entry.getKey(), obj);
                                decoded = true;
                            }
                        }
                    }
                }
            }
            if (!decoded) {
                values.put(entry.getKey(), raw);
            }
        }

        MiniGame newGm = new MiniGame(id);
        newGm.setValues(values);

        Object rawDefs = gm.get("gamemap-values");
        Map<String, Object> defsMap = null;
        if (rawDefs instanceof MemorySection sec) {
            defsMap = sec.getValues(false);
        } else if (rawDefs instanceof Map<?, ?> m) {
            //noinspection unchecked
            defsMap = (Map<String, Object>) m;
        }
        if (defsMap != null) {
            Map<String, CustomValue> defs = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : defsMap.entrySet()) {
                Map<String, Object> cvMap = null;
                if (entry.getValue() instanceof MemorySection cvSec) {
                    cvMap = cvSec.getValues(false);
                } else if (entry.getValue() instanceof Map<?, ?> m) {
                    //noinspection unchecked
                    cvMap = (Map<String, Object>) m;
                }
                if (cvMap != null) {
                    defs.put(entry.getKey(), CustomValue.deserialize(cvMap));
                }
            }
            newGm.setGameMapValueDefs(defs);
        }

        Object rawSessionDefs = gm.get("session-values");
        Map<String, Object> sessionDefsMap = null;
        if (rawSessionDefs instanceof MemorySection sec) {
            sessionDefsMap = sec.getValues(false);
        } else if (rawSessionDefs instanceof Map<?, ?> m) {
            //noinspection unchecked
            sessionDefsMap = (Map<String, Object>) m;
        }
        if (sessionDefsMap != null) {
            Map<String, CustomValue> defs = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : sessionDefsMap.entrySet()) {
                Map<String, Object> cvMap = null;
                if (entry.getValue() instanceof MemorySection cvSec) {
                    cvMap = cvSec.getValues(false);
                } else if (entry.getValue() instanceof Map<?, ?> m) {
                    //noinspection unchecked
                    cvMap = (Map<String, Object>) m;
                }
                if (cvMap != null) {
                    defs.put(entry.getKey(), CustomValue.deserialize(cvMap));
                }
            }
            newGm.setSessionValueDefs(defs);
        }

        Object rawTags = gm.get("tags");
        if (rawTags instanceof String s && !s.isEmpty()) {
            Set<MinigameTag> tagSet = EnumSet.noneOf(MinigameTag.class);
            for (String part : s.split(",")) {
                try { tagSet.add(MinigameTag.valueOf(part.trim())); } catch (IllegalArgumentException ignored) {}
            }
            newGm.setTags(tagSet);
        }

        Object rawCancelEvents = gm.get("cancel-events");
        if (rawCancelEvents instanceof String s && !s.isEmpty()) {
            Set<CancellableEventType> evSet = EnumSet.noneOf(CancellableEventType.class);
            for (String part : s.split(",")) {
                try { evSet.add(CancellableEventType.valueOf(part.trim())); } catch (IllegalArgumentException ignored) {}
            }
            newGm.setCancelledEvents(evSet);
        }

        // Teams: new section format, with CSV fallback for legacy data
        Object rawTeams = gm.get("teams");
        if (rawTeams instanceof String s && !s.isEmpty()) {
            // Legacy CSV — id-only TeamEntry, no name/icon
            List<TeamEntry> entries = new ArrayList<>();
            for (String part : s.split(",")) entries.add(new TeamEntry(part.trim(), null, null));
            newGm.setTeamEntries(entries);
        } else {
            Map<String, Object> teamsMap = null;
            if (rawTeams instanceof MemorySection sec) teamsMap = sec.getValues(false);
            else if (rawTeams instanceof Map<?, ?> m) //noinspection unchecked
                teamsMap = (Map<String, Object>) m;
            if (teamsMap != null) {
                List<TeamEntry> entries = new ArrayList<>();
                for (Map.Entry<String, Object> e : teamsMap.entrySet()) {
                    String teamId = e.getKey();
                    String displayName = null;
                    ItemStack icon = null;
                    Object v = e.getValue();
                    if (v instanceof MemorySection sec) {
                        displayName = sec.getString("name");
                        Object iconRaw = sec.get("icon");
                        if (iconRaw instanceof MemorySection iconSec) {
                            try { icon = ItemStack.deserialize(iconSec.getValues(false)); } catch (Exception ignored) {}
                        }
                    } else if (v instanceof Map<?, ?> td) {
                        Object nameObj = td.get("name");
                        if (nameObj instanceof String) displayName = (String) nameObj;
                        Object iconObj = td.get("icon");
                        if (iconObj instanceof Map<?, ?>) {
                            try {
                                //noinspection unchecked
                                icon = ItemStack.deserialize((Map<String, Object>) iconObj);
                            } catch (Exception ignored) {}
                        }
                    }
                    entries.add(new TeamEntry(teamId, displayName, icon));
                }
                newGm.setTeamEntries(entries);
            }
        }

        Object rawMode = gm.get("team-assignment");
        if (rawMode instanceof String s) {
            try { newGm.setTeamAssignment(TeamAssignmentMode.valueOf(s)); }
            catch (IllegalArgumentException ignored) {}
        }

        return newGm;
    }
}
