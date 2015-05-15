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
package org.talend.designer.maven.deploy;

import java.util.Map;

import org.talend.core.nexus.NexusServerBean;
import org.talend.core.nexus.NexusServerUtil;

/**
 * created by wchen on 2015-5-14 Detailled comment
 *
 */
public class ArtifactsDeployer {

    private static ArtifactsDeployer deployer;

    private NexusServerBean nexusServer;

    private ArtifactsDeployer() {
        nexusServer = NexusServerUtil.getNexusServer(true);
    }

    public static ArtifactsDeployer getInstance() {
        if (deployer == null) {
            deployer = new ArtifactsDeployer();
        }
        return deployer;
    }

    /**
     * 
     * DOC Talend Comment method "deployToLocalMaven".
     * 
     * @param jarSourceAndMavenUri a map with key : can be a filePath or platform uri , value :maven uri
     * @throws Exception
     */
    public void deployToLocalMaven(Map<String, String> jarSourceAndMavenUri) throws Exception {
        // TODO deploy the jar with the mavenUri to local maven
        if (nexusServer != null && !nexusServer.isOfficial()) {
            // deploy to nexus server if it is not null and not official server

        }
    }

    /**
     * 
     * DOC Talend Comment method "deployToLocalMaven".
     * 
     * @param uriOrPath can be a filePath or platform uri
     * @param mavenUri maven uri
     * @throws Exception
     */
    public void deployToLocalMaven(String uriOrPath, String mavenUri) throws Exception {
        // TODO deploy the jar with the mavenUri to local maven
        if (nexusServer != null && !nexusServer.isOfficial()) {
            // deploy to nexus server if it is not null and not official server

        }
    }

}
