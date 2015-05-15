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

/**
 * created by wchen on 2015-5-12 Detailled comment
 *
 */
public class NexusServerUtil {

    private static final String KEY_NEXUS_RUL = "nexusUrl";//$NON-NLS-1$ 

    private static final String KEY_NEXUS_USER = "username";//$NON-NLS-1$ 

    private static final String KEY_NEXUS_PASS = "password";//$NON-NLS-1$ 

    private static final String KEY_NEXUS_REPOSITORY = "repository";//$NON-NLS-1$ 

    // TODO to fill the offical server information
    private static final String TALEND_NEXUS_SERVER = "";

    private static final String TALEND_NEXUS_USER = "";

    private static final String TALEND_NEXUS_PASSWORD = "";

    private static final String TALEND_NEXUS_REPOSITORY = "";

    public static NexusServerBean getNexusServer(boolean createDefault) {
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
                NexusServerBean serverBean = getNexusServer(adminUrl, userName, password);
                if (serverBean != null) {
                    return serverBean;
                }
            }
        } finally {
            if (createDefault) {
                NexusServerBean serverBean = new NexusServerBean(true);
                serverBean.setServer(TALEND_NEXUS_SERVER);
                serverBean.setUserName(TALEND_NEXUS_USER);
                serverBean.setPassword(TALEND_NEXUS_PASSWORD);
                serverBean.setRepositoryId(TALEND_NEXUS_REPOSITORY);
                return serverBean;
            }
        }
        return null;
    }

    public static NexusServerBean getNexusServer(String adminUrl, String userName, String password) {
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

}
