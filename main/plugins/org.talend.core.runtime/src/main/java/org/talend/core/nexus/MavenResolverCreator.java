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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.ExceptionHandler;

/**
 * created by wchen on Apr 24, 2015 Detailled comment
 *
 */
public class MavenResolverCreator {

    private static MavenResolverCreator creator;

    private MavenResolverCreator() {

    }

    public static synchronized MavenResolverCreator getInstance() {
        if (creator == null) {
            creator = new MavenResolverCreator();
        }
        return creator;
    }

    public MavenResolver getMavenResolver(Dictionary<String, String> properties) {
        return MavenResolvers.createMavenResolver(properties, ServiceConstants.PID);
    }

    public MavenResolver getMavenResolver(NexusServerBean nexusServer) {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        if (nexusServer != null) {
            String nexusUrl = nexusServer.getServer();
            if (nexusUrl.endsWith(NexusConstants.SLASH)) {
                nexusUrl = nexusUrl.substring(0, nexusUrl.length() - 1);
            }
            String urlSeparator = "://";//$NON-NLS-1$
            String[] split = nexusUrl.split(urlSeparator);
            if (split.length != 2) {
                ExceptionHandler.process(new BusinessException("Nexus url is not valid ,please contract the administrator"));
            }
            String newUrl = split[0]
                    + ":" + nexusServer.getUserName() + ":" + nexusServer.getPassword() + "@//" + split[1] + NexusConstants.CONTENT_REPOSITORIES + nexusServer.getRepositoryId()//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                    + "@id=" + nexusServer.getRepositoryId();//$NON-NLS-1$
            properties.put("org.ops4j.pax.url.mvn.repositories", newUrl);//$NON-NLS-1$
        }
        return MavenResolvers.createMavenResolver(properties, ServiceConstants.PID);
    }

    public String setupMavenWithNexus(String url, String userName, String password, String repId) {
        // return the repository id configured in the settings.xml
        String repositoryIdToReture = repId;
        try {
            if (url == null || userName == null || password == null) {
                throw new MavenSetupException("Please specifiy a nexus url to setup");
            }
            String userConfigPath = System.getProperty("user.home") + "/.m2/settings.xml";
            boolean modified = true;
            String nexusUrl = url.trim();

            URL settingsUrl = getSettingsFileUrl();

            Document document = new SAXReader(false).read(new File(settingsUrl.getPath()));
            Element root = document.getRootElement();
            Element profiles = getOrCreateElement(root, "profiles");

            Element activeProf = getOrCreateElement(root, "activeProfiles");
            List<String> activeProfileIds = getSubElementValues(activeProf, "activeProfile");

            Element servers = getOrCreateElement(root, "servers");

            Set<String> existingProIds = new HashSet<String>();
            Set<String> existingRepIds = new HashSet<String>();

            if (profiles != null) {
                Iterator profileIter = profiles.elementIterator("profile");
                String profileId = null;
                String repositoryId = null;
                boolean found = false;
                while (profileIter.hasNext()) {
                    Element profile = (Element) profileIter.next();
                    Element pIdElement = profile.element("id");
                    if (pIdElement != null) {
                        profileId = pIdElement.getText();
                    }
                    Element repositories = profile.element("repositories");
                    if (repositories != null) {
                        Iterator repIter = repositories.elementIterator("repository");
                        while (repIter.hasNext()) {
                            Element repository = (Element) repIter.next();
                            Element repIdElement = repository.element("id");
                            if (repIdElement != null) {
                                repositoryId = repIdElement.getText();
                                existingRepIds.add(repositoryId);
                            }
                            Element urlElement = repository.element("url");
                            if (urlElement != null) {
                                String repUrl = urlElement.getText().trim();
                                if (nexusUrl.equals(repUrl)) {
                                    repositoryIdToReture = repositoryId;
                                    found = true;
                                    modified = false;
                                    break;
                                }
                            }
                        }

                    }
                }
                if (found) {
                    // recheck active profile
                    if (!activeProfileIds.contains(profileId)) {
                        addElement(activeProf, "activeProfile", profileId);
                    }
                } else {
                    // create profile
                    repositoryIdToReture = createProfile(profiles, servers, activeProf, existingProIds, existingRepIds, url,
                            userName, password, repId);
                }
            }
            if (modified) {
                OutputFormat format = OutputFormat.createPrettyPrint();
                format.setEncoding("UTF-8");
                XMLWriter writer = null;
                try {
                    writer = new XMLWriter(new FileWriter(userConfigPath), format);
                    writer.write(document);
                    writer.close();
                } catch (IOException e) {
                    ExceptionHandler.process(e);
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            ExceptionHandler.process(e);
                        }
                    }
                }

            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }

        return repositoryIdToReture;
    }

    private String createProfile(Element profiles, Element servers, Element activeProfiles, Set<String> exsitProfIds,
            Set<String> existRepIds, String server_url, String user, String password, String repositoryId) {
        Element profile = profiles.addElement("profile");
        // profile id
        Element profIdElement = profile.addElement("id");
        String profileId = getValidId(exsitProfIds, repositoryId);
        profIdElement.setText(profileId);

        Element repositories = profile.addElement("repositories");
        Element repository = repositories.addElement("repository");
        // repository id
        Element repIdElement = repository.addElement("id");
        String repId = getValidId(existRepIds, repositoryId);
        repIdElement.setText(repId);
        // url
        Element urlElement = repository.addElement("url");
        urlElement.setText(server_url);

        Element releases = repository.addElement("releases");
        Element r_enabled = releases.addElement("enabled");
        r_enabled.setText("true");

        Element snapshots = repository.addElement("snapshots");
        Element s_enabled = snapshots.addElement("enabled");
        s_enabled.setText("true");

        // add to active profiles
        Element activeProfile = activeProfiles.addElement("activeProfile");
        activeProfile.setText(profileId);
        // add server
        Element server = servers.addElement("server");
        Element serverId = server.addElement("id");
        serverId.setText(repId);
        Element serverUser = server.addElement("username");
        serverUser.setText(user);
        Element serverPass = server.addElement("password");
        serverPass.setText(password);

        return repId;
    }

    private String getValidId(Set<String> existingId, String id) {
        String generatedId = id;
        if (!existingId.contains(id)) {
            return generatedId;
        } else {
            return getValidId(existingId, id + "_1");
        }
    }

    private List<String> getSubElementValues(Element parent, String elemName) {
        Iterator elementIterator = parent.elementIterator(elemName);
        List<String> values = new ArrayList<String>();
        while (elementIterator.hasNext()) {
            Element type = (Element) elementIterator.next();
            values.add(type.getText());
        }
        return values;
    }

    private void addElement(Element pElement, String qName, String value) {
        Element addElement = pElement.addElement(qName);
        if (value != null) {
            addElement.setText(value);
        }
    }

    private Element getOrCreateElement(Element pElement, String qName) {
        Element element = pElement.element(qName);
        if (element == null) {
            element = pElement.addElement(qName);
        }
        return element;
    }

    private URL getSettingsFileUrl() throws MavenSetupException {
        String spec = null;
        if (spec == null) {
            spec = safeGetFile(System.getProperty("user.home") + "/.m2/settings.xml");
        }
        if (spec == null) {
            spec = safeGetFile(System.getProperty("maven.home") + "/conf/settings.xml");
        }
        if (spec == null) {
            spec = safeGetFile(System.getenv("M2_HOME") + "/conf/settings.xml");
        }
        if (spec != null) {
            try {
                return new URL(spec);
            } catch (MalformedURLException e) {
                throw new MavenSetupException("Settings file [" + spec
                        + "] cannot be used and will be skipped (malformed url or file does not exist)");
            }
        } else {
            throw new MavenSetupException("Maven settings file can not be found ,please check if maven correctly installed");
        }
    }

    private String safeGetFile(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists() && file.canRead() && file.isFile()) {
                try {
                    return file.toURI().toURL().toExternalForm();
                } catch (MalformedURLException e) {
                    // Ignore
                }
            }
        }
        return null;
    }

    // public static void main(String[] args) {
    // MavenResolverCreator test = new MavenResolverCreator();
    // String url = "http://localhost:8081/nexus/content/repositories/org.talend.libraries/";
    // String userName = "admin";
    // String password = "admin123";
    // try {
    // test.setupMavenWithNexus(url, userName, password);
    // } catch (MavenSetupException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (DocumentException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }
}
