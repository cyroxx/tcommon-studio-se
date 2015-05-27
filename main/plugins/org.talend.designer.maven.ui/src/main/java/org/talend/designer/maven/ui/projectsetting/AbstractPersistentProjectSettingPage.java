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
package org.talend.designer.maven.ui.projectsetting;

import java.io.IOException;

import org.talend.core.runtime.projectsetting.AbstractScriptProjectSettingPage;
import org.talend.designer.maven.ui.DesignerMavenUiPlugin;

/**
 * DOC ggu class global comment. Detailled comment
 */
public abstract class AbstractPersistentProjectSettingPage extends AbstractScriptProjectSettingPage {

    @Override
    protected void initStore() {
        this.setPreferenceStore(DesignerMavenUiPlugin.getDefault().getProjectPreferenceManager().getPreferenceStore());
    }

    public void load() throws IOException {
        // nothing to do
    }

    public void save() throws IOException {
        // nothing to do
    }
}
