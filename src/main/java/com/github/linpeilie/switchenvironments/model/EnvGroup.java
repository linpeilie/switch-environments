package com.github.linpeilie.switchenvironments.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

public class EnvGroup {
    private String id;
    private String name;
    private String description;
    private boolean active;
    private List<EnvVariable> variables;
    private boolean editable = true;
    private boolean showAllVariables = false;

    public EnvGroup() {
        this.id = UUID.randomUUID().toString();
        this.variables = new ArrayList<>();
        this.active = true;
    }

    public EnvGroup(String name, String description) {
        this();
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<EnvVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<EnvVariable> variables) {
        this.variables = variables;
    }

    public void addVariable(EnvVariable variable) {
        variable.setGroupId(this.id);
        this.variables.add(variable);
    }

    public void removeVariable(EnvVariable variable) {
        this.variables.remove(variable);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isShowAllVariables() {
        return showAllVariables;
    }

    public void setShowAllVariables(boolean showAllVariables) {
        this.showAllVariables = showAllVariables;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnvGroup envGroup = (EnvGroup) o;
        return active == envGroup.active && editable == envGroup.editable &&
               showAllVariables == envGroup.showAllVariables && Objects.equals(id, envGroup.id) &&
               Objects.equals(name, envGroup.name) &&
               Objects.equals(description, envGroup.description) &&
               Objects.equals(variables, envGroup.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, active, variables, editable, showAllVariables);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EnvGroup.class.getSimpleName() + "[", "]")
            .add("active=" + active)
            .add("id='" + id + "'")
            .add("name='" + name + "'")
            .add("description='" + description + "'")
            .add("variables=" + variables)
            .add("editable=" + editable)
            .add("showAllVariables=" + showAllVariables)
            .toString();
    }
}