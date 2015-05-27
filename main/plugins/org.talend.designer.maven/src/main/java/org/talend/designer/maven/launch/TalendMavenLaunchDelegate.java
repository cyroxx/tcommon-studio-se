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
package org.talend.designer.maven.launch;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.m2e.internal.launch.MavenLaunchDelegate;

/**
 * created by ycbai on 2015年5月27日 Detailled comment
 *
 */
@SuppressWarnings("restriction")
public class TalendMavenLaunchDelegate extends MavenLaunchDelegate {

    @Override
    public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
        String programArguments = super.getProgramArguments(configuration);
        String arguments = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""); //$NON-NLS-1$
        if (StringUtils.isNotEmpty(arguments)) {
            programArguments += " " + arguments; //$NON-NLS-1$
        }

        return programArguments;
    }

}
