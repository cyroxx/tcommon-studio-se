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
 * created by wchen on Apr 27, 2015 Detailled comment
 *
 */
public class MavenSetupException extends Exception {

    private static final long serialVersionUID = 1L;

    public MavenSetupException(String message) {
        super(message);
    }

    public MavenSetupException(String message, Throwable cause) {
        super(message, cause);
    }

}
