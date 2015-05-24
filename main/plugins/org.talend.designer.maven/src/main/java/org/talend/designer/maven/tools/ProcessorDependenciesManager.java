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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.core.runtime.IProgressMonitor;
import org.talend.core.model.process.JobInfo;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.utils.PomUtil;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.ProcessorException;

/**
 * created by ycbai on 2015年4月2日 Detailled comment
 *
 */
public class ProcessorDependenciesManager {

    private final IProcessor processor;

    public ProcessorDependenciesManager(IProcessor processor) {
        this.processor = processor;
    }

    public boolean updateDependencies(IProgressMonitor progressMonitor, Model model) throws ProcessorException {
        try {
            List<Dependency> neededDependencies = new ArrayList<Dependency>();

            // add the job modules.
            Set<String> neededLibraries = processor.getNeededLibraries();
            for (String lib : neededLibraries) {
                Dependency dependency = PomUtil.createModuleDependency(lib);
                if (dependency != null) {
                    neededDependencies.add(dependency);
                }
            }
            String parentId = processor.getProperty().getId();
            Set<JobInfo> jobInfos = processor.getBuildChildrenJobs();
            for (JobInfo jobInfo : jobInfos) {
                if (jobInfo.getFatherJobInfo() != null && jobInfo.getFatherJobInfo().getJobId().equals(parentId)) {
                    Dependency dependency = new Dependency();
                    dependency.setGroupId(TalendMavenConstants.DEFAULT_JOB_GROUP_ID);
                    dependency.setArtifactId(jobInfo.getJobName());
                    dependency.setVersion(jobInfo.getJobVersion());
                    neededDependencies.add(dependency);
                }
            }

            return updateDependencies(progressMonitor, model, neededDependencies, false);

        } catch (Exception e) {
            throw new ProcessorException(e);
        }
    }

    /**
     * 
     * DOC ggu Comment method "updateDependencies". add the job Needed Libraries for current model.
     * 
     * @param model the job of pom model
     * @param fresh if true, will remove old dependencies, else will add the new dependencies in the head.
     * @return if there are some changes, will return true
     */
    public static boolean updateDependencies(IProgressMonitor progressMonitor, Model model, List<Dependency> neededDependencies,
            boolean fresh) throws ProcessorException {
        boolean changed = false;
        try {
            List<Dependency> existedDependencies = model.getDependencies();
            if (existedDependencies == null) {
                existedDependencies = new ArrayList<Dependency>();
                model.setDependencies(existedDependencies);
            }
            // record existed list
            Map<String, Dependency> existedDependenciesMap = new LinkedHashMap<String, Dependency>();
            if (!fresh) { // just in order to make the performance better.
                for (Dependency dependency : existedDependencies) {
                    // need remove the old non-existed dependencies, else won't compile the project.
                    if (!PomUtil.isAvailable(dependency)) {
                        continue;
                    }
                    existedDependenciesMap.put(PomUtil.generateMvnUrl(dependency), dependency);
                }
            }
            // clear all of existed list
            existedDependencies.clear();

            for (Dependency dependency : neededDependencies) {
                Dependency cloneDependency = dependency.clone();
                // FIXME, need check the new dependency existed or not? or just let the compile error for m2?
                // if (!PomUtil.isAvailable(cloneDependency)) {
                // continue;
                // }
                existedDependencies.add(cloneDependency); // add the needed in the head.

                if (fresh) {
                    changed = true; // after added, true always
                } else {
                    // remove it in old list.
                    String mvnUrl = PomUtil.generateMvnUrl(dependency);
                    Dependency existedDependency = existedDependenciesMap.remove(mvnUrl);
                    if (existedDependency != null) { // existed before.
                        // nothing to do.
                    } else { // added new
                        changed = true;
                    }
                }
            }

            if (!fresh) {
                // add the left dependencies.
                existedDependencies.addAll(existedDependenciesMap.values());
            }
        } catch (Exception e) {
            throw new ProcessorException(e);
        }
        return changed;
    }

}
