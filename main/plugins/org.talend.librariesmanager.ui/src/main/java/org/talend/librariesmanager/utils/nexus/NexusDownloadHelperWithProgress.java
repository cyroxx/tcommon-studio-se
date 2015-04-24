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

import org.talend.core.download.DownloadHelperWithProgress;
import org.talend.core.download.IDownloadHelper;

/**
 * created by wchen on Apr 24, 2015 Detailled comment
 *
 */
public class NexusDownloadHelperWithProgress extends DownloadHelperWithProgress {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.download.DownloadHelperWithProgress#createDownloadHelperDelegate(org.talend.core.download.
     * DownloadHelperWithProgress.DownloadListenerImplementation)
     */
    @Override
    protected IDownloadHelper createDownloadHelperDelegate(final DownloadListenerImplementation downloadProgress) {

        NexusDownloader downloadHelper = new NexusDownloader() {

            /*
             * (non-Javadoc)
             * 
             * @see org.talend.core.download.DownloadHelper#isCancel()
             */
            @Override
            public boolean isCancel() {
                return downloadProgress.isCanceled();
            }
        };
        downloadHelper.addDownloadListener(downloadProgress);
        return downloadHelper;

    }

}
