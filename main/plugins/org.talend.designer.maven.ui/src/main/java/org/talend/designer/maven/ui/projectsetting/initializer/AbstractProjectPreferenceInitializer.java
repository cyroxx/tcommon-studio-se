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
package org.talend.designer.maven.ui.projectsetting.initializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.services.resource.IExportScriptResourcesService;

/**
 * DOC ggu class global comment. Detailled comment
 */
public abstract class AbstractProjectPreferenceInitializer extends AbstractPreferenceInitializer {

    protected abstract IPreferenceStore getPreferenceStore();

    protected String getContents(File scriptFile) {
        if (scriptFile != null && scriptFile.exists()) {
            try {
                return new Scanner(scriptFile).useDelimiter("\\A").next(); //$NON-NLS-1$
            } catch (FileNotFoundException e) {
                ExceptionHandler.process(e);
            }
        }
        return null;
    }

    protected void setDefaultValue(IPreferenceStore preferenceStore, IExportScriptResourcesService service, String scriptPath,
            String profKey) {
        String pomContents = getContents(new File(service.getMavenScriptFilePath(scriptPath)));
        if (pomContents != null && pomContents.length() > 0) {
            preferenceStore.setDefault(profKey, pomContents);
        }
    }

    @Override
    public void initializeDefaultPreferences() {
        initializeFields(getPreferenceStore());
    }

    protected void initializeFields(IPreferenceStore preferenceStore) {
        //
    }

}
