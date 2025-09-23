package cn.easii.plugin.switchenvironments.model;

import java.util.Objects;

public class EnvVariable {
    private String name;
    private String value;

    private String groupId;

    public EnvVariable() {
    }

    public EnvVariable(String name, String value, String groupId) {
        this.name = name;
        this.value = value;
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvVariable that = (EnvVariable) o;
        return Objects.equals(name, that.name) && Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, groupId);
    }

    @Override
    public String toString() {
        return "EnvVariable{" +
               "name='" + name + '\'' +
               ", value='" + value + '\'' +
               ", groupId='" + groupId + '\'' +
               '}';
    }
}