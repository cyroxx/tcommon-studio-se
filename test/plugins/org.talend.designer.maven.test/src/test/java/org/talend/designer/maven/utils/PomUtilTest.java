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

import org.apache.maven.model.Dependency;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.junit.Assert;
import org.junit.Test;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.model.general.Project;
import org.talend.repository.ProjectManager;
import org.talend.utils.io.FilesUtils;

/**
 * DOC ggu class global comment. Detailled comment
 */
public class PomUtilTest {

    @Test
    public void testGetPomFileName() {
        String pomFileName = PomUtil.getPomFileName(null);
        Assert.assertEquals("pom.xml", pomFileName);

        pomFileName = PomUtil.getPomFileName("");
        Assert.assertEquals("pom.xml", pomFileName);

        pomFileName = PomUtil.getPomFileName("ABC");
        Assert.assertEquals("pom_ABC.xml", pomFileName);
    }

    @Test
    public void testGetAssemblyFileName() {
        String assemblyFileName = PomUtil.getAssemblyFileName(null);
        Assert.assertEquals("assembly.xml", assemblyFileName);

        assemblyFileName = PomUtil.getAssemblyFileName("");
        Assert.assertEquals("assembly.xml", assemblyFileName);

        assemblyFileName = PomUtil.getAssemblyFileName("ABC");
        Assert.assertEquals("assembly_ABC.xml", assemblyFileName);
    }

    @Test
    public void testCreateModuleDependency() {
        Dependency moduleDependency = PomUtil.createDependency(null, null, null, null);
        Assert.assertNull(moduleDependency);

        moduleDependency = PomUtil.createDependency(null, "test", null, null);
        Assert.assertNotNull(moduleDependency);
        Assert.assertEquals("org.talend.libraries", moduleDependency.getGroupId());
        Assert.assertEquals("test", moduleDependency.getArtifactId());
        Assert.assertEquals("1.0.0", moduleDependency.getVersion());
        Assert.assertEquals("jar", moduleDependency.getType());

        moduleDependency = PomUtil.createDependency("org.talend.job", "test", "0.1", "zip");
        Assert.assertNotNull(moduleDependency);
        Assert.assertEquals("org.talend.job", moduleDependency.getGroupId());
        Assert.assertEquals("test", moduleDependency.getArtifactId());
        Assert.assertEquals("0.1", moduleDependency.getVersion());
        Assert.assertEquals("zip", moduleDependency.getType());
    }

    @Test
    public void testGetTemplateFileNull() throws Exception {
        File templateFile = PomUtil.getTemplateFile(null, null, null);
        Assert.assertNull(templateFile);

        templateFile = PomUtil.getTemplateFile(null, new Path(""), null);
        Assert.assertNull(templateFile);

        templateFile = PomUtil.getTemplateFile(null, new Path(""), "");
        Assert.assertNull(templateFile);

    }

    @Test
    public void testGetTemplateFileNonExisted() throws Exception {
        Project currentProject = ProjectManager.getInstance().getCurrentProject();
        Assert.assertNotNull(currentProject);
        IProject project = ResourceUtils.getProject(currentProject.getTechnicalLabel());
        IFolder tempFolder = project.getFolder("temp");
        if (!tempFolder.exists()) {
            tempFolder.create(true, true, null);
        }
        IFolder baseFolder = tempFolder.getFolder("TemplateBaseFolder");
        if (!baseFolder.exists()) {
            baseFolder.create(true, true, null);
        }
        File baseFile = baseFolder.getLocation().toFile();
        if (baseFile.exists()) { // clean folder
            FilesUtils.deleteFolder(baseFile, false);
        }
        File template1File = new File(baseFile, "template1.txt");
        File folder1 = new File(baseFile, "folder1");
        File subfolder1 = new File(baseFile, "folder1/subfolder1");
        File template2File = new File(folder1, "template2.txt");
        File subTemplate2File = new File(subfolder1, "template2.txt");

        File foundFile = PomUtil.getTemplateFile(baseFolder, null, "template1.txt"); // current
        Assert.assertNull(foundFile); // not existed the template1

        foundFile = PomUtil.getTemplateFile(baseFolder, new Path("folder1/subfolder1"), "template1.txt");
        Assert.assertNull(foundFile); // not existed the template1

        foundFile = PomUtil.getTemplateFile(baseFolder, new Path("folder1"), "template2.txt");
        Assert.assertNull(foundFile); // not existed the template2

        foundFile = PomUtil.getTemplateFile(baseFolder, new Path("folder1/subfolder1"), "template2.txt");
        Assert.assertNull(foundFile); // not existed the template2

        subfolder1.mkdirs();
        template1File.createNewFile();
        template2File.createNewFile();
        subTemplate2File.createNewFile();

        Assert.assertTrue(template1File.exists());
        Assert.assertTrue(template2File.exists());
        Assert.assertTrue(subfolder1.exists());

        foundFile = PomUtil.getTemplateFile(baseFolder, null, "template1.txt"); // from current base folder directly
        Assert.assertNotNull(foundFile);
        Assert.assertEquals(template1File, foundFile);

        foundFile = PomUtil.getTemplateFile(baseFolder, null, "template2.txt"); // from current base folder directly
        Assert.assertNull(foundFile);

        foundFile = PomUtil.getTemplateFile(baseFolder, new Path("folder1"), "template2.txt");
        Assert.assertNotNull(foundFile); // first template2
        Assert.assertEquals(template2File, foundFile);

        foundFile = PomUtil.getTemplateFile(baseFolder, new Path("folder1/subfolder1"), "template2.txt");
        Assert.assertNotNull(foundFile); // sub template2
        Assert.assertEquals(subTemplate2File, foundFile);

        foundFile = PomUtil.getTemplateFile(baseFolder, new Path("folder1/subfolder1"), "template1.txt");
        Assert.assertNotNull(foundFile); // find from parent foler until base folder
        Assert.assertEquals(template1File, foundFile);

        foundFile = PomUtil.getTemplateFile(baseFolder, new Path("folder1/subfolder1"), "template3.txt");
        Assert.assertNull(foundFile); // base folder don't contain template3

        File template3File = new File(baseFile.getParentFile(), "template3.txt");
        template3File.createNewFile();
        Assert.assertTrue(template3File.exists());

        foundFile = PomUtil.getTemplateFile(baseFolder, new Path("folder1/subfolder1"), "template3.txt");
        Assert.assertNull(foundFile); // base folder don't contain template3

        foundFile = PomUtil.getTemplateFile(tempFolder, new Path("folder1/subfolder1"), "template3.txt");
        Assert.assertNull(foundFile); // path not contained the template3

        foundFile = PomUtil.getTemplateFile(tempFolder, new Path("TemplateBaseFolder/folder1/subfolder1"), "template3.txt");
        Assert.assertNotNull(foundFile);
        Assert.assertEquals(template3File, foundFile);

        // clean test files
        template3File.delete();
        FilesUtils.deleteFolder(baseFile, true);

    }

}
