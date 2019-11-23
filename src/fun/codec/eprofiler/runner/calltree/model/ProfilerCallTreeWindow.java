package fun.codec.eprofiler.runner.calltree.model;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.impl.ToolWindowImpl;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.ClassUtil;
import fun.codec.eprofiler.runner.ProfilerCollector;
import fun.codec.eprofiler.runner.calltree.action.RefreshAction;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author: echo
 * @create: 2018-12-12 10:51
 */
public class ProfilerCallTreeWindow {

    private JTree jtree;
    private JPanel jpanel;
    private JScrollPane jscrollPane;
    private Project project;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode root;

    public ProfilerCallTreeWindow(ToolWindow toolWindow, Project project) {
        this.project = project;
        RefreshAction refresh = new RefreshAction("Refresh", "Refresh EProfiler", AllIcons.Actions.Refresh);
        ((ToolWindowImpl) toolWindow).setTabActions(refresh);
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
                project.getComponent(ProfilerCollector.class).print(false);

            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
            }
        });
        jtree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode note = (DefaultMutableTreeNode) jtree.getLastSelectedPathComponent();
                String name = note.toString();
                String allName = name.substring(0, name.lastIndexOf("("));
                String className = allName.substring(0, allName.lastIndexOf("."));
                String methodName = allName.substring(allName.lastIndexOf(".") + 1).trim();
                PsiClass psiClass = ClassUtil.findPsiClass(PsiManager.getInstance(project), className);
                if (psiClass != null) {
                    PsiMethod[] allMethods = psiClass.getAllMethods();
                    if (allMethods != null) {
                        for (PsiMethod method : allMethods) {
                            if (method.getName().equals(methodName)) {
                                Navigatable navigatable = (Navigatable) method.getNavigationElement();
                                navigatable.navigate(true);
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    public DefaultMutableTreeNode getRoot() {
        return root;
    }

    public void reload() {
        treeModel.reload();
    }

    public JPanel getContent() {
        return jpanel;
    }
}
