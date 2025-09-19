package com.github.linpeilie.switchenvironments.ui;

import com.github.linpeilie.switchenvironments.model.EnvGroup;
import com.github.linpeilie.switchenvironments.service.EnvGroupService;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EnvGroupDialog extends DialogWrapper {
    private final JBTextField nameField;
    private final JBCheckBox activeCheckBox;
    private final EnvGroup originalGroup;
    private EnvGroup group;

    public EnvGroupDialog(@Nullable EnvGroup group) {
        super(true);
        this.originalGroup = group;
        this.group = group != null ? copyGroup(group) : new EnvGroup();

        nameField = new JBTextField(20);
        activeCheckBox = new JBCheckBox("Active");

        setTitle(group == null ? "Add Environment Group" : "Edit Environment Group");

        if (group != null) {
            nameField.setText(group.getName());
            activeCheckBox.setSelected(group.isActive());

            // Disable editing of special groups
            if ("imported".equals(group.getId()) || "all_variables".equals(group.getId())) {
                nameField.setEditable(false);
                if ("all_variables".equals(group.getId())) {
                    activeCheckBox.setEnabled(false); // All variables group is always active
                }
                setTitle("View Environment Group");
            }
        } else {
            activeCheckBox.setSelected(true);
        }

        init();
        validate();
    }

    private EnvGroup copyGroup(EnvGroup source) {
        EnvGroup copy = new EnvGroup();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        copy.setActive(source.isActive());
        copy.setVariables(source.getVariables());
        return copy;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel("Name:"), nameField, true)
            .addComponent(activeCheckBox)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(JBUI.size(350, 100));

        return panel;
    }

    @Override
    protected void doOKAction() {
        if (validateInput()) {
            group.setName(nameField.getText().trim());
            group.setActive(activeCheckBox.isSelected());
            super.doOKAction();
        }
    }

    private boolean validateInput() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            setErrorText("Group name cannot be empty");
            nameField.requestFocus();
            return false;
        }

        // Check for duplicate names (except for current group)
        EnvGroupService groupService = EnvGroupService.getInstance();
        for (EnvGroup existingGroup : groupService.getEnvGroups()) {
            if (!existingGroup.getId().equals(group.getId()) &&
                name.equals(existingGroup.getName())) {
                setErrorText("Group name already exists");
                nameField.requestFocus();
                return false;
            }
        }

        setErrorText(null);
        return true;
    }

    public EnvGroup getGroup() {
        return group;
    }
}
