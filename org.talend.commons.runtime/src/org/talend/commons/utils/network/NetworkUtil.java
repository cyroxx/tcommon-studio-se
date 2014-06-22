// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.commons.utils.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ggu class global comment. Detailled comment
 */
public class NetworkUtil {

    private static final String[] windowsCommand = { "ipconfig", "/all" }; //$NON-NLS-1$ //$NON-NLS-2$

    private static final String[] linuxCommand = { "/sbin/ifconfig", "-a" }; //$NON-NLS-1$ //$NON-NLS-2$

    private static final Pattern macPattern = Pattern
            .compile(".*((:?[0-9a-f]{2}[-:]){5}[0-9a-f]{2}).*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private final static List<String> getMacAddressList() throws IOException {
        final ArrayList<String> macAddressList = new ArrayList<String>();

        final String os = System.getProperty("os.name"); //$NON-NLS-1$

        final String[] command;
        if (os.startsWith("Windows")) { //$NON-NLS-1$
            command = windowsCommand;
        } else if (os.startsWith("Linux")) { //$NON-NLS-1$
            command = linuxCommand;
        } else {
            throw new IOException("Unknown operating system: " + os); //$NON-NLS-1$
        }

        final Process process = Runtime.getRuntime().exec(command);
        // Discard the stderr
        new Thread() {

            public void run() {
                try {
                    InputStream errorStream = process.getErrorStream();
                    while (errorStream.read() != -1) {
                    }
                    errorStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // Extract the MAC addresses from stdout
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        for (String line = null; (line = reader.readLine()) != null;) {
            Matcher matcher = macPattern.matcher(line);
            if (matcher.matches()) {
                // macAddressList.add(matcher.group(1));
                macAddressList.add(matcher.group(1).replaceAll("[-:]", "")); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        reader.close();
        return macAddressList;
    }

    public static String getMacAddress() {
        try {
            List<String> addressList = getMacAddressList();
            StringBuffer sb = new StringBuffer();
            for (Iterator<String> iter = addressList.iterator(); iter.hasNext();) {
                sb.append(iter.next());
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean isNetworkValid() {

        try {
            URL url = new URL("http://www.talend.com"); //$NON-NLS-1$
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);

            conn.setRequestMethod("HEAD"); //$NON-NLS-1$
            String strMessage = conn.getResponseMessage();
            if (strMessage.compareTo("Not Found") == 0) { //$NON-NLS-1$
                return false;
            }
            if (strMessage.equals("OK")) { //$NON-NLS-1$
                return true;
            }
            conn.disconnect();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public final static void main(String[] args) {
        try {
            System.out.println("  MAC Address: " + getMacAddress()); //$NON-NLS-1$
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}