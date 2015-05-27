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
package org.talend.core.runtime.maven;

import org.eclipse.core.runtime.Assert;
import org.osgi.framework.Version;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.VersionUtils;

/**
 * DOC ggu class global comment. Detailled comment
 * 
 * mvn-uri:='mvn:'[repository-url '!'] group-id '/' artifact-id ['/' [version] ['/' [type] ['/' classifier ]]]]
 */
@SuppressWarnings("nls")
public class MavenUrlHelper {

    public static final String MVN_PROTOCOL = "mvn:";

    public static final String REPO_SEPERATOR = "!";

    public static final String SEPERATOR = "/";

    public static final String VERSION_LATEST = "LATEST";

    public static final String VERSION_SNAPSHOT = "SNAPSHOT";

    public static MavenArtifact parseMvnUrl(String mvnUrl) {
        if (mvnUrl == null || !mvnUrl.startsWith(MVN_PROTOCOL)) {
            return null;
        }
        MavenArtifact artifact = new MavenArtifact();
        try {
            String substring = mvnUrl.substring(MVN_PROTOCOL.length());

            // repo
            int repoUrlIndex = substring.indexOf(REPO_SEPERATOR);
            if (repoUrlIndex > 0) { // has repo url
                artifact.setRepositoryUrl(substring.substring(0, repoUrlIndex));
                substring = substring.substring(repoUrlIndex + 1);
            }
            String[] segments = substring.split(SEPERATOR);
            // only group-id and artifact-id is required
            if (segments.length < 2 || segments[0].trim().length() == 0 || segments[1].trim().length() == 0) {
                return null;
            }
            artifact.setGroupId(segments[0].trim());
            artifact.setArtifactId(segments[1].trim());

            // version
            if (segments.length >= 3 && segments[2].trim().length() > 0) {
                // validate the version
                String verStr = segments[2].trim();
                if (VERSION_LATEST.equals(verStr)) {
                    artifact.setVersion(VERSION_LATEST);
                } else if (VERSION_SNAPSHOT.equals(verStr)) {
                    artifact.setVersion(VERSION_SNAPSHOT);
                } else if (verStr.length() > 0) {
                    // range := ( '[' | '(' ) version ',' version ( ')' | ']' )
                    // TODO, maybe need parse the range for version.
                    if ((verStr.startsWith("[") || verStr.startsWith("(")) && (verStr.endsWith(")") || verStr.endsWith("]"))) {
                        artifact.setVersion(verStr);
                    } else { // for number only, like 6.0.0
                        // only call the new Version here to validate it's a valid version
                        // if the version is not valid, it will throw an exception.                       
                        new Version(verStr);
                        // We keep original string here in case the version set was like :1.1
                        // (since artifact won't resolve if it's with 1.1.0, value returned by the Version.toString)
                        artifact.setVersion(verStr);
                    }
                }
            }
            if (segments.length >= 4 && segments[3].trim().length() > 0) { // has packaging
                artifact.setType(segments[3].trim());
            }

            if (segments.length >= 5 && segments[4].trim().length() > 0) {// has classifier
                // classifier is can be null.
                artifact.setClassifier(segments[4].trim());
            }

            /*
             * set default values.
             */
            if (artifact.getVersion() == null) {
                // if not set, will be LATEST by default
                artifact.setVersion(VERSION_LATEST);
            }
            if (artifact.getType() == null) {
                // if not set, will be jar by default
                artifact.setType(MavenConstants.TYPE_JAR);
            }
        } catch (Exception e) {
            ExceptionHandler.process(new Exception("Problem happened when install this maven URL: " + mvnUrl, e));
            return null;
        }

        return artifact;
    }

    /**
     * if startsWith "mvn:", will return true.
     */
    public static boolean isMvnUrl(String str) {
        MavenArtifact parseMvnUrl = parseMvnUrl(str);
        if (parseMvnUrl != null) {
            return true;
        }
        return false;
    }

    /**
     * will build the mvn url with default groupId and version.
     * "mvn:org.talend.libraries/<jarNameWithoutExtension>/currentVersion/<extension>"
     */
    public static String generateMvnUrlForJarName(String jarName) {
        if (jarName != null && jarName.length() > 0) {
            String artifactId = jarName;
            String type = null;
            if (jarName.endsWith(MavenConstants.TYPE_JAR)) { // remove the extension .jar
                artifactId = jarName.substring(0, jarName.lastIndexOf(MavenConstants.TYPE_JAR) - 1);
                type = MavenConstants.TYPE_JAR;
            } else { // need process normal files?
                int dotIndex = jarName.lastIndexOf('.');
                if (dotIndex == 0) {// don't support the file without name, only extension.
                    return null;
                } else if (dotIndex > 0) {
                    artifactId = jarName.substring(0, dotIndex);
                    type = jarName.substring(dotIndex + 1);
                }
            }
            String currentVersion = VersionUtils.getTalendVersion();
            if (currentVersion == null) {
                currentVersion = MavenConstants.DEFAULT_VERSION;
            }
            return generateMvnUrl(MavenConstants.DEFAULT_LIB_GROUP_ID, artifactId, currentVersion, type, null);
        }
        return null;
    }

    /**
     * 
     * mvn:groupId/artifactId/version/packaging/classifier
     */
    public static String generateMvnUrl(String groupId, String artifactId, String version, String packaging, String classifier) {
        Assert.isNotNull(groupId);
        Assert.isNotNull(artifactId);

        StringBuffer mvnUrl = new StringBuffer(100);
        mvnUrl.append(MVN_PROTOCOL);

        mvnUrl.append(groupId);
        mvnUrl.append(SEPERATOR);
        mvnUrl.append(artifactId);

        if (version != null) {
            mvnUrl.append(SEPERATOR);
            mvnUrl.append(version);
        } else {
            if (packaging != null || classifier != null) { // if has packaging or classifier
                // add one empty seperator
                mvnUrl.append(SEPERATOR);
            }
        }
        if (packaging != null) {
            mvnUrl.append(SEPERATOR);
            mvnUrl.append(packaging);
        } else {
            if (classifier != null) { // if has classifier
                // add one empty seperator
                mvnUrl.append(SEPERATOR);
            }
        }
        if (classifier != null) {
            mvnUrl.append(SEPERATOR);
            mvnUrl.append(classifier);
        }

        return mvnUrl.toString();
    }

}
