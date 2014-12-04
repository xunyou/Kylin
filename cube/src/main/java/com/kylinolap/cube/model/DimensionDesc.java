/*
 * Copyright 2013-2014 eBay Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kylinolap.cube.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kylinolap.common.util.StringSplitter;
import com.kylinolap.common.util.StringUtil;
import com.kylinolap.metadata.model.JoinDesc;
import com.kylinolap.metadata.model.LookupDesc;
import com.kylinolap.metadata.model.TableDesc;
import com.kylinolap.metadata.model.realization.TblColRef;

/**
 * Created with IntelliJ IDEA. User: lukhan Date: 9/24/13 Time: 10:40 AM To
 * change this template use File | Settings | File Templates.
 */
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class DimensionDesc {

    @JsonProperty("id")
    private int id;
    @JsonProperty("name")
    private String name;
    private JoinDesc join;
    private HierarchyDesc[] hierarchy;
    @JsonProperty("hierarchy")
    private boolean isHierarchy;
    @JsonProperty("table")
    private String table;
    private String database;
    @JsonProperty("column")
    private String[] column;
    @JsonProperty("derived")
    private String[] derived;

    // computed
    private TblColRef[] columnRefs;
    private TblColRef[] derivedColRefs;

    public boolean isHierarchyColumn(TblColRef col) {
        if (hierarchy == null)
            return false;

        for (HierarchyDesc hier : hierarchy) {
            if (hier.getColumnRef().equals(col))
                return true;
        }
        return false;
    }

    public String getTable() {
        if (database == null) {
            return ("DEFAULT." + table).toUpperCase();
        } else {
            return (database + "." + table).toUpperCase();
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public JoinDesc getJoin() {
        return join;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TblColRef[] getColumnRefs() {
        return this.columnRefs;
    }

    public void setColumnRefs(TblColRef[] colRefs) {
        this.columnRefs = colRefs;
    }

    public String[] getColumn() {
        return this.column;
    }

    public void setColumn(String[] column) {
        this.column = column;
    }

    public HierarchyDesc[] getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(HierarchyDesc[] hierarchy) {
        this.hierarchy = hierarchy;
    }

    public String[] getDerived() {
        return derived;
    }

    public void setDerived(String[] derived) {
        this.derived = derived;
    }

    public TblColRef[] getDerivedColRefs() {
        return derivedColRefs;
    }

    public void setDerivedColRefs(TblColRef[] derivedColRefs) {
        this.derivedColRefs = derivedColRefs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DimensionDesc that = (DimensionDesc) o;

        if (id != that.id)
            return false;
        if (!name.equals(that.name))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DimensionDesc [name=" + name + ", join=" + join + ", hierarchy=" + Arrays.toString(hierarchy) + ", table=" + table + ", column=" + Arrays.toString(column) + ", derived=" + Arrays.toString(derived) + "]";
    }

    public void init(CubeDesc cubeDesc, Map<String, TableDesc> tables, Map<String, List<String>> columnTableMap) {
        if (name != null)
            name = name.toUpperCase();

        this.table = null;
        this.database = null;
        this.join = null;

        for (int i = 0, n = this.column.length; i < n; i++) {
            String thisColumn = this.column[i];
            String[] splits = StringSplitter.split(thisColumn, ".");
            if (splits.length > 1) {
                String thisTable = splits[splits.length - 2].toUpperCase();
                if (table == null) {
                    table = thisTable;
                } else if (thisTable != null && !table.equalsIgnoreCase(thisTable)) {
                    throw new IllegalStateException("One dimension can only refer to the columns on the same table: '" + table + "' and '" + thisTable + "'.");
                }

                if (database == null && splits.length > 2) {
                    database = splits[splits.length - 3].toUpperCase();
                }

                //this.column[i] = splits[splits.length - 1].toUpperCase();
            } else {
                // if no table specified, seek the table among all tables
                List<String> tableNames = columnTableMap.get(thisColumn);
                if (tableNames != null && tableNames.size() == 1) {
                    table = tableNames.get(0);
                } else {
                    if (tableNames == null)
                        throw new IllegalStateException("The column '" + thisColumn + "' doesn't belong to any table.");
                    if (tableNames.size() > 1)
                        throw new IllegalStateException("The column '" + thisColumn + "' is ambiguous; the name appeared in more than one tables, please specify table name together with the column name.");
                }
            }
        }

        TableDesc tableDesc = tables.get(getTable());
        if (tableDesc == null)
            throw new IllegalStateException("Can't find table " + table + " on dimension " + name);

        for (LookupDesc lookup : cubeDesc.getModel().getLookups()) {
            if (lookup.getTable().equalsIgnoreCase(table)) {
                this.join = lookup.getJoin();
                break;
            }
        }

        if (isHierarchy && this.column.length > 0) {
            List<HierarchyDesc> hierarchyList = new ArrayList<HierarchyDesc>(3);
            for (int i = 0, n = this.column.length; i < n; i++) {
                String aColumn = this.column[i];
                HierarchyDesc aHierarchy = new HierarchyDesc();
                aHierarchy.setLevel(String.valueOf(i + 1));
                aHierarchy.setColumn(aColumn);
                hierarchyList.add(aHierarchy);
            }

            this.hierarchy = hierarchyList.toArray(new HierarchyDesc[hierarchyList.size()]);
        }

        if (hierarchy != null && hierarchy.length == 0)
            hierarchy = null;
        if (derived != null && derived.length == 0)
            derived = null;

        if (hierarchy != null) {
            for (HierarchyDesc h : hierarchy)
                h.setColumn(h.getColumn().toUpperCase());
        }

        if (derived != null) {
            StringUtil.toUpperCaseArray(derived, derived);
        }
    }

}