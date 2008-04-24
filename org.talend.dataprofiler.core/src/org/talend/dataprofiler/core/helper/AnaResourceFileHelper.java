// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.helper;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.manager.DQStructureManager;
import org.talend.dataprofiler.core.ui.wizard.report.provider.AnalysisEntity;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.analysis.util.AnalysisSwitch;
import org.talend.dq.analysis.AnalysisWriter;
import org.talend.utils.sugars.ReturnCode;

/**
 * DOC rli class global comment. Detailled comment
 */
public final class AnaResourceFileHelper extends ResourceFileMap {

    private static Logger log = Logger.getLogger(AnaResourceFileHelper.class);

    private static AnaResourceFileHelper instance;

    private Map<IFile, AnalysisEntity> allAnalysisMap = new HashMap<IFile, AnalysisEntity>();

    private AnaResourceFileHelper() {
        super();
    }

    public static AnaResourceFileHelper getInstance() {
        if (instance == null) {
            instance = new AnaResourceFileHelper();
        }
        return instance;
    }

    public Collection<AnalysisEntity> getAllAnalysis() {
        if (resourceChanged) {
            allAnalysisMap.clear();
            IFolder defaultAnalysFolder = ResourcesPlugin.getWorkspace().getRoot().getProject(
                    PluginConstant.DATA_PROFILING_PROJECTNAME).getFolder(DQStructureManager.ANALYSIS);
            try {
                searchAllAnalysis(defaultAnalysFolder);
            } catch (CoreException e) {
                e.printStackTrace();
            }
            resourceChanged = false;
        }
        return allAnalysisMap.values();
    }

    private void searchAllAnalysis(IFolder folder) throws CoreException {
        for (IResource resource : folder.members()) {
            if (resource.getType() == IResource.FOLDER) {
                searchAllAnalysis(folder.getFolder(resource.getName()));
            }
            IFile file = (IFile) resource;
            findAnalysis(file);
            
        }
    }

    public Analysis findAnalysis(IFile file) {
        AnalysisEntity analysisEntity = allAnalysisMap.get(file);
        if (analysisEntity != null) {
            return analysisEntity.getAnalysis();
        }
        Resource fileResource = getFileResource(file);
        Analysis analysis = retireAnalysis(fileResource);
        if (analysis != null) {
            AnalysisEntity entity = new AnalysisEntity(analysis);
            allAnalysisMap.put(file, entity);
        }
        return analysis;
    }

    public Analysis findAnalysis(File file) {
        Resource fileResource = getFileResource(file);
        Analysis analysis = retireAnalysis(fileResource);
        return analysis;
    }

    /**
     * DOC rli Comment method "retireAnalysis".
     * 
     * @param fileResource
     * @return
     */
    private Analysis retireAnalysis(Resource fileResource) {
        EList<EObject> contents = fileResource.getContents();
        if (contents.isEmpty()) {
            log.error("No content in " + fileResource);
        }
        log.info("Nb elements in contents " + contents.size());
        AnalysisSwitch<Analysis> mySwitch = new AnalysisSwitch<Analysis>() {

            public Analysis caseAnalysis(Analysis object) {
                return object;
            }
        };
        Analysis analysis = null;
        if (contents != null && contents.size() != 0) {
            analysis = mySwitch.doSwitch(contents.get(0));
        }
        return analysis;
    }

    public void remove(IFile file) {
        super.remove(file);
        this.allAnalysisMap.remove(file);
    }

    public void clear() {
        super.clear();
        this.allAnalysisMap.clear();
    }

    public ReturnCode save(Analysis analysis) {
        AnalysisWriter writer = new AnalysisWriter();
        File file = new File(analysis.getUrl());
        ReturnCode saved = writer.save(analysis, file);
        if (saved.isOk()) {
            setResourceChanged(true);
        }
        return saved;
    }
}
