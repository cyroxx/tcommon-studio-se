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
package org.talend.designer.maven.tools.creator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.JobInfo;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.Project;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.model.repository.SVNConstant;
import org.talend.core.model.utils.JavaResourcesHelper;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.core.runtime.process.JobInfoProperties;
import org.talend.designer.core.IDesignerCoreService;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.template.IProjectSettingPreferenceConstants;
import org.talend.designer.maven.template.MavenTemplateConstants;
import org.talend.designer.maven.template.MavenTemplateManager;
import org.talend.designer.maven.tools.MavenPomSynchronizer;
import org.talend.designer.maven.tools.ProcessorDependenciesManager;
import org.talend.designer.maven.utils.PomUtil;
import org.talend.designer.maven.utils.TalendCodeProjectUtil;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.utils.io.FilesUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * created by ggu on 4 Feb 2015 Detailled comment
 *
 */
public class CreateMavenTestPom extends CreateMavenBundleTemplatePom {

    private final IProcessor jobProcessor;

    private Set<JobInfo> clonedJobInfos = new HashSet<JobInfo>();

    private final ProcessorDependenciesManager processorDependenciesManager;

    private IFolder objectTypeFolder;

    private IPath itemRelativePath;

    public CreateMavenTestPom(IProcessor jobProcessor, IFile pomFile) {
        super(pomFile, MavenTemplateConstants.POM_TEST_TEMPLATE_FILE_NAME);
        Assert.isNotNull(jobProcessor);
        this.jobProcessor = jobProcessor;
        this.processorDependenciesManager = new ProcessorDependenciesManager(jobProcessor);
    }

    public IProcessor getJobProcessor() {
        return this.jobProcessor;
    }

    public IFolder getObjectTypeFolder() {
        return objectTypeFolder;
    }

    public void setObjectTypeFolder(IFolder objectTypeFolder) {
        this.objectTypeFolder = objectTypeFolder;
    }

    public IPath getItemRelativePath() {
        return itemRelativePath;
    }

    public void setItemRelativePath(IPath itemRelativePath) {
        this.itemRelativePath = itemRelativePath;
    }

    private Set<JobInfo> getClonedJobInfos() {
        if (this.clonedJobInfos.isEmpty()) {
            Set<JobInfo> buildChildrenJobs = getJobProcessor().getBuildChildrenJobs();
            for (JobInfo jobInfo : buildChildrenJobs) {
                JobInfo newJobInfo = new JobInfo(jobInfo.getJobId(), jobInfo.getContextName(), jobInfo.getJobVersion());

                newJobInfo.setJobName(jobInfo.getJobName());
                newJobInfo.setApplyContextToChildren(jobInfo.isApplyContextToChildren());
                newJobInfo.setContext(jobInfo.getContext());
                newJobInfo.setProjectFolderName(jobInfo.getProjectFolderName());
                newJobInfo.setProcessItem(jobInfo.getProcessItem());

                ProcessItem processItem = newJobInfo.getProcessItem();
                if (processItem == null) {
                    try {
                        final IRepositoryViewObject obj = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory()
                                .getSpecificVersion(jobInfo.getJobId(), jobInfo.getJobVersion(), true);
                        if (obj != null) {
                            final Item item = obj.getProperty().getItem();
                            if (item instanceof ProcessItem) {
                                processItem = (ProcessItem) item;
                                newJobInfo.setProcessItem(processItem);
                            }
                        }
                    } catch (PersistenceException e) {
                        //
                    }

                }

                if (processItem != null) {
                    IProcess process = jobInfo.getProcess();
                    // get the type
                    if (process == null && GlobalServiceRegister.getDefault().isServiceRegistered(IDesignerCoreService.class)) {
                        IDesignerCoreService service = (IDesignerCoreService) GlobalServiceRegister.getDefault().getService(
                                IDesignerCoreService.class);
                        process = service.getProcessFromItem(processItem);
                        newJobInfo.setProcess(process);
                    }
                    final String projectFolderName = JavaResourcesHelper.getProjectFolderName(processItem);
                    newJobInfo.setProjectFolderName(projectFolderName);
                }
                clonedJobInfos.add(newJobInfo);
            }
        }
        return this.clonedJobInfos;
    }

    @SuppressWarnings("nls")
    @Override
    protected Model createModel() {
        Model model = super.createModel();
        //
        final IProcessor jProcessor = getJobProcessor();
        final IProcess process = jProcessor.getProcess();

        // don't change the template setting for groupId
        // model.setGroupId(TalendMavenContants.DEFAULT_JOB_GROUP_ID);
        final String jobName = JavaResourcesHelper.escapeFileName(process.getName());
        String artifact = jobName;

        model.setArtifactId(artifact);
        model.setVersion(process.getVersion());

        Property property = jProcessor.getProperty();
        if (property != null && property.getItem() != null) {
            ERepositoryObjectType itemType = ERepositoryObjectType.getItemType(property.getItem());
            if (itemType != null) {
                model.setName(jobName + '(' + itemType.getLabel() + ')');
            }
        }

        PomUtil.checkParent(model, this.getPomFile());

        addDependencies(model);

        return model;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.designer.maven.tools.creator.CreateMavenBundleTemplatePom#getTemplateStream()
     */
    @Override
    protected InputStream getTemplateStream() throws IOException {
        File templateFile = PomUtil.getTemplateFile(getObjectTypeFolder(), getItemRelativePath(),
                TalendMavenConstants.POM_FILE_NAME);
        return MavenTemplateManager.getTemplateStream(templateFile,
                IProjectSettingPreferenceConstants.MAVEN_SCRIPT_AUTONOMOUSJOB_TEMPLATE, getBundleTemplateName());
    }

    /**
     * 
     * Add the properties for job.
     */
    @Override
    @SuppressWarnings("nls")
    protected void addProperties(Model model) {
        super.addProperties(model);

        Properties properties = model.getProperties();

        final IProcessor jProcessor = getJobProcessor();
        final IProcess process = jProcessor.getProcess();
        final IContext context = jProcessor.getContext();
        final Property property = jProcessor.getProperty();

        // same as JavaProcessor.initCodePath
        String jobClassPackageFolder = JavaResourcesHelper.getJobClassPackageFolder(property.getItem(), true);

        Project project = ProjectManager.getInstance().getProject(property);
        if (project == null) { // current project
            project = ProjectManager.getInstance().getCurrentProject().getEmfProject();
        }
        String mainProjectBranch = ProjectManager.getInstance().getMainProjectBranch(project);
        if (mainProjectBranch == null) {
            mainProjectBranch = SVNConstant.NAME_TRUNK;
        }

        JobInfoProperties jobInfoProp = new JobInfoProperties((ProcessItem) property.getItem(), context.getName(), false, false);

        checkPomProperty(properties, "talend.job.path", "@JobPath@", jobClassPackageFolder);
        // checkPomProperty(properties, "talend.job.jarName", "@JobJarName@", jobFolderName);
        /*
         * for jobInfo.properties
         * 
         * should be same as JobInfoBuilder
         */
        checkPomProperty(properties, "talend.project.name", "@ProjectName@",
                jobInfoProp.getProperty(JobInfoProperties.PROJECT_NAME, project.getTechnicalLabel()));
        checkPomProperty(properties, "talend.project.id", "@ProjectId@",
                jobInfoProp.getProperty(JobInfoProperties.PROJECT_ID, String.valueOf(project.getId())));
        checkPomProperty(properties, "talend.project.branch", "@ProjectBranch@",
                jobInfoProp.getProperty(JobInfoProperties.BRANCH, mainProjectBranch));
        if (TalendCodeProjectUtil.stripVersion) {
            checkPomProperty(properties, "talend.job.name", null, // keep empty, in order to replace it.
                    jobInfoProp.getProperty(JobInfoProperties.JOB_NAME, property.getLabel()));
        } else {
            checkPomProperty(properties, "talend.job.name", "@JobName@", "${project.artifactId}");
        }
        checkPomProperty(properties, "talend.job.version", "@JobVersion@", "${project.version}");
        checkPomProperty(properties, "talend.job.date", "@JobDate@",
                jobInfoProp.getProperty(JobInfoProperties.DATE, JobInfoProperties.DATAFORMAT.format(new Date())));
        checkPomProperty(properties, "talend.job.context", "@JobContext@",
                jobInfoProp.getProperty(JobInfoProperties.CONTEXT_NAME, context.getName()));
        checkPomProperty(properties, "talend.job.id", "@JobId@",
                jobInfoProp.getProperty(JobInfoProperties.JOB_ID, process.getId()));
        if (TalendCodeProjectUtil.stripVersion) {
            checkPomProperty(properties, "talend.job.class", null, // keep empty, in order to replace it.
                    jProcessor.getMainClass());
        } else {
            checkPomProperty(properties, "talend.job.class", "@JobClass@", "${talend.job.package}.${talend.job.name}");
        }
        checkPomProperty(properties, "talend.product.version", "@ProductVersion@",
                jobInfoProp.getProperty(JobInfoProperties.COMMANDLINE_VERSION, VersionUtils.getVersion()));

    }

    /**
     * add dependencies for pom.
     */
    protected void addDependencies(Model model) {
        try {
            processorDependenciesManager.updateDependencies(null, model);

            // add children jobs in dependencies
            final List<Dependency> dependencies = model.getDependencies();
            String parentId = getJobProcessor().getProperty().getId();
            final Set<JobInfo> clonedChildrenJobInfors = getClonedJobInfos();
            for (JobInfo jobInfo : clonedChildrenJobInfors) {
                if (jobInfo.getFatherJobInfo() != null && jobInfo.getFatherJobInfo().getJobId().equals(parentId)) {
                    Dependency d = PomUtil.createDependency(model.getGroupId(), jobInfo.getJobName(), jobInfo.getJobVersion(),
                            null);
                    dependencies.add(d);
                }
            }

        } catch (ProcessorException e) {
            ExceptionHandler.process(e);
        }
    }

    @Override
    public void create(IProgressMonitor monitor) throws Exception {
        super.create(monitor);
        // generate routines
        MavenPomSynchronizer pomSync = new MavenPomSynchronizer(this.getJobProcessor().getTalendJavaProject());
        pomSync.syncRoutinesPom(false);

        // refresh
        getPomFile().getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
    }
}
