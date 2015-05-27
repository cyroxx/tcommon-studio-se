// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.maven.ui.dialog.model.nodes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.core.runtime.projectsetting.AbstractProjectSettingPage;
import org.talend.core.runtime.projectsetting.ProjectPreferenceManager;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.template.IProjectSettingPreferenceConstants;
import org.talend.designer.maven.ui.DesignerMavenUiPlugin;
import org.talend.designer.maven.ui.dialog.model.IRepositoryPreferenceNodeContainer;
import org.talend.designer.maven.ui.i18n.Messages;
import org.talend.designer.maven.ui.utils.DesignerMavenUiHelper;
import org.talend.repository.ProjectManager;
import org.talend.repository.RepositoryWorkUnit;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;

/**
 * DOC ggu class global comment. Detailled comment
 */
public class FolderMavenSettingPage extends AbstractProjectSettingPage {

    private RepositoryNode node;

    private static final String ID_MAVEN_PROJECT_SETTING = "projectsetting.Maven"; //$NON-NLS-1$

    private Link messageLink, noteLink;

    private Button createBtn, deleteBtn;

    private Text pathTxt;

    private Composite pathComposite, noteComposite;

    private boolean readonly = false;

    private SelectionAdapter linkListener = new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent e) {
            processLinkId(e);
        }
    };

    public FolderMavenSettingPage(RepositoryNode node) {
        super();
        this.node = node;

        IProxyRepositoryFactory factory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
        if (factory.isUserReadOnlyOnCurrentProject() || !ProjectManager.getInstance().isInCurrentMainProject(this.getNode())) {
            readonly = true;
        }
        noDefaultAndApplyButton();
    }

    protected RepositoryNode getNode() {
        return node;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.runtime.preference.AbstractProjectSettingPage#initStore()
     */
    @Override
    protected void initStore() {
        // because no filename to set, so won't save and load it.
        this.setPreferenceStore(new PreferenceStore() {

            @Override
            public void load() throws IOException {
                // super.load();
            }

            @Override
            public void save() throws IOException {
                // super.save();
            }

        });
    }

    @Override
    protected void createFieldEditors() {

        Composite fieldEditorParent = getFieldEditorParent();
        fieldEditorParent.setLayout(new GridLayout());
        messageLink = new Link(fieldEditorParent, SWT.WRAP);
        messageLink.addSelectionListener(linkListener);

        pathComposite = createPathComposite(fieldEditorParent);
        // noteComposite = createNoteComposite(fieldEditorParent);

        createBtn = new Button(fieldEditorParent, SWT.PUSH);
        createBtn.setLayoutData(new GridData());
        createBtn.setText(Messages.getString("FolderMavenSettingPage_CreateButtonText"));//$NON-NLS-1$
        createBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                createMavenFiles();
            }

        });

        deleteBtn = new Button(fieldEditorParent, SWT.PUSH);
        deleteBtn.setLayoutData(new GridData());
        deleteBtn.setText(Messages.getString("FolderMavenSettingPage_DeleteButtonText"));//$NON-NLS-1$
        deleteBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteMavenFiles();
            }

        });

        updateFields();
    }

    private Composite createPathComposite(Composite parent) {
        Composite composite = createLabelComposite(parent, Messages.getString("FolderMavenSettingPage_CreatingMavenSettingPath")); //$NON-NLS-1$

        pathTxt = new Text(composite, SWT.READ_ONLY);
        pathTxt.setBackground(composite.getBackground());
        pathTxt.setFont(parent.getFont());
        pathTxt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return composite;
    }

    private Composite createNoteComposite(Composite parent) {
        Composite composite = createLabelComposite(parent, Messages.getString("FolderMavenSettingPage_CreatingMavenSettingNote")); //$NON-NLS-1$

        noteLink = new Link(composite, SWT.WRAP);
        noteLink.setFont(parent.getFont());
        noteLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        noteLink.addSelectionListener(linkListener);
        return composite;
    }

    protected void updateFields() {
        IFolder nodeFolder = DesignerMavenUiHelper.getNodeFolder(node);
        boolean created = DesignerMavenUiHelper.existMavenSetting(nodeFolder);
        StringBuffer messages = new StringBuffer(200);
        // existed
        if (created) {
            String pomLinkStr = buildLink(TalendMavenConstants.POM_FILE_NAME);
            String assemblyLinkStr = buildLink(TalendMavenConstants.ASSEMBLY_FILE_NAME);
            messages.append(Messages.getString("FolderMavenSettingPage_ExistedMavenSettingMessage",//$NON-NLS-1$
                    pomLinkStr, assemblyLinkStr));
        } else {
            messages.append(Messages.getString("FolderMavenSettingPage_CreatingMavenSettingMessage", //$NON-NLS-1$
                    TalendMavenConstants.POM_FILE_NAME, TalendMavenConstants.ASSEMBLY_FILE_NAME));
            messages.append('\n');
            messages.append('\n');

            messages.append(Messages.getString("FolderMavenSettingPage_CreatingMavenSettingNote"));//$NON-NLS-1$
            messages.append(' ');
            String mvnProjectSettingLinkStr = "<a href=\"" + ID_MAVEN_PROJECT_SETTING + "\">Maven</a>";//$NON-NLS-1$ //$NON-NLS-2$
            messages.append(Messages.getString("FolderMavenSettingPage_CreatingMavenSettingNoteMessage", //$NON-NLS-1$
                    mvnProjectSettingLinkStr));
        }
        messages.append('\n');
        messageLink.setText(messages.toString());

        // path
        IPath projectRelativePath = nodeFolder.getProjectRelativePath();
        pathTxt.setText("<Project>/" + projectRelativePath.toString());//$NON-NLS-1$
        pathTxt.setToolTipText(projectRelativePath.toString());
        GridData pathTxtGridData = (GridData) pathTxt.getLayoutData();
        Point size = messageLink.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        pathTxtGridData.widthHint = size.x;

        // GridData pathCompGridData = (GridData) pathComposite.getLayoutData();
        // pathCompGridData.exclude = true; // hide path first.
        // pathComposite.setLayoutData(pathCompGridData);

        // // note
        //        String mvnProjectSettingLinkStr = "<a href=\"" + ID_MAVEN_PROJECT_SETTING + "\">Maven</a>";//$NON-NLS-1$ //$NON-NLS-2$
        //        noteLink.setText(Messages.getString("FolderMavenSettingPage_CreatingMavenSettingNoteMessage", //$NON-NLS-1$
        // mvnProjectSettingLinkStr));
        //
        // GridData noteCompGridData = (GridData) noteComposite.getLayoutData();
        // noteCompGridData.exclude = created;
        // noteComposite.setLayoutData(noteCompGridData);

        // FIXME, there are some problem for display, so don't hide it first.
        // createBtn.setEnabled(!readonly);
        // GridData createBtnGridData = (GridData) createBtn.getLayoutData();
        // createBtnGridData.exclude = created;
        //
        // deleteBtn.setEnabled(!readonly);
        // GridData deleteBtnGridData = (GridData) deleteBtn.getLayoutData();
        // deleteBtnGridData.exclude = !created;
        //
        // messageLink.getParent().layout();

        createBtn.setEnabled(!created && !readonly);
        deleteBtn.setEnabled(created && !readonly);
    }

    private String buildLink(String fileWithExtension) {
        String name = fileWithExtension;
        String displayName = fileWithExtension;

        if (!DesignerMavenUiHelper.idWithExtension) {
            name = fileWithExtension.substring(0, fileWithExtension.lastIndexOf('.'));
        }

        if (!DesignerMavenUiHelper.displayWithExtension) {
            displayName = fileWithExtension.substring(0, fileWithExtension.lastIndexOf('.'));
        }
        return "<a href=\"" + name + '\"' + '>' //$NON-NLS-1$ 
                + displayName + "</a>";//$NON-NLS-1$ 
    }

    protected void processLinkId(SelectionEvent e) {
        String id = e.text;
        if (ID_MAVEN_PROJECT_SETTING.equals(id)) {
            openProjectSettingDialog(id);
        } else if (TalendMavenConstants.POM_FILE_NAME.equals(id) || TalendMavenConstants.ASSEMBLY_FILE_NAME.equals(id)) {
            String childId = DesignerMavenUiHelper.buildRepositoryPreferenceNodeId(getPrefNodeId(), id);
            openChildPage(childId);
        }
    }

    protected void openChildPage(String childId) {
        IPreferencePageContainer container = getContainer();
        if (container instanceof IWorkbenchPreferenceContainer) {
            ((IWorkbenchPreferenceContainer) container).openPage(childId, getNode());
        } else if (container instanceof IRepositoryPreferenceNodeContainer) {
            ((IRepositoryPreferenceNodeContainer) container).openPage(childId, getNode());
        }
    }

    protected void openProjectSettingDialog(String projectSettingId) {
        // TODO
    }

    protected void createMavenFiles() {
        processFiles(new RepositoryWorkUnit(createBtn.getText()) {

            @Override
            protected void run() throws LoginException, PersistenceException {

                ProjectPreferenceManager projectPreferenceManager = DesignerMavenUiPlugin.getDefault()
                        .getProjectPreferenceManager();

                final IFolder nodeFolder = DesignerMavenUiHelper.getNodeFolder(getNode());
                final IFile pomFile = nodeFolder.getFile(TalendMavenConstants.POM_FILE_NAME);
                final IFile assemblyFile = nodeFolder.getFile(TalendMavenConstants.ASSEMBLY_FILE_NAME);
                File pomF = pomFile.getLocation().toFile();
                File assemblyF = assemblyFile.getLocation().toFile();

                boolean ok = true;
                try {
                    String pomContent = projectPreferenceManager
                            .getValue(IProjectSettingPreferenceConstants.MAVEN_SCRIPT_AUTONOMOUSJOB_TEMPLATE);
                    if (pomContent == null) { // use project setting always
                        // pomContent =
                        // MavenTemplateManager.getTemplateContent(MavenTemplateConstants.POM_JOB_TEMPLATE_FILE_NAME);
                    }
                    if (pomContent != null && pomContent.length() > 0) {
                        writeContent(pomF, pomContent);
                    } else {
                        ok = false;
                    }
                } catch (Exception e) {
                    ok = false;
                    ExceptionHandler.process(e);
                }
                if (ok) {
                    try {
                        String assemblyContent = projectPreferenceManager
                                .getValue(IProjectSettingPreferenceConstants.MAVEN_SCRIPT_AUTONOMOUSJOB_ASSEMBLY_TEMPLATE);
                        if (assemblyContent == null) { // use project setting always
                            // assemblyContent = MavenTemplateManager
                            // .getTemplateContent(MavenTemplateConstants.ASSEMBLY_JOB_TEMPLATE_FILE_NAME);
                        }
                        if (assemblyContent != null && assemblyContent.length() > 0) {
                            writeContent(assemblyF, assemblyContent);
                        } else {
                            ok = false;
                        }
                    } catch (Exception e) {
                        ok = false;
                        ExceptionHandler.process(e);
                    }
                }

                if (!ok) { // if failed, clean the created files.
                    if (pomF.exists()) {
                        pomF.delete();
                    }
                    if (assemblyF.exists()) {
                        assemblyF.delete();
                    }

                    //
                    final Shell shell = FolderMavenSettingPage.this.getShell();
                    shell.getDisplay().syncExec(new Runnable() {

                        @Override
                        public void run() {
                            MessageDialog.openError(shell,
                                    Messages.getString("FolderMavenSettingPage_CreatingMavenSettingErrorTitle"), //$NON-NLS-1$
                                    Messages.getString("FolderMavenSettingPage_CreatingMavenSettingErrorMessage") //$NON-NLS-1$
                                            + '\n' + nodeFolder.getProjectRelativePath().toString());

                        }
                    });
                }
                // update the tree view to add the new nodes
                final Shell shell = FolderMavenSettingPage.this.getShell();
                shell.getDisplay().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        List<IPreferenceNode> autonomousJobChildrenNodes = DesignerMavenUiHelper.createAutonomousJobChildNode(
                                nodeFolder, getNode(), getPrefNodeId(), false); // have created, no need check again.

                        IPreferencePageContainer container = getContainer();
                        if (container instanceof IRepositoryPreferenceNodeContainer) {
                            ((IRepositoryPreferenceNodeContainer) container).addChildrenPreferenceNodes(getPrefNodeId(),
                                    autonomousJobChildrenNodes);
                        }
                    }
                });

                // refresh
                try {
                    nodeFolder.refreshLocal(IResource.DEPTH_ONE, null);
                } catch (CoreException e) {
                    ExceptionHandler.process(e);
                }
            }

        });

    }

    private void writeContent(File file, String contents) throws IOException {
        FileWriter pomWriter = new FileWriter(file);
        pomWriter.write(contents);
        pomWriter.close();
    }

    protected void deleteMavenFiles() {
        processFiles(new RepositoryWorkUnit(deleteBtn.getText()) {

            @Override
            protected void run() throws LoginException, PersistenceException {
                //
                try {
                    final IFolder nodeFolder = DesignerMavenUiHelper.getNodeFolder(getNode());

                    final IFile pomFile = nodeFolder.getFile(TalendMavenConstants.POM_FILE_NAME);
                    final IFile assemblyFile = nodeFolder.getFile(TalendMavenConstants.ASSEMBLY_FILE_NAME);

                    pomFile.delete(true, null);
                    assemblyFile.delete(true, null);

                    // update the tree view to add the new nodes
                    final Shell shell = FolderMavenSettingPage.this.getShell();
                    shell.getDisplay().syncExec(new Runnable() {

                        @Override
                        public void run() {
                            List<String> childIds = new ArrayList<String>();

                            // pom and assembly
                            String pomId = DesignerMavenUiHelper.buildRepositoryPreferenceNodeId(getPrefNodeId(), pomFile);
                            String assemblyId = DesignerMavenUiHelper.buildRepositoryPreferenceNodeId(getPrefNodeId(),
                                    assemblyFile);
                            childIds.add(pomId);
                            childIds.add(assemblyId);

                            IPreferencePageContainer container = getContainer();
                            if (container instanceof IRepositoryPreferenceNodeContainer) {
                                ((IRepositoryPreferenceNodeContainer) container).removeChildrenPreferenceNodes(getPrefNodeId(),
                                        childIds);
                            }
                        }
                    });

                    nodeFolder.refreshLocal(IResource.DEPTH_ONE, null);
                } catch (CoreException e) {
                    ExceptionHandler.process(e);
                }
            }

        });
    }

    protected void processFiles(final RepositoryWorkUnit rwu) {

        final IWorkspaceRunnable op = new IWorkspaceRunnable() {

            @Override
            public void run(IProgressMonitor monitor) throws CoreException {
                final Shell shell = FolderMavenSettingPage.this.getShell();
                shell.getDisplay().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        // don't commit it yet for remote
                        // CoreRuntimePlugin.getInstance().getProxyRepositoryFactory().executeRepositoryWorkUnit(rwu);
                        rwu.executeRun();
                        updateFields();
                    }
                });
            };
        };
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        try {
            workspace.run(op, workspace.getRoot(), IWorkspace.AVOID_UPDATE, null);
        } catch (CoreException e) {
            ExceptionHandler.process(e);
        }
    }
}
