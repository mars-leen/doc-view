package com.liuzhihang.doc.view.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.utils.ExportUtils;
import com.liuzhihang.doc.view.utils.NotificationUtils;
import com.liuzhihang.doc.view.utils.VelocityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Vector;

/**
 * @author liuzhihang
 * @date 2020/2/26 19:40
 */
public class PreviewForm extends DialogWrapper {
    private JPanel rootJPanel;
    private JSplitPane viewSplitPane;
    private JScrollPane leftScrollPane;
    private JList<String> catalogList;
    private JPanel previewPane;
    private JPanel rootToolPane;
    private JPanel previewEditorPane;
    // private JToolBar toolBar;

    private EditorEx markdownEditor;

    private Action copyAction;
    private Action uploadAction;
    private Action exportAction;

    private Document markdownDocument = EditorFactory.getInstance().createDocument("");

    private Project project;
    private PsiFile psiFile;
    private Editor editor;
    private PsiClass psiClass;
    private Map<String, DocView> docMap;

    private String currentMarkdownText;
    private DocView currentDocView;

    public PreviewForm(@Nullable Project project, PsiFile psiFile, Editor editor, PsiClass psiClass, Map<String, DocView> docMap) {
        super(project, true, DialogWrapper.IdeModalityType.PROJECT);
        this.project = project;
        this.psiFile = psiFile;
        this.editor = editor;
        this.psiClass = psiClass;
        this.docMap = docMap;

        init();

        // UI调整
        initUI();
        initRootToolbar();
        // 右侧文档
        initMarkdownEditor();
        initEditorToolbar();

        // 生成文档
        buildDoc();
        catalogList.setSelectedIndex(0);
    }

    private void initUI() {
        setTitle("Doc View");

        GuiUtils.replaceJSplitPaneWithIDEASplitter(rootJPanel, true);
        // 边框
        rootJPanel.setBorder(JBUI.Borders.empty());
        leftScrollPane.setBorder(JBUI.Borders.empty());
        viewSplitPane.setBorder(JBUI.Borders.empty());
        previewEditorPane.setBorder(JBUI.Borders.empty());
        previewPane.setBorder(JBUI.Borders.empty());

        catalogList.setBackground(UIUtil.getTextFieldBackground());

    }

    private void initRootToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction("Setting", "Doc view settings", AllIcons.General.GearPlain) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        });

        group.addSeparator();

        group.add(new ToggleAction("Pin", "Pin window", AllIcons.General.Pin_tab) {
            @Override
            public boolean isDumbAware() {
                return true;
            }

            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return false;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {

            }
        });


        ActionToolbar actionToolbar = ActionManager.getInstance()
                .createActionToolbar(ActionPlaces.POPUP, group, true);
        actionToolbar.setTargetComponent(rootToolPane);
        actionToolbar.setMiniMode(true);
        rootToolPane.add(actionToolbar.getComponent(), BorderLayout.EAST);
    }


    private void initMarkdownEditor() {
        // 会使用 velocity 渲染模版
        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension("md");

        final EditorHighlighter editorHighlighter =
                HighlighterFactory.createHighlighter(fileType, EditorColorsManager.getInstance().getGlobalScheme(), project);

        markdownEditor = (EditorEx) EditorFactory.getInstance().createEditor(markdownDocument, project, fileType, true);

        EditorSettings editorSettings = markdownEditor.getSettings();
        editorSettings.setAdditionalLinesCount(0);
        editorSettings.setAdditionalColumnsCount(0);
        editorSettings.setLineMarkerAreaShown(false);
        editorSettings.setLineNumbersShown(false);
        editorSettings.setVirtualSpace(false);
        editorSettings.setFoldingOutlineShown(false);

        editorSettings.setLanguageSupplier(() -> Language.findLanguageByID("Markdown"));

        markdownEditor.setHighlighter(editorHighlighter);

        JBScrollPane templateScrollPane = new JBScrollPane(markdownEditor.getComponent());
        previewPane.add(templateScrollPane, BorderLayout.CENTER);
    }


    private void initEditorToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction("Copy", "Copy to clipboard", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
            }
        });

        group.addSeparator();

        // init toolbar
        ActionToolbar actionToolbar = ActionManager.getInstance()
                .createActionToolbar(ActionPlaces.POPUP, group, true);
        actionToolbar.setTargetComponent(previewEditorPane);
        actionToolbar.getComponent().setBackground(markdownEditor.getBackgroundColor());
        actionToolbar.setMiniMode(true);

        previewEditorPane.setBackground(markdownEditor.getBackgroundColor());
        previewEditorPane.add(actionToolbar.getComponent(), BorderLayout.EAST);
    }

    private void buildDoc() {

        catalogList.setListData(new Vector<>(docMap.keySet()));

        catalogList.addListSelectionListener(catalog -> {

            String selectedValue = catalogList.getSelectedValue();

            currentDocView = docMap.get(selectedValue);

            // 将 docView 按照模版转换
            DocViewData docViewData = new DocViewData(currentDocView);
            if (currentDocView.getType().equalsIgnoreCase("Dubbo")) {
                currentMarkdownText = VelocityUtils.convert(TemplateSettings.getInstance(project).getDubboTemplate(), docViewData);
            } else {
                // 按照 Spring 模版
                currentMarkdownText = VelocityUtils.convert(TemplateSettings.getInstance(project).getSpringTemplate(), docViewData);
            }

            WriteCommandAction.runWriteCommandAction(project, () -> markdownDocument.setText(currentMarkdownText));

            // String html = MarkdownUtil.INSTANCE.generateMarkdownHtml(psiFile.getVirtualFile(), currentMarkdownText, project);
            //
            // html = "<html><head></head>" + html + "</html>";
            //
            // markdownHtmlPanel.setHtml(html, 0);
            // textPane.setText(html);
            // textPane.setCaretPosition(0);

        });

    }


    @NotNull
    @Override
    protected Action[] createActions() {

        myHelpAction.setEnabled(true);

        return new Action[]{};
    }

    private Action getExportAction() {

        return exportAction;
    }


    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        copyAction = new CopyAction();
        exportAction = new ExportAction();
    }

    @NotNull
    protected Action getCopyAction() {
        return copyAction;
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return rootJPanel;
    }

    /**
     * Copy 按钮的监听
     * 主要功能为: 复制 Markdown 到剪贴板, 并提示.
     */
    protected class CopyAction extends DialogWrapperAction {

        protected CopyAction() {
            super("Copy");
        }

        @Override
        protected void doAction(ActionEvent e) {

            StringSelection selection = new StringSelection(currentMarkdownText);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);

            NotificationUtils.infoNotify(DocViewBundle.message("notify.copy.success", currentDocView.getName()), project);
        }
    }

    /**
     * Export 按钮的监听
     * 主要功能为: 导出功能
     */
    protected class ExportAction extends DialogWrapperAction {

        protected ExportAction() {
            super("Export");
        }

        @Override
        protected void doAction(ActionEvent e) {

            ExportUtils.exportMarkdown(project, currentDocView.getName(), currentMarkdownText);
        }
    }

}
