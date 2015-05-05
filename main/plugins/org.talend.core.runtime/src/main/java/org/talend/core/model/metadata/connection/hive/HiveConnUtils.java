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
package org.talend.core.model.metadata.connection.hive;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

/**
 * @author Marvin Wang
 * @version 1.0 jdk1.6
 * @date Aug 9, 2012
 */
public class HiveConnUtils {

    /**
     * Returns the distribution names that can be used to show in UI. If no items, return <code>new String[0]</code>.
     * Added by Marvin Wang on Aug. 9, 2012.
     * 
     * @see #getDistributionNameList()
     * @return
     */
    public static String[] getDistributionNames() {
        List<String> list = getDistributionNameList();
        if (list == null) {
            return new String[0];
        } else {
            return list.toArray(new String[list.size()]);
        }
    }

    /**
     * Returns the name list of distribution. If no items, return <code>null</code>. These names are from
     * {@link HiveConnVersionInfo} that defines the level <code>0</code>. Added by Marvin Wang on Aug 9, 2012.
     * 
     * @see #extractVersionInfo(int)
     * @return
     */
    public static List<String> getDistributionNameList() {
        return getNameListByLevel(0);
    }

    /**
     * Returns the children of the given object, refer to the method {@link #getFollowersByKeyAndLevel(String, int)}.
     * Added by Marvin Wang on Aug 10, 2012.
     * 
     * @param obj
     * @return
     */
    protected static List<HiveConnVersionInfo> getFollowersOfObject(HiveConnVersionInfo obj) {
        String key = obj.getKey();
        int level = obj.getLevel();
        List<HiveConnVersionInfo> beans = getObjectsByLevel(level + 1);
        if (beans != null && beans.size() > 0) {
            List<HiveConnVersionInfo> listFollowingKey = new ArrayList<HiveConnVersionInfo>();
            for (HiveConnVersionInfo bean : beans) {
                extractObjectFollowsKey(key, bean, listFollowingKey);
            }
            return listFollowingKey;
        }
        return null;
    }

    /**
     * If the object from the given array <code>HiveConnVersionInfo[]</code> includes the key, then put the object into
     * the given list to store. Added by Marvin Wang on Aug 9, 2012.
     * 
     * @param key
     * @param beans
     * @param listFollowingKey
     */
    protected static void extractObjectFollowsKey(String key, HiveConnVersionInfo bean, List<HiveConnVersionInfo> listFollowingKey) {
        HiveConnVersionInfo[] followsBeans = bean.getFollows();
        if (followsBeans != null && followsBeans.length > 0) {
            for (HiveConnVersionInfo followsBean : followsBeans) {
                if (followsBean.getKey().equals(key)) {
                    listFollowingKey.add(bean);
                }
            }
        }
    }

    /**
     * Returns all names with a specified level. Added by Marvin Wang on Aug 9, 2012.
     * 
     * @param level
     * @return
     */
    protected static List<String> getNameListByLevel(int level) {
        List<HiveConnVersionInfo> beans = getObjectsByLevel(level);
        if (beans != null && beans.size() > 0) {
            List<String> nameList = new ArrayList<String>();
            for (HiveConnVersionInfo bean : beans) {
                nameList.add(bean.getDisplayName());
            }
            return nameList;
        }
        return null;
    }

    /**
     * Returns all {@link HiveConnVersionInfo} objects with the given level. Added by Marvin Wang on Aug 9, 2012.
     * 
     * @param level
     * @return
     */
    protected static List<HiveConnVersionInfo> getObjectsByLevel(int level) {
        HiveConnVersionInfo[] beans = HiveConnVersionInfo.values();
        if (beans != null && beans.length > 0) {
            List<HiveConnVersionInfo> list = new ArrayList<HiveConnVersionInfo>();
            for (HiveConnVersionInfo bean : beans) {
                int beanLevel = bean.getLevel();
                if (level == beanLevel) {
                    list.add(bean);
                }
            }
            // ADD msjian TDQ-6407 2012-11-26:for top not support hive embedded mode
            // if (PluginChecker.isOnlyTopLoaded() && (level == 0 || level == 2)) {
            // list.remove(0);
            // }
            // TDQ-6407~
            return list;
        }
        return null;
    }

    /**
     * Returns the index of distribution that is level 0 by display name. Added by Marvin Wang on Aug 9, 2012.
     * 
     * @param displayName
     * @return
     */
    public static int getDistributionIndex(String displayName) {
        String[] names = getDistributionNames();
        if (names != null && names.length > 0) {
            for (int i = 0; i < names.length; i++) {
                if (displayName != null && displayName.equals(names[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the key by display name and level. Added by Marvin Wang on Aug 9, 2012.
     * 
     * @param displayName
     * @param level
     * @return
     */
    protected static String getKeyByDisplayName(String displayName, int level) {
        List<HiveConnVersionInfo> beans = getObjectsByLevel(level);
        if (beans != null && beans.size() > 0) {
            for (HiveConnVersionInfo bean : beans) {
                if (displayName != null && displayName.equals(bean.getDisplayName())) {
                    return bean.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Returns all hive versions by distribution index, it means these hive versions follow the distribution. Added by
     * Marvin Wang on Aug 10, 2012.
     * 
     * @param level0Index
     * @return
     */
    protected static List<HiveConnVersionInfo> getHiveVersions(int distributionIndex) {
        List<HiveConnVersionInfo> level0Objects = getObjectsByLevel(0);
        HiveConnVersionInfo objOfLevel0ByIndex = level0Objects.get(distributionIndex);
        return getFollowersOfObject(objOfLevel0ByIndex);
    }

    protected static List<String> getHiveVersionNameList(int distributionIndex) {
        List<HiveConnVersionInfo> hiveVersionObjs = getHiveVersions(distributionIndex);
        if (hiveVersionObjs != null && hiveVersionObjs.size() > 0) {
            List<String> hiveVersionNameList = new ArrayList<String>();
            for (HiveConnVersionInfo bean : hiveVersionObjs) {
                hiveVersionNameList.add(bean.getDisplayName());
            }
            return hiveVersionNameList;
        }
        return null;
    }

    public static String[] getHiveVersionNames(int distributionIndex) {
        List<String> hiveVersionNameList = getHiveVersionNameList(distributionIndex);
        if (hiveVersionNameList != null && hiveVersionNameList.size() > 0) {
            return hiveVersionNameList.toArray(new String[hiveVersionNameList.size()]);
        } else {
            return new String[0];
        }
    }

    protected static List<HiveConnVersionInfo> getHiveModes(int distributionIndex, int versionIndex, int hiveServerIndex) {
        List<HiveConnVersionInfo> supportedModes = new ArrayList<HiveConnVersionInfo>();
        HiveConnVersionInfo hiveVersionObj = getHiveVersionObj(distributionIndex, versionIndex);
        if (hiveVersionObj != null) {
            boolean supportEmbedded = isSupportEmbedded(hiveVersionObj);
            boolean supportStandalone = isSupportStandalone(hiveVersionObj, hiveServerIndex);
            List<HiveConnVersionInfo> modes = getFollowersOfObject(hiveVersionObj);
            if (modes != null && modes.size() > 0) {
                for (HiveConnVersionInfo mode : modes) {
                    if (HiveConnVersionInfo.MODE_EMBEDDED.equals(mode) && supportEmbedded
                            || HiveConnVersionInfo.MODE_STANDALONE.equals(mode) && supportStandalone) {
                        supportedModes.add(mode);
                    }
                }
            }
        }

        return supportedModes;
    }

    private static boolean isSupportEmbedded(HiveConnVersionInfo hiveVersionObj) {
        return !(HiveConnVersionInfo.APACHE_0_20_203.equals(hiveVersionObj) || HiveConnVersionInfo.MAPR1.equals(hiveVersionObj)
                || HiveConnVersionInfo.MapR_EMR.equals(hiveVersionObj) || HiveConnVersionInfo.Cloudera_CDH3
                    .equals(hiveVersionObj));
    }

    private static boolean isSupportStandalone(HiveConnVersionInfo hiveVersionObj, int hiveServerIndex) {
        boolean isHiveServer1 = "HIVE".equals(HiveServerVersionUtils.extractKey(hiveServerIndex)); //$NON-NLS-1$
        return !(HiveConnVersionInfo.HDP_1_0.equals(hiveVersionObj) || isHiveServer1
                && (HiveConnVersionInfo.HDP_1_2.equals(hiveVersionObj) || HiveConnVersionInfo.HDP_1_3.equals(hiveVersionObj) || HiveConnVersionInfo.HDP_2_0
                        .equals(hiveVersionObj)));
    }

    protected static List<String> getHiveModeNameList(int distributionIndex, int versionIndex, int hiveServerIndex) {
        List<HiveConnVersionInfo> hiveModeObjs = getHiveModes(distributionIndex, versionIndex, hiveServerIndex);
        // ADD msjian TDQ-6407 2012-11-26: for top, not support hive embeded mode,hide this menu
        // if (PluginChecker.isOnlyTopLoaded() && hiveModeObjs.size() > 1) {
        // hiveModeObjs.remove(0);
        // }
        // TDQ-6407~
        if (hiveModeObjs != null && hiveModeObjs.size() > 0) {
            List<String> hiveModeNameList = new ArrayList<String>();
            for (HiveConnVersionInfo bean : hiveModeObjs) {
                hiveModeNameList.add(bean.getDisplayName());
            }
            return hiveModeNameList;
        }
        return null;
    }

    public static String[] getHiveModeNames(int distributionIndex, int versionIndex, int hiveServerIndex) {
        List<String> hiveModeNameList = getHiveModeNameList(distributionIndex, versionIndex, hiveServerIndex);
        if (hiveModeNameList != null && hiveModeNameList.size() > 0) {
            return hiveModeNameList.toArray(new String[hiveModeNameList.size()]);
        } else {
            return new String[0];
        }
    }

    public static int getIndexOfDistribution(String distributionKey) {
        int level = 0;
        List<HiveConnVersionInfo> distributions = getObjectsByLevel(level);
        if (distributions != null && distributions.size() > 0) {
            for (int i = 0; i < distributions.size(); i++) {
                if (distributions.get(i).getKey().equals(distributionKey)) {
                    return i;
                }
            }
        }
        return -1;
    }

    protected static int getIndexOfDistribution(HiveConnVersionInfo distribution) {
        String key = distribution.getKey();
        return getIndexOfDistribution(key);
    }

    public static int getIndexOfHiveVersion(String distributionKey, String hiveVersionKey) {
        int distributionIndex = getIndexOfDistribution(distributionKey);
        List<HiveConnVersionInfo> hiveVersions = getHiveVersions(distributionIndex);
        if (hiveVersions != null && hiveVersions.size() > 0) {
            for (int i = 0; i < hiveVersions.size(); i++) {
                if (hiveVersions.get(i).getKey().equals(hiveVersionKey)) {
                    return i;
                }
            }
        }
        return 0;
    }

    protected static int getIndexOfHiveVersion(HiveConnVersionInfo distribution, HiveConnVersionInfo hiveVersion) {
        String distributionKey = distribution.getKey();
        String hiveVersionKey = hiveVersion.getKey();
        return getIndexOfHiveVersion(distributionKey, hiveVersionKey);
    }

    public static int getIndexOfHiveMode(String distributionKey, String hiveVersionKey, String hiveModeKey, String hiveServerKey) {
        int distributionIndex = getIndexOfDistribution(distributionKey);
        int hiveVersionIndex = getIndexOfHiveVersion(distributionKey, hiveVersionKey);
        int hiveServerIndex = getIndexOfHiveServer(hiveServerKey);
        List<HiveConnVersionInfo> hiveModes = getHiveModes(distributionIndex, hiveVersionIndex, hiveServerIndex);
        if (hiveModes != null && hiveModes.size() > 0) {
            for (int i = 0; i < hiveModes.size(); i++) {
                if (hiveModes.get(i).getKey().equals(hiveModeKey)) {
                    return i;
                }
            }
        }
        return 0;
    }

    protected static int getIndexOfHiveMode(HiveConnVersionInfo distribution, HiveConnVersionInfo hiveVersion,
            HiveConnVersionInfo hiveMode, String hiveServerKey) {
        String distributionKey = distribution.getKey();
        String hiveModeKey = hiveMode.getKey();
        String hiveVersionkey = hiveVersion.getKey();
        return getIndexOfHiveMode(distributionKey, hiveVersionkey, hiveModeKey, hiveServerKey);
    }

    public static int getIndexOfHiveServer(String hiveServerKey) {
        List<String> hiveServers = HiveServerVersionUtils.extractListKeys();
        if (hiveServers != null && hiveServers.size() > 0) {
            for (int i = 0; i < hiveServers.size(); i++) {
                if (hiveServers.get(i).equals(hiveServerKey)) {
                    return i;
                }
            }
        }

        return 0;
    }

    public static boolean isEmbeddedMode(int distributionIndex, int hiveVersionIndex, int hiveModeIndex, int hiveServerIndex) {
        List<HiveConnVersionInfo> hiveModes = getHiveModes(distributionIndex, hiveVersionIndex, hiveServerIndex);
        if (hiveModes != null && hiveModes.size() > 0) {
            HiveConnVersionInfo hiveMode = hiveModes.get(hiveModeIndex);
            if (HiveConnVersionInfo.MODE_EMBEDDED.getKey().equals(hiveMode.getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the distribution <code>HiveConnVersionInfo</code> object by the given <code>index</code> from level 0.
     * Added by Marvin Wang on Aug 13, 2012.
     * 
     * @param index
     * @return
     */
    public static HiveConnVersionInfo getDistributionObj(int index) {
        List<HiveConnVersionInfo> distributions = getObjectsByLevel(0);
        if (distributions != null && distributions.size() > 0 && index != -1) {
            return distributions.get(index);
        }
        return null;
    }

    /**
     * Returns the hive version object by the given index of distribution and hive version. Added by Marvin Wang on Aug
     * 13, 2012.
     * 
     * @param distributionIndex
     * @param hiveVersionIndex
     * @return
     */
    public static HiveConnVersionInfo getHiveVersionObj(int distributionIndex, int hiveVersionIndex) {
        List<HiveConnVersionInfo> objs = getHiveVersions(distributionIndex);
        if (objs != null && objs.size() > 0) {
            return objs.get(hiveVersionIndex);
        }
        return null;
    }

    /**
     * Returns the hive mode object by the given index of distribution, version and mode. Added by Marvin Wang on Aug
     * 13, 2012.
     * 
     * @param distributionIndex
     * @param hiveVersionIndex
     * @param hiveModeIndex
     * @return
     */
    public static HiveConnVersionInfo getHiveModeObj(int distributionIndex, int hiveVersionIndex, int hiveModeIndex,
            int hiveServerIndex) {
        List<HiveConnVersionInfo> objs = getHiveModes(distributionIndex, hiveVersionIndex, hiveServerIndex);
        if (objs != null && objs.size() > 0) {
            return objs.get(hiveModeIndex);
        }
        return null;
    }

    public static String getHiveModeObjKey(int distributionIndex, int hiveVersionIndex, int hiveModeIndex, int hiveServerIndex) {
        HiveConnVersionInfo hiveModeObj = getHiveModeObj(distributionIndex, hiveVersionIndex, hiveModeIndex, hiveServerIndex);
        if (hiveModeObj != null) {
            return hiveModeObj.getKey();
        }
        return null;
    }

    /**
     * Checkes if the selected distro version supports hive server2. Added by Marvin Wang on Mar 25, 2013.
     * 
     * @param distributionIndex
     * @param hiveVersionIndex
     * @return
     */
    public static boolean isSupportHiveServer2(int distributionIndex, int hiveVersionIndex) {
        HiveConnVersionInfo distroVersion = getHiveVersionObj(distributionIndex, hiveVersionIndex);
        return distroVersion.isSupportHive2();
    }

    /**
     * Checks if the current distro is {@link HiveConnVersionInfo#CUSTOM}. If yes, then return <code>true</code> Added
     * by Marvin Wang on Mar 26, 2013.
     * 
     * @param distributionIndex
     * @return
     */
    public static boolean isCustomDistro(int distributionIndex) {
        HiveConnVersionInfo obj = getDistributionObj(distributionIndex);
        if (HiveConnVersionInfo.DISTRO_CUSTOM == obj) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isCustomDistro(String distroKey) {
        int distroIndex = getIndexOfDistribution(distroKey);
        return isCustomDistro(distroIndex);
    }

    /**
     * This is a temporary method to estimate whether or not one hive version support security. We need to refactor
     * {@link HiveConnVersionInfo}(A really bad class) to support this function later...
     * <P>
     * DOC ycbai Comment method "isSupportSecurity".
     * 
     * @param distributionIndex
     * @param hiveVersionIndex
     * @param hiveModeIndex
     * @param isHive2
     * @return
     */
    public static boolean isSupportSecurity(int distributionIndex, int hiveVersionIndex, int hiveModeIndex, boolean isHive2,
            int hiveServerIndex) {
        HiveConnVersionInfo hiveVersionObj = getHiveVersionObj(distributionIndex, hiveVersionIndex);
        if (hiveVersionObj.isSupportSecurity()
                && (isHive2 || isEmbeddedMode(distributionIndex, hiveVersionIndex, hiveModeIndex, hiveServerIndex))) {
            return true;
        }

        return false;
    }

    public static boolean isSupportTez(int distributionIndex, int hiveVersionIndex, int hiveModeIndex, int hiveServerIndex) {
        HiveConnVersionInfo hiveVersionObj = getHiveVersionObj(distributionIndex, hiveVersionIndex);
        boolean versionSupportTez = ArrayUtils.contains(HiveConnVersionInfo.getHiveVersionsSupportingTez(), hiveVersionObj);
        boolean isEmbeddedMode = isEmbeddedMode(distributionIndex, hiveVersionIndex, hiveModeIndex, hiveServerIndex);
        return versionSupportTez && isEmbeddedMode;
    }

}
