package com.github.linpeilie.switchenvironments.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

public class EnvGroup {
    private String id;
    private String name;
    private boolean active;
    private List<EnvVariable> variables;
    private int order;

    public EnvGroup() {
        this.id = UUID.randomUUID().toString();
        this.variables = new ArrayList<>();
        this.active = true;
    }

    public EnvGroup(String name) {
        this();
        this.name = name;
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnvGroup envGroup = (EnvGroup) o;
        return active == envGroup.active && order == envGroup.order && Objects.equals(id, envGroup.id) &&
               Objects.equals(name, envGroup.name) && Objects.equals(variables, envGroup.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, active, variables, order);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EnvGroup.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("name='" + name + "'")
            .add("active=" + active)
            .add("variables=" + variables)
            .add("order=" + order)
            .toString();
    }
}