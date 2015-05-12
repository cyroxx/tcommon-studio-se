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
package org.talend.core.model.metadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Exists only as a stub for the many javajet components that still use it. This class will disappear. Javajet
 * components should use the BigDataCodeGeneratorArgument instead.
 */
@Deprecated
public class AvroMetadataTable extends MetadataTable {

    public static HashMap<IMetadataTable, String> dedup;

    String connectionName;

    public AvroMetadataTable(Object ignored) {
        //
    }

    public void setConnectionTypeName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getConnectionTypeName() {
        if (connectionName != null) {
            return connectionName;
        }
        if (dedup != null) {
            for (Map.Entry<IMetadataTable, String> existingTables : dedup.entrySet()) {
                if (hasSameMetadata(this, existingTables.getKey())) {
                    connectionName = existingTables.getValue();
                    return connectionName;
                }
            }
        }
        return null; // This should never occur
    }

    /**
     * Check is two MetadataTable have the same metadata. Two metadata are the same if their columns share names and
     * types.
     * 
     * @param input input metadatatable
     * @param output output metadatatable
     * @return true If the two tables have columns that can reuse the same Avro structure.
     */
    private boolean hasSameMetadata(IMetadataTable input, IMetadataTable output) {
        // Error case => not the same metadata
        if ((input.getListColumns() == null) || (output.getListColumns() == null)
                || (input.getListColumns().size() != output.getListColumns().size())) {
            return false;
        }
        for (IMetadataColumn inColumn : input.getListColumns()) {
            IMetadataColumn outColumn = output.getColumn(inColumn.getLabel());
            if ((outColumn == null) || (!inColumn.getTalendType().equals(outColumn.getTalendType()))) {
                // Something not similar => not the same metadata
                return false;
            }
        }
        // Every label is mapped => same metadata
        return true;
    }

}
