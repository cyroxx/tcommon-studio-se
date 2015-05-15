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
package org.talend.librariesmanager.model.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.EMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.CommonExceptionHandler;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.io.FilesUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ILibraryManagerService;
import org.talend.core.ILibraryManagerUIService;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.components.IComponentsService;
import org.talend.core.model.general.ILibrariesService;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.runtime.maven.MavenArtifact;
import org.talend.core.runtime.maven.MavenUrlHelper;
import org.talend.designer.maven.deploy.ArtifactsDeployer;
import org.talend.designer.maven.utils.PomUtil;
import org.talend.librariesmanager.emf.librariesindex.LibrariesIndex;
import org.talend.librariesmanager.model.ExtensionModuleManager;
import org.talend.librariesmanager.model.ModulesNeededProvider;
import org.talend.librariesmanager.prefs.LibrariesManagerUtils;
import org.talend.osgi.hook.notification.JarMissingObservable;

/**
 * DOC ycbai class global comment. Detailled comment
 */
public class LocalLibraryManager implements ILibraryManagerService {

    private static Logger log = Logger.getLogger(LocalLibraryManager.class);

    private Set<String> jarList = new HashSet<String>();

    // map platform_uri/maven_uri to absolute path
    // key = null, means uri not tested yet....
    // value is null = jar not existingb
    // value set = absolute path of the jar
    private Map<String, String> studioJarInstalled = new HashMap<String, String>();

    // rules same as studioJarInstalled
    private Map<String, String> mavenJarInstalled = new HashMap<String, String>();

    private Set<String> jarsNeededForComponents = new HashSet<String>();

    private Set<String> urlWarned = new HashSet<String>();

    private JarMissingObservable missingJarObservable;

    boolean listToUpdate;

    /**
     * DOC nrousseau LocalLibraryManager constructor comment.
     */
    public LocalLibraryManager() {
        super();
        deployExtensionIndex();
    }

    @Override
    public boolean isInitialized() {
        File indexFile = new File(LibrariesIndexManager.getInstance().getStudioIndexPath());
        if (indexFile.exists()) {
            return LibrariesIndexManager.getInstance().getStudioLibIndex().isInitialized();
        }
        return false;
    }

    @Override
    public void setInitialized() {
        LibrariesIndexManager.getInstance().getStudioLibIndex().setInitialized(true);
        LibrariesIndexManager.getInstance().saveStudioIndexResource();
        LibrariesIndexManager.getInstance().getMavenLibIndex().setInitialized(true);
        LibrariesIndexManager.getInstance().saveMavenIndexResource();
    }

    @Override
    public void deploy(URI jarFileUri, IProgressMonitor... monitorWrap) {
        try {
            File file = new File(jarFileUri);
            if (file == null || !file.exists()) {
                return;
            }
            listToUpdate = true;
            Map<String, String> customUriToAdd = new HashMap<String, String>();
            if (file.isDirectory()) {
                List<File> jarFiles = FilesUtils.getJarFilesFromFolder(file, null);
                Map<String, String> sourceAndMavenUri = new HashMap<String, String>();
                if (!jarFiles.isEmpty()) {
                    for (File jarFile : jarFiles) {
                        String jarName = jarFile.getName();
                        String mavenUri = LibrariesIndexManager.getInstance().getMavenLibIndex().getJarsToRelativePath().map()
                                .get(jarName);
                        if (mavenUri == null) {
                            mavenUri = MavenUrlHelper.generateMvnUrlForJarName(jarName);
                            customUriToAdd.put(jarName, mavenUri);
                        }
                        sourceAndMavenUri.put(jarFile.getAbsolutePath(), mavenUri);
                    }
                    ArtifactsDeployer.getInstance().deployToLocalMaven(sourceAndMavenUri);
                }
            } else {
                String mavenUri = LibrariesIndexManager.getInstance().getMavenLibIndex().getJarsToRelativePath()
                        .get(file.getName());
                if (mavenUri == null) {
                    mavenUri = MavenUrlHelper.generateMvnUrlForJarName(file.getName());
                    customUriToAdd.put(file.getName(), mavenUri);
                }
                ArtifactsDeployer.getInstance().deployToLocalMaven(file.getAbsolutePath(), mavenUri);
            }
            // add custom uri map to the index
            if (!customUriToAdd.isEmpty()) {
                deployMavenIndex(customUriToAdd, monitorWrap);
            }
        } catch (IOException e) {
            CommonExceptionHandler.process(e);
        } catch (Exception e) {
            CommonExceptionHandler.process(e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.IRepositoryBundleService#deploy(java.util.Collection,
     * org.eclipse.core.runtime.IProgressMonitor[])
     */
    @Override
    public void deploy(Collection<URI> jarFileUris, IProgressMonitor... monitorWrap) {
        if (jarFileUris == null || jarFileUris.size() == 0) {
            return;
        }
        for (URI uri : jarFileUris) {
            deploy(uri, monitorWrap);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.IRepositoryBundleService#retrieve(java.lang.String, java.lang.String)
     */
    @Override
    public boolean retrieve(String jarNeeded, String pathToStore, IProgressMonitor... monitorWrap) {
        return retrieve(jarNeeded, pathToStore, true, monitorWrap);
    }

    private File getJarFile(String jarNeeded) throws MalformedURLException {
        String jarPath = getJarPath(jarNeeded);
        if (jarPath != null) {
            File file = new File(jarPath);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    @Override
    public boolean retrieve(String jarNeeded, String pathToStore, boolean popUp, IProgressMonitor... monitorWrap) {
        String sourcePath = null, targetPath = pathToStore;
        try {
            File jarFile = getJarFile(jarNeeded);
            if (jarFile == null) {
                if (popUp && !CommonsPlugin.isHeadless()) {
                    // popup dialog if needed to download the jar.
                    if (GlobalServiceRegister.getDefault().isServiceRegistered(ILibraryManagerUIService.class)) {
                        ILibraryManagerUIService libUiService = (ILibraryManagerUIService) GlobalServiceRegister.getDefault()
                                .getService(ILibraryManagerUIService.class);

                        libUiService.installModules(new String[] { jarNeeded });
                    }
                    jarFile = getJarFile(jarNeeded);
                    if (jarFile == null) {
                        // jar not found even after the popup > stop here
                        return false;
                    }
                    // jar found > reset the modules just after install the jars
                    if (GlobalServiceRegister.getDefault().isServiceRegistered(ILibrariesService.class)) {
                        ILibrariesService librariesService = (ILibrariesService) GlobalServiceRegister.getDefault().getService(
                                ILibrariesService.class);
                        librariesService.resetModulesNeeded();
                    }
                } else {
                    // jar not found, and no popup at this point
                    return false;
                }
            }
            File target = new File(StringUtils.trimToEmpty(pathToStore));
            if (!target.exists()) {
                target.mkdirs();
            }
            sourcePath = jarFile.getAbsolutePath();
            FilesUtils.copyFile(jarFile, new File(pathToStore, jarNeeded));
            return true;
        } catch (MalformedURLException e) {
            CommonExceptionHandler.process(e);
        } catch (IOException e) {
            CommonExceptionHandler.process(new Exception("Can not copy: " + sourcePath + " to :" + targetPath, e));
        }
        return false;

    }

    @Override
    public boolean retrieve(Collection<String> jarsNeeded, String pathToStore, boolean showDialog,
            IProgressMonitor... monitorWrap) {
        if (jarsNeeded == null || jarsNeeded.size() == 0) {
            return false;
        }
        List<String> jarNotFound = new ArrayList<String>();

        boolean allIsOK = true;
        for (String jar : jarsNeeded) {
            if (!retrieve(jar, pathToStore, false, monitorWrap)) {
                jarNotFound.add(jar);
                allIsOK = false;
            }
        }
        if (showDialog && !jarNotFound.isEmpty() && !CommonsPlugin.isHeadless()) {
            if (GlobalServiceRegister.getDefault().isServiceRegistered(ILibraryManagerUIService.class)) {
                ILibraryManagerUIService libUiService = (ILibraryManagerUIService) GlobalServiceRegister.getDefault().getService(
                        ILibraryManagerUIService.class);
                libUiService.installModules(jarNotFound.toArray(new String[jarNotFound.size()]));
                jarsNeeded = new ArrayList<String>(jarNotFound);
                jarNotFound.clear();
                allIsOK = true;
                boolean needResetModulesNeeded = false;
                for (String jar : jarsNeeded) {
                    if (!retrieve(jar, pathToStore, false, monitorWrap)) {
                        jarNotFound.add(jar);
                        allIsOK = false;
                    } else {
                        needResetModulesNeeded = true;
                    }
                }
                if (needResetModulesNeeded) {
                    if (GlobalServiceRegister.getDefault().isServiceRegistered(ILibrariesService.class)) {
                        ILibrariesService librariesService = (ILibrariesService) GlobalServiceRegister.getDefault().getService(
                                ILibrariesService.class);
                        librariesService.resetModulesNeeded();
                    }
                }
            }
        }

        return allIsOK;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.IRepositoryBundleService#retrieve(java.util.Collection, java.lang.String,
     * org.eclipse.core.runtime.IProgressMonitor[])
     */
    @Override
    public boolean retrieve(Collection<String> jarsNeeded, String pathToStore, IProgressMonitor... monitorWrap) {
        return retrieve(jarsNeeded, pathToStore, true, monitorWrap);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.IRepositoryBundleService#list(org.eclipse.core.runtime.IProgressMonitor[])
     */
    @Override
    public Set<String> list(IProgressMonitor... monitorWrap) {
        if (!jarList.isEmpty() && !listToUpdate) {
            return jarList;
        }
        listToUpdate = false;

        EMap<String, String> jarsToMavenUri = LibrariesIndexManager.getInstance().getMavenLibIndex().getJarsToRelativePath();
        for (String jarName : jarsToMavenUri.keySet()) {
            String artifactPath = PomUtil.getArtifactPath(MavenUrlHelper.parseMvnUrl(jarName));
            mavenJarInstalled.put(jarsToMavenUri.get(artifactPath), artifactPath);
            if (artifactPath != null) {
                jarList.add(jarName);
            }
        }

        EMap<String, String> jarsToRelative = LibrariesIndexManager.getInstance().getStudioLibIndex().getJarsToRelativePath();
        for (String jarName : jarsToRelative.keySet()) {
            String relativePath = jarsToRelative.get(jarName);
            boolean jarFound = false;
            if (relativePath != null && relativePath.startsWith("platform:/")) {
                jarFound = checkJarInstalledFromPlatform(relativePath);
            }
            if (jarFound) {
                jarList.add(jarName);
            }
        }

        return jarList;
    }

    @Override
    public Set<String> list(boolean withComponent, IProgressMonitor... monitorWrap) {
        return list(monitorWrap);
    }

    @Override
    public Set<String> listAllDllFiles() {
        Set<String> names = new HashSet<String>();
        try {
            List<File> dllFiles = FilesUtils.getDllFilesFromFolder(getStorageDirectory(), null);
            if (dllFiles.size() > 0) {
                for (File file : dllFiles) {
                    names.add(file.getName());
                }
            } else {

            }
        } catch (MalformedURLException e) {
            CommonExceptionHandler.process(e);
        }

        return names;
    }

    private File getStorageDirectory() {
        String librariesPath = LibrariesManagerUtils.getLibrariesPath(ECodeLanguage.JAVA);
        File storageDir = new File(librariesPath);
        return storageDir;
    }

    @Override
    public void clearCache() {
        if (isInitialized()) {
            LibrariesIndexManager.getInstance().clearAll();
        }
        listToUpdate = true;
    }

    @Override
    public boolean contains(String jarName) {
        return list().contains(jarName);
    }

    /*
     * (non-Jsdoc)
     * 
     * @see org.talend.core.ILibraryManagerService#delete(java.lang.String)
     */
    @Override
    public boolean delete(String jarName) {
        // only delete jar from lib/java, do not delete jars from original components providers.

        try {
            List<File> jarFiles = FilesUtils.getJarFilesFromFolder(getStorageDirectory(), null);
            if (jarFiles.size() > 0) {
                for (File file : jarFiles) {
                    if (file.getName().equals(jarName)) {
                        file.delete();
                        jarList.remove(jarName);
                        return true;
                    }
                }
            }
        } catch (MalformedURLException e) {
            CommonExceptionHandler.process(e);
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.ILibraryManagerService#deploy(java.util.Set, org.eclipse.core.runtime.IProgressMonitor[])
     */
    @Override
    public void deploy(Set<IComponent> componentList, IProgressMonitor... monitorWrap) {
        List<ModuleNeeded> modules = new ArrayList<ModuleNeeded>();

        for (IComponent component : componentList) {
            modules.addAll(component.getModulesNeeded());
        }
        deploy(modules, monitorWrap);
    }

    @Override
    public void deploy(List<ModuleNeeded> modules, IProgressMonitor... monitorWrap) {
        boolean modified = false;
        LibrariesIndex index = LibrariesIndexManager.getInstance().getStudioLibIndex();
        EMap<String, String> jarsToRelativePath = index.getJarsToRelativePath();
        for (ModuleNeeded module : modules) {
            String moduleLocation = module.getModuleLocaion();
            if (moduleLocation != null && moduleLocation.startsWith("platform:/")) {
                if (jarsToRelativePath.containsKey(module.getModuleName())) {
                    String relativePath = jarsToRelativePath.get(module.getModuleName());
                    if (relativePath.equals(moduleLocation)) {
                        continue;
                    } else {
                        if (!urlWarned.contains(moduleLocation)) {
                            System.out.println(module.getModuleName() + " was already defined with:" + relativePath
                                    + " but redefined now with:" + moduleLocation);
                            urlWarned.add(moduleLocation);
                        }
                    }
                }
                if (checkJarInstalledFromPlatform(moduleLocation)) {
                    jarsToRelativePath.put(module.getModuleName(), moduleLocation);
                    modified = true;
                }
            } else {
                jarsNeededForComponents.add(module.getModuleName());
            }
        }
        if (modified) {
            LibrariesIndexManager.getInstance().saveStudioIndexResource();
        }
    }

    @Override
    public String getJarPath(String jarName) {
        String libPath = null;

        if (jarName != null && jarName.startsWith(MavenUrlHelper.MVN_PROTOCOL)) {
            libPath = mavenJarInstalled.get(jarName);
            if (libPath != null) {
                return libPath;
            }
        }

        EMap<String, String> jarsToMavenUri = LibrariesIndexManager.getInstance().getMavenLibIndex().getJarsToRelativePath();
        String mavenUri = jarsToMavenUri.get(jarName);
        if (mavenUri != null && mavenUri.startsWith(MavenUrlHelper.MVN_PROTOCOL)) {
            libPath = mavenJarInstalled.get(mavenUri);
            if (libPath != null) {
                return libPath;
            }
        }

        EMap<String, String> jarsToRelative = LibrariesIndexManager.getInstance().getStudioLibIndex().getJarsToRelativePath();
        String relativePath = jarsToRelative.get(jarName);
        if (relativePath != null && relativePath.startsWith("platform:/")) { //$NON-NLS-1$
            boolean jarFound = checkJarInstalledFromPlatform(relativePath);
            if (jarFound) {
                libPath = studioJarInstalled.get(relativePath);
            }
        }

        return libPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.ILibraryManagerService#checkJarInstalledFromMaven(java.lang.String)
     */

    @Override
    public boolean checkJarInstalledFromPlatform(String uriPath) {
        if (studioJarInstalled.containsKey(uriPath)) {
            return studioJarInstalled.get(uriPath) != null;
        }
        if (uriPath.startsWith(MavenUrlHelper.MVN_PROTOCOL)) {
            return false;
        }
        String absolutePath = null;
        boolean jarFound = false;

        // prevent the missing jar manager to issu notifications is the lib is not found
        JarMissingObservable missingJarObservableService = getMissingJarObservableService();
        if (missingJarObservableService != null) {
            missingJarObservableService.prenventNotificationLock.lock();
        }
        try {
            try {
                if (uriPath.startsWith(ExtensionModuleManager.URIPATH_PREFIX)) {
                    String plugin = uriPath.substring(17);
                    plugin = plugin.substring(0, plugin.indexOf("/"));
                    String path = uriPath.substring(17 + plugin.length());
                    URL url = FileLocator.find(Platform.getBundle(plugin), new Path(path), null);
                    if (url != null) {
                        URL url2 = FileLocator.toFileURL(url);
                        File file = new File(url2.getFile());
                        if (file.exists()) {
                            jarFound = true;
                            absolutePath = file.getAbsolutePath();
                        }
                    }
                }
            } catch (Exception e) {
                // do nothing
            }

            if (!jarFound) {
                try {
                    URI uri = new URI(uriPath);
                    URL url = FileLocator.toFileURL(uri.toURL());
                    File file = new File(url.getFile());
                    if (file.exists()) {
                        jarFound = true;
                        absolutePath = file.getAbsolutePath();
                    }
                } catch (Exception e) {
                    // do nothing
                }
            }
            studioJarInstalled.put(uriPath, absolutePath);
        } finally {
            if (missingJarObservableService != null) {
                missingJarObservableService.prenventNotificationLock.unlock();
            }
        }
        return jarFound;
    }

    private void deployExtensionIndex() {
        Map<String, String> libsToRelativePath = new HashMap<String, String>();
        Map<String, String> libsToMavenUri = new HashMap<String, String>();
        Set<String> duplicateLocationJar = new HashSet<String>();
        Set<String> duplicateMavenUri = new HashSet<String>();
        List<ModuleNeeded> modulesNeededForApplication = ModulesNeededProvider.getModulesNeededForApplication();
        deploy(modulesNeededForApplication, null, libsToRelativePath, duplicateLocationJar, libsToMavenUri, duplicateMavenUri);
        if (!duplicateLocationJar.isEmpty()) {
            warnDuplicated(modulesNeededForApplication, duplicateLocationJar, "Library:");
        }
        if (!duplicateMavenUri.isEmpty()) {
            warnDuplicated(modulesNeededForApplication, duplicateMavenUri, "Maven Uri:");
        }
        listToUpdate = true;
    }

    @Override
    public void deployComponentsLibs(IProgressMonitor... monitorWrap) {
        // jars needed in component but didn't set platfrom uri
        Set<String> libsWithoutUri = new HashSet<String>();
        Map<String, String> libsToRelativePath = new HashMap<String, String>();
        Map<String, String> libsToMavenUri = new HashMap<String, String>();
        List<ModuleNeeded> modules = new ArrayList<ModuleNeeded>();
        Set<String> duplicateLocationJar = new HashSet<String>();
        Set<String> duplicateMavenUri = new HashSet<String>();

        IComponentsService service = (IComponentsService) GlobalServiceRegister.getDefault().getService(IComponentsService.class);

        for (IComponent component : service.getComponentsFactory().getComponents()) {
            modules.addAll(component.getModulesNeeded());
        }
        deploy(modules, libsWithoutUri, libsToRelativePath, duplicateLocationJar, libsToMavenUri, duplicateMavenUri, monitorWrap);
        if (!duplicateLocationJar.isEmpty()) {
            warnDuplicated(modules, duplicateLocationJar, "Library:");
        }
        if (!duplicateMavenUri.isEmpty()) {
            warnDuplicated(modules, duplicateMavenUri, "Maven Uri:");
        }
        deployLibForComponentProviders(service, libsWithoutUri, libsToRelativePath, libsToMavenUri);

        deploy(libsToRelativePath, monitorWrap);
        deployMavenIndex(libsToMavenUri, monitorWrap);
    }

    private void deployLibForComponentProviders(IComponentsService service, Set<String> libsWithoutUri,
            Map<String, String> libsToRelativePath, Map<String, String> libsToMavenUri) {
        Set<String> needToDeploy = new HashSet<String>();
        Map<String, File> componentsFolders = service.getComponentsFactory().getComponentsProvidersFolder();
        Set<String> contributeIdSet = componentsFolders.keySet();
        for (String contributeID : contributeIdSet) {
            try {
                File file = new File(componentsFolders.get(contributeID).toURI());
                if ("org.talend.designer.components.model.UserComponentsProvider".contains(contributeID)
                        || "org.talend.designer.components.exchange.ExchangeComponentsProvider".contains(contributeID)) {
                    if (file.isDirectory()) {
                        List<File> jarFiles = FilesUtils.getJarFilesFromFolder(file, null);
                        if (jarFiles.size() > 0) {
                            for (File jarFile : jarFiles) {
                                String name = jarFile.getName();
                                if (!libsWithoutUri.contains(name)) {
                                    continue;
                                }
                                needToDeploy.add(jarFile.getAbsolutePath());
                            }
                        }
                    } else {
                        if (!libsWithoutUri.contains(file.getName())) {
                            continue;
                        }
                        needToDeploy.add(file.getAbsolutePath());
                    }
                } else {
                    List<File> jarFiles = FilesUtils.getJarFilesFromFolder(file, null);
                    if (jarFiles.size() > 0) {
                        for (File jarFile : jarFiles) {
                            String name = jarFile.getName();
                            if (!libsWithoutUri.contains(name)) {
                                continue;
                            }
                            String path = libsToRelativePath.get(name);
                            int lengthBasePath = new Path(file.getParentFile().getAbsolutePath()).toPortableString().length();
                            String relativePath = new Path(jarFile.getAbsolutePath()).toPortableString()
                                    .substring(lengthBasePath);
                            String moduleLocation = "platform:/plugin/" + contributeID + relativePath;
                            if (path != null) {
                                if (path.equals(moduleLocation)) {
                                    continue;
                                } else {
                                    CommonExceptionHandler.warn(name + " is duplicated, locations:" + path + " and:"
                                            + moduleLocation);
                                    continue;
                                }
                            }
                            libsToRelativePath.put(name, moduleLocation);
                        }
                    }
                }
            } catch (Exception e) {
                ExceptionHandler.process(e);
                continue;
            }
        }

        // deploy needed jars for User and Exchange component providers
        Map<String, String> sourceAndMavenUri = new HashMap<String, String>();
        Map<String, String> customUriToAdd = new HashMap<String, String>();
        Map<String, String> libsToMavenUriAll = new HashMap<String, String>();
        libsToMavenUriAll.putAll(libsToMavenUri);
        libsToMavenUriAll.putAll(LibrariesIndexManager.getInstance().getMavenLibIndex().getJarsToRelativePath().map());
        if (!needToDeploy.isEmpty()) {
            for (String file : needToDeploy) {
                String jarName = new File(file).getName();
                String mavenUri = libsToMavenUriAll.get(jarName);
                if (mavenUri == null) {
                    mavenUri = MavenUrlHelper.generateMvnUrlForJarName(jarName);
                    customUriToAdd.put(jarName, mavenUri);
                }
                sourceAndMavenUri.put(file, mavenUri);
            }
            try {
                ArtifactsDeployer.getInstance().deployToLocalMaven(sourceAndMavenUri);
                // add custom uri map to the index
                if (!customUriToAdd.isEmpty()) {
                    deployMavenIndex(customUriToAdd);
                }
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }
        }
    }

    private void warnDuplicated(List<ModuleNeeded> modules, Set<String> duplicates, String type) {
        for (String lib : duplicates) {
            Set<String> components = new HashSet<String>();
            Set<String> locations = new HashSet<String>();
            for (ModuleNeeded module : modules) {
                if (lib != null && lib.equals(module.getModuleName())) {
                    components.add(module.getContext());
                    locations.add(module.getModuleLocaion());
                }
            }
            CommonExceptionHandler.warn("Library:" + lib + " was defined with different locations.\n"
                    + "Components who define these jars are:" + components + "\n" + "Locations:" + locations);
        }
    }

    private void deploy(List<ModuleNeeded> modules, Set<String> libsWithoutUri, Map<String, String> libsToRelativePath,
            Set<String> duplicateLocationJar, Map<String, String> libsToMavenUri, Set<String> duplicateMavenUri,
            IProgressMonitor... monitorWrap) {
        Map<String, String> libsToMavenUriAll = new HashMap<String, String>();
        libsToMavenUriAll.putAll(libsToMavenUri);
        libsToMavenUriAll.putAll(LibrariesIndexManager.getInstance().getMavenLibIndex().getJarsToRelativePath().map());

        Map<String, String> libsToRelativeAll = new HashMap<String, String>();
        libsToRelativeAll.putAll(libsToRelativePath);
        libsToRelativeAll.putAll(LibrariesIndexManager.getInstance().getStudioLibIndex().getJarsToRelativePath().map());

        for (ModuleNeeded module : modules) {
            String moduleLocation = module.getModuleLocaion();
            String mavenUrl = module.getMavenUri();
            if (mavenUrl != null && mavenUrl.startsWith(MavenUrlHelper.MVN_PROTOCOL)) {
                String existUri = libsToMavenUriAll.get(module.getModuleName());
                if (existUri != null && !existUri.equals(mavenUrl)) {
                    duplicateMavenUri.add(module.getModuleName());
                    // TODO mabye need modify latter, currently if the same are configured with diffirent version ,
                    // only keep the latest version compare the current studio version .
                    MavenArtifact existArtifact = MavenUrlHelper.parseMvnUrl(existUri);
                    MavenArtifact mvnArtifact = MavenUrlHelper.parseMvnUrl(mavenUrl);
                    if (existArtifact != null && mvnArtifact != null) {
                        String existVStr = existArtifact.getVersion();
                        String vStr = mvnArtifact.getVersion();
                        if (existVStr != null && vStr != null) {
                            Version existVersion = new Version(existVStr);
                            Version version = new Version(vStr);
                            if (version.compareTo(existVersion) > 0) {
                                libsToMavenUri.put(module.getModuleName(), mavenUrl);
                                libsToMavenUriAll.put(module.getModuleName(), mavenUrl);
                            }
                        }
                    }
                } else {
                    libsToMavenUri.put(module.getModuleName(), existUri);
                    libsToMavenUriAll.put(module.getModuleName(), existUri);
                }
            }
            if (moduleLocation != null && moduleLocation.startsWith("platform:/")) {
                String relativePath = libsToRelativeAll.get(module.getModuleName());
                if (relativePath != null && !relativePath.equals(moduleLocation)) {
                    if (!duplicateLocationJar.contains(moduleLocation)) {
                        duplicateLocationJar.add(module.getModuleName());
                    }
                }
                libsToRelativePath.put(module.getModuleName(), moduleLocation);
                libsToRelativeAll.put(module.getModuleName(), moduleLocation);
            } else {
                if (libsWithoutUri != null) {
                    libsWithoutUri.add(module.getModuleName());
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.ILibraryManagerService#deploy(java.util.Map, org.eclipse.core.runtime.IProgressMonitor[])
     */
    @Override
    public void deploy(Map<String, String> libsToRelativePath, IProgressMonitor... monitorWrap) {
        EMap<String, String> jarsToRelative = LibrariesIndexManager.getInstance().getStudioLibIndex().getJarsToRelativePath();
        for (String key : libsToRelativePath.keySet()) {
            if (checkJarInstalledFromPlatform(libsToRelativePath.get(key))) {
                jarsToRelative.put(key, libsToRelativePath.get(key));
            }
        }
        LibrariesIndexManager.getInstance().saveStudioIndexResource();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.ILibraryManagerService#deployMavenIndex(java.util.Map,
     * org.eclipse.core.runtime.IProgressMonitor[])
     */
    @Override
    public void deployMavenIndex(Map<String, String> libsToMavenUri, IProgressMonitor... monitorWrap) {
        EMap<String, String> jarsToMavenuri = LibrariesIndexManager.getInstance().getMavenLibIndex().getJarsToRelativePath();
        jarsToMavenuri.putAll(libsToMavenUri);
        LibrariesIndexManager.getInstance().saveMavenIndexResource();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.ILibraryManagerService#forceListUpdate()
     */
    @Override
    public void forceListUpdate() {
        listToUpdate = true;
    }

    JarMissingObservable getMissingJarObservableService() {
        if (missingJarObservable == null) {
            BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
            if (bundleContext != null) {
                ServiceReference<?> serviceReference = bundleContext.getServiceReference(JarMissingObservable.class
                        .getCanonicalName());
                if (serviceReference != null) {
                    missingJarObservable = (JarMissingObservable) bundleContext.getService(serviceReference);
                } else {// could not find the hook registry service so log it
                    log.error("Could not find a registered OSGI service for : " + JarMissingObservable.class);
                }
            } else {// bundleContext is null should never happend but log it
                log.error("Could not get bundle context for : " + this.getClass());
            }
        }
        return missingJarObservable;
    }

}
