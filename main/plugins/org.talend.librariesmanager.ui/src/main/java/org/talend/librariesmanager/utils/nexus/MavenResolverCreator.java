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
package org.talend.librariesmanager.utils.nexus;

import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;
import org.ops4j.pax.url.mvn.ServiceConstants;

/**
 * created by wchen on Apr 24, 2015 Detailled comment
 *
 */
public class MavenResolverCreator {

    private static MavenResolver mvnResolver;

    public synchronized static MavenResolver getMavenResolver() {
        if (mvnResolver == null) {
            mvnResolver = MavenResolvers.createMavenResolver(null, ServiceConstants.PID);
        }
        return mvnResolver;
    }

}
