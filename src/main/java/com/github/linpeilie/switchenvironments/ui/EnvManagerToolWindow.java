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
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.jetbrains.annotations.NotNull;

public class EnvManagerToolWindow {
    private final JBPanel mainPanel;
    private final JBList<EnvGroup> groupList;
    private final JBTable variableTable;
    private final DefaultListModel<EnvGroup> listModel;
    private final DefaultTableModel tableModel;
    private final EnvManagerService envService;
    private final EnvGroupService groupService;
    private final JBLabel variableHeaderLabel;
    private final Splitter splitter;

    public EnvManagerToolWindow() {
        envService = EnvManagerService.getInstance();
        groupService = EnvGroupService.getInstance();

        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBackground(UIUtil.getPanelBackground());

        // Create list for groups with better styling
        listModel = new DefaultListModel<>();
        groupList = new JBList<>(listModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setCellRenderer(new GroupListCellRenderer());
        groupList.setBackground(UIUtil.getListBackground());
        groupList.setBorder(JBUI.Borders.empty());

        // Create table for variables with modern styling
        String[] columnNames = {"Name", "Value"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        variableTable = new JBTable(tableModel);
        variableTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        variableTable.setShowGrid(false);
        variableTable.setIntercellSpacing(JBUI.emptySize());
        variableTable.setRowHeight(JBUI.scale(24));
        variableTable.getColumnModel().getColumn(0).setPreferredWidth(JBUI.scale(200));
        variableTable.getColumnModel().getColumn(1).setPreferredWidth(JBUI.scale(400));
        variableTable.setDefaultRenderer(Object.class, new ModernTableCellRenderer());

        variableHeaderLabel = new JBLabel("Variables");
        variableHeaderLabel.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD));
        variableHeaderLabel.setForeground(UIUtil.getLabelForeground());

        splitter = new Splitter(false, 0.25f);
        splitter.setShowDividerControls(true);
        splitter.setShowDividerIcon(false);
        splitter.setDividerWidth(1);

        setupUI();
        setupActions();
        refreshData();
    }

    private void setupUI() {
        // Left panel with groups
        JBPanel leftPanel = createGroupPanel();

        // Right panel with variables
        JBPanel rightPanel = createVariablePanel();

        // Configure splitter
        splitter.setFirstComponent(leftPanel);
        splitter.setSecondComponent(rightPanel);

        mainPanel.add(splitter, BorderLayout.CENTER);
    }

    private JBPanel createGroupPanel() {
        JBPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(UIUtil.getPanelBackground());
        panel.setBorder(createTitledBorder("Groups"));

        // Scroll pane for group list
        JBScrollPane scrollPane = new JBScrollPane(groupList);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setBackground(UIUtil.getListBackground());
        panel.add(scrollPane, BorderLayout.CENTER);

        // Toolbar for group actions
        ActionToolbar toolbar = createGroupToolbar();
        JComponent toolbarComponent = toolbar.getComponent();
        //        toolbarComponent.setBorder(JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1, 0, 0, 0));
        panel.add(toolbarComponent, BorderLayout.SOUTH);

        return panel;
    }

    private JBPanel createVariablePanel() {
        JBPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(UIUtil.getPanelBackground());

        // Header with dynamic title
        JBPanel headerPanel = new JBPanel<>(new BorderLayout());
        headerPanel.setBackground(UIUtil.getPanelBackground());
        headerPanel.setBorder(JBUI.Borders.empty(8, 12));
        variableHeaderLabel.setBorder(JBUI.Borders.empty());
        headerPanel.add(variableHeaderLabel, BorderLayout.WEST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table in scroll pane
        JBScrollPane scrollPane = new JBScrollPane(variableTable);
        scrollPane.setBorder(createTitledBorder(""));
        scrollPane.setBackground(UIUtil.getTableBackground());
        panel.add(scrollPane, BorderLayout.CENTER);

        // Toolbar for variable actions
        ActionToolbar toolbar = createVariableToolbar();
        JComponent toolbarComponent = toolbar.getComponent();
        //        toolbarComponent.setBorder(JBUI.Borders.customLine(UIUtil.getBorderColor(), 1, 0, 0, 0));
        panel.add(toolbarComponent, BorderLayout.SOUTH);

        return panel;
    }

    private Border createTitledBorder(String title) {
        if (title.isEmpty()) {
            return JBUI.Borders.customLine(UIUtil.getWindowColor(), 1, 1, 1, 1);
        }
        return JBUI.Borders.customLine(UIUtil.getWindowColor(), 1, 1, 1, 1);
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

        // Mouse listener for toggle functionality
        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = groupList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    Rectangle cellBounds = groupList.getCellBounds(index, index);
                    EnvGroup group = listModel.getElementAt(index);

                    if (group != null && !"all_variables".equals(group.getId())) {
                        // Check if click was in the toggle area (right 60 pixels)
                        int toggleAreaWidth = JBUI.scale(60);
                        if (e.getX() >= cellBounds.x + cellBounds.width - toggleAreaWidth) {
                            toggleGroupActivation(group);
                        }
                    }
                }
            }
        });

        // Double-click to edit variable
        variableTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && variableTable.getSelectedRow() >= 0) {
                    editSelectedVariable();
                }
            }
        });
    }

    private void toggleGroupActivation(EnvGroup group) {
        boolean newState = !group.isActive();
        groupService.setGroupActive(group.getId(), newState);

        SwingUtilities.invokeLater(() -> {
            refreshGroupList();
            refreshAllVariablesViewIfNeeded();
        });
    }

    private ActionToolbar createGroupToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(createAction("Add Group", "Create a new environment group", AllIcons.General.Add,
            this::showAddGroupDialog));

        actionGroup.add(
            createAction("Edit Group", "Edit the selected group", AllIcons.Actions.Edit, this::editSelectedGroup,
                () -> {
                    EnvGroup selected = groupList.getSelectedValue();
                    return selected != null && !"all_variables".equals(selected.getId());
                }));

        actionGroup.add(createAction("Delete Group", "Delete the selected group", AllIcons.General.Remove,
            this::deleteSelectedGroup, () -> {
                EnvGroup selected = groupList.getSelectedValue();
                return selected != null && !"imported".equals(selected.getId()) &&
                       !"all_variables".equals(selected.getId());
            }));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("EnvGroupToolbar", actionGroup, true);
        toolbar.setTargetComponent(groupList);
        return toolbar;
    }

    private ActionToolbar createVariableToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(
            createAction("Add Variable", "Add a new environment variable", AllIcons.General.Add, this::addNewVariable,
                () -> {
                    EnvGroup selected = groupList.getSelectedValue();
                    return selected != null && !"all_variables".equals(selected.getId());
                }));

        actionGroup.add(createAction("Edit Variable", "Edit the selected variable", AllIcons.Actions.Edit,
            this::editSelectedVariable, () -> {
                EnvGroup selected = groupList.getSelectedValue();
                return selected != null && !"all_variables".equals(selected.getId()) &&
                       variableTable.getSelectedRow() >= 0;
            }));

        actionGroup.add(createAction("Delete Variable", "Delete the selected variable", AllIcons.General.Remove,
            this::deleteSelectedVariable, () -> {
                EnvGroup selected = groupList.getSelectedValue();
                return selected != null && !"all_variables".equals(selected.getId()) &&
                       variableTable.getSelectedRow() >= 0;
            }));

        actionGroup.addSeparator();

        actionGroup.add(
            createAction("Import File", "Import variables from file", AllIcons.Actions.Download, this::importFile,
                () -> {
                    EnvGroup selected = groupList.getSelectedValue();
                    return selected != null && !"all_variables".equals(selected.getId());
                }));

        ActionToolbar toolbar =
            ActionManager.getInstance().createActionToolbar("EnvVariableToolbar", actionGroup, true);
        toolbar.setTargetComponent(variableTable);
        return toolbar;
    }

    private AnAction createAction(String text, String description, Icon icon, Runnable action) {
        return createAction(text, description, icon, action, null);
    }

    private AnAction createAction(String text,
        String description,
        Icon icon,
        Runnable action,
        java.util.function.Supplier<Boolean> enabledSupplier) {
        return new AnAction(text, description, icon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                action.run();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                if (enabledSupplier != null) {
                    e.getPresentation().setEnabled(enabledSupplier.get());
                }
            }
        };
    }

    private void refreshData() {
        refreshGroupList();
        if (!listModel.isEmpty()) {
            groupList.setSelectedIndex(0);
            loadVariablesForGroup(listModel.getElementAt(0));
        }
    }

    private void refreshGroupList() {
        EnvGroup selectedGroup = groupList.getSelectedValue();
        listModel.clear();
        List<EnvGroup> groups = groupService.getEnvGroups();
        for (EnvGroup group : groups) {
            listModel.addElement(group);
        }

        // Restore selection
        if (selectedGroup != null) {
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.getElementAt(i).getId().equals(selectedGroup.getId())) {
                    groupList.setSelectedIndex(i);
                    break;
                }
            }
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
            Object[] rowData = {var.getName(), var.getValue()};
            tableModel.addRow(rowData);
        }
    }

    private void showAddGroupDialog() {
        showGroupDialog(null);
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
        if (selectedGroup != null && !"imported".equals(selectedGroup.getId()) &&
            !"all_variables".equals(selectedGroup.getId())) {

            int result = Messages.showYesNoDialog(mainPanel,
                "Are you sure you want to delete the group '" + selectedGroup.getName() + "'?", "Delete Group",
                Messages.getQuestionIcon());
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
            int result =
                Messages.showYesNoDialog(mainPanel, "Are you sure you want to delete the variable '" + varName + "'?",
                    "Delete Variable", Messages.getQuestionIcon());
            if (result == Messages.YES) {
                EnvVariable variable = envService.getVariablesByGroup(selectedGroup.getId())
                    .stream()
                    .filter(v -> v.getName().equals(varName))
                    .findFirst()
                    .orElse(null);
                if (variable != null) {
                    envService.removeEnvVariable(variable);
                    loadVariablesForGroup(selectedGroup);
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
        EnvGroup selectedGroup = groupList.getSelectedValue();
        if (selectedGroup == null || "all_variables".equals(selectedGroup.getId())) {
            Messages.showWarningDialog(mainPanel, "Please select a valid group to import variables into.",
                "No Group Selected");
            return;
        }

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
                envService.importFromFile(new File(file.getPath()), selectedGroup.getId());
                Messages.showInfoMessage(mainPanel,
                    "Environment variables imported successfully to group '" + selectedGroup.getName() + "'!",
                    "Import Complete");
                loadVariablesForGroup(selectedGroup);
                refreshAllVariablesViewIfNeeded();
            } catch (IOException e) {
                Messages.showErrorDialog(mainPanel, "Failed to import file: " + e.getMessage(), "Import Error");
            }
        }
    }

    public JComponent getContent() {
        return mainPanel;
    }

    // Modern list cell renderer for groups
    private class GroupListCellRenderer extends ColoredListCellRenderer<EnvGroup> {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends EnvGroup> list,
            EnvGroup value,
            int index,
            boolean selected,
            boolean hasFocus) {

            setBackground(selected ? UIUtil.getListSelectionBackground(true) : UIUtil.getListBackground());

            // Icon only for special group
            if ("all_variables".equals(value.getId())) {
                setIcon(AllIcons.Nodes.ConfigFolder);
            } else {
                setIcon(null);
            }

            // Group name
            if (value.isActive() || "all_variables".equals(value.getId())) {
                append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            } else {
                append(value.getName(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }

            // Toggle indicator for non-special groups
            if (!"all_variables".equals(value.getId())) {
                append("  ");
                if (value.isActive()) {
                    append("ON", new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, new Color(76, 175, 80)));
                } else {
                    append("OFF",
                        new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UIUtil.getInactiveTextColor()));
                }
            }

            setBorder(JBUI.Borders.empty(4, 8));
        }
    }

    // Modern table cell renderer
    private static class ModernTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setBorder(JBUI.Borders.empty(4, 8));

            if (isSelected) {
                setBackground(UIUtil.getTableSelectionBackground(true));
                setForeground(UIUtil.getTableSelectionForeground(true));
            } else {
                setBackground(row % 2 == 0 ? UIUtil.getTableBackground() : UIUtil.getTableBackground(true));
                setForeground(UIUtil.getTableForeground());
            }

            // Truncate long values with ellipsis
            if (value != null && column == 1) { // Value column
                String text = value.toString();
                if (text.length() > 50) {
                    setText(text.substring(0, 47) + "...");
                    setToolTipText(text);
                } else {
                    setToolTipText(null);
                }
            }

            return this;
        }
    }
}