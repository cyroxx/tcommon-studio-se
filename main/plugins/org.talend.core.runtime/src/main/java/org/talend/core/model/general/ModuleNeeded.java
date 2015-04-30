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

import java.util.List;

import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.runtime.CoreRuntimePlugin;

/**
 * This bean is use to manage needed moduless (perl) and libraries (java).<br/>
 *
 * $Id: ModuleNeeded.java 38013 2010-03-05 14:21:59Z mhirt $
 *
 */
public class ModuleNeeded {

    private String id;

    private String context;

    private String moduleName;

    private String informationMsg;

    private boolean required;

    private boolean mrRequired = false;// That indicates if the module is required by M/R job.

    private String requiredIf;

    // bundleName and bundleVersion for osgi system,feature 0023460
    private String bundleName;

    private String bundleVersion;

    private ELibraryInstallStatus status = ELibraryInstallStatus.UNKNOWN;

    private boolean isShow = true;

    List<InstallModule> installModule;

    List<String> installURL;

    private String moduleLocaion;

    private String mavenUrl;

    public static final String SINGLE_QUOTE = "'"; //$NON-NLS-1$

    public static final String QUOTATION_MARK = "\""; //$NON-NLS-1$

    /**
     * DOC smallet ModuleNeeded class global comment. Detailled comment <br/>
     *
     * $Id: ModuleNeeded.java 38013 2010-03-05 14:21:59Z mhirt $
     *
     */
    public enum ELibraryInstallStatus {
        UNKNOWN,
        INSTALLED,
        UNUSED,
        NOT_INSTALLED;
    }

    /**
     * DOC smallet ModuleNeeded constructor comment.
     *
     * @param context
     * @param moduleName
     * @param informationMsg
     * @param required
     * @param status
     */
    public ModuleNeeded(String context, String moduleName, String informationMsg, boolean required) {
        super();
        this.context = context;
        setModuleName(moduleName);
        this.informationMsg = informationMsg;
        this.required = required;
    }

    public ModuleNeeded(String context, String moduleName, String informationMsg, boolean required, List<String> installURL,
            String requiredIf, String mavenUrl) {
        super();
        this.context = context;
        setModuleName(moduleName);
        this.informationMsg = informationMsg;
        this.required = required;
        this.installURL = installURL;
        this.requiredIf = requiredIf;
        this.mavenUrl = mavenUrl;
    }

    public String getRequiredIf() {
        return requiredIf;
    }

    public void setRequiredIf(String requiredIf) {
        this.requiredIf = requiredIf;
    }

    /**
     * Check if the library is required depends the condition of "required if". Note that if the flag "required=true" in
     * the xml of component, it will never check in the required_if.
     *
     * In some cases where we only want to check the basic "required=true" and not the required_if (module view for
     * example), it's possible to simply give null parameter.
     *
     * @param listParam
     * @return
     */
    public boolean isRequired(List<? extends IElementParameter> listParam) {
        if (required) { // if flag required is set, then forget the "required if" test.
            return required;
        }
        boolean isRequired = false;

        if (requiredIf != null && !requiredIf.isEmpty() && listParam != null) {
            isRequired = CoreRuntimePlugin.getInstance().getDesignerCoreService().evaluate(requiredIf, listParam);
        }
        return isRequired;
    }

    /**
     * Getter for installURL.
     *
     * @return the installURL
     */
    public List<String> getInstallURL() {
        return this.installURL;
    }

    /**
     * Sets the installURL.
     *
     * @param installURL the installURL to set
     */
    public void setInstallURL(List<String> installURL) {
        this.installURL = installURL;
    }

    /**
     * Getter for component.
     *
     * @return the component
     */
    public String getContext() {
        return this.context;
    }

    /**
     * Sets the component.
     *
     * @param component the component to set
     */
    public void setContext(String component) {
        this.context = component;
    }

    public List<InstallModule> getInstallModule() {
        return this.installModule;
    }

    public void setInstallModule(List<InstallModule> installModule) {
        this.installModule = installModule;
    }

    public String getInformationMsg() {
        return this.informationMsg;
    }

    public void setInformationMsg(String informationMsg) {
        this.informationMsg = informationMsg;
    }

    public String getModuleName() {
        return this.moduleName;
    }

    public void setModuleName(String moduleName) {
        if (moduleName != null) {
            this.moduleName = moduleName.replace(QUOTATION_MARK, "").replace(SINGLE_QUOTE, //$NON-NLS-1$
                    ""); //$NON-NLS-1$
        } else {
            this.moduleName = moduleName;
        }
    }

    public boolean isRequired() {
        return this.required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public ELibraryInstallStatus getStatus() {
        return this.status;
    }

    public void setStatus(ELibraryInstallStatus status) {
        this.status = status;
    }

    /**
     * Getter for isShow.
     *
     * @return the isShow
     */
    public boolean isShow() {
        return this.isShow;
    }

    /**
     * Sets the isShow.
     *
     * @param isShow the isShow to set
     */
    public void setShow(boolean isShow) {
        this.isShow = isShow;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(String bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    /**
     * Getter for mrRequired.
     *
     * @return the mrRequired
     */
    public boolean isMrRequired() {
        return this.mrRequired;
    }

    /**
     * Sets the mrRequired.
     *
     * @param mrRequired the mrRequired to set
     */
    public void setMrRequired(boolean mrRequired) {
        this.mrRequired = mrRequired;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        if (bundleName == null) {
            return moduleName;
        } else if (bundleVersion == null) {
            return moduleName + "[" + bundleName + "]";
        } else {
            return moduleName + "[" + bundleName + ":" + bundleVersion + "]";
        }
    }

    public String getModuleLocaion() {
        return this.moduleLocaion;
    }

    public void setModuleLocaion(String moduleLocaion) {
        this.moduleLocaion = moduleLocaion;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hashCode = 31;
        if (this.getModuleName() != null) {
            hashCode *= this.getModuleName().hashCode();
        }
        if (this.getBundleName() != null) {
            hashCode *= this.getBundleName().hashCode();
        }
        if (this.getBundleVersion() != null) {
            hashCode *= this.getBundleVersion().hashCode();
        }

        hashCode *= new Boolean(this.isRequired()).hashCode();
        return hashCode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ModuleNeeded)) {
            return false;
        }
        ModuleNeeded other = (ModuleNeeded) obj;

        // ModuleName
        if (other.getModuleName() == null) {
            if (this.getModuleName() != null) {
                return false;
            }
        } else {
            if (this.getModuleName() == null) {
                return false;
            } else if (!other.getModuleName().equals(this.getModuleName())) {
                return false;
            }
        }
        // BundleName
        if (other.getBundleName() == null) {
            if (this.getBundleName() != null) {
                return false;
            }
        } else {
            if (this.getBundleName() == null) {
                return false;
            } else if (!other.getBundleName().equals(this.getBundleName())) {
                return false;
            }
        }
        // BundleVersion
        if (other.getBundleVersion() == null) {
            if (this.getBundleVersion() != null) {
                return false;
            }
        } else {
            if (this.getBundleVersion() == null) {
                return false;
            } else if (!other.getBundleVersion().equals(this.getBundleVersion())) {
                return false;
            }
        }

        // Module Location
        if (other.getModuleLocaion() == null) {
            if (this.getModuleLocaion() != null) {
                return false;
            }
        } else {
            if (this.getModuleLocaion() == null) {
                return false;
            } else if (!other.getModuleLocaion().equals(this.getModuleLocaion())) {
                return false;
            }
        }

        if (other.isRequired() != this.isRequired()) {
            return false;
        }
        return true;
    }

    /**
     * Getter for mavenUrl.
     * 
     * @return the mavenUrl
     */
    public String getMavenUrl() {
        if (mavenUrl == null || "".equals(mavenUrl) || !mavenUrl.startsWith(NexusConstants.MAVEN_PROTECAL)) {
            return getDefaulMavenUrl();
        }
        return this.mavenUrl;
    }

    private String getDefaulMavenUrl() {
        String artifactId = moduleName;
        int index = moduleName.lastIndexOf(".");
        if (index != -1) {
            artifactId = moduleName.substring(0, index);
        }
        ExceptionHandler.log("Warning : the groupid and version in the url may not be correct");
        return NexusConstants.MAVEN_PROTECAL + NexusConstants.DEFAULT_GROUP_ID + "/" + artifactId + "/"
                + NexusConstants.DEFAULT_VERSION;

    }

    /**
     * Sets the mavenUrl.
     * 
     * @param mavenUrl the mavenUrl to set
     */
    public void setMavenUrl(String mavenUrl) {
        this.mavenUrl = mavenUrl;
    }

}
