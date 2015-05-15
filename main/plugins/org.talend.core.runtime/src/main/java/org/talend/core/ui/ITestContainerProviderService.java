// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.ui;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.IService;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;

/**
 * created by hwang on Jan 7, 2015 Detailled comment
 *
 */
public interface ITestContainerProviderService extends IService {

    public boolean isMatchedPath(IPath topLevelNodeWorkspaceRelativePath, IPath path);

    public boolean isTestContainerType(ERepositoryObjectType type);

    public boolean isTestContainerProcess(IProcess process);

    public boolean isTestContainerItem(Item item);

    public ProcessType getTestContainerProcess(Item item);

    public Item getParentJobItem(Item item) throws PersistenceException;

    public IProcess getParentJobProcess(IProcess process) throws PersistenceException;

    public void switchToCurTestContainerView();

    public void updateDetect(final IProcess curJobProcess, final boolean updateAllJobs);

    public void reloadJunitsNodes(INode nc);

    public List<ProcessItem> getAllTestContainers(ProcessItem item);

    public void copyDataSetFiles(IProcess process, IPath srcPath);
}
