package com.github.linpeilie.switchenvironments.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class EnvManagerToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        EnvManagerToolWindow envManagerToolWindow = new EnvManagerToolWindow();
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(envManagerToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}