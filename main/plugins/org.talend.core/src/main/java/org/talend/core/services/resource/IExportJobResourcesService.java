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
package org.talend.core.services.resource;

import java.net.URL;
import java.util.List;

/**
 * DOC ycbai class global comment. Detailled comment
 */
public interface IExportJobResourcesService extends IExportScriptResourcesService {

    /**
     * DOC ycbai Comment method "getAntScriptFilePath".
     * 
     * Get the path of ant script template file.
     * 
     * @return
     * @deprecated the Ant have been removed
     */
    public String getAntScriptFilePath();

    /**
     * DOC ycbai Comment method "getAntRequiredLibs".
     * 
     * @return urls of libs which ant requires.
     * 
     * @deprecated the Ant have been removed
     */
    public List<URL> getAntRequiredLibs();

}
