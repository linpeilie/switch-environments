package cn.easii.plugin.switchenvironments.ui.model;

import cn.easii.plugin.switchenvironments.model.EnvVariable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EnvVariableTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Enabled", "Name", "Value", "Description"};
    private final Class<?>[] columnTypes = {Boolean.class, String.class, String.class, String.class};
    private List<EnvVariable> variables;

    public EnvVariableTableModel() {
        this.variables = new ArrayList<>();
    }

    public EnvVariableTableModel(List<EnvVariable> variables) {
        this.variables = new ArrayList<>(variables);
    }

    @Override
    public int getRowCount() {
        return variables.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0; // Only enabled column is editable
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= variables.size()) {
            return null;
        }

        EnvVariable variable = variables.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return variable.getName();
            case 1:
                return variable.getValue();
            default:
                return null;
        }
    }

    public void setVariables(List<EnvVariable> variables) {
        this.variables = new ArrayList<>(variables);
        fireTableDataChanged();
    }

    public List<EnvVariable> getVariables() {
        return new ArrayList<>(variables);
    }

    public EnvVariable getVariableAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= variables.size()) {
            return null;
        }
        return variables.get(rowIndex);
    }

    public void addVariable(EnvVariable variable) {
        variables.add(variable);
        int row = variables.size() - 1;
        fireTableRowsInserted(row, row);
    }

    public void removeVariable(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < variables.size()) {
            variables.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    public void updateVariable(int rowIndex, EnvVariable variable) {
        if (rowIndex >= 0 && rowIndex < variables.size()) {
            variables.set(rowIndex, variable);
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }

    // Custom cell renderer for better visual appearance
    public static class EnvVariableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Get the variable to check if it's enabled
            if (table.getModel() instanceof EnvVariableTableModel) {
                EnvVariableTableModel model = (EnvVariableTableModel) table.getModel();
                EnvVariable variable = model.getVariableAt(row);

                if (variable != null) {
                    // Special formatting for value column
                    if (column == 2) { // Value column
                        String valueText = value != null ? value.toString() : "";
                        if (valueText.length() > 50) {
                            setText(valueText.substring(0, 47) + "...");
                            setToolTipText(valueText);
                        } else {
                            setToolTipText(null);
                        }
                    }
                }
            }

            return component;
        }
    }
}
