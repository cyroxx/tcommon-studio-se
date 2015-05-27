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
public class CreateMavenJobPom extends CreateMavenBundleTemplatePom {

    private final IProcessor jobProcessor;

    private boolean applyContextToChild, addStat;

    private String windowsClasspath, unixClasspath;

    private Set<JobInfo> clonedJobInfos = new HashSet<JobInfo>();

    private final ProcessorDependenciesManager processorDependenciesManager;

    private IFile assemblyFile;

    private IFolder objectTypeFolder;

    private IPath itemRelativePath;

    public CreateMavenJobPom(IProcessor jobProcessor, IFile pomFile) {
        super(pomFile, MavenTemplateConstants.POM_JOB_TEMPLATE_FILE_NAME);
        Assert.isNotNull(jobProcessor);
        this.jobProcessor = jobProcessor;
        this.processorDependenciesManager = new ProcessorDependenciesManager(jobProcessor);
    }

    public IProcessor getJobProcessor() {
        return this.jobProcessor;
    }

    public boolean isApplyContextToChild() {
        return this.applyContextToChild;
    }

    public void setApplyContextToChild(boolean applyContextToChild) {
        this.applyContextToChild = applyContextToChild;
    }

    public boolean isAddStat() {
        return this.addStat;
    }

    public void setAddStat(boolean addStat) {
        this.addStat = addStat;
    }

    public String getWindowsClasspath() {
        return this.windowsClasspath;
    }

    public void setWindowsClasspath(String windowsClasspath) {
        this.windowsClasspath = windowsClasspath;
    }

    public String getUnixClasspath() {
        return this.unixClasspath;
    }

    public void setUnixClasspath(String unixClasspath) {
        this.unixClasspath = unixClasspath;
    }

    public IFile getAssemblyFile() {
        return assemblyFile;
    }

    public void setAssemblyFile(IFile assemblyFile) {
        this.assemblyFile = assemblyFile;
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
        if (!FilesUtils.allInSameFolder(templateFile, TalendMavenConstants.ASSEMBLY_FILE_NAME)) {
            templateFile = null; // force to set null, in order to use the template from other places.
        }
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
        String jobClassPackageFolder = JavaResourcesHelper.getJobClassPackageFolder(property.getItem());
        String jobClassPackage = JavaResourcesHelper.getJobClassPackageName(property.getItem());
        String jobFolderName = JavaResourcesHelper.getJobFolderName(property.getLabel(), property.getVersion());

        Project project = ProjectManager.getInstance().getProject(property);
        if (project == null) { // current project
            project = ProjectManager.getInstance().getCurrentProject().getEmfProject();
        }
        String mainProjectBranch = ProjectManager.getInstance().getMainProjectBranch(project);
        if (mainProjectBranch == null) {
            mainProjectBranch = SVNConstant.NAME_TRUNK;
        }

        JobInfoProperties jobInfoProp = new JobInfoProperties((ProcessItem) property.getItem(), context.getName(),
                isApplyContextToChild(), isAddStat());

        checkPomProperty(properties, "talend.job.path", "@JobPath@", jobClassPackageFolder);
        checkPomProperty(properties, "talend.job.package", "@JobPackage@", jobClassPackage);
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
        checkPomProperty(properties, "talend.job.stat", "@JobStat@",
                jobInfoProp.getProperty(JobInfoProperties.ADD_STATIC_CODE, Boolean.FALSE.toString()));
        checkPomProperty(properties, "talend.job.applyContextToChildren", "@JobApplyContextToChildren@",
                jobInfoProp.getProperty(JobInfoProperties.APPLY_CONTEXY_CHILDREN, Boolean.FALSE.toString()));
        checkPomProperty(properties, "talend.product.version", "@ProductVersion@",
                jobInfoProp.getProperty(JobInfoProperties.COMMANDLINE_VERSION, VersionUtils.getVersion()));

        /*
         * for bat/sh in assembly
         */
        checkPomProperty(properties, "talend.job.bat.classpath", "@JobBatClasspath@", this.getWindowsClasspath());
        checkPomProperty(properties, "talend.job.sh.classpath", "@JobShClasspath@", this.getUnixClasspath());

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

        generateAssemblyFile(monitor);

        // generate routines
        MavenPomSynchronizer pomSync = new MavenPomSynchronizer(this.getJobProcessor().getTalendJavaProject());
        pomSync.syncRoutinesPom(false);

        // refresh
        getPomFile().getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
    }

    protected void generateAssemblyFile(IProgressMonitor monitor) throws Exception {
        IFile assemblyFile = this.getAssemblyFile();
        if (assemblyFile != null) {
            boolean set = false;
            // read template from project setting
            try {
                File templateFile = PomUtil.getTemplateFile(getObjectTypeFolder(), getItemRelativePath(),
                        TalendMavenConstants.ASSEMBLY_FILE_NAME);
                if (!FilesUtils.allInSameFolder(templateFile, TalendMavenConstants.POM_FILE_NAME)) {
                    templateFile = null; // force to set null, in order to use the template from other places.
                }

                String content = MavenTemplateManager.getTemplateContent(templateFile,
                        IProjectSettingPreferenceConstants.MAVEN_SCRIPT_AUTONOMOUSJOB_ASSEMBLY_TEMPLATE,
                        MavenTemplateConstants.ASSEMBLY_JOB_TEMPLATE_FILE_NAME);
                if (content != null) {
                    FileWriter writer = new FileWriter(assemblyFile.getLocation().toFile());
                    writer.write(content);
                    writer.close();

                    assemblyFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
                    set = true;
                }
            } catch (IOException e) {
                ExceptionHandler.process(e);
            }

            if (set) {
                // add children resources in assembly.
                addChildrenJobsInAssembly(monitor, assemblyFile);

                assemblyFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
            }
        }
    }

    @SuppressWarnings("nls")
    protected void addChildrenJobsInAssembly(IProgressMonitor monitor, IFile assemblyFile) throws Exception {
        if (!assemblyFile.exists()) {
            return;
        }
        final File file = assemblyFile.getLocation().toFile();
        // assemblyFile
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(file);
        if (document == null) {
            throw new IOException("Can't parse the file: " + file);
        }

        // files
        Node filesElem = getElement(document.getDocumentElement(), "files", 1);

        // fileSets
        Node fileSetsElem = getElement(document.getDocumentElement(), "fileSets", 1);
        if (fileSetsElem == null) {
            fileSetsElem = document.createElement("fileSets");
            document.appendChild(fileSetsElem);
        }

        List<String> childrenPomsIncludes = new ArrayList<String>();
        List<String> childrenFolderResourcesIncludes = new ArrayList<String>();

        final Set<JobInfo> clonedChildrenJobInfors = getClonedJobInfos();
        for (JobInfo child : clonedChildrenJobInfors) {
            String jobClassPackageFolder = null;
            if (child.getProcessItem() != null) {
                jobClassPackageFolder = JavaResourcesHelper.getJobClassPackageFolder(child.getProcessItem());
            } else {
                String projectName = null;
                String jobId = child.getJobId();
                if (jobId != null) {
                    IProxyRepositoryFactory proxyRepositoryFactory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
                    IRepositoryViewObject lastVersion = proxyRepositoryFactory.getLastVersion(jobId);
                    if (lastVersion != null) {
                        Property property = lastVersion.getProperty();
                        if (property != null) {
                            Project project = ProjectManager.getInstance().getProject(property.getItem());
                            projectName = project.getTechnicalLabel();
                        }
                    }
                }
                if (projectName == null) {// use current one
                    projectName = ProjectManager.getInstance().getCurrentProject().getTechnicalLabel();
                }
                jobClassPackageFolder = JavaResourcesHelper.getJobClassPackageFolder(projectName, child.getJobName(),
                        child.getJobVersion());
            }
            // children poms
            childrenPomsIncludes.add(PomUtil.getPomFileName(child.getJobName()));
            // conext resources
            childrenFolderResourcesIncludes.add(jobClassPackageFolder + "/**"); // add all context

        }
        /*
         * FIXME, if change the profiles setting for directory, must need change this parts.
         */
        if (!clonedChildrenJobInfors.isEmpty()) {
            // poms
            addAssemblyFileSets(fileSetsElem, "${poms.dir}", "${talend.job.name}", false, childrenPomsIncludes, null, null, null,
                    null, false, "add children pom files.");

            // src
            addAssemblyFileSets(fileSetsElem, "${sourcecodes.dir}", "${talend.job.name}/src/main/java/", false,
                    childrenFolderResourcesIncludes, null, null, null, null, false, "add children src resources files.");

            // contexts
            addAssemblyFileSets(fileSetsElem, "${resources.dir}", "${talend.job.name}/src/main/resources/", false,
                    childrenFolderResourcesIncludes, null, null, null, null, false, "add children context files to resources.");
            addAssemblyFileSets(fileSetsElem, "${contexts.running.dir}", "${talend.job.name}", false,
                    childrenFolderResourcesIncludes, null, null, null, null, false, "add children context files for running.");

            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transFormer = transFactory.newTransformer();
            transFormer.setOutputProperty(OutputKeys.INDENT, "yes");
            transFormer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(file)));

            // clean for children poms
            cleanChildrenPomSettings(monitor, childrenPomsIncludes);

            // refresh the project level for children poms
            assemblyFile.getProject().refreshLocal(IResource.DEPTH_ONE, monitor);
        }

    }

    protected void cleanChildrenPomSettings(IProgressMonitor monitor, List<String> childrenPomsIncludes) throws Exception {
        for (String childPomFile : childrenPomsIncludes) {
            IFile childPom = assemblyFile.getProject().getFile(childPomFile);
            if (childPom.exists()) {
                Model childModel = MODEL_MANAGER.readMavenModel(childPom);
                List<Plugin> childPlugins = new ArrayList<Plugin>(childModel.getBuild().getPlugins());
                Iterator<Plugin> childIterator = childPlugins.iterator();
                while (childIterator.hasNext()) {
                    Plugin p = childIterator.next();
                    if (p.getArtifactId().equals("maven-assembly-plugin")) { //$NON-NLS-1$
                        // must remove the assembly plugins for children poms, else will be some errors.
                        childIterator.remove();
                    } else if (p.getArtifactId().equals("maven-antrun-plugin")) { //$NON-NLS-1$
                        // because no assembly, so no need copy the scripts and rename it.
                        childIterator.remove();
                    }

                }

                childModel.getBuild().setPlugins(childPlugins);

                /*
                 * FIXME, Won't have assembly, maybe also move the profiles, because so far, the profiles are useful for
                 * assembly only. If later, the profiles will use by other tasks, should remove this codes.
                 */
                childModel.getProfiles().clear();

                PomUtil.savePom(monitor, childModel, childPom);
            }
        }
    }

    private Node getElement(Node parent, String elemName, int level) {
        NodeList childrenNodeList = parent.getChildNodes();
        for (int i = 0; i < childrenNodeList.getLength(); i++) {
            Node child = childrenNodeList.item(i);
            if (child != null && child.getNodeType() == Node.ELEMENT_NODE) {
                if (child.getNodeName().equals(elemName)) {
                    return child;
                }
            }
            if (level > 1) {
                Node element = getElement(child, elemName, --level);
                if (element != null) {
                    return element;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("nls")
    private void addAssemblyFiles(Node filesElem, String source, String outputDirectory, String destName, String fileMode,
            String lineEnding, boolean filtered, String comment) {
        Assert.isNotNull(filesElem);
        Assert.isNotNull(source);
        Assert.isNotNull(outputDirectory);

        Document doc = filesElem.getOwnerDocument();

        Element fileEle = doc.createElement("file");
        filesElem.appendChild(fileEle);

        if (comment != null) {
            fileEle.appendChild(doc.createComment(comment));
        }

        Element sourceElement = doc.createElement("source");
        sourceElement.setTextContent(source);
        fileEle.appendChild(sourceElement);

        Element outputDirectoryElement = doc.createElement("outputDirectory");
        outputDirectoryElement.setTextContent(outputDirectory);
        fileEle.appendChild(outputDirectoryElement);

        if (destName != null) { // if not set, will be same as source
            Element destNameElement = doc.createElement("destName");
            destNameElement.setTextContent(destName);
            fileEle.appendChild(destNameElement);
        }
        if (fileMode != null) {
            Element fileModeElement = doc.createElement("fileMode");
            fileModeElement.setTextContent(fileMode);
            fileEle.appendChild(fileModeElement);
        }
        if (lineEnding != null) {
            Element lineEndingElement = doc.createElement("lineEnding");
            lineEndingElement.setTextContent(lineEnding);
            fileEle.appendChild(lineEndingElement);
        }
        if (filtered) { // by default is false
            Element filteredElement = doc.createElement("filtered");
            filteredElement.setTextContent(Boolean.TRUE.toString());
            fileEle.appendChild(filteredElement);
        }
    }

    @SuppressWarnings("nls")
    private void addAssemblyFileSets(Node fileSetsNode, String directory, String outputDirectory, boolean useDefaultExcludes,
            List<String> includes, List<String> excludes, String fileMode, String directoryMode, String lineEnding,
            boolean filtered, String comment) {
        Assert.isNotNull(fileSetsNode);
        Assert.isNotNull(outputDirectory);
        Assert.isNotNull(directory);

        Document doc = fileSetsNode.getOwnerDocument();

        Element fileSetEle = doc.createElement("fileSet");
        fileSetsNode.appendChild(fileSetEle);

        if (comment != null) {
            fileSetEle.appendChild(doc.createComment(comment));
        }

        Element outputDirectoryElement = doc.createElement("outputDirectory");
        outputDirectoryElement.setTextContent(outputDirectory);
        fileSetEle.appendChild(outputDirectoryElement);

        Element directoryElement = doc.createElement("directory");
        directoryElement.setTextContent(directory);
        fileSetEle.appendChild(directoryElement);

        if (useDefaultExcludes) { // false by default
            Element useDefaultExcludesElement = doc.createElement("useDefaultExcludes");
            useDefaultExcludesElement.setTextContent(Boolean.TRUE.toString());
            fileSetEle.appendChild(useDefaultExcludesElement);
        }

        if (includes != null && !includes.isEmpty()) {

            Element includesEle = doc.createElement("includes");
            fileSetEle.appendChild(includesEle);
            for (String in : includes) {
                Element includeElement = doc.createElement("include");
                includeElement.setTextContent(in);
                includesEle.appendChild(includeElement);
            }
        }

        if (excludes != null && !excludes.isEmpty()) {
            Element excludesEle = doc.createElement("excludes");
            fileSetEle.appendChild(excludesEle);
            for (String ex : excludes) {
                Element excludeElement = doc.createElement("exclude");
                excludeElement.setTextContent(ex);
                excludesEle.appendChild(excludeElement);

            }
        }
        if (fileMode != null) {
            Element fileModeElement = doc.createElement("fileMode");
            fileModeElement.setTextContent(fileMode);
            fileSetEle.appendChild(fileModeElement);
        }
        if (directoryMode != null) {
            Element directoryModeElement = doc.createElement("directoryMode");
            directoryModeElement.setTextContent(directoryMode);
            fileSetEle.appendChild(directoryModeElement);
        }

        if (lineEnding != null) {
            Element lineEndingElement = doc.createElement("lineEnding");
            lineEndingElement.setTextContent(lineEnding);
            fileSetEle.appendChild(lineEndingElement);
        }
        if (filtered) { // by default is false
            Element filteredElement = doc.createElement("filtered");
            filteredElement.setTextContent(Boolean.TRUE.toString());
            fileSetEle.appendChild(filteredElement);
        }
    }
}
