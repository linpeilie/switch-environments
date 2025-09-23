package cn.easii.plugin.switchenvironments.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class OpenEnvManagerAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow("Environment Variables");
            if (toolWindow != null) {
                toolWindow.activate(null);
            }
        }
    }
}
