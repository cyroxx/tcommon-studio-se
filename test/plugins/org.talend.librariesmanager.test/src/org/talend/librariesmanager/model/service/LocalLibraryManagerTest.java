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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.EMap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.talend.commons.exception.CommonExceptionHandler;
import org.talend.commons.utils.io.FilesUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.database.conn.version.EDatabaseVersion4Drivers;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.components.IComponentsService;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.general.ModuleNeeded.ELibraryInstallStatus;
import org.talend.librariesmanager.emf.librariesindex.LibrariesIndex;
import org.talend.librariesmanager.model.ModulesNeededProvider;
import org.talend.librariesmanager.prefs.LibrariesManagerUtils;

/**
 * DOC hwang class global comment. Detailled comment
 */
public class LocalLibraryManagerTest {

    private static final String JAR_INDEX = "/index.xml"; //$NON-NLS-1$

    private final Set<String> jarList = new HashSet<String>();

    private final LocalLibraryManager localLibMana = new LocalLibraryManager();

    private List<String> notDilivers = new ArrayList<String>();

    /**
     * DOC Administrator Comment method "setUp".
     *
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        notDilivers.add("nzjdbc.jar");
        notDilivers.add("sas.svc.connection.jar");
        notDilivers.add("sas.core.jar");
        notDilivers.add("sas.intrnet.javatools.jar");
        notDilivers.add("sas.security.sspi.jar");
        notDilivers.add("db2jcc.jar");
        notDilivers.add("interclient.jar");
        notDilivers.add("db2jcc_license_cisuz.jar");
        notDilivers.add("ifxjdbc.jar");
        notDilivers.add("jconn3.jar");
        notDilivers.add("sapdbc.jar");
        notDilivers.add("db2jcc_license_cu.jar");
    }

    /**
     * Test method for
     * {@link org.talend.librariesmanager.model.service.LocalLibraryManagerTest#deploy(java.net.URI, org.eclipse.core.runtime.IProgressMonitor[])}
     * .
     *
     * @throws IOException
     */
    @Test
    public void testDeployURIIProgressMonitorArray() throws IOException {
        String librariesPath = LibrariesManagerUtils.getLibrariesPath(ECodeLanguage.JAVA);
        File storageDir = new File(librariesPath);
        String installLocation = storageDir.getAbsolutePath();
        IComponentsService service = (IComponentsService) GlobalServiceRegister.getDefault().getService(IComponentsService.class);
        Map<String, File> componentsFolders = service.getComponentsFactory().getComponentsProvidersFolder();
        Set<String> contributeIdSet = componentsFolders.keySet();
        String jarFileUri = new Path(ResourcesPlugin.getWorkspace().getRoot().getLocationURI().getPath()).removeLastSegments(1)
                .toOSString() + File.separator + "temp" + File.separator + "classpath.jar";
        File file = new File(jarFileUri);
        String contributeID = "";
        for (String contributor : contributeIdSet) {
            if (file.getAbsolutePath().contains(contributor)) {
                contributeID = contributor;
                break;
            }
        }
        if (file == null || !file.exists()) {
            return;
        }
        if (contributeID.equals("")) {
            if (file.isDirectory()) {
                FilesUtils.copyFolder(new File(jarFileUri), storageDir, false, FilesUtils.getExcludeSystemFilesFilter(),
                        FilesUtils.getAcceptJARFilesFilter(), false, new NullProgressMonitor());
            } else {
                File target = new File(storageDir.getAbsolutePath(), file.getName());
                FilesUtils.copyFile(file, target);
            }
        } else {
            if ("org.talend.designer.components.model.UserComponentsProvider".contains(contributeID)
                    || "org.talend.designer.components.exchange.ExchangeComponentsProvider".contains(contributeID)) {
                if (file.isDirectory()) {
                    FilesUtils.copyFolder(new File(jarFileUri), storageDir, false, FilesUtils.getExcludeSystemFilesFilter(),
                            FilesUtils.getAcceptJARFilesFilter(), false, new NullProgressMonitor());
                } else {
                    File target = new File(storageDir.getAbsolutePath(), file.getName());
                    FilesUtils.copyFile(file, target);
                }
            } else {
                LibrariesIndex index = LibrariesIndexManager.getInstance().getStudioLibIndex();
                EMap<String, String> jarsToRelativePath = index.getJarsToRelativePath();
                List<File> jarFiles = FilesUtils.getJarFilesFromFolder(file, null);
                boolean modified = false;
                if (jarFiles.size() > 0) {
                    for (File jarFile : jarFiles) {
                        String name = jarFile.getName();
                        String fullPath = jarFile.getAbsolutePath();
                        // caculate the relative path
                        if (fullPath.indexOf(contributeID) != -1) {
                            fullPath = new Path(fullPath).toPortableString();
                            String relativePath = fullPath.substring(fullPath.indexOf(contributeID));
                            if (!jarsToRelativePath.keySet().contains(name)) {
                                jarsToRelativePath.put(name, relativePath);
                                modified = true;
                            }
                        }
                    }
                    if (modified) {
                        LibrariesIndexManager.getInstance().saveStudioIndexResource();
                    }
                }
            }
        }
    }

    /**
     * Test method for
     * {@link org.talend.librariesmanager.model.service.LocalLibraryManagerTest#retrieve(java.lang.String, java.lang.String, org.eclipse.core.runtime.IProgressMonitor[])}
     * .
     *
     * @throws IOException
     */
    @Test
    public void testRetrieveStringStringIProgressMonitorArray() throws IOException {
        String pathToStore = LibrariesManagerUtils.getLibrariesPath(ECodeLanguage.JAVA);
        String jarNeeded = "mysql-connector-java-5.1.0-bin.jar";
        String sourcePath = null, targetPath = pathToStore;
        List<File> jarFiles = FilesUtils.getJarFilesFromFolder(getStorageDirectory(), jarNeeded);
        if (jarFiles.size() > 0) {
            File jarFile = jarFiles.get(0);
            File target = new File(StringUtils.trimToEmpty(pathToStore));
            if (!target.exists()) {
                target.mkdirs();
            }
            sourcePath = jarFile.getAbsolutePath();
            FilesUtils.copyFile(jarFile, new File(pathToStore, jarFile.getName()));
            return;
        }
        // retrieve jar from the index.xml if not find in lib/java
        else {
            EMap<String, String> jarsToRelative = LibrariesIndexManager.getInstance().getStudioLibIndex().getJarsToRelativePath();
            String relativePath = jarsToRelative.get(jarNeeded);
            if (relativePath == null) {
                return;
            }
            String bundleLocation = "";
            String jarLocation = "";
            IComponentsService service = (IComponentsService) GlobalServiceRegister.getDefault().getService(
                    IComponentsService.class);
            Map<String, File> componentsFolders = service.getComponentsFactory().getComponentsProvidersFolder();
            Set<String> contributeIdSet = componentsFolders.keySet();
            boolean jarFound = false;
            for (String contributor : contributeIdSet) {
                if (relativePath.contains(contributor)) {
                    // caculate the the absolute path of the jar
                    bundleLocation = componentsFolders.get(contributor).getAbsolutePath();
                    int index = bundleLocation.indexOf(contributor);
                    jarLocation = new Path(bundleLocation.substring(0, index)).append(relativePath).toPortableString();
                    jarFound = true;
                    break;
                }
            }
            sourcePath = jarLocation;
            if (!jarFound) {
                CommonExceptionHandler.process(new Exception("Jar: " + jarNeeded + " not found, not in the plugins available:"
                        + contributeIdSet));
                return;
            }
            FilesUtils.copyFile(new File(jarLocation), new File(pathToStore, jarNeeded));
            return;
        }

    }

    /**
     * Test method for
     * {@link org.talend.librariesmanager.model.service.LocalLibraryManagerTest#list(org.eclipse.core.runtime.IProgressMonitor[])}
     * .
     */
    @Test
    public void testList() throws MalformedURLException {
        Set<String> names = new HashSet<String>();
        List<File> jarFiles = FilesUtils.getJarFilesFromFolder(getStorageDirectory(), null);
        if (jarFiles.size() > 0) {
            for (File file : jarFiles) {
                names.add(file.getName());
            }
        }

        EMap<String, String> jarsToRelative = LibrariesIndexManager.getInstance().getStudioLibIndex().getJarsToRelativePath();
        names.addAll(jarsToRelative.keySet());

        assertTrue(names.size() > 0);

    }

    /**
     * Test method for studio have all the lib for the system of db connection .
     */
    @Test
    @Ignore
    public void testMissingJar() throws MalformedURLException {
        Set<String> names = new HashSet<String>();
        List<File> jarFiles = FilesUtils.getJarFilesFromFolder(getStorageDirectory(), null);
        if (jarFiles.size() > 0) {
            for (File file : jarFiles) {
                names.add(file.getName());
            }
        }
        EMap<String, String> jarsToRelative = LibrariesIndexManager.getInstance().getStudioLibIndex().getJarsToRelativePath();
        names.addAll(jarsToRelative.keySet());

        List<String> allJars = new ArrayList<String>();
        EDatabaseVersion4Drivers[] values = EDatabaseVersion4Drivers.values();
        for (EDatabaseVersion4Drivers driver : values) {
            Set<String> providerDrivers = driver.getProviderDrivers();
            allJars.addAll(providerDrivers);
        }

        Set<String> missJars = new HashSet<String>();
        for (String jar : allJars) {
            boolean hadInstalled = false;
            for (String installJar : names) {
                if (jar.equals(installJar)) {
                    hadInstalled = true;
                }
            }
            if (!hadInstalled) {
                missJars.add(jar);
            }
        }

        if (missJars.size() > 0) {
            for (String notDiliver : notDilivers) {
                if (missJars.contains(notDiliver)) {
                    missJars.remove(notDiliver);
                }
            }
        }
        if (missJars.size() > 0) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("db system missing jars! \n");
            for (String missJar : missJars) {
                buffer.append(missJar + "\n");
            }
            throw new RuntimeException(buffer.toString());
        }
        assertTrue(missJars.size() == 0);
    }

    @Test
    public void testUnusedJars() throws URISyntaxException {

        Bundle currentBundle = Platform.getBundle("org.talend.librariesmanager"); //$NON-NLS-1$
        Bundle[] bundles = currentBundle.getBundleContext().getBundles();
        Long totalSize = 0L;
        StringBuffer strBuff = new StringBuffer();
        List<String> neededJars = new ArrayList<String>();
        ModulesNeededProvider.reset();
        for (ModuleNeeded module : ModulesNeededProvider.getModulesNeeded()) {
            if (module.getStatus() != ELibraryInstallStatus.UNUSED) {
                neededJars.add(module.getModuleName());
            }
        }
        for (Bundle bundle : bundles) {
            String name = bundle.getSymbolicName();
            if (name.startsWith("org.talend.libraries")) {
                String classpath = bundle.getHeaders().get("Bundle-ClassPath");
                List<URL> urls = FilesUtils.getFilesFromFolder(bundle, "/lib", ".jar", true, true);
                for (URL url : urls) {
                    String jarFile = new Path(url.getFile()).lastSegment();
                    if (!neededJars.contains(jarFile) && (classpath == null || !classpath.contains(jarFile))) {
                        File file = new File(url.toURI());
                        Long fileSize = file.length();
                        String path = url.getPath().substring(url.getPath().indexOf("/plugins/") + 9);
                        strBuff.append(path + " : " + fileSize + "\n");
                        totalSize += fileSize;
                    }

                }
            }
            if (name.contains(".components.")) {
                List<URL> urls = FilesUtils.getFilesFromFolder(bundle, "/components", ".jar", true, true);
                for (URL url : urls) {
                    String jarFile = new Path(url.getFile()).lastSegment();
                    if (!neededJars.contains(jarFile)) {
                        File file = new File(url.toURI());
                        Long fileSize = file.length();
                        String path = url.getPath().substring(url.getPath().indexOf("/plugins/") + 9);
                        strBuff.append(path + " : " + fileSize + "\n");
                        totalSize += fileSize;
                    }
                }
            }
        }

        if (strBuff.length() != 0) {
            fail("Unused jars still in product (total byte size: " + totalSize + "):\n" + strBuff);
        }
    }

    /**
     * Test method for
     * {@link org.talend.librariesmanager.model.service.LocalLibraryManagerTest#delete(java.lang.String)}.
     */
    @Test
    public void testDelete() throws MalformedURLException {
        String jarName = "classpath.jar";
        List<File> jarFiles = FilesUtils.getJarFilesFromFolder(getStorageDirectory(), null);
        if (jarFiles.size() > 0) {
            for (File file : jarFiles) {
                if (file.getName().equals(jarName)) {
                    file.delete();
                    jarList.remove(jarName);
                }
            }
        }
    }

    private File getStorageDirectory() {
        String librariesPath = LibrariesManagerUtils.getLibrariesPath(ECodeLanguage.JAVA);
        File storageDir = new File(librariesPath);
        return storageDir;
    }

}
