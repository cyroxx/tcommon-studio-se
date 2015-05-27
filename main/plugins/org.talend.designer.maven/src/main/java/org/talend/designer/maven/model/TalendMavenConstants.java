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
package org.talend.designer.maven.model;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.talend.commons.utils.generation.JavaUtils;

/**
 * created by ggu on 2 Feb 2015 Detailled comment
 *
 */
@SuppressWarnings("nls")
public interface TalendMavenConstants {

    static final String POM_VERSION = "4.0.0";

    /* same as MavenArtifactComponent.JAR */
    static final String PACKAGING_JAR = "jar";

    /* same as MavenArtifactComponent.POM */
    static final String PACKAGING_POM = "pom";

    static final String POM_NAME = "pom";

    static final String XML_EXT = ".xml";

    static final String POM_FILE_NAME = IMavenConstants.POM_FILE_NAME; // POM_NAME+XML_EXT

    static final String ASSEMBLY_NAME = "assembly";

    static final String ASSEMBLY_FILE_NAME = ASSEMBLY_NAME + XML_EXT;

    static final String PREFIX_ARG = "-D";

    static final String PREFIX_PROFILE = "-P"; //$NON-NLS-1$

    /**
     * goal
     */
    static final String GOAL_CLEAN = "clean";

    static final String GOAL_COMPILE = "compile";

    static final String GOAL_TEST = "test";

    static final String GOAL_TEST_COMPILE = "test-compile";

    static final String GOAL_PACKAGE = "package";

    static final String GOAL_INSTALL = "install";

    static final String PROFILE_INCLUDE_BINARIES = "include-binaries";

    static final String PROFILE_INCLUDE_CONTEXTS = "include-contexts";

    static final String PROFILE_INCLUDE_ITEMS = "include-items";

    static final String PROFILE_INCLUDE_JAVA_SOURCES = "include-java-sources";

    static final String PROFILE_INCLUDE_TEST_SOURCES = "include-test-sources";

    static final String PROFILE_INCLUDE_LIBS = "include-libs";

    static final String ARG_SKIPTESTS = "-DskipTests";

    /* same as the JavaUtils.JAVA_PROJECT_NAME */
    static final String PROJECT_NAME = ".Java";

    static final String CURRENT_PATH = ".";

    static final String DEFAULT_ENCODING = "UTF-8";

    static final String DEFAULT_GROUP_ID = "org.talend";

    static final String DEFAULT_VERSION = "6.0.0";

    /*
     * for .Java/pom.xml
     */
    static final String DEFAULT_CODE_PROJECT_GROUP_ID = DEFAULT_GROUP_ID + ".master";

    static final String DEFAULT_CODE_PROJECT_ARTIFACT_ID = "master.Codes";

    /*
     * for routines
     */
    static final String DEFAULT_ROUTINES_GROUP_ID = DEFAULT_GROUP_ID + ".code";

    static final String DEFAULT_ROUTINES_ARTIFACT_ID = JavaUtils.ROUTINE_JAR_NAME;

    static final String DEFAULT_ROUTINE_VERSION = DEFAULT_VERSION;

    /*
     * for job
     */
    static final String DEFAULT_JOB_GROUP_ID = DEFAULT_GROUP_ID + ".job";

    /*
     * for test container
     */
    static final String DEFAULT_JUNIT_ARTIFACT_GROUP = "junit";

    static final String DEFAULT_JUNIT_ARTIFACT_ID = "junit";

    static final String DEFAULT_JUNIT_ARTIFACT_VERSION = "3.8.1";

    static final String DEFAULT_JUNIT_ARTIFACT_SCOPE = "test";

}
