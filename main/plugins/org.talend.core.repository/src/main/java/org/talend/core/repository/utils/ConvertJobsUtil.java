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
package org.talend.core.repository.utils;

import org.eclipse.swt.custom.CCombo;
import org.talend.core.hadoop.HadoopConstants;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.designer.core.convert.IProcessConvertService;
import org.talend.designer.core.convert.IProcessConvertToAllTypeService;
import org.talend.designer.core.convert.ProcessConvertManager;
import org.talend.designer.core.convert.ProcessConverterType;

/**
 * created by hcyi on May 25, 2015 Detailled comment
 *
 */
public class ConvertJobsUtil {

    public static final String FRAMEWORK = HadoopConstants.FRAMEWORK;

    public static final String STORM_FRAMEWORK = HadoopConstants.FRAMEWORK_STORM;

    public static final String SPARKSTREAMING_FRAMEWORK = HadoopConstants.FRAMEWORK_SPARKSTREAMING;

    public static final String MAPREDUCE_FRAMEWORK = HadoopConstants.FRAMEWORK_MAPREDUCE;

    public static final String SPARK_FRAMEWORK = HadoopConstants.FRAMEWORK_SPARK;

    public static enum JobType {
        STANDARD("Standard", "_STANDARD_"), //$NON-NLS-1$ //$NON-NLS-2$
        BIGDATASTREAMING("Big Data Streaming", "_BIG_DATA_STREAMING_"), //$NON-NLS-1$ //$NON-NLS-2$
        BIGDATABATCH("Big Data Batch", "_BIG_DATA_BATCH_"); //$NON-NLS-1$ //$NON-NLS-2$

        private String displayName;

        private String fileName;

        JobType(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getFileName() {
            return this.fileName;
        }

        public static String[] getJobTypeToDispaly() {
            String[] dispalyNames = new String[values().length];
            for (int i = 0; i < values().length; i++) {
                dispalyNames[i] = values()[i].getDisplayName();
            }
            return dispalyNames;
        }
    }

    public static enum JobStreamingFramework {
        STORMFRAMEWORK("Storm", "_STORM_STORM_FRAMEWORK_"), //$NON-NLS-1$ //$NON-NLS-2$
        SPARKSTREAMINGFRAMEWORK("Spark Streaming", "_STORM_SPARKSTREAMING_FRAMEWORK_"); //$NON-NLS-1$ //$NON-NLS-2$

        private String displayName;

        private String fileName;

        JobStreamingFramework(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getFileName() {
            return this.fileName;
        }

        public static String[] getFrameworkToDispaly() {
            String[] dispalyNames = new String[values().length];
            for (int i = 0; i < values().length; i++) {
                dispalyNames[i] = values()[i].getDisplayName();
            }
            return dispalyNames;
        }
    }

    public static enum JobBatchFramework {
        MAPREDUCEFRAMEWORK("MapReduce", "_MAPREDUCE_FRAMEWORK_"), //$NON-NLS-1$ //$NON-NLS-2$
        SPARKFRAMEWORK("Spark", "_SPARK_FRAMEWORK_"); //$NON-NLS-1$ //$NON-NLS-2$

        private String displayName;

        private String fileName;

        JobBatchFramework(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getFileName() {
            return this.fileName;
        }

        public static String[] getFrameworkToDispaly() {
            String[] dispalyNames = new String[values().length];
            for (int i = 0; i < values().length; i++) {
                dispalyNames[i] = values()[i].getDisplayName();
            }
            return dispalyNames;
        }
    }

    public static enum Status {
        DEVELOPMENT("development", "_DEVELOPMENT_"), //$NON-NLS-1$ //$NON-NLS-2$
        TESTING("testing", "_TESTING_"), //$NON-NLS-1$ //$NON-NLS-2$
        PRODUCTION("production", "_PRODUCTION_"); //$NON-NLS-1$ //$NON-NLS-2$

        private String displayName;

        private String fileName;

        Status(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getFileName() {
            return this.fileName;
        }

        public static String[] getStatusToDispaly() {
            String[] dispalyNames = new String[values().length];
            for (int i = 0; i < values().length; i++) {
                dispalyNames[i] = values()[i].getDisplayName();
            }
            return dispalyNames;
        }
    }

    public static void updateJobFrameworkPart(String jobTypeValue, CCombo frameworkCombo) {
        if (JobType.STANDARD.getDisplayName().equals(jobTypeValue)) {
            frameworkCombo.setItems(new String[0]);
        } else if (JobType.BIGDATABATCH.getDisplayName().equals(jobTypeValue)) {
            String[] items = JobBatchFramework.getFrameworkToDispaly();
            frameworkCombo.setItems(items);
            if (items.length > 0) {
                frameworkCombo.select(0);
            }
        } else if (JobType.BIGDATASTREAMING.getDisplayName().equals(jobTypeValue)) {
            String[] items = JobStreamingFramework.getFrameworkToDispaly();
            frameworkCombo.setItems(items);
            if (items.length > 0) {
                frameworkCombo.select(0);
            }
        }
    }

    /**
     * get the target execution framework from the field in Job properties
     * 
     * @param item
     * @return
     */
    public static Object getFramework(Item item) {
        if (item != null) {
            Property property = item.getProperty();
            if (property != null && property.getAdditionalProperties() != null
                    && property.getAdditionalProperties().containsKey(FRAMEWORK)) {
                return property.getAdditionalProperties().get(FRAMEWORK);
            }
        }
        return null;
    }

    public static String getJobTypeFromFramework(Item item) {
        Object frameworkObj = ConvertJobsUtil.getFramework(item);
        if (JobBatchFramework.MAPREDUCEFRAMEWORK.getDisplayName().equals(frameworkObj)
                || JobBatchFramework.SPARKFRAMEWORK.getDisplayName().equals(frameworkObj)) {
            return JobType.BIGDATABATCH.getDisplayName();
        } else if (JobStreamingFramework.STORMFRAMEWORK.getDisplayName().equals(frameworkObj)
                || JobStreamingFramework.SPARKSTREAMINGFRAMEWORK.getDisplayName().equals(frameworkObj)) {
            return JobType.BIGDATASTREAMING.getDisplayName();
        } else {
            return JobType.STANDARD.getDisplayName();
        }
    }

    public static String[] getFrameworkItemsByJobType(Item item) {
        String jobType = getJobTypeFromFramework(item);
        if (JobType.BIGDATABATCH.getDisplayName().equals(jobType)) {
            return JobBatchFramework.getFrameworkToDispaly();
        } else if (JobType.BIGDATASTREAMING.getDisplayName().equals(jobType)) {
            return JobStreamingFramework.getFrameworkToDispaly();
        } else {
            return new String[0];
        }
    }

    public static void createOperation(final String newJobName, final String jobTypeValue, final String frameworkValue,
            final IRepositoryViewObject sourceObject) {
        IProcessConvertService converter = null;
        if (sourceObject == null || sourceObject.getProperty() == null) {
            return;
        }
        Item item = sourceObject.getProperty().getItem();
        if (JobType.STANDARD.getDisplayName().equals(jobTypeValue)) {
            String sourceJobType = getJobTypeFromFramework(item);
            if (JobType.BIGDATASTREAMING.getDisplayName().equals(sourceJobType)
                    || ERepositoryObjectType.PROCESS_STORM == sourceObject.getRepositoryObjectType()) {
                converter = ProcessConvertManager.getInstance().extractConvertService(ProcessConverterType.CONVERTER_FOR_STORM);
            } else if (JobType.BIGDATABATCH.getDisplayName().equals(sourceJobType)
                    || ERepositoryObjectType.PROCESS_MR == sourceObject.getRepositoryObjectType()) {
                converter = ProcessConvertManager.getInstance().extractConvertService(
                        ProcessConverterType.CONVERTER_FOR_MAPREDUCE);
            }
            if (converter != null && converter instanceof IProcessConvertToAllTypeService) {
                ((IProcessConvertToAllTypeService) converter).convertToProcess(item, sourceObject, newJobName, jobTypeValue);
            }
        } else if (JobType.BIGDATASTREAMING.getDisplayName().equals(jobTypeValue)) {
            converter = ProcessConvertManager.getInstance().extractConvertService(ProcessConverterType.CONVERTER_FOR_STORM);
            if (converter != null && converter instanceof IProcessConvertToAllTypeService) {
                Item newItem = ((IProcessConvertToAllTypeService) converter).convertToProcessStreaming(item, sourceObject,
                        newJobName, jobTypeValue, frameworkValue);
                if (newItem != null) {
                    Property newProperty = newItem.getProperty();
                    if (newProperty.getAdditionalProperties() != null) {
                        newProperty.getAdditionalProperties().put(FRAMEWORK, frameworkValue);
                    }
                }
            }
        } else if (JobType.BIGDATABATCH.getDisplayName().equals(jobTypeValue)) {
            converter = ProcessConvertManager.getInstance().extractConvertService(ProcessConverterType.CONVERTER_FOR_MAPREDUCE);
            if (converter != null && converter instanceof IProcessConvertToAllTypeService) {
                Item newItem = ((IProcessConvertToAllTypeService) converter).convertToProcessBatch(item, sourceObject,
                        newJobName, jobTypeValue, frameworkValue);
                if (newItem != null) {
                    Property newProperty = newItem.getProperty();
                    if (newProperty.getAdditionalProperties() != null) {
                        newProperty.getAdditionalProperties().put(FRAMEWORK, frameworkValue);
                    }
                }
            }
        }
    }
}
