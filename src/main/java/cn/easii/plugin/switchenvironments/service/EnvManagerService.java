package cn.easii.plugin.switchenvironments.service;

import cn.easii.plugin.switchenvironments.model.EnvGroup;
import cn.easii.plugin.switchenvironments.model.EnvVariable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.PROJECT)
@State(name = "EnvManagerService", storages = @Storage("envSettings.xml"))
public final class EnvManagerService implements PersistentStateComponent<EnvManagerService.State> {

    public static class State {
        public List<EnvGroup> envGroups = new ArrayList<>();
    }

    private State state = new State();

    @Override
    public @Nullable State getState() {
        return this.state;
    }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }

    public void clearAllData() {
        state.envGroups.clear();
    }

    public List<EnvGroup> getEnvGroups() {
        return state.envGroups;
    }

    public void addEnvGroup(EnvGroup envGroup) {
        state.envGroups.add(envGroup);
    }

    public void removeEnvGroup(String groupId) {
        state.envGroups.removeIf(group -> group.getId().equals(groupId));
    }

    public void updateEnvGroup(EnvGroup envGroup) {
        for (int i = 0; i < state.envGroups.size(); i++) {
            if (state.envGroups.get(i).getId().equals(envGroup.getId())) {
                state.envGroups.set(i, envGroup);
                break;
            }
        }
    }

    public Optional<EnvGroup> getGroupById(String groupId) {
        return state.envGroups.stream().filter(group -> group.getId().equals(groupId)).findFirst();
    }

    public void setGroupActive(String groupId, boolean active) {
        getGroupById(groupId).ifPresent(envGroup -> envGroup.setActive(active));
    }

    public boolean isGroupActive(String groupId) {
        return getGroupById(groupId).map(EnvGroup::isActive).orElse(false);
    }

    /********************** variable *************************/

    private List<EnvVariable> allEnvVariables() {
        return state.envGroups.stream()
            .map(EnvGroup::getVariables)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    public void addEnvVariable(EnvVariable envVariable) {
        getGroupById(envVariable.getGroupId()).ifPresent(envGroup -> {
            envGroup.addVariable(envVariable);
        });
    }

    public void removeEnvVariable(EnvVariable envVariable) {
        getGroupById(envVariable.getGroupId()).ifPresent(envGroup -> {
            envGroup.getVariables().removeIf(variable -> variable.getName().equals(envVariable.getName()));
        });
    }

    public void updateEnvVariable(EnvVariable oldVar, EnvVariable newVar) {
        getGroupById(oldVar.getGroupId()).ifPresent(envGroup -> {
            for (int i = 0; i < envGroup.getVariables().size(); i++) {
                if (envGroup.getVariables().get(i).getName().equals(newVar.getName())) {
                    envGroup.getVariables().get(i).setValue(newVar.getValue());
                    return;
                }
            }
        });
    }

    public List<EnvVariable> getVariablesByGroup(String groupId) {
        return getGroupById(groupId).map(EnvGroup::getVariables).orElse(new ArrayList<>());
    }

    /**
     * 获取激活的环境变量列表
     */
    public List<EnvVariable> getActiveVariables() {
        return allEnvVariables().stream()
            .filter(var -> isGroupActive(var.getGroupId()))
            .collect(Collectors.toList());
    }

    /**
     * 导入变量
     */
    public void importEnvVariablesFromFile(File file, String targetGroupId) throws IOException {
        List<EnvVariable> importedVars = new ArrayList<>();
        String fileName = file.getName().toLowerCase();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip empty lines and comments (for .env and .properties files)
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                    continue;
                }

                // Parse KEY=VALUE format
                int equalIndex = line.indexOf('=');
                if (equalIndex > 0) {
                    String key = line.substring(0, equalIndex).trim();
                    String value = line.substring(equalIndex + 1).trim();

                    // Remove quotes if present (common in .env files)
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    } else if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }

                    // Handle escaped characters in properties files
                    if (fileName.endsWith(".properties")) {
                        value =
                            value.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
                        key = key.replace("\\ ", " ")
                            .replace("\\:", ":")
                            .replace("\\=", "=")
                            .replace("\\#", "#")
                            .replace("\\!", "!");
                    }

                    if (fileName.endsWith(".sh")) {
                        if (key.startsWith("export ") || key.startsWith("EXPORT ")) {
                            key = key.substring(7)
                                    .trim();
                        }
                    }

                    EnvVariable envVar = new EnvVariable(key, value, targetGroupId);
                    importedVars.add(envVar);
                }
            }
        }

        // Add imported variables
        for (EnvVariable var : importedVars) {
            addEnvVariable(var);
        }
    }

    public void reorderGroups(int fromIndex, int toIndex) {
        List<EnvGroup> groups = getEnvGroups();

        EnvGroup moved = groups.get(fromIndex - 1);
        groups.remove(moved);
        groups.add(toIndex - 1, moved);

        saveGroupsOrder(groups);
    }

    private void saveGroupsOrder(List<EnvGroup> orderedGroups) {
        // 这里实现具体存储逻辑，例如：
        // 1. 更新每个分组的order字段
        // 2. 将新顺序保存到配置文件
        // 示例伪代码：
        for (int i = 0; i < orderedGroups.size(); i++) {
            orderedGroups.get(i).setOrder(i);
        }
        clearAllData();
        orderedGroups.forEach(this::addEnvGroup);
    }
}
