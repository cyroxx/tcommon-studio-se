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
package org.talend.core.nexus;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.model.properties.User;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.core.service.IRemoteService;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.RepositoryConstants;
import org.talend.utils.json.JSONException;
import org.talend.utils.json.JSONObject;

import com.sun.org.apache.xml.internal.security.utils.Base64;

/**
 * created by wchen on 2015-5-12 Detailled comment
 *
 */
public class NexusServerManager {

    // for SoftwareUpdate
    private static final String KEY_NEXUS_RUL = "nexusUrl";//$NON-NLS-1$ 

    private static final String KEY_NEXUS_USER = "username";//$NON-NLS-1$ 

    private static final String KEY_NEXUS_PASS = "password";//$NON-NLS-1$ 

    private static final String KEY_NEXUS_REPOSITORY = "repository";//$NON-NLS-1$ 

    // for SoftwareUpdate
    private static final String KEY_LIB_RUL = "Url";//$NON-NLS-1$ 

    private static final String KEY_LIB_USER = "Username";//$NON-NLS-1$ 

    private static final String KEY_LIB_PASS = "Password";//$NON-NLS-1$ 

    private static final String TALEND_LIB_SERVER = "https://talend-update.talend.com";//$NON-NLS-1$ 

    private static final String TALEND_LIB_USER = "";//$NON-NLS-1$ 

    private static final String TALEND_LIB_PASSWORD = "";//$NON-NLS-1$ 

    private static final String TALEND_LIB_REPOSITORY = "org.talend.libraries";//$NON-NLS-1$ 

    /**
     * 
     * DOC Talend Comment method "getLibrariesNexusServer". get nexus server for libraries
     * 
     * @param createTalendIfCustomNotExsit , true : if custom libraries nexus server not configured in tac , return the
     * default one. false : if custom libraries nexus server not configured in tac , return null.
     * @return
     */
    public static NexusServerBean getLibrariesNexusServer(boolean createTalendIfCustomNotExsit) {
        try {
            IProxyRepositoryFactory factory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
            if (factory.getRepositoryContext() != null && factory.getRepositoryContext().getFields() != null) {
                String adminUrl = factory.getRepositoryContext().getFields().get(RepositoryConstants.REPOSITORY_URL);
                String userName = "";
                String password = "";
                User user = factory.getRepositoryContext().getUser();
                if (user != null) {
                    userName = user.getLogin();
                    password = new String(user.getPassword());
                }
                NexusServerBean serverBean = null;
                if (adminUrl != null && !"".equals(adminUrl)
                        && GlobalServiceRegister.getDefault().isServiceRegistered(IRemoteService.class)) {
                    serverBean = new NexusServerBean();
                    IRemoteService remoteService = (IRemoteService) GlobalServiceRegister.getDefault().getService(
                            IRemoteService.class);
                    JSONObject updateRepositoryUrl;
                    updateRepositoryUrl = remoteService.getLibLocation(userName, password, adminUrl);
                    // the url like http://localhost:8081/nexus/content/repositories/org.talend.libraries/ which
                    // contains the repository id
                    String nexus_url = updateRepositoryUrl.getString(KEY_LIB_RUL);
                    String nexus_user = updateRepositoryUrl.getString(KEY_LIB_USER);
                    String nexus_pass = updateRepositoryUrl.getString(KEY_LIB_PASS);
                    String url = nexus_url;
                    if (url.endsWith(NexusConstants.SLASH)) {
                        url = url.substring(0, url.length() - 1);
                    }
                    if (!nexus_url.endsWith(NexusConstants.SLASH)) {
                        nexus_url = nexus_url + NexusConstants.SLASH;
                    }
                    serverBean.setRepositoryUrl(nexus_url);

                    String server = url.substring(0, url.indexOf(NexusConstants.CONTENT_REPOSITORIES));
                    String repositoryId = url.substring(url.indexOf(NexusConstants.CONTENT_REPOSITORIES)
                            + NexusConstants.CONTENT_REPOSITORIES.length(), url.length());
                    serverBean.setServer(server);
                    serverBean.setUserName(nexus_user);
                    serverBean.setPassword(nexus_pass);
                    serverBean.setRepositoryId(repositoryId);
                }
                if (serverBean != null) {
                    return serverBean;
                }
            }
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
        } catch (LoginException e) {
            ExceptionHandler.process(e);
        } catch (JSONException e) {
            ExceptionHandler.process(e);
        } finally {
            if (createTalendIfCustomNotExsit) {
                NexusServerBean serverBean = new NexusServerBean(true);
                serverBean.setServer(TALEND_LIB_SERVER);
                serverBean.setUserName(TALEND_LIB_USER);
                serverBean.setPassword(TALEND_LIB_PASSWORD);
                serverBean.setRepositoryId(TALEND_LIB_REPOSITORY);
                return serverBean;
            }
        }
        return null;
    }

    /**
     * 
     * DOC Talend Comment method "getSoftwareUpdateNexusServer". get nexus server configured in TAC for patch
     * 
     * @param adminUrl
     * @param userName
     * @param password
     * @return
     */
    public static NexusServerBean getSoftwareUpdateNexusServer(String adminUrl, String userName, String password) {
        try {
            if (adminUrl != null && !"".equals(adminUrl)
                    && GlobalServiceRegister.getDefault().isServiceRegistered(IRemoteService.class)) {
                IRemoteService remoteService = (IRemoteService) GlobalServiceRegister.getDefault().getService(
                        IRemoteService.class);
                JSONObject updateRepositoryUrl;
                updateRepositoryUrl = remoteService.getUpdateRepositoryUrl(userName, password, adminUrl);
                String nexus_url = updateRepositoryUrl.getString(KEY_NEXUS_RUL);
                String nexus_user = updateRepositoryUrl.getString(KEY_NEXUS_USER);
                String nexus_pass = updateRepositoryUrl.getString(KEY_NEXUS_PASS);
                String nexus_repository = updateRepositoryUrl.getString(KEY_NEXUS_REPOSITORY);
                NexusServerBean serverBean = new NexusServerBean();
                serverBean.setServer(nexus_url);
                serverBean.setUserName(nexus_user);
                serverBean.setPassword(nexus_pass);
                serverBean.setRepositoryId(nexus_repository);
            }
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
        } catch (LoginException e) {
            ExceptionHandler.process(e);
        } catch (JSONException e) {
            ExceptionHandler.process(e);
        }

        return null;
    }

    public static List<NexusArtifact> search(String nexusUrl, String userName, String password, String repositoryId,
            String groupIdToSearch, String versionToSearch) throws BusinessException {
        List<NexusArtifact> artifacts = new ArrayList<NexusArtifact>();
        HttpURLConnection urlConnection = null;
        try {
            String service = NexusConstants.SERVICES_SEARCH + getSearchQuery(versionToSearch, repositoryId, groupIdToSearch);
            urlConnection = getHttpURLConnection(nexusUrl, service, userName, password);
            SAXReader saxReader = new SAXReader();

            InputStream inputStream = urlConnection.getInputStream();
            Document document = saxReader.read(inputStream);
            List<Node> list = document.selectNodes("/searchNGResponse/data/artifact");//$NON-NLS-1$ 
            for (Node arNode : list) {
                NexusArtifact artifact = new NexusArtifact();
                artifacts.add(artifact);
                artifact.setGroupId(arNode.selectSingleNode("groupId").getText());//$NON-NLS-1$ 
                artifact.setArtifactId(arNode.selectSingleNode("artifactId").getText());//$NON-NLS-1$ 
                artifact.setVersion(arNode.selectSingleNode("version").getText());
                Node descNode = arNode.selectSingleNode("description");
                if (descNode != null) {
                    artifact.setDescription(descNode.getText());
                }
                Node urlNode = arNode.selectSingleNode("url");
                if (urlNode != null) {
                    artifact.setUrl(urlNode.getText());
                }
                Node licenseNode = arNode.selectSingleNode("license");
                if (licenseNode != null) {
                    artifact.setLicense(licenseNode.getText());
                }

                Node licenseUrlNode = arNode.selectSingleNode("licenseUrl");
                if (licenseUrlNode != null) {
                    artifact.setLicenseUrl(licenseUrlNode.getText());
                }
                List<Node> artLinks = arNode.selectNodes("artifactHits/artifactHit/artifactLinks/artifactLink");//$NON-NLS-1$ 
                for (Node link : artLinks) {
                    Node extensionElement = link.selectSingleNode("extension");//$NON-NLS-1$ 
                    String extension = null;
                    String classifier = null;
                    if (extensionElement != null) {
                        if ("pom".equals(extensionElement.getText())) {
                            continue;
                        }
                        extension = extensionElement.getText();
                    }
                    Node classifierElement = link.selectSingleNode("classifier");//$NON-NLS-1$ 
                    if (classifierElement != null) {
                        classifier = classifierElement.getText();
                    }
                    artifact.setType(extension);
                    artifact.setClassifier(classifier);
                }
            }
        } catch (Exception e) {
            if (e instanceof ConnectException) {
                throw new BusinessException("Can not connect to nexus server ,please contact the administrator", nexusUrl);
            } else {
                throw new BusinessException(e);
            }

        } finally {
            if (null != urlConnection) {
                urlConnection.disconnect();
            }
        }

        return artifacts;

    }

    private static String getSearchQuery(String version, String repositoryId, String groupId) {
        String query = "";//$NON-NLS-1$ 
        if (version != null) {
            query = "v=" + version + "&";//$NON-NLS-1$ //$NON-NLS-2$ 
        }
        return query + "repositoryId=" + repositoryId + "&g=" + groupId;//$NON-NLS-1$ //$NON-NLS-2$ 
    }

    private static HttpURLConnection getHttpURLConnection(String nexusUrl, String restService, String userName, String password)
            throws IOException {
        if (!nexusUrl.endsWith(NexusConstants.SLASH)) {
            nexusUrl = nexusUrl + NexusConstants.SLASH;
        }
        URL url = new URL(nexusUrl + restService);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", "Basic " + Base64.encode((userName + ":" + password).getBytes()));//$NON-NLS-1$ //$NON-NLS-2$
        return urlConnection;
    }

}
