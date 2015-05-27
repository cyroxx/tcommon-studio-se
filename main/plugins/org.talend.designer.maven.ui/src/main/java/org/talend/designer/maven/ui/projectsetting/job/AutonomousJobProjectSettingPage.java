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
package org.talend.designer.maven.ui.projectsetting.job;

import org.talend.designer.maven.template.IProjectSettingPreferenceConstants;
import org.talend.designer.maven.ui.i18n.Messages;
import org.talend.designer.maven.ui.projectsetting.AbstractPersistentProjectSettingPage;

/**
 * DOC ggu class global comment. Detailled comment
 */
public class AutonomousJobProjectSettingPage extends AbstractPersistentProjectSettingPage {

    @Override
    protected String getPreferenceKey() {
        return IProjectSettingPreferenceConstants.MAVEN_SCRIPT_AUTONOMOUSJOB_TEMPLATE;
    }

    @Override
    protected String getHeadTitle() {
        return Messages.getString("AutonomousJobProjectSettingPage_Title"); //$NON-NLS-1$
    }

}
