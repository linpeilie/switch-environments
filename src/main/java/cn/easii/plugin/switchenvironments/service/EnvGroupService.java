package cn.easii.plugin.switchenvironments.service;

import cn.easii.plugin.switchenvironments.model.EnvGroup;
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

@Service(Service.Level.PROJECT)
@State(name = "EnvGroupService", storages = @Storage("envGroupSettings.xml"))
public final class EnvGroupService implements PersistentStateComponent<EnvGroupService> {

    private List<EnvGroup> envGroups = new ArrayList<>();

    public EnvGroupService() {
    }

    @Override
    public @Nullable EnvGroupService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull EnvGroupService state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    void addEnvGroup(EnvGroup envGroup) {
        envGroups.removeIf(group -> group.getId().equals(envGroup.getId()));
        envGroup.setOrder(envGroups.size() + 1);
        envGroups.add(envGroup);
    }

    void removeEnvGroup(String groupId) {
        envGroups.removeIf(group -> group.getId().equals(groupId));
    }

    void updateEnvGroup(EnvGroup envGroup) {
        for (int i = 0; i < envGroups.size(); i++) {
            if (envGroups.get(i).getId().equals(envGroup.getId())) {
                envGroups.set(i, envGroup);
                break;
            }
        }
    }

    void setGroupActive(String groupId, boolean active) {
        getGroupById(groupId).ifPresent(group -> {
            group.setActive(active);
        });
    }

    List<EnvGroup> getEnvGroups() {
        return new ArrayList<>(envGroups);
    }

    Optional<EnvGroup> getGroupById(String groupId) {
        return envGroups.stream()
            .filter(group -> group.getId().equals(groupId))
            .findFirst();
    }

    public void clear() {
        envGroups.clear();
    }
}
