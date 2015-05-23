// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.maven.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.tools.creator.CreateMavenCodeProject;

/**
 * created by ggu on 23 Jan 2015 Detailled comment
 *
 */
public final class TalendCodeProjectUtil {

    /**
     * must be same maven-dependency-plugin in job pom.
     * 
     * later, if false, need remove this flag maybe.
     */
    public static boolean stripVersion = true;

    @SuppressWarnings("restriction")
    public static IProject initCodeProject(IProgressMonitor monitor) throws Exception {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        IProject codeProject = root.getProject(TalendMavenConstants.PROJECT_NAME);
        boolean recreate = false;
        if (codeProject.exists()) {
            IFile pomFile = codeProject.getFile(TalendMavenConstants.POM_FILE_NAME);
            // if no pom or maven nature, will re-create pure Maven project.
            if (!pomFile.exists() || !codeProject.hasNature(IMavenConstants.NATURE_ID)) {
                codeProject.close(monitor);
                codeProject.delete(true, true, monitor); // delete
                recreate = true;
            }
        } else {
            recreate = true;
        }
        if (recreate) {
            CreateMavenCodeProject createProject = new CreateMavenCodeProject(codeProject);
            createProject.create(monitor);
            codeProject = createProject.getProject();
        }

        codeProject.open(IProject.BACKGROUND_REFRESH, monitor);

        return codeProject;
    }

    // public static IMarker[] getMavenMarks(IFile file) throws CoreException {
    // IMarker[] findMarkers = file.findMarkers(IMavenConstants.MARKER_CONFIGURATION_ID, true, IResource.DEPTH_ONE);
    // return findMarkers;
    // }

}
