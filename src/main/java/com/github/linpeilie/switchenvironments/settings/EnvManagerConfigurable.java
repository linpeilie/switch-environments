package com.github.linpeilie.switchenvironments.settings;

import com.github.linpeilie.switchenvironments.model.EnvGroup;
import com.github.linpeilie.switchenvironments.service.EnvGroupService;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

public class EnvManagerConfigurable implements Configurable {
    private JBPanel mainPanel;
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

        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBackground(UIUtil.getPanelBackground());

        // Title with modern styling
        JBLabel titleLabel = new JBLabel("Active Environment Groups");
        titleLabel.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(UIUtil.getLabelForeground());
        titleLabel.setBorder(JBUI.Borders.emptyBottom(16));

        // Description
        JBLabel descLabel = new JBLabel("Select which environment variable groups should be active:");
        descLabel.setFont(JBUI.Fonts.label());
        descLabel.setForeground(UIUtil.getInactiveTextColor());
        descLabel.setBorder(JBUI.Borders.emptyBottom(12));

        // Groups panel with modern layout
        JBPanel groupsPanel = new JBPanel<>();
        groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));
        groupsPanel.setBackground(UIUtil.getPanelBackground());

        List<EnvGroup> groups = groupService.getEnvGroups();
        for (EnvGroup group : groups) {
            // Skip the "all variables" group as it's always active and read-only
            if ("all_variables".equals(group.getId())) {
                continue;
            }

            JBCheckBox checkBox = new JBCheckBox(group.getName());
            checkBox.setSelected(group.isActive());
            checkBox.setFont(JBUI.Fonts.label());
            checkBox.setBorder(JBUI.Borders.empty(4, 0));
            checkBox.setBackground(UIUtil.getPanelBackground());

            groupCheckBoxes.put(group.getId(), checkBox);
            groupsPanel.add(checkBox);
        }

        // Wrap in scroll pane for many groups
        JBScrollPane scrollPane = new JBScrollPane(groupsPanel);
        scrollPane.setBorder(JBUI.Borders.customLine(UIUtil.getWindowColor(), 1));
        scrollPane.setBackground(UIUtil.getPanelBackground());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Build the main panel
        FormBuilder formBuilder = FormBuilder.createFormBuilder()
            .addComponent(titleLabel)
            .addComponent(descLabel)
            .addComponentFillVertically(scrollPane, 1);

        JPanel contentPanel = formBuilder.getPanel();
        contentPanel.setBorder(JBUI.Borders.empty(20));
        contentPanel.setBackground(UIUtil.getPanelBackground());

        mainPanel.add(contentPanel, BorderLayout.CENTER);

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