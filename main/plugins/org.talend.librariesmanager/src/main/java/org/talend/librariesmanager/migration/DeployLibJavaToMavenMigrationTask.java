// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.librariesmanager.migration;

import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ILibraryManagerService;
import org.talend.core.model.general.Project;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.migration.AbstractMigrationTask;
import org.talend.migration.IProjectMigrationTask;
import org.talend.repository.ProjectManager;

/**
 * created by wchen on 2015-5-15 Detailled comment
 * 
 * This migration task will deploy project/lib to local maven repository
 */
public class DeployLibJavaToMavenMigrationTask extends AbstractMigrationTask implements IProjectMigrationTask {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.migration.IMigrationTask#getOrder()
     */
    @Override
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2015, 5, 15, 12, 0, 0);
        return gc.getTime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.migration.IProjectMigrationTask#execute(org.talend.core.model.general.Project)
     */
    @Override
    public ExecutionResult execute(Project project) {
        try {
            String path = new Path(Platform.getInstanceLocation().getURL().getPath()).toFile().getPath();
            String projectLabel = ProjectManager.getInstance().getCurrentProject().getTechnicalLabel();
            path = path + File.separatorChar + projectLabel + File.separatorChar
                    + ERepositoryObjectType.getFolderName(ERepositoryObjectType.LIBS);
            File libsTargetFile = new File(path);
            if (libsTargetFile.exists()) {
                ILibraryManagerService librariesManager = (ILibraryManagerService) GlobalServiceRegister.getDefault().getService(
                        ILibraryManagerService.class);
                librariesManager.deploy(libsTargetFile.toURI());
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
            return ExecutionResult.FAILURE;
        }
        return ExecutionResult.SUCCESS_WITH_ALERT;
    }

    @Override
    public final ExecutionResult execute(Project project, Item item) {
        return ExecutionResult.NOTHING_TO_DO;
    }

    @Override
    public ExecutionResult execute(Project project, boolean doSave) {
        return execute(project);
    }

    @Override
    public final boolean isApplicableOnItems() {
        return false;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

}
