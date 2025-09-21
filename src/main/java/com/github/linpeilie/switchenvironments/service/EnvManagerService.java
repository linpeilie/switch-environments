package com.github.linpeilie.switchenvironments.service;

import com.github.linpeilie.switchenvironments.model.EnvVariable;
import com.intellij.openapi.application.ApplicationManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service
@State(name = "EnvManagerService", storages = @Storage("envManagerSettings.xml"))
public final class EnvManagerService implements PersistentStateComponent<EnvManagerService> {

    private List<EnvVariable> envVariables = new ArrayList<>();
    private final Map<String, String> currentEnvVariables = new HashMap<>();

    public static EnvManagerService getInstance() {
        return ApplicationManager.getApplication().getService(EnvManagerService.class);
    }

    @Override
    public @Nullable EnvManagerService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull EnvManagerService state) {
        XmlSerializerUtil.copyBean(state, this);
        updateCurrentEnvironmentVariables();
    }

    public List<EnvVariable> getEnvVariables() {
        return new ArrayList<>(envVariables);
    }

    public void setEnvVariables(List<EnvVariable> envVariables) {
        this.envVariables = new ArrayList<>(envVariables);
        updateCurrentEnvironmentVariables();
    }

    public void addEnvVariable(EnvVariable envVariable) {
        envVariables.removeIf(var -> var.getName().equals(envVariable.getName()) &&
                                     var.getGroupId().equals(envVariable.getGroupId()));
        envVariables.add(envVariable);
        updateCurrentEnvironmentVariables();
    }

    public void removeEnvVariable(EnvVariable envVariable) {
        envVariables.remove(envVariable);
        updateCurrentEnvironmentVariables();
    }

    public void updateEnvVariable(EnvVariable oldVar, EnvVariable newVar) {
        int index = envVariables.indexOf(oldVar);
        if (index >= 0) {
            envVariables.set(index, newVar);
            updateCurrentEnvironmentVariables();
        }
    }

    public List<EnvVariable> getVariablesByGroup(String groupId) {
        return envVariables.stream()
            .filter(var -> var.getGroupId() != null && var.getGroupId().equals(groupId))
            .collect(Collectors.toList());
    }

    public List<EnvVariable> getActiveVariables() {
        EnvGroupService groupService = EnvGroupService.getInstance();
        return envVariables.stream()
            .filter(var -> groupService.isGroupActive(var.getGroupId()))
            .collect(Collectors.toList());
    }

    public Map<String, String> getCurrentEnvVariables() {
        return new HashMap<>(currentEnvVariables);
    }

    public void updateCurrentEnvironmentVariables() {
        currentEnvVariables.clear();
        getActiveVariables().forEach(var -> {
            if (var.getName() != null && var.getValue() != null) {
                currentEnvVariables.put(var.getName(), var.getValue());
            }
        });
    }

    public void importFromFile(File file, String targetGroupId) throws IOException {
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
                        value = value.replace("\\n", "\n")
                            .replace("\\r", "\r")
                            .replace("\\t", "\t")
                            .replace("\\\\", "\\");
                        key = key.replace("\\ ", " ")
                            .replace("\\:", ":")
                            .replace("\\=", "=")
                            .replace("\\#", "#")
                            .replace("\\!", "!");
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


}