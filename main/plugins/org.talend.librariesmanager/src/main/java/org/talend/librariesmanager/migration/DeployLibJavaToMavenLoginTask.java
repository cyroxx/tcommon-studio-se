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
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ILibraryManagerService;
import org.talend.core.language.ECodeLanguage;
import org.talend.librariesmanager.prefs.LibrariesManagerUtils;
import org.talend.login.AbstractLoginTask;

/**
 * created by wchen on 2015-5-15 Detailled comment
 *
 * This logon task will deploy jars from configration/lib/java/... to local maven repository
 */
public class DeployLibJavaToMavenLoginTask extends AbstractLoginTask {

    private static IEclipsePreferences prefs = new ConfigurationScope().getNode("org.talend.librariesmanager");

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.login.ILoginTask#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
            // keep in preference and only deploy one time
            if (prefs.get("org.talend.librariesmanager.migration.DeployLibJavaToMavenLoginTask", null) != null) {
                return;
            }
            String librariesPath = LibrariesManagerUtils.getLibrariesPath(ECodeLanguage.JAVA);
            File storageDir = new File(librariesPath);
            if (storageDir.exists()) {
                ILibraryManagerService librariesManager = (ILibraryManagerService) GlobalServiceRegister.getDefault().getService(
                        ILibraryManagerService.class);
                librariesManager.deploy(storageDir.toURI());
            }
            prefs.put("org.talend.librariesmanager.migration.DeployLibJavaToMavenLoginTask", "done");
            prefs.flush();
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
