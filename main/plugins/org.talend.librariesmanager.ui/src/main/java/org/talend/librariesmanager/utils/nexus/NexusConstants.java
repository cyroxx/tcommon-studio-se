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

/**
 * created by wchen on Apr 23, 2015 Detailled comment
 *
 */
public class NexusConstants {

    public static final String BASE_VERSION = "6.0.0";

    public static final String DEFAULT_GROUP_ID = "org.talend.libraries";

    public static final String MAVEN_PROTECAL = "mvn:";

    public static final String INDEX_GROUP_ID = "org.talend.libraries_index";

    public static final String INDEX_ARTIFACT_ID = "libraries_index";

    public static final String INDEX_PACKAGE = "zip";

    public static final String MODULE_INDEX_SPEC = MAVEN_PROTECAL + INDEX_GROUP_ID + "/" + INDEX_ARTIFACT_ID + "/";
}
