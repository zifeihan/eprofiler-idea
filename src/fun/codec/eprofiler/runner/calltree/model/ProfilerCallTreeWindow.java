package fun.codec.eprofiler.runner.calltree.model;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.impl.ToolWindowImpl;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import fun.codec.eprofiler.runner.ProfilerCollector;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author: echo
 * @create: 2018-12-12 10:51
 */
public class ProfilerCallTreeWindow implements ToolWindowFactory {

    public ProfilerCallTreeWindow() {
    }

    private JPanel jpanel;
    private JScrollPane jscrollPane;
    private JTree jtree;

    private static DefaultMutableTreeNode root;

    private static DefaultTreeModel treeModel;

    public void init(ToolWindow window) {
        AnAction refresh = new AnAction("Refresh", "Refresh EProfiler", AllIcons.Actions.Refresh) {
            public void actionPerformed(@NotNull AnActionEvent e) {
                ProfilerCollector.PrintTree.print(true);
            }
        };
        ((ToolWindowImpl) window).setTabActions(refresh);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager cm = toolWindow.getContentManager();
        ContentFactory factory = cm.getFactory();
        Content content = factory.createContent(jpanel, "", false);
        cm.addContent(content);
    }

    private void createUIComponents() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("JTree");
        this.root = root;
        this.treeModel = new DefaultTreeModel(root);
        this.jtree = new JTree(treeModel);
        jtree.setRootVisible(false);
        DefaultTreeCellRenderer render = (DefaultTreeCellRenderer) (jtree.getCellRenderer());
        render.setLeafIcon(null);
        render.setClosedIcon(null);
        render.setOpenIcon(null);
        jtree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                //stop print stack tree.
                ProfilerCollector.PrintTree.print(false);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {

            }
        });
    }

    public static DefaultMutableTreeNode getRoot() {
        return root;
    }

    public static void reload() {
        treeModel.reload();
    }
}
