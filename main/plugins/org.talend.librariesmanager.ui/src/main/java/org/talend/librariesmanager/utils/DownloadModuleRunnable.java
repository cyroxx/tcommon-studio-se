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
package org.talend.librariesmanager.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.ops4j.pax.url.mvn.Handler;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.io.FilesUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ILibraryManagerService;
import org.talend.core.download.DownloadHelperWithProgress;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.general.ModuleToInstall;
import org.talend.librariesmanager.prefs.LibrariesManagerUtils;
import org.talend.librariesmanager.ui.LibManagerUiPlugin;
import org.talend.librariesmanager.ui.i18n.Messages;
import org.talend.librariesmanager.ui.wizards.AcceptModuleLicensesWizard;
import org.talend.librariesmanager.ui.wizards.AcceptModuleLicensesWizardDialog;
import org.talend.librariesmanager.utils.nexus.NexusDownloadHelperWithProgress;

abstract public class DownloadModuleRunnable implements IRunnableWithProgress {

    protected List<ModuleToInstall> toDownload;

    protected Set<String> downloadFailed;

    protected Set<String> installedModules;

    /**
     * DOC sgandon DownloadModuleRunnable constructor comment.
     * 
     * @param shell, never null, used to ask the user to accept the licenses
     * @param toDownload
     */
    public DownloadModuleRunnable(List<ModuleToInstall> toDownload) {
        this.toDownload = toDownload;
        downloadFailed = new HashSet<String>();
        installedModules = new HashSet<String>();
    }

    @Override
    public void run(final IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor,
                Messages.getString("ExternalModulesInstallDialog.downloading2"), toDownload.size() * 10 + 5); //$NON-NLS-1$
        if (checkAndAcceptLicenses(subMonitor)) {
            downLoad(subMonitor);
        }
        if (monitor != null) {
            monitor.setCanceled(subMonitor.isCanceled());
            monitor.done();
        }
    }

    private void downLoad(final IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor,
                Messages.getString("ExternalModulesInstallDialog.downloading2"), toDownload.size() + 1); //$NON-NLS-1$

        if (RemoteModulesHelper.nexus_available) {
            Map<String, String> customUriToAdd = new HashMap<String, String>();
            for (final ModuleToInstall module : toDownload) {
                if (!monitor.isCanceled()) {
                    monitor.subTask(module.getName());
                    boolean accepted;
                    if (module.getUrl_download() != null && !"".equals(module.getUrl_download())) { //$NON-NLS-1$
                        try {
                            // check license
                            boolean isLicenseAccepted = LibManagerUiPlugin.getDefault().getPreferenceStore()
                                    .getBoolean(module.getLicenseType());
                            accepted = isLicenseAccepted;
                            if (!accepted) {
                                subMonitor.worked(1);
                                continue;
                            }
                            if (monitor.isCanceled()) {
                                return;
                            }
                            NexusDownloadHelperWithProgress downloader = new NexusDownloadHelperWithProgress();
                            downloader.download(new URL(null, module.getMavenUri(), new Handler()), null, subMonitor.newChild(1));
                            customUriToAdd.put(module.getName(), module.getMavenUri());
                        } catch (Exception e) {
                            downloadFailed.add(module.getName());
                            ExceptionHandler.process(e);
                            continue;
                        }
                    }
                    accepted = false;
                } else {
                    downloadFailed.add(module.getName());
                }
            }
            // reset the module install status
            if (!customUriToAdd.isEmpty()) {
                ILibraryManagerService libraryManagerService = (ILibraryManagerService) GlobalServiceRegister.getDefault()
                        .getService(ILibraryManagerService.class);
                libraryManagerService.deployMavenIndex(customUriToAdd, monitor);
                libraryManagerService.forceListUpdate();
                LibManagerUiPlugin.getDefault().getLibrariesService().resetModulesNeeded();
            }
            subMonitor.worked(1);
        } else {
            // TODO to be removed after nexus server available
            final List<URL> downloadOk = new ArrayList<URL>();
            for (final ModuleToInstall module : toDownload) {
                if (!monitor.isCanceled()) {
                    monitor.subTask(module.getName());
                    String librariesPath = LibrariesManagerUtils.getLibrariesPath(ECodeLanguage.JAVA);
                    File target = new File(librariesPath);
                    boolean accepted;
                    if (module.getUrl_download() != null && !"".equals(module.getUrl_download())) { //$NON-NLS-1$
                        try {
                            // check license
                            boolean isLicenseAccepted = LibManagerUiPlugin.getDefault().getPreferenceStore()
                                    .getBoolean(module.getLicenseType());
                            accepted = isLicenseAccepted;
                            if (!accepted) {
                                subMonitor.worked(1);
                                continue;
                            }
                            if (monitor.isCanceled()) {
                                return;
                            }
                            File destination = new File(target.toString() + File.separator + module.getName());
                            File destinationTemp = target.createTempFile(destination.getName(), ".jar");
                            DownloadHelperWithProgress downloader = new DownloadHelperWithProgress();
                            downloader.download(new URL(module.getUrl_download()), destinationTemp, subMonitor.newChild(1));
                            // if the jar had download complete , will copy it from system temp path to "lib/java"
                            if (!monitor.isCanceled()) {
                                FilesUtils.copyFile(destinationTemp, destination);
                                downloadOk.add(destination.toURI().toURL());
                                installedModules.add(module.getName());
                            }
                            if (destinationTemp.exists()) {
                                destinationTemp.delete();
                            }
                        } catch (Exception e) {
                            downloadFailed.add(module.getName());
                            ExceptionHandler.process(e);
                            continue;
                        }
                    }
                    accepted = false;
                } else {
                    downloadFailed.add(module.getName());
                }
            }
            if (!downloadOk.isEmpty()) {
                try {
                    LibManagerUiPlugin.getDefault().getLibrariesService()
                            .deployLibrarys(downloadOk.toArray(new URL[downloadOk.size()]));
                } catch (IOException e) {
                    ExceptionHandler.process(e);
                }
            }
            subMonitor.worked(1);
        }
    }

    protected boolean hasLicensesToAccept() {
        if (toDownload != null && toDownload.size() > 0) {
            for (ModuleToInstall module : toDownload) {
                String licenseType = module.getLicenseType();
                if (licenseType != null) {
                    boolean isLicenseAccepted = LibManagerUiPlugin.getDefault().getPreferenceStore()
                            .getBoolean(module.getLicenseType());
                    if (!isLicenseAccepted) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected boolean checkAndAcceptLicenses(final IProgressMonitor monitor) {
        final AtomicBoolean accepted = new AtomicBoolean(true);
        if (hasLicensesToAccept()) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    AcceptModuleLicensesWizard licensesWizard = new AcceptModuleLicensesWizard(toDownload);
                    AcceptModuleLicensesWizardDialog wizardDialog = new AcceptModuleLicensesWizardDialog(PlatformUI
                            .getWorkbench().getActiveWorkbenchWindow().getShell(), licensesWizard, toDownload, monitor);
                    wizardDialog.setPageSize(700, 380);
                    wizardDialog.create();
                    if (wizardDialog.open() != Window.OK) {
                        accepted.set(false);
                    }
                }
            });

        }

        return accepted.get();
    }

    /**
     * DOC sgandon Comment method "acceptLicence".
     * 
     * @param module
     */
    abstract protected boolean acceptLicence(ModuleToInstall module);

    /**
     * Getter for downloadFailed.
     * 
     * @return the downloadFailed
     */
    public Set<String> getDownloadFailed() {
        return this.downloadFailed;
    }

    /**
     * Getter for installedModules.
     * 
     * @return the installedModules
     */
    public Set<String> getInstalledModules() {
        return this.installedModules;
    }
}
