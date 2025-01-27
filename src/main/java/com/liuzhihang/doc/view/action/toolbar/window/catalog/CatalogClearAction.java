package com.liuzhihang.doc.view.action.toolbar.window.catalog;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.ui.window.DocViewWindowTreeNode;
import com.liuzhihang.doc.view.utils.CustomFileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author liuzhihang
 * @date 2021/10/23 19:55
 */
public class CatalogClearAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        SimpleTree simpleTree = e.getData(DocViewDataKeys.WINDOW_CATALOG_TREE);

        if (simpleTree == null || project == null) {
            return;
        }


        if (simpleTree.getLastSelectedPathComponent() instanceof DocViewWindowTreeNode) {
            DocViewWindowTreeNode node = (DocViewWindowTreeNode) simpleTree.getLastSelectedPathComponent();
            File file = new File(node.getTempFilePath());
            CustomFileUtils.delete(file, project);
        }


    }


}
