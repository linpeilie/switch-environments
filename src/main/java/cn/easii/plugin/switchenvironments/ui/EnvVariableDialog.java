package cn.easii.plugin.switchenvironments.ui;

import cn.easii.plugin.switchenvironments.model.EnvGroup;
import cn.easii.plugin.switchenvironments.model.EnvVariable;
import cn.easii.plugin.switchenvironments.service.EnvManagerService;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nullable;

public class EnvVariableDialog extends DialogWrapper {
    private final JBTextField nameField;
    private final JBTextField valueField;
    private final EnvVariable originalVariable;
    private final EnvGroup group;
    private EnvVariable variable;
    private final EnvManagerService envManagerService;

    public EnvVariableDialog(@Nullable EnvVariable variable, EnvGroup group, EnvManagerService envManagerService) {
        super(true);
        this.envManagerService = envManagerService;
        this.originalVariable = variable;
        this.group = group;
        this.variable = variable != null ? copyVariable(variable) : new EnvVariable();

        nameField = new JBTextField(30);
        valueField = new JBTextField(30);

        setTitle(variable == null ? "Add Environment Variable" : "Edit Environment Variable");

        if (variable != null) {
            nameField.setText(variable.getName());
            nameField.setEnabled(false);
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
        JPanel nameFieldPanel = new JPanel(new BorderLayout());
        nameFieldPanel.add(nameField, BorderLayout.CENTER);

        JPanel valueFieldPanel = new JPanel(new BorderLayout());
        valueFieldPanel.add(valueField, BorderLayout.CENTER);

        JPanel panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel("Name:"), nameFieldPanel, true)
            .addLabeledComponent(new JBLabel("Value:"), valueFieldPanel, true)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(JBUI.size(500, 150));

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

        // Check for duplicate names in the same group (except for current variable)
        for (EnvVariable existingVar : envManagerService.getVariablesByGroup(group.getId())) {
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