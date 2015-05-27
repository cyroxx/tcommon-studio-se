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

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.talend.designer.maven.ui.dialog.model.RepositoryMavenSettingStore;
import org.talend.designer.maven.ui.projectsetting.job.AutonomousJobProjectSettingPage;

/**
 * DOC ggu class global comment. Detailled comment
 */
public class RepositoryMavenPomNode extends RepositoryMavenSettingNode {

    public RepositoryMavenPomNode(String id, IFile pomFile) {
        super(id, pomFile);
    }

    @Override
    protected PreferencePage createPreferencePage() {
        return new AutonomousJobProjectSettingPage() {

            @Override
            public void createControl(Composite parent) {
                noDefaultAndApplyButton();
                this.setTitle(this.getHeadTitle());

                super.createControl(parent);
            }

            @Override
            protected void initStore() {
                this.setPreferenceStore(new RepositoryMavenSettingStore(this.getPreferenceKey(), getFile()));
            }

            @Override
            public void load() throws IOException {
                if (getPreferenceStore() instanceof PreferenceStore) {
                    ((PreferenceStore) getPreferenceStore()).load();
                }
                super.load();
            }

            @Override
            public void save() throws IOException {
                if (getPreferenceStore() instanceof PreferenceStore) {
                    ((PreferenceStore) getPreferenceStore()).save();
                }
                super.save();
            }

        };
    }
}
