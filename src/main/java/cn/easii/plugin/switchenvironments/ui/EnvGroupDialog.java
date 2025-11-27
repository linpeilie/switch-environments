package cn.easii.plugin.switchenvironments.ui;

import cn.easii.plugin.switchenvironments.model.EnvGroup;
import cn.easii.plugin.switchenvironments.service.EnvManagerService;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nullable;

public class EnvGroupDialog extends DialogWrapper {
    private final JBTextField nameField;
    private final JBCheckBox activeCheckBox;
    private final EnvManagerService envManagerService;
    private EnvGroup group;

    public EnvGroupDialog(@Nullable EnvGroup group,
        @Nullable EnvManagerService envManagerService) {
        super(true);
        this.envManagerService = envManagerService;
        this.group = group != null ? copyGroup(group) : new EnvGroup();

        nameField = new JBTextField(30);
        activeCheckBox = new JBCheckBox("Active");

        setTitle(group == null ? "Add Environment Group" : "Edit Environment Group");

        if (group != null) {
            nameField.setText(group.getName());
            activeCheckBox.setSelected(group.isActive());

            // Disable editing of special groups
            if ("all_variables".equals(group.getId())) {
                nameField.setEditable(false);
                activeCheckBox.setEnabled(false); // All variables group is always active
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
        copy.setActive(source.isActive());
        copy.setVariables(source.getVariables());
        return copy;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel nameFieldPanel = new JPanel(new BorderLayout());
        nameFieldPanel.add(nameField, BorderLayout.CENTER);

        JPanel panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel("Name:"), nameFieldPanel, true)
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
        for (EnvGroup existingGroup : envManagerService.getEnvGroups()) {
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
