package com.github.linpeilie.switchenvironments;

import com.github.linpeilie.switchenvironments.model.EnvVariable;
import com.github.linpeilie.switchenvironments.service.EnvManagerService;
import com.github.linpeilie.switchenvironments.service.EnvVariableService;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnvironmentInjector extends RunConfigurationExtension {

    private static final Logger LOG = Logger.getInstance(EnvironmentInjector.class);

    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(@NotNull T configuration,
        @NotNull JavaParameters params,
        @Nullable RunnerSettings runnerSettings) throws ExecutionException {
        if (configuration instanceof RunConfigurationBase) {
            EnvManagerService envManagerService = new EnvManagerService(configuration.getProject());

            List<EnvVariable> activeVariables = envManagerService.getActiveVariables();

            Map<String, String> env = params.getEnv();

            for (EnvVariable activeVariable : activeVariables) {
                if (env.containsKey(activeVariable.getName())) {
                    LOG.info("environment name : " + activeVariable.getName() + " already exists");
                } else {
                    env.put(activeVariable.getName(), activeVariable.getValue());
                    LOG.info(
                        "environment name : " + activeVariable.getName() + ", value : " + activeVariable.getValue() +
                        " injected.");
                }
            }
        }
    }

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase<?> configuration) {
        return true;
    }
}
