package com.github.linpeilie.switchenvironments.service;

import com.github.linpeilie.switchenvironments.model.EnvGroup;
import com.github.linpeilie.switchenvironments.model.EnvVariable;
import com.intellij.openapi.project.Project;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EnvManagerService {

    private final EnvGroupService envGroupService;
    private final EnvVariableService envVariableService;

    public EnvManagerService(Project project) {
        envGroupService = project.getService(EnvGroupService.class);
        envVariableService = project.getService(EnvVariableService.class);
    }

    public void clearAllData() {
        envGroupService.clear();
        envVariableService.clear();
    }

    public List<EnvGroup> getEnvGroups() {
        return envGroupService.getEnvGroups();
    }

    public void addEnvGroup(EnvGroup envGroup) {
        envGroupService.addEnvGroup(envGroup);
    }

    public void removeEnvGroup(String groupId) {
        envGroupService.removeEnvGroup(groupId);
    }

    public void updateEnvGroup(EnvGroup envGroup) {
        envGroupService.updateEnvGroup(envGroup);
    }

    public Optional<EnvGroup> getGroupById(String groupId) {
        return envGroupService.getGroupById(groupId);
    }

    public void setGroupActive(String groupId, boolean active) {
        envGroupService.setGroupActive(groupId, active);
    }

    public boolean isGroupActive(String groupId) {
        return getGroupById(groupId).map(EnvGroup::isActive).orElse(false);
    }

    public void addEnvVariable(EnvVariable envVariable) {
        envVariableService.addEnvVariable(envVariable);
    }

    public void removeEnvVariable(EnvVariable envVariable) {
        envVariableService.removeEnvVariable(envVariable);
    }

    public void updateEnvVariable(EnvVariable oldVar, EnvVariable newVar) {
        envVariableService.updateEnvVariable(oldVar, newVar);
    }

    public List<EnvVariable> getVariablesByGroup(String groupId) {
        return envVariableService.getVariablesByGroup(groupId);
    }

    /**
     * 获取激活的环境变量列表
     */
    public List<EnvVariable> getActiveVariables() {
        return envVariableService.getAllVariables()
            .stream()
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

                    EnvVariable envVar = new EnvVariable(key, value, targetGroupId);
                    importedVars.add(envVar);
                }
            }
        }

        // Add imported variables
        for (EnvVariable var : importedVars) {
            envVariableService.addEnvVariable(var);
        }
    }

}
