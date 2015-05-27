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
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ILibraryManagerService;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.general.Project;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.librariesmanager.prefs.LibrariesManagerUtils;
import org.talend.login.AbstractLoginTask;
import org.talend.repository.ProjectManager;

/**
 * created by wchen on 2015-5-15 Detailled comment
 *
 * This logon task will deploy jars from configration/lib/java/... and project/lib/... to local maven repository
 * configration/lib/java/... will be migrated only one time for each workspace , project/lib/... will be migrated one
 * time for each project
 */
public class DeployLibJavaToMavenLoginTask extends AbstractLoginTask {

    private static IEclipsePreferences prefs = new ConfigurationScope().getNode("org.talend.librariesmanager");

    private static final String migration_key = "org.talend.librariesmanager.migration.DeployLibJavaToMavenLoginTask.";

    private static final String configureation_key = migration_key + "configration";

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.login.ILoginTask#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {

            Project currentProject = ProjectManager.getInstance().getCurrentProject();
            boolean configureMigrated = prefs.getBoolean(configureation_key, false);
            boolean projectMigrated = prefs.getBoolean(migration_key + currentProject.getTechnicalLabel(), false);

            boolean modified = false;
            // configration/lib/java/...
            if (!configureMigrated) {
                String librariesPath = LibrariesManagerUtils.getLibrariesPath(ECodeLanguage.JAVA);
                File storageDir = new File(librariesPath);
                if (storageDir.exists()) {
                    ILibraryManagerService librariesManager = (ILibraryManagerService) GlobalServiceRegister.getDefault()
                            .getService(ILibraryManagerService.class);
                    librariesManager.deploy(storageDir.toURI());
                }
                prefs.putBoolean(configureation_key, true);
                modified = true;
            }
            // project/lib/...
            if (!projectMigrated) {
                String path = new Path(Platform.getInstanceLocation().getURL().getPath()).toFile().getPath();
                String projectLabel = currentProject.getTechnicalLabel();
                path = path + File.separatorChar + projectLabel + File.separatorChar
                        + ERepositoryObjectType.getFolderName(ERepositoryObjectType.LIBS);
                File libsTargetFile = new File(path);
                if (libsTargetFile.exists()) {
                    ILibraryManagerService librariesManager = (ILibraryManagerService) GlobalServiceRegister.getDefault()
                            .getService(ILibraryManagerService.class);
                    librariesManager.deploy(libsTargetFile.toURI());
                }
                prefs.putBoolean(migration_key + currentProject.getTechnicalLabel(), true);
                modified = true;
            }
            if (modified) {
                prefs.flush();
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.login.AbstractLoginTask#getOrder()
     */
    @Override
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2015, 5, 15, 12, 0, 0);
        return gc.getTime();
    }

}
