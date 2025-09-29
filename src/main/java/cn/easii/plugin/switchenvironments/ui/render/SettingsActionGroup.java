package cn.easii.plugin.switchenvironments.ui.render;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

public class SettingsActionGroup extends DefaultActionGroup {

    public SettingsActionGroup() {
        super("Settings", true);
        getTemplatePresentation().setIcon(AllIcons.General.Settings);
    }

}
