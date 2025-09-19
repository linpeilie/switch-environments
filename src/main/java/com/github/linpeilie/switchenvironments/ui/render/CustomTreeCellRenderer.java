package com.github.linpeilie.switchenvironments.ui.render;

import com.github.linpeilie.switchenvironments.model.EnvGroup;
import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class CustomTreeCellRenderer extends ColoredTreeCellRenderer {

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected,
        boolean expanded, boolean leaf, int row, boolean hasFocus) {

        if (value instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();

            if (userObject instanceof EnvGroup) {
                EnvGroup group = (EnvGroup) userObject;

                // Set icon based on group type and active state
                if ("default".equals(group.getId())) {
                    setIcon(AllIcons.Nodes.HomeFolder);
                } else if ("imported".equals(group.getId())) {
                    setIcon(AllIcons.Actions.Download);
                } else {
                    // TODO:关闭图标修改
                    setIcon(group.isActive() ? AllIcons.Nodes.Folder : AllIcons.Nodes.ExtractedFolder);
                }

                // Set text with different attributes based on active state
                if (group.isActive()) {
                    append(group.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    if (group.getDescription() != null && !group.getDescription().isEmpty()) {
                        append(" - " + group.getDescription(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    }
                } else {
                    append(group.getName(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    if (group.getDescription() != null && !group.getDescription().isEmpty()) {
                        append(" - " + group.getDescription(), SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
                    }
                    append(" (inactive)", new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, JBColor.GRAY));
                }
            } else if (userObject instanceof String) {
                // Root node
                setIcon(AllIcons.Nodes.ConfigFolder);
                append((String) userObject, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            }
        }
    }
}
