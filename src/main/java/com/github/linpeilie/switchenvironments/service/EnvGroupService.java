package com.github.linpeilie.switchenvironments.service;

import com.github.linpeilie.switchenvironments.model.EnvGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service
@State(name = "EnvGroupService", storages = @Storage("envGroupSettings.xml"))
public final class EnvGroupService implements PersistentStateComponent<EnvGroupService> {

    private List<EnvGroup> envGroups = new ArrayList<>();

    public EnvGroupService() {
        // Create default groups
        if (envGroups.isEmpty()) {
            // Create all variables group (read-only, always active) - first position
            EnvGroup allVarsGroup = new EnvGroup("环境变量", "All active environment variables");
            allVarsGroup.setId("all_variables");
            allVarsGroup.setActive(true);
            allVarsGroup.setEditable(false);
            envGroups.add(allVarsGroup);
        }
    }

    public static EnvGroupService getInstance() {
        return ApplicationManager.getApplication().getService(EnvGroupService.class);
    }

    @Override
    public @Nullable EnvGroupService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull EnvGroupService state) {
        XmlSerializerUtil.copyBean(state, this);
        // Ensure default groups exist after loading
        ensureDefaultGroups();
    }

    private void ensureDefaultGroups() {
        boolean hasAllVars = envGroups.stream().anyMatch(g -> "all_variables".equals(g.getId()));

        if (!hasAllVars) {
            EnvGroup allVarsGroup = new EnvGroup("环境变量", "All active environment variables");
            allVarsGroup.setId("all_variables");
            allVarsGroup.setActive(true);
            allVarsGroup.setEditable(false);
            envGroups.add(0, allVarsGroup); // Always first
        }
    }

    public List<EnvGroup> getEnvGroups() {
        return new ArrayList<>(envGroups);
    }

    public void setEnvGroups(List<EnvGroup> envGroups) {
        this.envGroups = new ArrayList<>(envGroups);
    }

    public void addEnvGroup(EnvGroup envGroup) {
        envGroups.removeIf(group -> group.getId().equals(envGroup.getId()));
        envGroups.add(envGroup);
    }

    public void removeEnvGroup(String groupId) {
        envGroups.removeIf(group -> group.getId().equals(groupId));
    }

    public void updateEnvGroup(EnvGroup envGroup) {
        for (int i = 0; i < envGroups.size(); i++) {
            if (envGroups.get(i).getId().equals(envGroup.getId())) {
                envGroups.set(i, envGroup);
                break;
            }
        }
    }

    public Optional<EnvGroup> getGroupById(String groupId) {
        return envGroups.stream()
            .filter(group -> group.getId().equals(groupId))
            .findFirst();
    }

    public void setGroupActive(String groupId, boolean active) {
        getGroupById(groupId).ifPresent(group -> {
            group.setActive(active);
            // Trigger service update
            EnvManagerService.getInstance().updateCurrentEnvironmentVariables();
        });
    }

    public boolean isGroupActive(String groupId) {
        return getGroupById(groupId)
            .map(EnvGroup::isActive)
            .orElse(false);
    }

    public List<EnvGroup> getActiveGroups() {
        return envGroups.stream()
            .filter(EnvGroup::isActive)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}
