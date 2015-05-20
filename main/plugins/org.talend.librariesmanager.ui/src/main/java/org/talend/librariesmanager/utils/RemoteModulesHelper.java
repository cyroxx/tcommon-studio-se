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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.osgi.framework.Version;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.VersionUtils;
import org.talend.commons.utils.network.NetworkUtil;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.general.ModuleToInstall;
import org.talend.core.nexus.MavenResolverCreator;
import org.talend.core.nexus.NexusServerBean;
import org.talend.core.nexus.NexusServerManager;
import org.talend.core.runtime.maven.MavenConstants;
import org.talend.core.runtime.maven.MavenUrlHelper;
import org.talend.librariesmanager.model.service.LibrariesIndexManager;
import org.talend.librariesmanager.ui.dialogs.IModulesListener;
import org.talend.librariesmanager.ui.i18n.Messages;
import org.talend.utils.io.FilesUtils;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

/**
 * created by Administrator on 2012-9-19 Detailled comment
 * 
 */
public class RemoteModulesHelper {

    private static final String SLASH = "/";//$NON-NLS-1$ 

    // TODO to be removed after nexus server available
    public static final boolean nexus_available = false;

    // true if user was warned the network connection is not possible
    static private boolean alreadyWarnedAboutConnectionIssue = false;

    /**
     * created by sgandon on 24 sept. 2013 Detailled comment
     * 
     */
    private final class RemoteModulesFetchRunnable implements IRunnableWithProgress {

        // TODO to be removed after nexus server available
        private Set<String> globalUnavailableModulesToBeRemoved = new HashSet<String>();

        /**
         * 
         */
        private final String jarNames;

        /**
         * 
         */
        private final List<ModuleToInstall> toInstall;

        /**
         * 
         */
        private final Map<String, List<ModuleNeeded>> contextMap;

        private String messages;

        /**
         * DOC sgandon IRunnableWithProgressImplementation constructor comment.
         * 
         * @param jarNames
         * @param toInstall
         * @param contextMap
         */
        private RemoteModulesFetchRunnable(String jarNames, List<ModuleToInstall> toInstall,
                Map<String, List<ModuleNeeded>> contextMap) {
            this.jarNames = jarNames;
            this.toInstall = toInstall;
            this.contextMap = contextMap;
        }

        public String getMessages() {
            return this.messages;
        }

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            if (nexus_available) {
                String[] jars = jarNames.split(SEPARATOR_SLIP);
                int size = jars.length;
                Set<String> unavailableModules = new HashSet<String>();
                monitor.beginTask(Messages.getString("RemoteModulesHelper.fetch.module.info"), size * 10 + 10);//$NON-NLS-1$
                // if the network is not valid, all jars are not available.
                boolean networkValid = false;
                if (cache == null) {
                    networkValid = NetworkUtil.isNetworkValid();
                    if (!networkValid) {
                        unavailableModules.addAll(Arrays.asList(jars));
                        messages = Messages.getString("RemoteModulesHelper.offlineMessages"); //$NON-NLS-1$
                        if (!alreadyWarnedAboutConnectionIssue) {
                            log.warn("failed to connect to internet");
                            alreadyWarnedAboutConnectionIssue = true;
                        }// else already warned so do nothing
                    }
                }
                if (networkValid && (cache == null || recheckCache)) {
                    // only check from the index one time after start studio
                    try {
                        // TODO need to remove the index and use pluged lucene search latter
                        // TODO need modify latter , only parse the index of 6.0.0 by default
                        // mvn:org.talend.libraries_index/libraries_index/6.0.0/zip
                        cache = new HashMap<String, ModuleToInstall>();
                        File resolve = mvnResolver.resolve(MavenUrlHelper.generateMvnUrl(MavenConstants.INDEX_GROUP_ID,
                                MavenConstants.INDEX_ARTIFACT_ID, MavenConstants.DEFAULT_VERSION, MavenConstants.INDEX_PACKAGE,
                                null));
                        // .m2/repository/org/talend/libraries_index/libraries_index/6.0.0/unziped
                        String targetFolder = resolve.getParent() + "/unziped/";
                        FilesUtils.unzip(resolve.getAbsolutePath(), targetFolder);

                        final String currentVersion = VersionUtils.getTalendVersion();

                        File unziped = new File(targetFolder);
                        File[] validIndexFiles = unziped.listFiles(new FileFilter() {

                            @Override
                            public boolean accept(File pathname) {
                                String name = pathname.getName();
                                int startIndex = name.lastIndexOf("_");
                                int endIndex = name.lastIndexOf(".");
                                if (startIndex != -1 && endIndex != -1) {
                                    String fileVersion = name.substring(startIndex + 1, endIndex);
                                    Version current = new Version(currentVersion);
                                    Version fVersion = new Version(fileVersion);
                                    if (current.getMajor() >= fVersion.getMajor() && current.getMicro() >= fVersion.getMicro()
                                            && current.getMinor() >= fVersion.getMinor()) {
                                        return true;
                                    }
                                }

                                return false;
                            }
                        });
                        for (File indexFile : validIndexFiles) {
                            FileInputStream openStream = new FileInputStream(indexFile);
                            Document document = new SAXReader(false).read(openStream);
                            Element root = document.getRootElement();
                            Element group = root.element("Group");
                            String groupId = group.attributeValue("id");
                            Iterator iter = group.elementIterator("Module");
                            while (iter.hasNext()) {
                                Element element = (Element) iter.next();
                                String artifactId = element.attributeValue("artifact_Id");
                                String packageName = element.attributeValue("package");
                                String version = element.attributeValue("version");
                                String description = element.attributeValue("description");
                                String url_description = element.attributeValue("url");
                                String download_url = element.attributeValue("download_url");
                                String license = element.attributeValue("license");
                                String license_url = element.attributeValue("license_url");
                                ModuleToInstall m = new ModuleToInstall();
                                //
                                m.setName(artifactId + "." + packageName);
                                String mvnUri = MavenUrlHelper.MVN_PROTOCOL + groupId + "/" + artifactId + "/" + version;

                                m.setLicenseType(license);
                                m.setLicenseUrl(license_url);
                                m.setDescription(description);
                                m.setUrl_description(url_description);
                                if (download_url == null || "".equals(download_url) || "null".equals(download_url)) {//$NON-NLS-1$
                                    m.setUrl_download(null);
                                } else {
                                    m.setUrl_download(download_url);
                                }
                                setContext(m, contextMap);

                                cache.put(m.getMavenUri(), m);
                            }
                            if (monitor.isCanceled()) {
                                recheckCache = true;
                                return;
                            }
                            monitor.worked(10);
                        }

                    } catch (Exception e1) {
                        ExceptionHandler.process(e1);
                        recheckCache = true;
                    }
                }

                for (String name : jars) {
                    String artifact2Check = name;
                    String key = getMavenUri(artifact2Check);
                    ModuleToInstall moduleToInstall = cache.get(key);
                    if (moduleToInstall != null) {
                        toInstall.add(moduleToInstall);
                    } else {
                        unavailableModules.add(name);
                        ExceptionHandler.log("The download URL for " + name + " is not available");//$NON-NLS-1$//$NON-NLS-2$
                    }
                    monitor.worked(10);
                }

                addUnavailableModulesToToBeInstalledModules(unavailableModules, toInstall, contextMap);
                monitor.done();
            } else {
                // TODO need to remove after nexus server available
                if (cache == null) {
                    cache = new HashMap<String, ModuleToInstall>();
                }
                String[] jars = jarNames.split(SEPARATOR_SLIP);
                int size = jars.length;
                Set<String> unavailableModules = new HashSet<String>();
                monitor.beginTask(Messages.getString("RemoteModulesHelper.fetch.module.info"), size * 10);//$NON-NLS-1$
                // if the network is not valid, all jars are not available.
                boolean networkValid = NetworkUtil.isNetworkValid();
                if (!networkValid) {
                    globalUnavailableModulesToBeRemoved.addAll(Arrays.asList(jars));
                    unavailableModules.addAll(Arrays.asList(jars));
                    messages = Messages.getString("RemoteModulesHelper.offlineMessages"); //$NON-NLS-1$
                    if (!alreadyWarnedAboutConnectionIssue) {
                        log.warn("failed to connect to internet");
                        alreadyWarnedAboutConnectionIssue = true;
                    }// else already warned so do nothing
                } else {
                    try {
                        int index = 0;
                        int limit = 100;
                        while (index < jars.length) {
                            // get block of 100 jars
                            String jarsToCheck = "";
                            while (index < limit && index < jars.length) {
                                jarsToCheck += jars[index];
                                index++;
                                if (index < limit && index < jars.length) {
                                    jarsToCheck += "|";
                                }
                            }
                            limit += 100;
                            JSONObject message = new JSONObject();
                            JSONObject child = new JSONObject();
                            child.put("vaction", "getModules");//$NON-NLS-1$
                            child.put("name", jarsToCheck);//$NON-NLS-1$
                            message.put("module", child);//$NON-NLS-1$
                            String url = serviceUrl + "?data=" + message;
                            monitor.worked(10);
                            if (monitor.isCanceled()) {
                                return;
                            }
                            JSONObject resultStr = readJsonFromUrl(url);
                            if (resultStr != null) {
                                JSONArray jsonArray = resultStr.getJSONArray("result");//$NON-NLS-1$
                                if (jsonArray != null) {
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject obj = jsonArray.getJSONObject(i);
                                        if (obj != null) {
                                            String url_description = obj.getString("url_description");//$NON-NLS-1$
                                            String url_download = obj.getString("url_download");//$NON-NLS-1$
                                            String name = obj.getString("filename");//$NON-NLS-1$
                                            if ((url_description == null && url_download == null)
                                                    || (("".equals(url_description) || "null".equals(url_description)) && (""//$NON-NLS-1$
                                                    .equals(url_download) || "null".equals(url_download)))) {//$NON-NLS-1$
                                                ExceptionHandler.log("The download URL for " + name + " is not available");//$NON-NLS-1$
                                                if (CommonsPlugin.isDebugMode()) {
                                                    appendToLogFile(name + "\n");
                                                }
                                                // keep null in cache no need to check from server again
                                                cache.put(name, null);
                                                globalUnavailableModulesToBeRemoved.add(name);
                                                unavailableModules.add(name);
                                                continue;
                                            }

                                            ModuleToInstall m = new ModuleToInstall();

                                            m.setName(name);
                                            setContext(m, contextMap);
                                            String license = obj.getString("licence");//$NON-NLS-1$
                                            m.setLicenseType(license);
                                            if ("".equals(license) || "null".equals(license)) {//$NON-NLS-1$
                                                m.setLicenseType(null);
                                            }
                                            String description = obj.getString("description");//$NON-NLS-1$
                                            if (description == null || "".equals(description) || "null".equals(description)) {//$NON-NLS-1$
                                                description = m.getName();
                                            }
                                            m.setDescription(description);
                                            m.setUrl_description(url_description);
                                            if (url_download == null || "".equals(url_download) || "null".equals(url_download)) {//$NON-NLS-1$
                                                m.setUrl_download(null);
                                            } else {
                                                m.setUrl_download(url_download);
                                            }
                                            this.toInstall.add(m);
                                            cache.put(m.getName(), m);
                                        }
                                        if (monitor.isCanceled()) {
                                            return;
                                        }
                                        monitor.worked(10);
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        ExceptionHandler.process(e);
                    } catch (IOException e) {
                        ExceptionHandler.process(e);
                    }
                }
                addUnavailableModulesToToBeInstalledModules(unavailableModules, toInstall, contextMap);
                monitor.done();

            }
        }
    }

    private String getMavenUri(String jarOrUrl) {
        if (jarOrUrl != null) {
            if (MavenUrlHelper.isMvnUrl(jarOrUrl)) {
                return jarOrUrl;
            } else {
                String mvnUriFromIndex = LibrariesIndexManager.getInstance().getMvnUriFromIndex(jarOrUrl);
                if (mvnUriFromIndex == null) {
                    ExceptionHandler.log("Warning : the groupid and version in the url may not be correct");
                    mvnUriFromIndex = MavenUrlHelper.generateMvnUrlForJarName(jarOrUrl);
                }
                return mvnUriFromIndex;
            }
        } else {
            return null;
        }
    }

    /**
     * DOC sgandon Comment method "addUnavailableModulesToToBeInstalledModules".
     * 
     * @param unavailableModules
     * @param toInstall2
     * @param contextMap
     */
    protected void addUnavailableModulesToToBeInstalledModules(Set<String> unavailableModules, List<ModuleToInstall> toInstall2,
            Map<String, List<ModuleNeeded>> contextMap) {
        // add all unavailable modules, cause they need to be installed even if the are not available from remote
        // site.
        for (String unavailableModuleName : unavailableModules) {
            ModuleToInstall m = createUnavailableModuleToInstall(unavailableModuleName, contextMap);
            toInstall2.add(m);
        }
    }

    /**
     * DOC sgandon Comment method "createUnavailableModuleToInstall".
     * 
     * @param unavailableModuleName
     * @param contextMap, may be null
     * @return
     */
    private ModuleToInstall createUnavailableModuleToInstall(String unavailableModuleName,
            Map<String, List<ModuleNeeded>> contextMap) {
        ModuleToInstall m = new ModuleToInstall();
        m.setName(unavailableModuleName);
        setContext(m, contextMap);
        if (contextMap != null) {
            m.setDescription(getFirstDescription(contextMap.get(unavailableModuleName)));
        }// there will be no description

        return m;
    }

    /**
     * DOC sgandon Comment method "setContext".
     * 
     * @param m
     * @param contextMap
     */
    protected void setContext(ModuleToInstall m, Map<String, List<ModuleNeeded>> contextMap) {
        if (contextMap != null) {
            List<ModuleNeeded> nm = contextMap.get(m.getName());
            m.setContext(getContext(nm));
            m.setRequired(isRequired(nm));
        } else {
            m.setContext("Current Operation");//$NON-NLS-1$
            m.setRequired(true);
        }
    }

    /**
     * DOC sgandon Comment method "getDescription".
     * 
     * @param nm
     * @return
     */
    private String getFirstDescription(List<ModuleNeeded> neededModules) {
        if (neededModules == null) {
            return ""; //$NON-NLS-1$
        }
        for (ModuleNeeded module : neededModules) {
            if (module.getInformationMsg() != null && !"".equals(module.getInformationMsg())) { //$NON-NLS-1$
                return module.getInformationMsg();
            }
        }
        return ""; //$NON-NLS-1$

    }

    private void appendToLogFile(String logTxt) {
        Path absolutePath = new Path(Platform.getInstallLocation().getURL().getPath());
        File fullLogFile = new File(absolutePath.append("NotAvailableJarsFromWebservice.txt").toPortableString());

        FileWriter writer = null;
        try {
            writer = new FileWriter(fullLogFile, true);
            writer.append(logTxt);
        } catch (IOException e) {
            // nothing
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }

    }

    private static Logger log = Logger.getLogger(RemoteModulesHelper.class);

    private static RemoteModulesHelper helper;

    // private static final String serviceUrl = "http://www.talend.com/TalendRegisterWS/modules.php";

    private static final String serviceUrl = "http://www.talendforge.org/modules/webservices/modules.php";//$NON-NLS-1$

    private static final String SEPARATOR_DISPLAY = " | ";//$NON-NLS-1$

    private static final String SEPARATOR = "|";//$NON-NLS-1$

    private static final String SEPARATOR_SLIP = "\\|";//$NON-NLS-1$

    private MavenResolver mvnResolver;

    private boolean recheckCache = false;

    private Map<String, ModuleToInstall> cache;

    private RemoteModulesHelper() {
        NexusServerBean nexusServer = NexusServerManager.getNexusServer(true);
        mvnResolver = MavenResolverCreator.getInstance().getMavenResolver(nexusServer);
    }

    public synchronized static RemoteModulesHelper getInstance() {
        if (helper == null) {
            helper = new RemoteModulesHelper();
        }
        return helper;
    }

    public void getNotInstalledModules(List<ModuleNeeded> neededModules, List<ModuleToInstall> toInstall,
            IModulesListener listener) {
        getNotInstalledModules(neededModules, toInstall, listener, false);
    }

    public void getNotInstalledModules(List<ModuleNeeded> neededModules, List<ModuleToInstall> toInstall,
            IModulesListener listener, boolean isUser) {
        RemoteModulesFetchRunnable fecthUninstalledModulesRunnable = getNotInstalledModulesRunnable(neededModules, toInstall);
        if (fecthUninstalledModulesRunnable == null) {
            listener.listModulesDone();
            return;
        }

        scheduleJob(fecthUninstalledModulesRunnable, listener, isUser);

    }

    /**
     * return a runnable to fetch remote modules or null if no nothing need be fetched, this means that toInstall array
     * is filled with existing modules found in cache
     * */
    public RemoteModulesFetchRunnable getNotInstalledModulesRunnable(List<ModuleNeeded> neededModules,
            List<ModuleToInstall> toInstall) {
        Map<String, List<ModuleNeeded>> contextMap = new HashMap<String, List<ModuleNeeded>>();

        StringBuffer jars = new StringBuffer();
        for (ModuleNeeded module : neededModules) {
            String moduleName = module.getModuleName().trim();

            if (!contextMap.keySet().contains(moduleName)) {
                List<ModuleNeeded> modules = new ArrayList<ModuleNeeded>();
                modules.add(module);
                contextMap.put(moduleName, modules);
                // only check that, but, it don't check available on site
                if (cache == null || !cache.keySet().contains(moduleName)) {
                    if (jars.length() != 0) {
                        jars.append(SEPARATOR);
                    }
                    jars.append(module.getModuleName());
                }// have checked
            } else {
                contextMap.get(moduleName).add(module);
            }
        }
        String jarNames = jars.toString();
        Set<String> notCachedModulesName = new HashSet<String>();
        // get from cache first
        for (String moduleName : contextMap.keySet()) {
            ModuleToInstall moduleToInstall = null;
            if (cache != null && !cache.isEmpty()) {
                moduleToInstall = cache.get(moduleName);
            }
            if (moduleToInstall != null) {
                List<ModuleNeeded> moduleContext = contextMap.get(moduleName);
                moduleToInstall.setContext(getContext(moduleContext));
                if (moduleContext != null && moduleContext.size() > 0) {
                    for (ModuleNeeded needed : moduleContext) {
                        if (moduleToInstall.getName().equals(needed.getModuleName())) {
                            moduleToInstall.setRequired(needed.isRequired());
                        }
                    }
                }
                toInstall.add(moduleToInstall);
            } else {// else not found in cache
                notCachedModulesName.add(moduleName);
            }
        }

        if (jarNames.isEmpty()) {
            addUnavailableModulesToToBeInstalledModules(notCachedModulesName, toInstall, contextMap);
            return null; // if all have been in cache, no need fetching runnable again.
        }
        // fetch the jars which are not in cache.
        return createRemoteModuleFetchRunnable(jarNames, toInstall, contextMap);

    }

    public void getNotInstalledModules(String[] names, List<ModuleToInstall> toInstall, IModulesListener listener) {
        StringBuffer toInstalljars = new StringBuffer();
        // check that modules are already in cache or not
        if (names != null && names.length > 0) {
            for (String module : names) {
                String moduleName = module.trim();
                ModuleToInstall moduleToInstall = null;
                if (cache != null) {
                    moduleToInstall = cache.get(moduleName);
                }
                if (moduleToInstall != null) { // if not existed, or not available on site.
                    moduleToInstall.setContext("Current Operation");//$NON-NLS-1$
                    toInstall.add(moduleToInstall);
                } else { // not existed
                    if (toInstalljars.length() != 0) {
                        toInstalljars.append(SEPARATOR);
                    }
                    toInstalljars.append(moduleName);
                }
            }
        }

        String toInstallJarNames = toInstalljars.toString();
        if (toInstallJarNames.isEmpty()) {
            listener.listModulesDone();
            return;
        }
        scheduleJob(createRemoteModuleFetchRunnable(toInstallJarNames, toInstall, null), listener, false);

    }

    private synchronized void scheduleJob(final RemoteModulesFetchRunnable runnableWithProgress, final IModulesListener listener,
            boolean isUser) {

        Job job = new Job(Messages.getString("RemoteModulesHelper.job.title")) {//$NON-NLS-1$

            @Override
            protected IStatus run(IProgressMonitor progressMonitor) {
                try {
                    runnableWithProgress.run(progressMonitor);
                } catch (InvocationTargetException e) {
                    log.warn("fetching remote Modules data failed", e); //$NON-NLS-1$
                    return Status.CANCEL_STATUS;
                } catch (InterruptedException e) {
                    log.warn("fetching remote Modules data failed", e); //$NON-NLS-1$
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        job.addJobChangeListener(new JobChangeAdapter() {

            @Override
            public void done(IJobChangeEvent event) {
                listener.listModulesDone();
            }
        });

        job.setUser(isUser);
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    /**
     * DOC sgandon Comment method "createRemoteModuleFetchRunnable".
     * 
     * @param jarNames
     * @param toInstall
     * @param contextMap
     * @return
     */
    public RemoteModulesFetchRunnable createRemoteModuleFetchRunnable(final String jarNames,
            final List<ModuleToInstall> toInstall, final Map<String, List<ModuleNeeded>> contextMap) {
        return new RemoteModulesFetchRunnable(jarNames, toInstall, contextMap);
    }

    private JSONObject readJsonFromUrl(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        String jsonText = "";
        JSONObject json = null;
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));//$NON-NLS-1$
            jsonText = readAll(rd);
            json = new JSONObject(jsonText);
        } catch (Exception e) {
            ExceptionHandler.process(new Exception(Messages.getString("RemoteModulesHelper.readJsonFromUrl.error")));
        } finally {
            is.close();
        }
        return json;
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private String getContext(List<ModuleNeeded> neededModules) {
        if (neededModules == null) {
            return "";
        }
        StringBuffer context = new StringBuffer();
        for (ModuleNeeded module : neededModules) {
            if (context.length() != 0) {
                context.append(SEPARATOR_DISPLAY);
            }
            context.append(module.getContext());
        }
        return context.toString();

    }

    private boolean isRequired(List<ModuleNeeded> neededModules) {
        if (neededModules == null) {
            return false;
        }
        boolean isRequired = false;
        for (ModuleNeeded module : neededModules) {
            isRequired = isRequired | module.isRequired();
            if (isRequired) {
                return isRequired;
            }
        }
        return isRequired;
    }

    public String getLicenseUrl(String licenseType) {
        JSONObject message = new JSONObject();
        try {
            JSONObject child = new JSONObject();
            child.put("vaction", "getLicense");//$NON-NLS-1$
            child.put("label", licenseType);//$NON-NLS-1$
            message.put("module", child);//$NON-NLS-1$
            String url = serviceUrl + "?data=" + message;
            JSONObject resultStr = readJsonFromUrl(url);
            JSONArray jsonArray = resultStr.getJSONArray("result");//$NON-NLS-1$
            if (jsonArray != null) {
                JSONObject object = jsonArray.getJSONObject(0);
                if (object != null) {
                    return object.getString("licenseText");//$NON-NLS-1$
                }
            }
        } catch (JSONException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public List<ModuleToInstall> getTest() {
        List<ModuleToInstall> toInstall = new ArrayList<ModuleToInstall>();
        ModuleToInstall m1 = new ModuleToInstall();
        m1 = new ModuleToInstall();
        m1.setName("jtds-1.2.5.jar");
        m1.setContext("tMysqlInput | tMysqlOutput");
        m1.setDescription("Mysql Driver");
        m1.setUrl_description("http://jtds.sourceforge.net/");
        m1.setUrl_download(null);
        m1.setLicenseType("LGPL_v3");
        toInstall.add(m1);
        m1 = new ModuleToInstall();
        m1 = new ModuleToInstall();
        m1.setName("test.jar");
        m1.setContext("tMysqalInput | tMysfqlOutput");
        m1.setDescription("testaaaaa");
        m1.setUrl_description("http://jtds.sourceforge.net/");
        m1.setUrl_download(null);
        m1.setLicenseType("LGPL_v3");
        toInstall.add(m1);
        return toInstall;
    }

    /**
     * DOC sgandon Comment method "getNotInstalledModulesRunnable".
     * 
     * @param requiredJars, list of the jars that are required
     * @param toInstall, list to be filled with modules definition
     * @return Runnable that will resolve the modules from distant update site or null if all modules are laready in
     * cache
     */
    public IRunnableWithProgress getNotInstalledModulesRunnable(String[] requiredJars, List<ModuleToInstall> toInstall) {
        StringBuffer toInstallJars = new StringBuffer();
        // check that modules are already in cache or not
        if (requiredJars != null && requiredJars.length > 0) {
            for (String module : requiredJars) {
                String moduleName = module.trim();
                ModuleToInstall moduleToInstall = null;
                if (cache != null) {
                    moduleToInstall = cache.get(moduleName);
                }
                if (moduleToInstall != null) {
                    moduleToInstall.setContext("Current Operation");//$NON-NLS-1$
                    toInstall.add(moduleToInstall);
                } else { // not existed, or not available on site.
                    if (toInstallJars.length() != 0) {
                        toInstallJars.append(SEPARATOR);
                    }
                    toInstallJars.append(moduleName);
                }
            }
        }
        String toInstallJarNames = toInstallJars.toString();

        if (toInstallJarNames.isEmpty()) {
            return null;
        } else {
            return createRemoteModuleFetchRunnable(toInstallJarNames, toInstall, null);
        }
    }

    public String getLicenseContentByUrl(String licenseUrl) {
        if (licenseUrl != null && licenseUrl.length() > 0) {
            try {
                URL url = new URL(licenseUrl);
                URLConnection urlConnection = url.openConnection();
                Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
                if (headerFields != null) {
                    List<String> contentType = headerFields.get("Content-Type"); //$NON-NLS-1$
                    if (contentType != null) {
                        if (contentType.contains("text/plain")) { //$NON-NLS-1$
                            // Get the plain text from connection.
                            InputStream inputStream = urlConnection.getInputStream();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream(500);
                            byte[] b = new byte[1024];
                            int len;
                            while ((len = inputStream.read(b)) != -1) {
                                baos.write(b, 0, len);
                            }
                            inputStream.close();
                            return baos.toString();

                            // } else if (contentType.contains("text/html")) {
                            // return url too.
                        }
                    }
                }
                return licenseUrl; // else, just return the URL.
            } catch (MalformedURLException e) {
                //
            } catch (IOException e) {
                //
            }
        }
        return null;
    }

    /**
     * Getter for mvnResolver.
     * 
     * @return the mvnResolver
     */
    public MavenResolver getMvnResolver() {
        return this.mvnResolver;
    }
}
