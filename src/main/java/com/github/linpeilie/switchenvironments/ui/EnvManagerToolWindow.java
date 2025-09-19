package com.github.linpeilie.switchenvironments.ui;

import com.github.linpeilie.switchenvironments.model.EnvGroup;
import com.github.linpeilie.switchenvironments.model.EnvVariable;
import com.github.linpeilie.switchenvironments.service.EnvGroupService;
import com.github.linpeilie.switchenvironments.service.EnvManagerService;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.jetbrains.annotations.NotNull;

public class EnvManagerToolWindow {
    private final JPanel mainPanel;
    private final JBList<EnvGroup> groupList;
    private final JBTable variableTable;
    private final DefaultListModel<EnvGroup> listModel;
    private final DefaultTableModel tableModel;
    private final EnvManagerService envService;
    private final EnvGroupService groupService;
    private final JLabel variableHeaderLabel;

    public EnvManagerToolWindow() {
        envService = EnvManagerService.getInstance();
        groupService = EnvGroupService.getInstance();

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(8));

        // Create list for groups
        listModel = new DefaultListModel<>();
        groupList = new JBList<>(listModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setCellRenderer(new GroupListCellRenderer());

        // Create table for variables
        String[] columnNames = {"Name", "Value"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };

        variableTable = new JBTable(tableModel);
        variableTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        variableTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        variableTable.getColumnModel().getColumn(1).setPreferredWidth(350);

        variableHeaderLabel = new JLabel("Variables");
        variableHeaderLabel.setFont(variableHeaderLabel.getFont().deriveFont(Font.BOLD));

        setupUI();
        setupActions();
        refreshData();
    }

    private void setupUI() {
        // Left panel with groups list
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, -1));
        leftPanel.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 1));

        leftPanel.add(new JBScrollPane(groupList), BorderLayout.CENTER);

        // Group actions toolbar
        ActionToolbar groupToolbar = createGroupToolbar();
        leftPanel.add(groupToolbar.getComponent(), BorderLayout.SOUTH);

        // Right panel with variables table
        JPanel rightPanel = new JPanel(new BorderLayout());

        variableHeaderLabel.setBorder(JBUI.Borders.empty(0, 0, 5, 0));
        rightPanel.add(variableHeaderLabel, BorderLayout.NORTH);
        rightPanel.add(new JBScrollPane(variableTable), BorderLayout.CENTER);

        // Variable actions toolbar
        ActionToolbar variableToolbar = createVariableToolbar();
        rightPanel.add(variableToolbar.getComponent(), BorderLayout.SOUTH);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(200);
        splitPane.setResizeWeight(0.25);

        mainPanel.add(splitPane, BorderLayout.CENTER);
    }

    private void setupActions() {
        // List selection listener
        groupList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    EnvGroup selectedGroup = groupList.getSelectedValue();
                    if (selectedGroup != null) {
                        loadVariablesForGroup(selectedGroup);
                    }
                }
            }
        });

        // Double-click to edit variable
        variableTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedVariable();
                }
            }
        });
    }

    private ActionToolbar createGroupToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Add Group", "Add new environment group", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showGroupDialog(null);
            }
        });

        actionGroup.add(new AnAction("Edit Group", "Edit selected group", AllIcons.Actions.Edit) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                editSelectedGroup();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                EnvGroup selected = groupList.getSelectedValue();
                e.getPresentation().setEnabled(selected != null &&
                                               !"all_variables".equals(selected.getId()));
            }
        });

        actionGroup.add(new AnAction("Delete Group", "Delete selected group", AllIcons.General.Remove) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                deleteSelectedGroup();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                EnvGroup selected = groupList.getSelectedValue();
                e.getPresentation().setEnabled(selected != null &&
                                               !"imported".equals(selected.getId()) &&
                                               !"all_variables".equals(selected.getId()));
            }
        });

        actionGroup.add(new AnAction("Import File", "Import variables from file", AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                importFile();
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance()
            .createActionToolbar("EnvGroupToolbar", actionGroup, true);
        toolbar.setTargetComponent(mainPanel); // ✅ 显式绑定目标组件
        return toolbar;
    }

    private ActionToolbar createVariableToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Add Variable", "Add new environment variable", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                addNewVariable();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                EnvGroup selected = groupList.getSelectedValue();
                e.getPresentation().setEnabled(selected != null &&
                                               !"all_variables".equals(selected.getId()));
            }
        });

        actionGroup.add(new AnAction("Edit Variable", "Edit selected variable", AllIcons.Actions.Edit) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                editSelectedVariable();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                EnvGroup selected = groupList.getSelectedValue();
                e.getPresentation().setEnabled(selected != null &&
                                               !"all_variables".equals(selected.getId()) &&
                                               variableTable.getSelectedRow() >= 0);
            }
        });

        actionGroup.add(new AnAction("Delete Variable", "Delete selected variable", AllIcons.General.Remove) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                deleteSelectedVariable();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                EnvGroup selected = groupList.getSelectedValue();
                e.getPresentation().setEnabled(selected != null &&
                                               !"all_variables".equals(selected.getId()) &&
                                               variableTable.getSelectedRow() >= 0);
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance()
            .createActionToolbar("EnvVariableToolbar", actionGroup, true);
        toolbar.setTargetComponent(mainPanel); // ✅ 显式绑定目标组件
        return toolbar;
    }

    private void refreshData() {
        refreshGroupList();
        // Load first group variables (环境变量 group)
        if (!listModel.isEmpty()) {
            groupList.setSelectedIndex(0);
            loadVariablesForGroup(listModel.getElementAt(0));
        }
    }

    private void refreshGroupList() {
        listModel.clear();
        List<EnvGroup> groups = groupService.getEnvGroups();
        for (EnvGroup group : groups) {
            listModel.addElement(group);
        }
    }

    private void loadVariablesForGroup(EnvGroup group) {
        tableModel.setRowCount(0);
        variableHeaderLabel.setText(group.getName());

        List<EnvVariable> variables;
        if ("all_variables".equals(group.getId())) {
            variables = envService.getActiveVariables();
        } else {
            variables = envService.getVariablesByGroup(group.getId());
        }

        for (EnvVariable var : variables) {
            Object[] rowData = {
                var.getName(),
                var.getValue()
            };
            tableModel.addRow(rowData);
        }
    }

    private void showGroupDialog(EnvGroup group) {
        EnvGroupDialog dialog = new EnvGroupDialog(group);
        if (dialog.showAndGet()) {
            EnvGroup result = dialog.getGroup();
            if (group == null) {
                groupService.addEnvGroup(result);
            } else {
                groupService.updateEnvGroup(result);
            }
            refreshGroupList();
        }
    }

    private void editSelectedGroup() {
        EnvGroup selectedGroup = groupList.getSelectedValue();
        if (selectedGroup != null && !"all_variables".equals(selectedGroup.getId())) {
            showGroupDialog(selectedGroup);
        }
    }

    private void deleteSelectedGroup() {
        EnvGroup selectedGroup = groupList.getSelectedValue();
        if (selectedGroup != null &&
            !"imported".equals(selectedGroup.getId()) &&
            !"all_variables".equals(selectedGroup.getId())) {

            int result = Messages.showYesNoDialog(
                "Are you sure you want to delete the group '" + selectedGroup.getName() + "'?",
                "Confirm Delete",
                Messages.getQuestionIcon()
            );
            if (result == Messages.YES) {
                groupService.removeEnvGroup(selectedGroup.getId());
                refreshGroupList();
                refreshData();
            }
        }
    }

    private void addNewVariable() {
        EnvGroup selectedGroup = groupList.getSelectedValue();
        if (selectedGroup != null && !"all_variables".equals(selectedGroup.getId())) {
            showVariableDialog(null, selectedGroup);
        } else {
            Messages.showWarningDialog("Please select a modifiable group first.", "No Group Selected");
        }
    }

    private void editSelectedVariable() {
        int selectedRow = variableTable.getSelectedRow();
        EnvGroup selectedGroup = groupList.getSelectedValue();

        if (selectedRow >= 0 && selectedGroup != null && !"all_variables".equals(selectedGroup.getId())) {
            String varName = (String) tableModel.getValueAt(selectedRow, 0);
            EnvVariable variable = envService.getVariablesByGroup(selectedGroup.getId())
                .stream()
                .filter(v -> v.getName().equals(varName))
                .findFirst()
                .orElse(null);
            if (variable != null) {
                showVariableDialog(variable, selectedGroup);
            }
        }
    }

    private void deleteSelectedVariable() {
        int selectedRow = variableTable.getSelectedRow();
        EnvGroup selectedGroup = groupList.getSelectedValue();

        if (selectedRow >= 0 && selectedGroup != null && !"all_variables".equals(selectedGroup.getId())) {
            String varName = (String) tableModel.getValueAt(selectedRow, 0);
            int result = Messages.showYesNoDialog(
                "Are you sure you want to delete the variable '" + varName + "'?",
                "Confirm Delete",
                Messages.getQuestionIcon()
            );
            if (result == Messages.YES) {
                EnvVariable variable = envService.getVariablesByGroup(selectedGroup.getId())
                    .stream()
                    .filter(v -> v.getName().equals(varName))
                    .findFirst()
                    .orElse(null);
                if (variable != null) {
                    envService.removeEnvVariable(variable);
                    loadVariablesForGroup(selectedGroup);
                    // Refresh all variables view if it's currently selected
                    refreshAllVariablesViewIfNeeded();
                }
            }
        }
    }

    private void showVariableDialog(EnvVariable variable, EnvGroup group) {
        EnvVariableDialog dialog = new EnvVariableDialog(variable, group);
        if (dialog.showAndGet()) {
            EnvVariable result = dialog.getVariable();
            if (variable == null) {
                envService.addEnvVariable(result);
            } else {
                envService.updateEnvVariable(variable, result);
            }
            loadVariablesForGroup(group);
            // Refresh all variables view if it's currently selected
            refreshAllVariablesViewIfNeeded();
        }
    }

    private void refreshAllVariablesViewIfNeeded() {
        EnvGroup selectedGroup = groupList.getSelectedValue();
        if (selectedGroup != null && "all_variables".equals(selectedGroup.getId())) {
            loadVariablesForGroup(selectedGroup);
        }
    }

    private void importFile() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        descriptor.setTitle("Import Environment Variables");
        descriptor.setDescription("Select a .env, .properties, or .txt file to import environment variables");
        descriptor.withFileFilter(file -> {
            String name = file.getName().toLowerCase();
            return name.endsWith(".env") || name.endsWith(".properties") || name.endsWith(".txt");
        });

        VirtualFile file = FileChooser.chooseFile(descriptor, null, null);
        if (file != null) {
            try {
                envService.importFromFile(new File(file.getPath()));
                Messages.showInfoMessage("Environment variables imported successfully!", "Import Complete");
                refreshData();
                refreshAllVariablesViewIfNeeded();
            } catch (IOException e) {
                Messages.showErrorDialog("Failed to import file: " + e.getMessage(), "Import Error");
            }
        }
    }

    public JPanel getContent() {
        return mainPanel;
    }

    // Custom list cell renderer for groups with toggle button functionality
    private class GroupListCellRenderer extends JPanel implements ListCellRenderer<EnvGroup> {
        private final JLabel nameLabel;
        private final JToggleButton toggleButton;

        public GroupListCellRenderer() {
            setLayout(new BorderLayout());
            setBorder(JBUI.Borders.empty(4, 8));

            nameLabel = new JLabel();
            toggleButton = new JToggleButton();
            toggleButton.setPreferredSize(new Dimension(20, 20));
            toggleButton.setText("");
            toggleButton.setFocusable(false);

            add(nameLabel, BorderLayout.CENTER);
            add(toggleButton, BorderLayout.EAST);

            // Add action listener for toggle button
            toggleButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int index = groupList.getSelectedIndex();
                    if (index >= 0) {
                        EnvGroup group = listModel.getElementAt(index);
                        if (!"all_variables".equals(group.getId())) {
                            groupService.setGroupActive(group.getId(), toggleButton.isSelected());
                            refreshAllVariablesViewIfNeeded();
                        }
                    }
                }
            });
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends EnvGroup> list, EnvGroup value, int index,
            boolean isSelected, boolean cellHasFocus) {

            nameLabel.setText(value.getName());

            // Set icon only for "环境变量" group
            if ("all_variables".equals(value.getId())) {
                nameLabel.setIcon(AllIcons.Nodes.ConfigFolder);
                toggleButton.setVisible(false); // No toggle for this group
            } else {
                nameLabel.setIcon(null);
                toggleButton.setVisible(true);
                toggleButton.setSelected(value.isActive());
            }

            // Set colors based on selection and activation state
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                nameLabel.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                if (value.isActive() || "all_variables".equals(value.getId())) {
                    nameLabel.setForeground(list.getForeground());
                    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN));
                } else {
                    nameLabel.setForeground(Color.GRAY);
                    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.ITALIC));
                }
            }

            setOpaque(true);
            return this;
        }
    }
}