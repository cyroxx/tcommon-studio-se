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

import org.eclipse.jface.preference.PreferencePage;
import org.talend.designer.maven.ui.dialog.model.EMavenScriptCategory;
import org.talend.designer.maven.ui.utils.DesignerMavenUiHelper;
import org.talend.repository.model.RepositoryNode;

/**
 * DOC ggu class global comment. Detailled comment
 */
public class RepositoryMavenScriptCategoryNode extends RepositoryPreferenceNode {

    public RepositoryMavenScriptCategoryNode(String parentId, EMavenScriptCategory category, RepositoryNode node) {
        super(DesignerMavenUiHelper.buildRepositoryPreferenceNodeId(parentId, category.name()), category.getLabel(), category
                .getImageDesc(), node);
    }

    @Override
    protected PreferencePage createPreferencePage() {
        return new FolderMavenSettingPage(this.getNode());
    }
}
