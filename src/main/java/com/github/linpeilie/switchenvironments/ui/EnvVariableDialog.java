package com.github.linpeilie.switchenvironments.ui;

import com.github.linpeilie.switchenvironments.model.EnvGroup;
import com.github.linpeilie.switchenvironments.model.EnvVariable;
import com.github.linpeilie.switchenvironments.service.EnvManagerService;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nullable;

public class EnvVariableDialog extends DialogWrapper {
    private final JBTextField nameField;
    private final JBTextField valueField;
    private final EnvVariable originalVariable;
    private final EnvGroup group;
    private EnvVariable variable;

    public EnvVariableDialog(@Nullable EnvVariable variable, EnvGroup group) {
        super(true);
        this.originalVariable = variable;
        this.group = group;
        this.variable = variable != null ? copyVariable(variable) : new EnvVariable();

        nameField = new JBTextField(30);
        valueField = new JBTextField(30);

        setTitle(variable == null ? "Add Environment Variable" : "Edit Environment Variable");

        if (variable != null) {
            nameField.setText(variable.getName());
            valueField.setText(variable.getValue());
        } else {
            this.variable.setGroupId(group.getId());
        }

        init();
        validate();
    }

    private EnvVariable copyVariable(EnvVariable source) {
        EnvVariable copy = new EnvVariable();
        copy.setName(source.getName());
        copy.setValue(source.getValue());
        copy.setGroupId(source.getGroupId());
        return copy;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel("Name:"), nameField, true)
            .addLabeledComponent(new JBLabel("Value:"), valueField, true)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(JBUI.size(500, 250));

        return panel;
    }

    @Override
    protected void doOKAction() {
        if (validateInput()) {
            variable.setName(nameField.getText().trim());
            variable.setValue(valueField.getText());
            variable.setGroupId(group.getId());
            super.doOKAction();
        }
    }

    private boolean validateInput() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            setErrorText("Variable name cannot be empty");
            nameField.requestFocus();
            return false;
        }

        // Check for valid variable name (basic validation)
        if (!name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            setErrorText("Variable name must start with a letter or underscore and contain only letters, numbers, and underscores");
            nameField.requestFocus();
            return false;
        }

        // Check for duplicate names in the same group (except for current variable)
        EnvManagerService envService = EnvManagerService.getInstance();
        for (EnvVariable existingVar : envService.getVariablesByGroup(group.getId())) {
            if ((originalVariable == null || !existingVar.equals(originalVariable)) &&
                name.equals(existingVar.getName())) {
                setErrorText("Variable name already exists in this group");
                nameField.requestFocus();
                return false;
            }
        }

        setErrorText(null);
        return true;
    }

    public EnvVariable getVariable() {
        return variable;
    }
}