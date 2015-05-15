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
package org.talend.designer.maven.utils;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ILibraryManagerService;
import org.talend.core.model.general.Project;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.JobInfo;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.utils.JavaResourcesHelper;
import org.talend.core.runtime.maven.MavenArtifact;
import org.talend.core.runtime.maven.MavenConstants;
import org.talend.core.runtime.maven.MavenUrlHelper;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.tools.repo.LocalRepositoryManager;
import org.talend.designer.runprocess.IProcessor;
import org.talend.repository.ProjectManager;

/**
 * created by ggu on 6 Feb 2015 Detailled comment
 *
 */
public class PomUtil {

    public static void savePom(IProgressMonitor monitor, Model model, IFile pomFile) throws Exception {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        if (pomFile == null) {
            throw new NullPointerException("the output file is null.");
        }

        /*
         * need find one way to do overwrite.
         */
        // IModelManager modelManager = StructuredModelManager.getModelManager();
        // IStructuredModel sModel = modelManager.getModelForRead(pomFile);
        // IDOMModel domModel = (IDOMModel)
        // modelManager.getModelForEdit(sModel.getStructuredDocument());
        // ElementValueProvider privider = new ElementValueProvider(PomEdits.ARTIFACT_ID);
        // Element el = privider.get(domModel.getDocument());
        // PomEdits.setText(el, model.getArtifactId());
        // sModel.save();

        pomFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
        if (pomFile.exists()) {
            pomFile.delete(true, monitor);
        }

        MavenPlugin.getMavenModelManager().createMavenModel(pomFile, model);
    }

    /**
     * 
     * just unify for routines with template.
     */
    private static Model templateRoutinesModel, templateCodeProjectMOdel;

    public static Model getRoutinesTempalteModel() {
        if (templateRoutinesModel == null) {
            templateRoutinesModel = new Model();

            templateRoutinesModel.setGroupId(TalendMavenConstants.DEFAULT_ROUTINES_GROUP_ID);
            templateRoutinesModel.setArtifactId(TalendMavenConstants.DEFAULT_ROUTINES_ARTIFACT_ID);
            templateRoutinesModel.setVersion(PomUtil.getDefaultMavenVersion());
        }
        return templateRoutinesModel;
    }

    public static Model getCodeProjectTemplateModel() {
        if (templateCodeProjectMOdel == null) {
            templateCodeProjectMOdel = new Model();

            templateCodeProjectMOdel.setGroupId(TalendMavenConstants.DEFAULT_CODE_PROJECT_GROUP_ID);
            templateCodeProjectMOdel.setArtifactId(TalendMavenConstants.DEFAULT_CODE_PROJECT_ARTIFACT_ID);
            templateCodeProjectMOdel.setVersion(PomUtil.getDefaultMavenVersion());

        }
        return templateCodeProjectMOdel;
    }

    public static Model getJunitTemplateModel() {
        Model junitModel = new Model();
        junitModel.setGroupId(TalendMavenConstants.DEFAULT_JUNIT_ARTIFACT_GROUP);
        junitModel.setArtifactId(TalendMavenConstants.DEFAULT_JUNIT_ARTIFACT_ID);
        junitModel.setVersion(TalendMavenConstants.DEFAULT_JUNIT_ARTIFACT_VERSION);
        return junitModel;
    }

    public static boolean isJunitArtifact(String artifactId) {
        if (TalendMavenConstants.DEFAULT_JUNIT_ARTIFACT_ID.equals(artifactId)) {
            return true;
        }
        return false;
    }

    /**
     * 
     * get the pom name, if name is null, return default one "pom.xml", else will be "pom_<name>.xml"
     */
    public static String getPomFileName(String name) {
        String pomFileName = TalendMavenConstants.POM_FILE_NAME;
        if (name != null && name.length() > 0) {
            pomFileName = TalendMavenConstants.POM_NAME + '_' + name + TalendMavenConstants.XML_EXT;
        }
        return pomFileName;
    }

    /**
     * get the assembly name, if name is null, return default one "assembly.xml", else will be "assembly_<name>.xml"
     */
    public static String getAssemblyFileName(String name) {
        String assemblyFileName = TalendMavenConstants.ASSEMBLY_FILE_NAME;
        if (name != null && name.length() > 0) {
            assemblyFileName = TalendMavenConstants.ASSEMBLY_NAME + '_' + name + TalendMavenConstants.XML_EXT;
        }
        return assemblyFileName;
    }

    /**
     * 
     * DOC ggu Comment method "getDefaultMavenVersion".
     * 
     * @return 6.0.0, without classifier.
     */
    public static String getDefaultMavenVersion() {
        String version = VersionUtils.getVersion();
        try {
            org.osgi.framework.Version v = new org.osgi.framework.Version(version);
            // only get major.minor.micro
            org.osgi.framework.Version simpleVersion = new org.osgi.framework.Version(v.getMajor(), v.getMinor(), v.getMicro());
            version = simpleVersion.toString();
        } catch (IllegalArgumentException e) {
            version = TalendMavenConstants.DEFAULT_VERSION;
        }
        return version;
    }

    /**
     * 
     * DOC ggu Comment method "checkParent". make sure the parent are unified.
     * 
     * @param curModel
     * @param curPomFile
     */
    public static void checkParent(Model curModel, IFile curPomFile) {
        Parent parent = curModel.getParent();
        if (parent == null) {
            parent = new Parent();
            curModel.setParent(parent);
        }
        Model codeProjectTemplateModel = null;
        try {
            IFile projectPomFile = curPomFile.getParent().getFile(new Path(TalendMavenConstants.POM_FILE_NAME));
            if (projectPomFile.exists()) {
                codeProjectTemplateModel = MavenPlugin.getMavenModelManager().readMavenModel(projectPomFile);
            }
        } catch (CoreException e) {
            //
        }
        if (codeProjectTemplateModel == null) {
            /*
             * same CreateMavenCodeProject.covertJavaProjectToPom
             */
            codeProjectTemplateModel = PomUtil.getCodeProjectTemplateModel();
        }
        parent.setGroupId(codeProjectTemplateModel.getGroupId());
        parent.setArtifactId(codeProjectTemplateModel.getArtifactId());
        parent.setVersion(codeProjectTemplateModel.getVersion());

        parent.setRelativePath("./" + TalendMavenConstants.POM_FILE_NAME);

    }

    /**
     * DOC ggu Comment method "createModuleSystemScopeDependency".
     * 
     * @return
     */
    public static Dependency createModuleDependency(String groupId, String artifactId, String version, String type) {
        if (artifactId == null) {
            return null;
        }
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId == null ? MavenConstants.DEFAULT_LIB_GROUP_ID : groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version == null ? MavenConstants.DEFAULT_LIB_VERSION : version);
        dependency.setType(type == null ? TalendMavenConstants.PACKAGING_JAR : type);

        return dependency;
    }

    public static Dependency createModuleDependency(String str) {
        if (str == null) {
            return null;
        }
        String mvnUrl = str;
        if (!MavenUrlHelper.isMvnUrl(str)) {
            mvnUrl = MavenUrlHelper.generateMvnUrlForJarName(str);
        }

        MavenArtifact artifact = MavenUrlHelper.parseMvnUrl(mvnUrl);
        if (artifact != null) {
            return createModuleDependency(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                    artifact.getType());
        }
        return null;
    }

    /**
     * 
     * something like org.talend.demo.
     */
    public static String getCurProjectGroup() {
        final Project currentProject = ProjectManager.getInstance().getCurrentProject();
        return JavaResourcesHelper.getGroupName(currentProject.getTechnicalLabel());
    }

    /**
     * 
     * According to the process, generate the groud id, like org.talend.process.di.demo.
     */
    public static String generateGroupId(final IProcessor jProcessor) {
        final Property property = jProcessor.getProperty();
        final IProcess process = jProcessor.getProcess();

        final String projectFolderName = JavaResourcesHelper.getProjectFolderName(property.getItem());
        return generateGroupId(projectFolderName, process.getComponentsType());
    }

    public static String generateGroupId(String projectFolderName, String type) {
        String groupId = JavaResourcesHelper.getGroupName(projectFolderName);

        if (type != null) {
            groupId += '.' + type.toLowerCase();
        }
        return groupId;
    }

    public static String generateGroupId(final JobInfo jobInfo) {
        ProcessItem processItem = jobInfo.getProcessItem();
        if (processItem != null) {
            String componentsType = null;
            IProcess process = jobInfo.getProcess();
            if (process != null) {
                componentsType = process.getComponentsType();
            }

            final String projectFolderName = JavaResourcesHelper.getProjectFolderName(processItem);
            return generateGroupId(projectFolderName, componentsType);
        } else { // return one default one.
            return generateGroupId(null, null);
        }

    }

    public static boolean isAvailable(Dependency dependency) {
        MavenArtifact artifact = convertToArtifact(dependency);
        return artifact != null && isAvailable(artifact);
    }

    public static boolean isAvailable(MavenArtifact artifact) {
        boolean unavailable = true;
        if (artifact != null) {
            try {
                // only check local repository. the set the remote repositories null.
                unavailable = MavenPlugin.getMaven().isUnavailable(artifact.getGroupId(), artifact.getArtifactId(),
                        artifact.getVersion(), artifact.getType(), artifact.getClassifier(), null);
            } catch (CoreException e) {
                ExceptionHandler.process(e);
            }
        }
        return !unavailable;
    }

    /**
     * return the list of existed maven artifacts in local repository.
     */
    public static Set<String> availableArtifacts(IProgressMonitor monitor, String[] mvnUrls) throws Exception {
        Set<String> existedMvnUrls = new LinkedHashSet<String>();
        if (mvnUrls != null) {
            for (String mvnUrl : mvnUrls) {
                MavenArtifact artifact = MavenUrlHelper.parseMvnUrl(mvnUrl);
                if (isAvailable(artifact)) {
                    existedMvnUrls.add(mvnUrl);
                }
            }
        }
        return existedMvnUrls;
    }

    public static String generateMvnUrl(Dependency dependency) {
        if (dependency != null) {
            return MavenUrlHelper.generateMvnUrl(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                    dependency.getType(), dependency.getClassifier());
        }
        return null;
    }

    public static MavenArtifact convertToArtifact(Dependency dependency) {
        if (dependency != null) {
            MavenArtifact artifact = new MavenArtifact();

            artifact.setGroupId(dependency.getGroupId());
            artifact.setArtifactId(dependency.getArtifactId());
            artifact.setVersion(dependency.getVersion());
            artifact.setClassifier(dependency.getClassifier());
            artifact.setType(dependency.getType());

            return artifact;
        }
        return null;
    }

    /**
     * 
     * check the dependencies is custom jar or not, if existed, and invalid in m2/repo, just install in local.
     */
    public static void installDependencies(List<Dependency> dependencies) {
        if (dependencies != null && GlobalServiceRegister.getDefault().isServiceRegistered(ILibraryManagerService.class)) {
            ILibraryManagerService libService = (ILibraryManagerService) GlobalServiceRegister.getDefault().getService(
                    ILibraryManagerService.class);

            LocalRepositoryManager repoManager = null;
            // FIXME,the launcher is not stable, sometimes, can't install successfully.
            repoManager = LocalRepositoryManager.LAUNCHER;
            // repoManager = LocalRepositoryManager.AETHER;

            for (Dependency d : dependencies) {
                // only process talend libs
                if (MavenConstants.DEFAULT_LIB_GROUP_ID.equals(d.getGroupId())) {
                    String type = d.getType();
                    if (type == null || type.trim().length() == 0) {
                        type = MavenConstants.TYPE_JAR; // jar by default
                    }
                    String jarPath = libService.getJarPath(d.getArtifactId() + '.' + type);
                    try {
                        installJar(repoManager, new File(jarPath), PomUtil.convertToArtifact(d));
                    } catch (Exception e) {
                        ExceptionHandler.process(e);
                    }
                }
            }

            repoManager.cleanup(null);

        }
    }

    public static void installJar(LocalRepositoryManager repoManager, File libFile, MavenArtifact artifact) throws Exception {
        // in lib/java, and not existed in m2/repo
        if (libFile.exists() && !PomUtil.isAvailable(artifact)) {
            repoManager.install(libFile, artifact);
        }
    }

    public static String getArtifactPath(MavenArtifact artifact) {
        if (artifact == null) {
            return null;
        }
        IMaven maven = MavenPlugin.getMaven();
        try {
            String artifactPath = maven.getArtifactPath(maven.getLocalRepository(), artifact.getGroupId(),
                    artifact.getArtifactId(), artifact.getVersion(), artifact.getType(), artifact.getClassifier());
            if (artifactPath == null) {
                return null;
            }
            File file = new File(artifactPath);
            if (file.exists()) {
                return artifactPath;
            }
        } catch (CoreException e) {
            ExceptionHandler.process(e);
        }
        return null;
    }

}
