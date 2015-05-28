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
package org.talend.metadata.managment.ui.wizard.process;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.exception.MessageBoxExceptionHandler;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.utils.ConvertJobsUtil;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.designer.core.convert.ProcessConvertManager;
import org.talend.designer.core.convert.ProcessConverterType;
import org.talend.metadata.managment.ui.i18n.Messages;
import org.talend.metadata.managment.ui.wizard.PropertiesWizard;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.IProxyRepositoryFactory;

/**
 * Created by Marvin Wang on Feb 18, 2013.
 */
public class EditProcessPropertiesWizard extends PropertiesWizard {

    private IRepositoryViewObject repositoryViewObject;

    private EditProcessPropertiesWizardPage mainPage;

    /**
     * DOC marvin EditProcessPropertiesWizard constructor comment.
     * 
     * @param repositoryViewObject
     * @param path
     * @param useLastVersion
     */
    public EditProcessPropertiesWizard(IRepositoryViewObject repositoryViewObject, IPath path, boolean useLastVersion) {
        super(repositoryViewObject, path, useLastVersion);
        this.repositoryViewObject = repositoryViewObject;
        setWindowTitle(Messages.getString("EditProcessPropertiesWizard.wizardTitle")); //$NON-NLS-1$
    }

    @Override
    public void addPages() {
        mainPage = new EditProcessPropertiesWizardPage(Messages.getString("EditProcessPropertiesWizard.pageName"), //$NON-NLS-1$
                object.getProperty(), path, isReadOnly(), false, lastVersionFound);
        mainPage.setItem(object.getProperty().getItem());
        mainPage.setConverter(ProcessConvertManager.getInstance().extractConvertService(
                ProcessConverterType.CONVERTER_FOR_MAPREDUCE));
        addPage(mainPage);
    }

    @Override
    public boolean performFinish() {
        if (alreadyEditedByUser) {
            return false;
        }
        Item item = object.getProperty().getItem();
        String sourceJobType = ConvertJobsUtil.getJobTypeFromFramework(item);
        if (sourceJobType != null && sourceJobType.equals(mainPage.jobTypeCCombo.getText())) {
            return super.performFinish();
        }
        final IProxyRepositoryFactory proxyRepositoryFactory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
        // Convert
        ConvertJobsUtil
                .createOperation(object.getLabel(), mainPage.jobTypeCCombo.getText(), mainPage.framework.getText(), object);

        IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

            @Override
            public void run(final IProgressMonitor monitor) throws CoreException {
                try {
                    proxyRepositoryFactory.unlock(object);
                    proxyRepositoryFactory.deleteObjectPhysical(object);
                    proxyRepositoryFactory.saveProject(ProjectManager.getInstance().getCurrentProject());
                } catch (PersistenceException e1) {
                    e1.printStackTrace();
                } catch (LoginException e) {
                    e.printStackTrace();
                }
            }
        };
        // unlockObject();
        // alreadyEditedByUser = true; // to avoid 2 calls of unlock

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        try {
            ISchedulingRule schedulingRule = workspace.getRoot();
            // the update the project files need to be done in the workspace runnable to avoid all notification
            // of changes before the end of the modifications.
            workspace.run(runnable, schedulingRule, IWorkspace.AVOID_UPDATE, null);
            return true;
        } catch (CoreException e) {
            MessageBoxExceptionHandler.process(e.getCause());
            return false;
        }
        // return super.performFinish();
    }

    /**
     * Getter for repositoryViewObject.
     * 
     * @return the repositoryViewObject
     */
    public IRepositoryViewObject getRepositoryViewObject() {
        return this.repositoryViewObject;
    }

    /**
     * Sets the repositoryViewObject.
     * 
     * @param repositoryViewObject the repositoryViewObject to set
     */
    public void setRepositoryViewObject(IRepositoryViewObject repositoryViewObject) {
        this.repositoryViewObject = repositoryViewObject;
    }

}
