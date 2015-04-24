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
package org.talend.core.model.general;

/**
 * created by WCHEN on 2012-9-17 Detailled comment
 * 
 */
public class ModuleToInstall {

    private String name;

    private String context;

    private String description;

    private String url_description;

    private String url_download;

    private boolean required;

    private String licenseType;

    private String licenseUrl;

    /* version defined in mvn: protocol */
    private String version = "6.0.0";//$NON-NLS-1$

    private String artifactId;

    private String groupId = "org.talend.libraries";//$NON-NLS-1$

    private String packageName = "jar";//$NON-NLS-1$

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContext() {
        return this.context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl_description() {
        return this.url_description;
    }

    public void setUrl_description(String url_description) {
        this.url_description = url_description;
    }

    public String getUrl_download() {
        return this.url_download;
    }

    public void setUrl_download(String url_download) {
        this.url_download = url_download;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return this.required;
    }

    public String getLicenseType() {
        return this.licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    /**
     * Getter for version.
     * 
     * @return the version
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Sets the version.
     * 
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Getter for artifactId.
     * 
     * @return the artifactId
     */
    public String getArtifactId() {
        return this.artifactId;
    }

    /**
     * Sets the artifactId.
     * 
     * @param artifactId the artifactId to set
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Getter for licenseUrl.
     * 
     * @return the licenseUrl
     */
    public String getLicenseUrl() {
        return this.licenseUrl;
    }

    /**
     * Sets the licenseUrl.
     * 
     * @param licenseUrl the licenseUrl to set
     */
    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    /**
     * Getter for packageName.
     * 
     * @return the packageName
     */
    public String getPackageName() {
        return this.packageName;
    }

    /**
     * Sets the packageName.
     * 
     * @param packageName the packageName to set
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Getter for groupId.
     * 
     * @return the groupId
     */
    public String getGroupId() {
        return this.groupId;
    }

    /**
     * Sets the groupId.
     * 
     * @param groupId the groupId to set
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
