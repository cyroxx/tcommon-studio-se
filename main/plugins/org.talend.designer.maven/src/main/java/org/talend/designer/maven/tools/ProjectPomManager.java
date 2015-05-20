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
package org.talend.designer.maven.tools;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.talend.core.model.process.JobInfo;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.tools.repo.LocalRepositoryManager;
import org.talend.designer.maven.utils.PomUtil;
import org.talend.designer.runprocess.IProcessor;

/**
 * DOC ggu class global comment. Detailled comment
 */
public class ProjectPomManager {

    protected static final MavenModelManager MODEL_MANAGER = MavenPlugin.getMavenModelManager();

    private IFile projectPomFile;

    /**
     * true by default, update all
     */
    private boolean updateModules = true;

    private boolean updateRepositories = true;

    private boolean updateDependencies = true;

    public ProjectPomManager(IProject project) {
        super();
        Assert.isNotNull(project);
        this.projectPomFile = project.getFile(TalendMavenConstants.POM_FILE_NAME);
    }

    public void setUpdateModules(boolean updateModules) {
        this.updateModules = updateModules;
    }

    public void setUpdateRepositories(boolean updateRepositories) {
        this.updateRepositories = updateRepositories;
    }

    public void setUpdateDependencies(boolean updateDependencies) {
        this.updateDependencies = updateDependencies;
    }

    protected boolean isUpdateModules() {
        return updateModules;
    }

    protected boolean isUpdateRepositories() {
        return updateRepositories;
    }

    protected boolean isUpdateDependencies() {
        return updateDependencies;
    }

    public void update(IProgressMonitor monitor, IProcessor processor) throws Exception {
        projectPomFile.getProject().refreshLocal(IResource.DEPTH_ONE, monitor);
        if (!projectPomFile.exists()) {// delete by user manually?
            // create it or nothing to do?
            return;
        }
        Model projectModel = MODEL_MANAGER.readMavenModel(projectPomFile);

        // modules
        updateModulesList(monitor, processor, projectModel);
        // check repository
        updateRepositories(monitor, processor, projectModel);
        // dependencies
        updateDependencies(monitor, processor, projectModel);

        PomUtil.savePom(monitor, projectModel, projectPomFile);

        projectPomFile.getProject().refreshLocal(IResource.DEPTH_ONE, monitor);
    }

    /**
     * 
     * update the modules list for project pom.
     * 
     * The routines should be added always.
     */
    protected void updateModulesList(IProgressMonitor monitor, IProcessor processor, Model projectModel) throws Exception {
        if (!isUpdateModules()) {
            return;
        }
        List<String> modulesList = new ArrayList<String>();
        // add routines always.
        final Model routinesModel = PomUtil.getRoutinesTempalteModel();
        String routinesModule = PomUtil.getPomFileName(routinesModel.getArtifactId());
        modulesList.add(routinesModule);

        for (JobInfo childJob : processor.getBuildChildrenJobs()) {
            modulesList.add(PomUtil.getPomFileName(childJob.getJobName()));
        }
        modulesList.add(PomUtil.getPomFileName(processor.getProperty().getLabel()));

        // check the modules
        List<String> modules = projectModel.getModules();
        if (modules == null) {
            modules = new ArrayList<String>();
            projectModel.setModules(modules);
        } else if (!modules.isEmpty()) {
            modules.clear(); // clean all?
        }

        modules.addAll(modulesList);
    }

    /**
     * When the enable the local repo in codes project(.Java), make sure set the local repository.
     */
    protected void updateRepositories(IProgressMonitor monitor, IProcessor processor, Model projectModel) throws Exception {
        if (!isUpdateRepositories()) {
            return;
        }
        // set repositories.
        List<Repository> repositories = projectModel.getRepositories();
        if (repositories == null) {
            repositories = new ArrayList<Repository>();
            projectModel.setRepositories(repositories);
        }
        Repository localRep = null;
        for (Repository r : repositories) {
            if (r.getId().equals(LocalRepositoryManager.REPO_ID)) {
                localRep = r;
                break;
            }
        }
        if (LocalRepositoryManager.ENABLE_LOCAL_REPO) {
            if (localRep == null) { // not existed, add one
                Repository localRepo = new Repository();

                localRepo.setId(LocalRepositoryManager.REPO_ID);
                localRepo.setUrl("file://${basedir}/" + LocalRepositoryManager.REPO_FOLDER);

                repositories.add(localRepo);
            }
        } else {
            if (localRep != null) { // existed, delete it
                repositories.remove(localRep);
            }
        }

    }

    /**
     * If standard job and base job pom file existed, will use the dependences of job pom directly.
     */
    protected void updateDependencies(IProgressMonitor monitor, IProcessor processor, Model projectModel) throws Exception {
        if (!isUpdateDependencies()) {
            return;
        }
        IFile basePomFile = getBasePomFile();
        if (isStandardJob() && basePomFile != null && basePomFile.getLocation().toFile().exists()) {
            if (!basePomFile.exists()) {
                basePomFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
            }
            Model jobModel = MODEL_MANAGER.readMavenModel(basePomFile);

            // fresh is false, make sure all jobs can be compile ok
            ProcessorDependenciesManager.updateDependencies(monitor, projectModel, jobModel.getDependencies(), false);

        } else {
            ProcessorDependenciesManager processorDependenciesManager = new ProcessorDependenciesManager(processor);
            processorDependenciesManager.updateDependencies(monitor, projectModel);
        }

        // install custom jar to m2/repo
        PomUtil.installDependencies(projectModel.getDependencies());
    }

    protected boolean isStandardJob() {
        return true;
    }

    protected IFile getBasePomFile() {
        return null;
    }

}
