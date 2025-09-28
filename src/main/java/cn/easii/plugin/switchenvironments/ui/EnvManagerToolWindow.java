package cn.easii.plugin.switchenvironments.ui;

import cn.easii.plugin.switchenvironments.model.EnvGroup;
import cn.easii.plugin.switchenvironments.model.EnvVariable;
import cn.easii.plugin.switchenvironments.service.EnvManagerService;
import cn.easii.plugin.switchenvironments.ui.render.SettingsActionGroup;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

public class EnvManagerToolWindow {

    private static final String ALL_VARIABLES_ID = "all_variables";

    private final JBPanel mainPanel;

    private final JBTable groupTable; // 替换为 JBTable
    private final DefaultTableModel groupTableModel; // 替换为 DefaultTableModel

    private final JBTable variableTable;
    private final DefaultTableModel tableModel;

    private final EnvManagerService envManagerService;

    private final JBLabel variableHeaderLabel;

    private final Splitter splitter;

    private final Project project;

    private final EnvGroup allVariablesGroup; // 单独存储特殊分组

    public EnvManagerToolWindow(Project project) {
        this.project = project;

        envManagerService = project.getService(EnvManagerService.class);

        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBackground(UIUtil.getPanelBackground());

        // 创建表格用于分组（替换原来的列表）
        groupTableModel = new DefaultTableModel(new Object[] {"Group"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        groupTable = new JBTable(groupTableModel);
        groupTable.setTableHeader(null); // 隐藏表头
        groupTable.setShowGrid(false); // 隐藏网格线
        groupTable.setRowHeight(JBUI.scale(24)); // 设置行高
        groupTable.setIntercellSpacing(JBUI.emptySize());
        groupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupTable.setBackground(UIUtil.getListBackground());
        groupTable.setBorder(JBUI.Borders.empty());
        // 设置自定义渲染器
        groupTable.setDefaultRenderer(Object.class, new GroupTableCellRenderer(this));
        // 设置列宽填满整个面板
        groupTable.getColumnModel().getColumn(0).setCellRenderer(new GroupTableCellRenderer(this));

        groupTable.setDragEnabled(true);
        groupTable.setDropMode(DropMode.INSERT_ROWS);
        groupTable.setTransferHandler(new GroupRowTransferHandler());

        // 创建表格用于变量
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
        splitter.setDividerWidth(1);

        // 创建特殊分组
        allVariablesGroup = new EnvGroup();
        allVariablesGroup.setId(ALL_VARIABLES_ID);
        allVariablesGroup.setName("环境变量");
        allVariablesGroup.setActive(true);

        setupUI();
        setupActions();
        refreshData();
    }

    private void setupUI() {
        JBPanel leftPanel = createGroupPanel();
        JBPanel rightPanel = createVariablePanel();

        splitter.setFirstComponent(leftPanel);
        splitter.setSecondComponent(rightPanel);

        mainPanel.add(splitter, BorderLayout.CENTER);
    }

    private JBPanel createGroupPanel() {
        JBPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(UIUtil.getPanelBackground());
        panel.setBorder(createTitledBorder("Groups"));

        ActionToolbar toolbar = createGroupToolbar();
        panel.add(toolbar.getComponent(), BorderLayout.NORTH);

        JBScrollPane scrollPane = new JBScrollPane(groupTable);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setBackground(UIUtil.getListBackground());
        scrollPane.setViewportBorder(JBUI.Borders.empty(5));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JBPanel createVariablePanel() {
        JBPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBackground(UIUtil.getPanelBackground());

        JBPanel headerPanel = new JBPanel<>(new BorderLayout());
        headerPanel.setBackground(UIUtil.getPanelBackground());
        headerPanel.setBorder(JBUI.Borders.empty(8, 12));
        variableHeaderLabel.setBorder(JBUI.Borders.empty());
        headerPanel.add(variableHeaderLabel, BorderLayout.WEST);

        ActionToolbar toolbar = createVariableToolbar();
        headerPanel.add(toolbar.getComponent(), BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);

        JBScrollPane scrollPane = new JBScrollPane(variableTable);
        scrollPane.setBorder(createTitledBorder(""));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private Border createTitledBorder(String title) {
        return JBUI.Borders.customLine(UIUtil.getWindowColor(), 1, 1, 1, 1);
    }

    private void setupActions() {
        groupTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                EnvGroup selectedGroup = getSelectedGroup();
                if (selectedGroup != null) {
                    loadVariablesForGroup(selectedGroup);
                }
            }
        });

        // 添加鼠标监听器来处理复选框点击
        groupTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = groupTable.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }

                EnvGroup group = (EnvGroup) groupTableModel.getValueAt(row, 0);
                Rectangle cellRect = groupTable.getCellRect(row, 0, true);

                // 计算复选框位置（右侧）
                int checkboxSize = 20;
                int checkboxX = cellRect.x + cellRect.width - checkboxSize - 5;
                int checkboxY = cellRect.y + (cellRect.height - checkboxSize) / 2;
                Rectangle checkboxBounds = new Rectangle(checkboxX, checkboxY, checkboxSize, checkboxSize);

                if (checkboxBounds.contains(e.getPoint()) && !ALL_VARIABLES_ID.equals(group.getId())) {
                    toggleGroupActivation(group);
                    groupTable.repaint(cellRect); // 重绘该行
                }
            }
        });

        // 双击编辑变量
        variableTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && variableTable.getSelectedRow() >= 0) {
                    editSelectedVariable();
                }
            }
        });
    }

    // 辅助方法：获取当前选中的分组
    private EnvGroup getSelectedGroup() {
        int row = groupTable.getSelectedRow();
        if (row >= 0) {
            return (EnvGroup) groupTableModel.getValueAt(row, 0);
        }
        return null;
    }

    public void toggleGroupActivation(EnvGroup group) {
        boolean newState = !group.isActive();
        envManagerService.setGroupActive(group.getId(), newState);

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
                    EnvGroup selected = getSelectedGroup();
                    return selected != null && !ALL_VARIABLES_ID.equals(selected.getId());
                }));

        actionGroup.add(createAction("Delete Group", "Delete the selected group", AllIcons.General.Remove,
            this::deleteSelectedGroup, () -> {
                EnvGroup selected = getSelectedGroup();
                return selected != null && !ALL_VARIABLES_ID.equals(selected.getId());
            }));

        // 设置按钮（带下拉菜单）
        DefaultActionGroup settingsGroup = new SettingsActionGroup(); // true = popup
        settingsGroup.add(new AnAction("Import Config", "Import configuration", AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                importConfig();
            }
        });
        settingsGroup.add(new AnAction("Export Config", "Export configuration", AllIcons.Actions.Upload) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                exportConfig();
            }
        });

        actionGroup.addSeparator();
        actionGroup.add(settingsGroup);

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("EnvGroupToolbar", actionGroup, true);
        toolbar.setTargetComponent(groupTable);
        return toolbar;
    }

    private ActionToolbar createVariableToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(
            createAction("Add Variable", "Add a new environment variable", AllIcons.General.Add, this::addNewVariable,
                () -> {
                    EnvGroup selected = getSelectedGroup();
                    return selected != null && !ALL_VARIABLES_ID.equals(selected.getId());
                }));

        actionGroup.add(createAction("Edit Variable", "Edit the selected variable", AllIcons.Actions.Edit,
            this::editSelectedVariable, () -> {
                EnvGroup selected = getSelectedGroup();
                return selected != null && !ALL_VARIABLES_ID.equals(selected.getId()) &&
                       variableTable.getSelectedRow() >= 0;
            }));

        actionGroup.add(createAction("Delete Variable", "Delete the selected variable", AllIcons.General.Remove,
            this::deleteSelectedVariable, () -> {
                EnvGroup selected = getSelectedGroup();
                return selected != null && !ALL_VARIABLES_ID.equals(selected.getId()) &&
                       variableTable.getSelectedRow() >= 0;
            }));

        actionGroup.addSeparator();

        actionGroup.add(
            createAction("Import File", "Import variables from file", AllIcons.Actions.Download, this::importFile,
                () -> {
                    EnvGroup selected = getSelectedGroup();
                    return selected != null && !ALL_VARIABLES_ID.equals(selected.getId());
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
        Supplier<Boolean> enabledSupplier) {
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
        if (groupTableModel.getRowCount() > 0) {
            groupTable.setRowSelectionInterval(0, 0);
            loadVariablesForGroup((EnvGroup) groupTableModel.getValueAt(0, 0));
        }
    }

    private void refreshGroupList() {
        EnvGroup selectedGroup = getSelectedGroup();
        groupTableModel.setRowCount(0);

        groupTableModel.addRow(new Object[] {allVariablesGroup});

        List<EnvGroup> groups = envManagerService.getEnvGroups();
        for (EnvGroup group : groups) {
            groupTableModel.addRow(new Object[] {group});
        }

        if (selectedGroup != null) {
            for (int i = 0; i < groupTableModel.getRowCount(); i++) {
                EnvGroup group = (EnvGroup) groupTableModel.getValueAt(i, 0);
                if (group.getId().equals(selectedGroup.getId())) {
                    groupTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private void loadVariablesForGroup(EnvGroup group) {
        tableModel.setRowCount(0);
        variableHeaderLabel.setText(group.getName());

        List<EnvVariable> variables;
        if (ALL_VARIABLES_ID.equals(group.getId())) {
            variables = envManagerService.getActiveVariables();
        } else {
            variables = envManagerService.getVariablesByGroup(group.getId());
        }

        for (EnvVariable var : variables) {
            tableModel.addRow(new Object[] {var.getName(), var.getValue()});
        }
    }

    private void showAddGroupDialog() {
        showGroupDialog(null);
    }

    private void showGroupDialog(EnvGroup group) {
        EnvGroupDialog dialog = new EnvGroupDialog(group, envManagerService);
        if (dialog.showAndGet()) {
            EnvGroup result = dialog.getGroup();
            if (group == null) {
                envManagerService.addEnvGroup(result);
            } else {
                envManagerService.updateEnvGroup(result);
            }
            refreshGroupList();
        }
    }

    private void editSelectedGroup() {
        EnvGroup selectedGroup = getSelectedGroup();
        if (selectedGroup != null && !ALL_VARIABLES_ID.equals(selectedGroup.getId())) {
            showGroupDialog(selectedGroup);
        }
    }

    private void deleteSelectedGroup() {
        EnvGroup selectedGroup = getSelectedGroup();
        if (selectedGroup != null && !ALL_VARIABLES_ID.equals(selectedGroup.getId())) {
            int result = Messages.showYesNoDialog(mainPanel,
                "Are you sure you want to delete the group '" + selectedGroup.getName() + "'?", "Delete Group",
                Messages.getQuestionIcon());
            if (result == Messages.YES) {
                envManagerService.removeEnvGroup(selectedGroup.getId());
                refreshGroupList();
                refreshData();
            }
        }
    }

    private void addNewVariable() {
        EnvGroup selectedGroup = getSelectedGroup();
        if (selectedGroup != null && !ALL_VARIABLES_ID.equals(selectedGroup.getId())) {
            showVariableDialog(null, selectedGroup);
        }
    }

    private void editSelectedVariable() {
        int selectedRow = variableTable.getSelectedRow();
        EnvGroup selectedGroup = getSelectedGroup();

        if (selectedRow >= 0 && selectedGroup != null && !ALL_VARIABLES_ID.equals(selectedGroup.getId())) {
            String varName = (String) tableModel.getValueAt(selectedRow, 0);
            EnvVariable variable = envManagerService.getVariablesByGroup(selectedGroup.getId())
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
        EnvGroup selectedGroup = getSelectedGroup();

        if (selectedRow >= 0 && selectedGroup != null && !ALL_VARIABLES_ID.equals(selectedGroup.getId())) {
            String varName = (String) tableModel.getValueAt(selectedRow, 0);
            int result =
                Messages.showYesNoDialog(mainPanel, "Are you sure you want to delete the variable '" + varName + "'?",
                    "Delete Variable", Messages.getQuestionIcon());
            if (result == Messages.YES) {
                EnvVariable variable = envManagerService.getVariablesByGroup(selectedGroup.getId())
                    .stream()
                    .filter(v -> v.getName().equals(varName))
                    .findFirst()
                    .orElse(null);
                if (variable != null) {
                    envManagerService.removeEnvVariable(variable);
                    loadVariablesForGroup(selectedGroup);
                    refreshAllVariablesViewIfNeeded();
                }
            }
        }
    }

    private void showVariableDialog(EnvVariable variable, EnvGroup group) {
        EnvVariableDialog dialog = new EnvVariableDialog(variable, group, envManagerService);
        if (dialog.showAndGet()) {
            EnvVariable result = dialog.getVariable();
            if (variable == null) {
                envManagerService.addEnvVariable(result);
            } else {
                envManagerService.updateEnvVariable(variable, result);
            }
            loadVariablesForGroup(group);
            refreshAllVariablesViewIfNeeded();
        }
    }

    private void refreshAllVariablesViewIfNeeded() {
        EnvGroup selectedGroup = getSelectedGroup();
        if (selectedGroup != null && ALL_VARIABLES_ID.equals(selectedGroup.getId())) {
            loadVariablesForGroup(selectedGroup);
        }
    }

    private void importFile() {
        EnvGroup selectedGroup = getSelectedGroup();
        if (selectedGroup == null || ALL_VARIABLES_ID.equals(selectedGroup.getId())) {
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
                envManagerService.importEnvVariablesFromFile(new File(file.getPath()), selectedGroup.getId());
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

    // 自定义分组表格渲染器
    private static class GroupTableCellRenderer extends DefaultTableCellRenderer {
        private final JBCheckBox checkBox = new JBCheckBox();
        private final JPanel panel = new JPanel(new BorderLayout());
        private final JBLabel nameLabel = new JBLabel();

        public GroupTableCellRenderer(EnvManagerToolWindow parent) {
            panel.setBorder(JBUI.Borders.empty(4, 8));
            panel.add(nameLabel, BorderLayout.CENTER);
            panel.add(checkBox, BorderLayout.EAST);
            checkBox.setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
            EnvGroup group = (EnvGroup) value;
            nameLabel.setText(group.getName());

            // 设置选中状态样式
            if (isSelected) {
                panel.setBackground(UIUtil.getListSelectionBackground(true));
                nameLabel.setForeground(UIUtil.getListSelectionForeground(true));
            } else {
                panel.setBackground(UIUtil.getListBackground());
                nameLabel.setForeground(
                    group.isActive() ? UIUtil.getLabelForeground() : UIUtil.getLabelDisabledForeground());
            }

            // 特殊分组隐藏复选框
            if (ALL_VARIABLES_ID.equals(group.getId())) {
                checkBox.setVisible(false);
            } else {
                checkBox.setVisible(true);
                checkBox.setSelected(group.isActive());
            }

            // 第一行底部添加分割线
            if (row == 0) {
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 1, 0, UIUtil.getBoundsColor()),
                    JBUI.Borders.empty(4, 8)
                ));
            } else {
                panel.setBorder(JBUI.Borders.empty(4, 8));
            }

            return panel;
        }
    }

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
                setBackground(UIUtil.getTableBackground());
                setForeground(UIUtil.getTableForeground());
            }
            return this;
        }
    }

    private void exportConfig() {
        FileSaverDescriptor descriptor =
            new FileSaverDescriptor("Export Environment Config", "Save environment config as JSON", "json");
        FileSaverDialog saveDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, mainPanel);

        VirtualFileWrapper fileWrapper = saveDialog.save("env_config.json");
        if (fileWrapper == null) {
            return; // 用户取消
        }

        File file = fileWrapper.getFile();

        try (Writer writer = new FileWriter(file)) {
            List<EnvGroup> groups = envManagerService.getEnvGroups();

            groups.forEach(group -> {
                group.setVariables(envManagerService.getVariablesByGroup(group.getId()));
            });

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(groups, writer);

            Messages.showInfoMessage(mainPanel, "Export successful:\n" + file.getAbsolutePath(), "Export");
        } catch (Exception e) {
            Messages.showErrorDialog(mainPanel, "Export failed: " + e.getMessage(), "Error");
        }
    }

    private void importConfig() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        descriptor.setTitle("Import Environment Config");
        descriptor.setDescription("Select a JSON file to import environment groups");

        VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
        if (file == null) {
            return; // 用户取消
        }

        try (InputStream inputStream = file.getInputStream(); Reader reader = new InputStreamReader(inputStream)) {

            Gson gson = new Gson();
            List<EnvGroup> importedGroups = gson.fromJson(reader, new TypeToken<List<EnvGroup>>() {}.getType());

            if (importedGroups.isEmpty()) {
                Messages.showWarningDialog(mainPanel, "The selected file contains no groups.", "Import");
                return;
            }

            // 先清空，再导入
            envManagerService.clearAllData();
            importedGroups.forEach(envManagerService::addEnvGroup);

            refreshGroupList();
            refreshAllVariablesViewIfNeeded();
            Messages.showInfoMessage(mainPanel, "Import successful, loaded " + importedGroups.size() + " groups.",
                "Import");
        } catch (Exception e) {
            Messages.showErrorDialog(mainPanel, "Import failed: " + e.getMessage(), "Error");
        }
    }

    // 内部类 - 处理拖拽逻辑
    private class GroupRowTransferHandler extends TransferHandler {
        private int sourceRow = -1;

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            sourceRow = groupTable.getSelectedRow();
            if (sourceRow < 0 || sourceRow >= groupTableModel.getRowCount()) {
                return null;
            }

            // 跳过特殊分组（第一行）
            if (sourceRow == 0) {
                return null;
            }

            return new StringSelection(String.valueOf(sourceRow));
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor) &&
                   support.getComponent() == groupTable;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            sourceRow = -1;
            groupTable.setCursor(Cursor.getDefaultCursor());
            groupTable.repaint();
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
                int targetRow = dl.getRow();

                // 目标行不能是第一行
                if (targetRow <= 0) {
                    return false;
                }

                // 调整索引：如果源行在目标行之前，移除源行后目标行会前移
                if (sourceRow < targetRow) {
                    targetRow--;
                }

                // 获取原始行数据
                EnvGroup movedGroup = (EnvGroup) groupTableModel.getValueAt(sourceRow, 0);

                // 更新数据模型
                groupTableModel.removeRow(sourceRow);
                groupTableModel.insertRow(targetRow, new Object[] {movedGroup});

                // 更新服务层顺序
                envManagerService.reorderGroups(sourceRow, targetRow);

                // 刷新UI
                groupTable.setRowSelectionInterval(targetRow, targetRow);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

}