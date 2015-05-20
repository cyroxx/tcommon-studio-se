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
package org.talend.osgi.hook;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.talend.osgi.hook.notification.JarMissingObservable;
import org.talend.osgi.hook.notification.JarMissingObservable.JarMissingEvent;

/**
 * created by sgandon on 4 févr. 2015 Detailled comment
 *
 */
public class OsgiLoaderTest {

    /**
     * 
     */
    private static final String EXISTING_JAR_NAME = "any-existing.jar"; //$NON-NLS-1$

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Bundle osgiBundle;

    public static final String FRAGMENT_WITH_LIB = "test.lib.fragment.with.existing.jar"; //$NON-NLS-1$

    private static String FRAGMENT_WITH_LIB_FOLDER_URL;

    public static final String BUNDLE_NO_LIB = "test.lib.plugin.with.no.jar"; //$NON-NLS-1$

    private static String BUNDLE_NO_LIB_FOLDER_URL;

    public static final String BUNDLE_CALLING_LIB = "test.call.plugin.lib.jar"; //$NON-NLS-1$

    private static String BUNDLE_CALLING_LIB_FOLDER_URL;

    final File libJavaFolderFile = MissingJarServices.getLibJavaFolderFile();

    @BeforeClass
    public static void staticInit() throws IOException {
        FRAGMENT_WITH_LIB_FOLDER_URL = FileLocator
                .toFileURL(OsgiLoaderTest.class.getResource("/plugins/" + FRAGMENT_WITH_LIB)).toExternalForm(); //$NON-NLS-1$
        BUNDLE_NO_LIB_FOLDER_URL = FileLocator
                .toFileURL(OsgiLoaderTest.class.getResource("/plugins/" + BUNDLE_NO_LIB)).toExternalForm(); //$NON-NLS-1$
        BUNDLE_CALLING_LIB_FOLDER_URL = FileLocator
                .toFileURL(OsgiLoaderTest.class.getResource("/plugins/" + BUNDLE_CALLING_LIB)).toExternalForm(); //$NON-NLS-1$
    }

    @Before
    public void init() throws IOException {
        // we use osgi bundle because the context of this bundle may not be created
        this.osgiBundle = Platform.getBundle("org.eclipse.osgi"); //$NON-NLS-1$
        // remote any file copied in the lib folder
        FileUtils.deleteDirectory(libJavaFolderFile);
        libJavaFolderFile.mkdirs();
    }

    @Test
    public void CheckFindingExistingJar() throws IOException, BundleException {
        Bundle fragmentBundle = osgiBundle.getBundleContext().installBundle(FRAGMENT_WITH_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        try {
            bundle.start();// I am not sure to understand why we need to start this bundle.
            // check finding of existing jar in fragment
            assertNotNull(bundle);
            URL jarURL = FileLocator.find(bundle, new Path("lib/any-existing.jar"), null); //$NON-NLS-1$ 
            checkFileExistsOrNot(jarURL, true);
        } finally {
            fragmentBundle.uninstall();
            bundle.uninstall();
        }
        bundle = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundle);
        fragmentBundle = Platform.getBundle(FRAGMENT_WITH_LIB);
        assertNull(fragmentBundle);
    }

    /**
     * DOC sgandon Comment method "checkFileExists".
     * 
     * @param jarURL
     * @param shouldExist
     * @throws IOException
     */
    public void checkFileExistsOrNot(URL jarURL, boolean shouldExist) throws IOException {
        URL fileURL = FileLocator.toFileURL(jarURL);
        File jarFile = new File(fileURL.getFile());
        if (shouldExist) {
            assertTrue("file should be found", jarFile.exists()); //$NON-NLS-1$
        } else {
            assertFalse("file should NOT be found", jarFile.exists()); //$NON-NLS-1$
        }
    }

    @Test
    public void CheckFindingExistingJarUsingFindEntries() throws IOException, BundleException {
        Bundle fragmentBundle = osgiBundle.getBundleContext().installBundle(FRAGMENT_WITH_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        try {
            bundle.start();// I am not sure to understand why we need to start this bundle.
            // check finding of existing jar in fragment
            assertNotNull(bundle);
            URL[] jarURL = FileLocator.findEntries(bundle, new Path("lib/any-existing.jar")); //$NON-NLS-1$ 
            assertEquals("There should be only one URL", 1, jarURL.length); //$NON-NLS-1$
            URL fileURL = FileLocator.toFileURL(jarURL[0]);
            File jarFile = new File(fileURL.getFile());
            assertTrue("file should be found", jarFile.exists()); //$NON-NLS-1$
        } finally {
            fragmentBundle.uninstall();
            bundle.uninstall();
        }
        bundle = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundle);
        fragmentBundle = Platform.getBundle(FRAGMENT_WITH_LIB);
        assertNull(fragmentBundle);
    }

    @Test
    public void CheckFindingExistingJarUsingFindEntriesDoesNotTriggerMissingJarObservable() throws IOException, BundleException {
        final Boolean[] observerCalled = new Boolean[1];
        observerCalled[0] = false;
        Observer observer = new Observer() {

            @Override
            public void update(Observable o, Object arg) {
                if (arg != null && arg instanceof JarMissingEvent) {
                    JarMissingEvent jarMissingEvent = (JarMissingEvent) arg;
                    String jarName = jarMissingEvent.getJarName();
                    if (jarName.equals(EXISTING_JAR_NAME)) {
                        observerCalled[0] = true;
                    } else {
                        throw new RuntimeException("Missing jar observer did not receive the expected jar name:" + jarName + "!=" //$NON-NLS-1$
                                + EXISTING_JAR_NAME);
                    }
                } else {// notification is not expected so thrown an exception for the test to fail
                    throw new RuntimeException("Missing jar observer did not receive the proper event :" + arg);
                }
            }
        };

        setupMissingJarLoadingObserver(observer);

        Bundle fragmentBundle = osgiBundle.getBundleContext().installBundle(FRAGMENT_WITH_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        try {
            bundle.start();// I am not sure to understand why we need to start this bundle.
            // check finding of existing jar in fragment
            assertNotNull(bundle);
            FileLocator.find(bundle, new Path("lib/any-existing.jar"), null); //$NON-NLS-1$ 
            assertFalse("Observer should never be called", observerCalled[0]); //$NON-NLS-1$
        } finally {
            fragmentBundle.uninstall();
            bundle.uninstall();
            unsetupMissingJarLoadingObserver(observer);
        }
        bundle = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundle);
        fragmentBundle = Platform.getBundle(FRAGMENT_WITH_LIB);
        assertNull(fragmentBundle);
    }

    @Test
    public void CheckAccessinClassInFragmentLib() throws IOException, BundleException {
        Bundle fragmentBundle = osgiBundle.getBundleContext().installBundle(FRAGMENT_WITH_LIB_FOLDER_URL);
        Bundle bundleNoJar = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_CALLING_LIB_FOLDER_URL);
        try {
            // this should fail if the Activator fails to load the class from the fragment lib
            bundle.start();
            // no assert here because the above should fail in cause of error.
        } finally {
            bundle.uninstall();
            bundleNoJar.uninstall();
            fragmentBundle.uninstall();
        }
        bundle = Platform.getBundle(BUNDLE_CALLING_LIB);
        assertNull(bundle);
        bundleNoJar = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundleNoJar);
        fragmentBundle = Platform.getBundle(FRAGMENT_WITH_LIB);
        assertNull(fragmentBundle);
    }

    @Test()
    public void CheckFailAccessinClassInFragmentLib() throws IOException, BundleException, SecurityException,
            IllegalArgumentException, ClassNotFoundException, InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Bundle bundleNoJar = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_CALLING_LIB_FOLDER_URL);
        try {
            // this should fail if the Activator fails to load the class from the fragment lib
            try {
                // this forces the bundle classloader to be initialised
                bundleNoJar.getResource("foo"); //$NON-NLS-1$
                callBundleMethodToCallLibApi(bundle);
                fail("starting bundle should have thrown an exception."); //$NON-NLS-1$
            } catch (InvocationTargetException be) {
                if (!(be.getCause() instanceof NoClassDefFoundError)) {
                    throw be;
                }// else do nothing cause this is an expected exception
            }
            // no assert here because the above should fail in case of error.
        } finally {
            bundle.uninstall();
            bundleNoJar.uninstall();
        }
        bundle = Platform.getBundle(BUNDLE_CALLING_LIB);
        assertNull(bundle);
        bundleNoJar = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundleNoJar);
    }

    @Test
    public void CheckNoExistingJarClassLoadGeneratesMissingJarObservableCallButNoJarReturned() throws IOException,
            BundleException, SecurityException, IllegalArgumentException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final Boolean[] observerCalled = new Boolean[1];
        observerCalled[0] = false;
        Observer observer = new Observer() {

            @Override
            public void update(Observable o, Object arg) {
                observerCalled[0] = true;
            }
        };
        setupMissingJarLoadingObserver(observer);
        Bundle bundleNoJar = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_CALLING_LIB_FOLDER_URL);
        try {
            try {
                // this forces the bundle classloader to be initialised
                bundleNoJar.getResource("foo"); //$NON-NLS-1$
                callBundleMethodToCallLibApi(bundle);
                fail("Bundle start should have thrown an exception cause no lib is provided"); //$NON-NLS-1$
            } catch (InvocationTargetException be) {
                if (!(be.getCause() instanceof NoClassDefFoundError)) {
                    throw be;
                }// else do nothing cause this is an expected exception
            }
            // check finding of existing jar in fragment
            assertTrue(observerCalled[0]);

        } finally {
            bundle.uninstall();
            bundleNoJar.uninstall();
            unsetupMissingJarLoadingObserver(observer);
        }
        bundle = Platform.getBundle(BUNDLE_CALLING_LIB);
        assertNull(bundle);
        bundleNoJar = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundleNoJar);
    }

    @Test
    public void CheckNoExistingJarClassLoadGeneratesMissingJarObservableCallAndJarReturned() throws IOException, BundleException,
            SecurityException, IllegalArgumentException, ClassNotFoundException, InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        final Boolean[] observerCalled = new Boolean[1];
        observerCalled[0] = false;
        Observer observer = new Observer() {

            @Override
            public void update(Observable o, Object arg) {
                if (arg != null && arg instanceof JarMissingEvent) {
                    JarMissingEvent jarMissingEvent = (JarMissingEvent) arg;
                    String jarName = jarMissingEvent.getJarName();
                    if (jarName.equals(EXISTING_JAR_NAME)) {

                        try {
                            File sourceFile = new File(new URI(FRAGMENT_WITH_LIB_FOLDER_URL + "lib/any-existing.jar")); //$NON-NLS-1$
                            File desctinationFile = new File(libJavaFolderFile, jarName);
                            try {
                                FileUtils.copyFile(sourceFile, desctinationFile);
                            } catch (IOException e) {
                                throw new RuntimeException("failed to copy jar", e); //$NON-NLS-1$
                            }
                            observerCalled[0] = true;
                        } catch (URISyntaxException e1) {
                            throw new RuntimeException(e1);
                        }
                    } else {
                        throw new RuntimeException("Missing jar observer did not receive the expected jar name:" + jarName + "!=" //$NON-NLS-1$
                                + EXISTING_JAR_NAME);
                    }
                } else {// notification is not expected so thrown an exception for the test to fail
                    throw new RuntimeException("Missing jar observer did not receive the proper event :" + arg);
                }
            }
        };
        setupMissingJarLoadingObserver(observer);
        Bundle bundleNoJar = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_CALLING_LIB_FOLDER_URL);
        try {
            // this forces the bundle classloader to be initialised
            bundleNoJar.getResource("foo"); //$NON-NLS-1$
            callBundleMethodToCallLibApi(bundle);
            assertTrue(observerCalled[0]);

        } finally {
            bundle.uninstall();
            bundleNoJar.uninstall();
            unsetupMissingJarLoadingObserver(observer);
        }
        bundle = Platform.getBundle(BUNDLE_CALLING_LIB);
        assertNull(bundle);
        bundleNoJar = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundleNoJar);
    }

    @Test
    public void CheckNoExistingJarClassLoadGeneratesMissingJarObservableCallAndJarReturnedLater() throws IOException,
            BundleException, ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException,
            NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        final Boolean[] observerCalled = new Boolean[1];
        observerCalled[0] = false;
        Observer observer = new Observer() {

            @Override
            public void update(Observable o, Object arg) {
                if (arg != null && arg instanceof JarMissingEvent) {
                    JarMissingEvent jarMissingEvent = (JarMissingEvent) arg;
                    String jarName = jarMissingEvent.getJarName();
                    if (jarName.equals(EXISTING_JAR_NAME)) {
                        try {
                            File sourceFile = new File(new URI(FRAGMENT_WITH_LIB_FOLDER_URL + "lib/any-existing.jar")); //$NON-NLS-1$
                            File desctinationFile = new File(libJavaFolderFile, jarName);
                            try {
                                FileUtils.copyFile(sourceFile, desctinationFile);
                            } catch (IOException e) {
                                throw new RuntimeException("failed to copy jar", e); //$NON-NLS-1$
                            }
                            observerCalled[0] = true;
                        } catch (URISyntaxException e1) {
                            throw new RuntimeException(e1);
                        }

                    } else {
                        throw new RuntimeException("Missing jar observer did not receive the expected jar name:" + jarName + "!=" //$NON-NLS-1$
                                + EXISTING_JAR_NAME);
                    }
                } else {// notification is not expected so thrown an exception for the test to fail
                    throw new RuntimeException("Missing jar observer did not receive the proper event :" + arg);
                }
            }
        };
        Bundle bundleNoJar = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_CALLING_LIB_FOLDER_URL);
        try {
            // this forces the bundle classloader to be initialised
            bundleNoJar.getResource("foo"); //$NON-NLS-1$
            URL jarURL = FileLocator.find(bundle, new Path("lib/any-existing.jar"), null); //$NON-NLS-1$
            checkFileExistsOrNot(jarURL, false);

            setupMissingJarLoadingObserver(observer);
            callBundleMethodToCallLibApi(bundle);
            assertTrue(observerCalled[0]);

        } finally {
            bundle.uninstall();
            bundleNoJar.uninstall();
            unsetupMissingJarLoadingObserver(observer);
        }
        bundle = Platform.getBundle(BUNDLE_CALLING_LIB);
        assertNull(bundle);
        bundleNoJar = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundleNoJar);
    }

    @Test
    public void CheckNoExistingJarClassLoadGeneratesMissingJarObservableCallAndJarReturnedLaterAfter2Find() throws IOException,
            BundleException, ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException,
            NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        final Boolean[] observerCalled = new Boolean[1];
        observerCalled[0] = false;
        Observer observer = new Observer() {

            @Override
            public void update(Observable o, Object arg) {
                if (arg != null && arg instanceof JarMissingEvent) {
                    JarMissingEvent jarMissingEvent = (JarMissingEvent) arg;
                    String jarName = jarMissingEvent.getJarName();
                    if (jarName.equals(EXISTING_JAR_NAME)) {
                        try {
                            File sourceFile = new File(new URI(FRAGMENT_WITH_LIB_FOLDER_URL + "lib/any-existing.jar")); //$NON-NLS-1$
                            File desctinationFile = new File(libJavaFolderFile, jarName);
                            try {
                                FileUtils.copyFile(sourceFile, desctinationFile);
                            } catch (IOException e) {
                                throw new RuntimeException("failed to copy jar", e); //$NON-NLS-1$
                            }
                            observerCalled[0] = true;
                        } catch (URISyntaxException e1) {
                            throw new RuntimeException(e1);
                        }
                    } else {
                        throw new RuntimeException("Missing jar observer did not receive the expected jar name:" + jarName + "!=" //$NON-NLS-1$
                                + EXISTING_JAR_NAME);
                    }
                } else {// notification is not expected so thrown an exception for the test to fail
                    throw new RuntimeException("Missing jar observer did not receive the proper event :" + arg);
                }
            }
        };
        Bundle bundleNoJar = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_CALLING_LIB_FOLDER_URL);
        try {
            // this forces the bundle classloader to be initialised
            bundleNoJar.getResource("foo"); //$NON-NLS-1$
            URL jarURL = FileLocator.find(bundleNoJar, new Path("lib/any-existing.jar"), null); //$NON-NLS-1$
            checkFileExistsOrNot(jarURL, false);
            jarURL = FileLocator.find(bundleNoJar, new Path("lib/any-existing.jar"), null); //$NON-NLS-1$
            checkFileExistsOrNot(jarURL, false);

            setupMissingJarLoadingObserver(observer);
            callBundleMethodToCallLibApi(bundle);
            assertTrue(observerCalled[0]);

        } finally {
            bundle.uninstall();
            bundleNoJar.uninstall();
            unsetupMissingJarLoadingObserver(observer);
        }
        bundle = Platform.getBundle(BUNDLE_CALLING_LIB);
        assertNull(bundle);
        bundleNoJar = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundleNoJar);
    }

    @Test
    public void CheckFindMissingJarObservableCallAndJarReturnedLaterAfter() throws IOException, BundleException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {
        final Boolean[] observerCalled = new Boolean[1];
        observerCalled[0] = false;
        Observer observer = new Observer() {

            @Override
            public void update(Observable o, Object arg) {
                if (arg != null && arg instanceof JarMissingEvent) {
                    JarMissingEvent jarMissingEvent = (JarMissingEvent) arg;
                    String jarName = jarMissingEvent.getJarName();
                    if (jarName.equals(EXISTING_JAR_NAME)) {
                        try {
                            File sourceFile = new File(new URI(FRAGMENT_WITH_LIB_FOLDER_URL + "lib/any-existing.jar")); //$NON-NLS-1$
                            File desctinationFile = new File(libJavaFolderFile, jarName);
                            try {
                                FileUtils.copyFile(sourceFile, desctinationFile);
                            } catch (IOException e) {
                                throw new RuntimeException("failed to copy jar", e); //$NON-NLS-1$
                            }
                            observerCalled[0] = true;
                        } catch (URISyntaxException e1) {
                            throw new RuntimeException(e1);
                        }
                    } else {
                        throw new RuntimeException("Missing jar observer did not receive the expected jar name:" + jarName + "!=" //$NON-NLS-1$
                                + EXISTING_JAR_NAME);
                    }
                } else {// notification is not expected so thrown an exception for the test to fail
                    throw new RuntimeException("Missing jar observer did not receive the proper event :" + arg);
                }
            }
        };
        Bundle bundleNoJar = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_CALLING_LIB_FOLDER_URL);
        try {
            // this forces the bundle classloader to be initialised
            bundleNoJar.getResource("foo"); //$NON-NLS-1$
            URL jarURL = FileLocator.find(bundleNoJar, new Path("lib/any-existing.jar"), null); //$NON-NLS-1$
            checkFileExistsOrNot(jarURL, false);
            setupMissingJarLoadingObserver(observer);
            jarURL = FileLocator.find(bundleNoJar, new Path("lib/any-existing.jar"), null); //$NON-NLS-1$
            checkFileExistsOrNot(jarURL, true);

            assertTrue(observerCalled[0]);

        } finally {
            bundle.uninstall();
            bundleNoJar.uninstall();
            unsetupMissingJarLoadingObserver(observer);
        }
        bundle = Platform.getBundle(BUNDLE_CALLING_LIB);
        assertNull(bundle);
        bundleNoJar = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundleNoJar);
    }

    @Test
    public void CheckInvokeMissingJarMethodAfterFindJarFailed() throws IOException, BundleException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException,
            InvocationTargetException {
        final Boolean[] observerCalled = new Boolean[1];
        observerCalled[0] = false;
        Observer observer = new Observer() {

            @Override
            public void update(Observable o, Object arg) {
                if (arg != null && arg instanceof JarMissingEvent) {
                    JarMissingEvent jarMissingEvent = (JarMissingEvent) arg;
                    String jarName = jarMissingEvent.getJarName();
                    if (jarName.equals(EXISTING_JAR_NAME)) {
                        try {
                            File sourceFile = new File(new URI(FRAGMENT_WITH_LIB_FOLDER_URL + "lib/any-existing.jar")); //$NON-NLS-1$
                            File desctinationFile = new File(libJavaFolderFile, jarName);
                            try {
                                FileUtils.copyFile(sourceFile, desctinationFile);
                            } catch (IOException e) {
                                throw new RuntimeException("failed to copy jar", e); //$NON-NLS-1$
                            }
                            observerCalled[0] = true;
                        } catch (URISyntaxException e1) {
                            throw new RuntimeException(e1);
                        }
                    } else {
                        throw new RuntimeException("Missing jar observer did not receive the expected jar name:" + jarName + "!=" //$NON-NLS-1$
                                + EXISTING_JAR_NAME);
                    }
                } else {// notification is not expected so thrown an exception for the test to fail
                    throw new RuntimeException("Missing jar observer did not receive the proper event :" + arg);
                }
            }
        };
        Bundle bundleNoJar = osgiBundle.getBundleContext().installBundle(BUNDLE_NO_LIB_FOLDER_URL);
        Bundle bundle = osgiBundle.getBundleContext().installBundle(BUNDLE_CALLING_LIB_FOLDER_URL);
        try {
            // this forces the bundle classloader to be initialised
            bundleNoJar.getResource("foo"); //$NON-NLS-1$
            URL jarURL = FileLocator.find(bundleNoJar, new Path("lib/any-existing.jar"), null); //$NON-NLS-1$
            checkFileExistsOrNot(jarURL, false);
            setupMissingJarLoadingObserver(observer);
            callBundleMethodToCallLibApi(bundle);
            assertTrue(observerCalled[0]);

        } finally {
            bundle.uninstall();
            bundleNoJar.uninstall();
            unsetupMissingJarLoadingObserver(observer);
        }
        bundle = Platform.getBundle(BUNDLE_CALLING_LIB);
        assertNull(bundle);
        bundleNoJar = Platform.getBundle(BUNDLE_NO_LIB);
        assertNull(bundleNoJar);
    }

    /**
     * DOC sgandon Comment method "unsetupMissingJarLoadingObserver".
     * 
     * @param observer
     */
    private void unsetupMissingJarLoadingObserver(Observer observer) {
        BundleContext bundleContext = osgiBundle.getBundleContext();
        @SuppressWarnings("unchecked")
        ServiceReference<JarMissingObservable> serviceReference = (ServiceReference<JarMissingObservable>) bundleContext
                .getServiceReference(JarMissingObservable.class.getCanonicalName());
        JarMissingObservable missingJarObservable = bundleContext.getService(serviceReference);
        missingJarObservable.deleteObserver(observer);
    }

    /**
     * looks for the OSGI service that notify that a jar is missing when loading a bundle and register this as a
     * listener
     */
    @SuppressWarnings("restriction")
    private void setupMissingJarLoadingObserver(Observer observer) {
        BundleContext bundleContext = osgiBundle.getBundleContext();
        @SuppressWarnings("unchecked")
        ServiceReference<JarMissingObservable> serviceReference = (ServiceReference<JarMissingObservable>) bundleContext
                .getServiceReference(JarMissingObservable.class.getCanonicalName());
        JarMissingObservable missingJarObservable = bundleContext.getService(serviceReference);
        missingJarObservable.addObserver(observer);
    }

    /**
     * DOC sgandon Comment method "callBundleMethodToCallLibApi".
     * 
     * @param bundle
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     */
    private void callBundleMethodToCallLibApi(Bundle bundle) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        Class<?> activatorClass = bundle.loadClass("tst.call.plugin.lib.Activator"); //$NON-NLS-1$
        Object newInstance = activatorClass.newInstance();
        Method startMethod = activatorClass.getMethod("libCall"); //$NON-NLS-1$
        startMethod.invoke(newInstance);
    }

}
