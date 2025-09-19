package com.github.linpeilie.switchenvironments.settings;

import com.github.linpeilie.switchenvironments.model.EnvGroup;
import com.github.linpeilie.switchenvironments.service.EnvGroupService;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvManagerConfigurable implements Configurable {
    private JPanel mainPanel;
    private Map<String, JBCheckBox> groupCheckBoxes;
    private EnvGroupService groupService;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Environment Variables Manager";
    }

    @Override
    public @Nullable JComponent createComponent() {
        groupService = EnvGroupService.getInstance();
        groupCheckBoxes = new HashMap<>();

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));

        JLabel titleLabel = new JBLabel("Active Environment Groups:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

        JPanel groupsPanel = new JPanel();
        groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));

        List<EnvGroup> groups = groupService.getEnvGroups();
        for (EnvGroup group : groups) {
            // Skip the "all variables" group as it's always active and read-only
            if ("all_variables".equals(group.getId())) {
                continue;
            }

            JBCheckBox checkBox = new JBCheckBox(group.getName());
            checkBox.setSelected(group.isActive());
            groupCheckBoxes.put(group.getId(), checkBox);
            groupsPanel.add(checkBox);
        }

        FormBuilder formBuilder = FormBuilder.createFormBuilder()
            .addComponent(titleLabel)
            .addVerticalGap(10)
            .addComponent(groupsPanel)
            .addComponentFillVertically(new JPanel(), 0);

        mainPanel.add(formBuilder.getPanel(), BorderLayout.CENTER);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        if (groupService == null || groupCheckBoxes == null) {
            return false;
        }

        List<EnvGroup> groups = groupService.getEnvGroups();
        for (EnvGroup group : groups) {
            JBCheckBox checkBox = groupCheckBoxes.get(group.getId());
            if (checkBox != null && checkBox.isSelected() != group.isActive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        if (groupService != null && groupCheckBoxes != null) {
            List<EnvGroup> groups = groupService.getEnvGroups();
            for (EnvGroup group : groups) {
                JBCheckBox checkBox = groupCheckBoxes.get(group.getId());
                if (checkBox != null) {
                    groupService.setGroupActive(group.getId(), checkBox.isSelected());
                }
            }
        }
    }

    @Override
    public void reset() {
        if (groupService != null && groupCheckBoxes != null) {
            List<EnvGroup> groups = groupService.getEnvGroups();
            for (EnvGroup group : groups) {
                JBCheckBox checkBox = groupCheckBoxes.get(group.getId());
                if (checkBox != null) {
                    checkBox.setSelected(group.isActive());
                }
            }
        }
    }
}
