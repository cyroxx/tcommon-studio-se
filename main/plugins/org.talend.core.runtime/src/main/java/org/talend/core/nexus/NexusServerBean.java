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

/**
 * created by wchen on 2015-5-12 Detailled comment
 *
 */
public class NexusServerBean {

    String server;

    String userName;

    String password;

    String repositoryId;

    boolean official;

    public NexusServerBean() {
    }

    public NexusServerBean(boolean official) {
        this.official = official;
    }

    /**
     * Getter for server.
     * 
     * @return the server
     */
    public String getServer() {
        return this.server;
    }

    /**
     * Sets the server.
     * 
     * @param server the server to set
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * Getter for userName.
     * 
     * @return the userName
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Sets the userName.
     * 
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Getter for password.
     * 
     * @return the password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Sets the password.
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Getter for repositoryId.
     * 
     * @return the repositoryId
     */
    public String getRepositoryId() {
        return this.repositoryId;
    }

    /**
     * Sets the repositoryId.
     * 
     * @param repositoryId the repositoryId to set
     */
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    /**
     * Getter for official.
     * 
     * @return the official
     */
    public boolean isOfficial() {
        return this.official;
    }

}
