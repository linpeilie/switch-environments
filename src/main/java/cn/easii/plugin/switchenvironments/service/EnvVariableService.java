package cn.easii.plugin.switchenvironments.service;

import cn.easii.plugin.switchenvironments.model.EnvVariable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
@State(name = "EnvVariableService", storages = @Storage("envVariableSettings.xml"))
public final class EnvVariableService implements PersistentStateComponent<EnvVariableService> {

    private final List<EnvVariable> envVariables = new ArrayList<>();

    @Override
    public @NotNull EnvVariableService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull EnvVariableService state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    void addEnvVariable(EnvVariable envVariable) {
        envVariables.removeIf(var -> var.getName().equals(envVariable.getName()) &&
                                     var.getGroupId().equals(envVariable.getGroupId()));
        envVariables.add(envVariable);
    }

    void removeEnvVariable(EnvVariable envVariable) {
        envVariables.remove(envVariable);
    }

    void updateEnvVariable(EnvVariable oldVar, EnvVariable newVar) {
        int index = envVariables.indexOf(oldVar);
        if (index >= 0) {
            envVariables.set(index, newVar);
        }
    }

    List<EnvVariable> getVariablesByGroup(String groupId) {
        return envVariables.stream()
            .filter(var -> var.getGroupId() != null && var.getGroupId().equals(groupId))
            .collect(Collectors.toList());
    }

    public List<EnvVariable> getAllVariables() {
        return envVariables;
    }

    public void clear() {
        envVariables.clear();
    }
}