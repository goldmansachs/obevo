/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevocomparer.output.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gs.obevocomparer.compare.CatoComparison;
import com.gs.obevocomparer.compare.CatoDataSide;
import com.gs.obevocomparer.compare.breaks.Break;
import com.gs.obevocomparer.compare.breaks.FieldBreak;
import com.gs.obevocomparer.compare.breaks.GroupBreak;
import com.gs.obevocomparer.output.CatoComparisonMetadata;

public class SimpleComparisonMetadata implements CatoComparisonMetadata {

    private final CatoComparison comparison;

    private final Set<String> leftFields = new LinkedHashSet<String>();
    private final Set<String> rightFields = new LinkedHashSet<String>();

    private final Set<String> fieldBreakFields = new LinkedHashSet<String>();
    private final Set<String> includedFieldBreakFields = new LinkedHashSet<String>();
    private final Set<String> excludedFieldBreakFields = new LinkedHashSet<String>();
    private final Set<String> groupBreakFields = new LinkedHashSet<String>();

    private int includedBreakSize = 0;
    private int excludedBreakSize = 0;
    private boolean hasGroupBreaks;

    private final Map<String, SimpleBreakTypeInfo> breakTypeInfoMap = new HashMap<String, SimpleBreakTypeInfo>();

    public SimpleComparisonMetadata(CatoComparison comparison) {

        this.comparison = comparison;

        FieldBreak fbr;
        String extraRecordBreak = "Only in " + comparison.getLeftDataSource().getShortName();
        String missingRecordBreak = "Only in " + comparison.getRightDataSource().getShortName();

        for (Break br : comparison.getBreaks()) {
            if (br.isExcluded()) {
                this.excludedBreakSize++;
            } else {
                this.includedBreakSize++;
            }

            if (br.getDataSide() == CatoDataSide.LEFT) {
                this.leftFields.addAll(br.getDataObject().getFields());
            } else if (br.getDataSide() == CatoDataSide.RIGHT) {
                this.rightFields.addAll(br.getDataObject().getFields());
            }

            if (br instanceof FieldBreak) {
                fbr = (FieldBreak) br;
                for (String field : fbr.getFields()) {
                    this.fieldBreakFields.add(field);

                    if (fbr.isExcluded(field)) {
                        this.excludedFieldBreakFields.add(field);
                    } else {
                        this.includedFieldBreakFields.add(field);
                    }

                    this.addBreakStat(field, fbr.isExcluded(field));
                }
            } else {
                if (br.getDataSide() == CatoDataSide.LEFT) {
                    this.addBreakStat(extraRecordBreak, br.isExcluded());
                } else if (br.getDataSide() == CatoDataSide.RIGHT) {
                    this.addBreakStat(missingRecordBreak, br.isExcluded());
                }
            }

            if (br instanceof GroupBreak) {
                this.hasGroupBreaks = true;
                this.groupBreakFields.addAll(((GroupBreak) br).getFields());
            }
        }
    }

    public CatoComparison getComparison() {
        return this.comparison;
    }

    private void addBreakStat(String field, boolean isExcluded) {
        if (!this.breakTypeInfoMap.containsKey(field)) {
            this.breakTypeInfoMap.put(field, new SimpleBreakTypeInfo(field));
        }

        if (isExcluded) {
            this.breakTypeInfoMap.get(field).addBreakExclude();
        } else {
            this.breakTypeInfoMap.get(field).addBreak();
        }
    }

    public List<BreakTypeInfo> getBreakTypeInfo() {
        List<BreakTypeInfo> breakTypeInfoList = new ArrayList<BreakTypeInfo>(this.breakTypeInfoMap.values());
        Collections.sort(breakTypeInfoList);
        return breakTypeInfoList;
    }

    public int getIncludedBreakSize() {
        return this.includedBreakSize;
    }

    public int getExcludedBreakSize() {
        return this.excludedBreakSize;
    }

    public boolean hasGroupBreaks() {
        return this.hasGroupBreaks;
    }

    public Set<String> getLeftFields() {
        return this.leftFields;
    }

    public Set<String> getRightFields() {
        return this.rightFields;
    }

    public Set<String> getFieldBreakFields() {
        return this.fieldBreakFields;
    }

    public Set<String> getIncludedFieldBreakFields() {
        return this.includedFieldBreakFields;
    }

    public Set<String> getExcludedFieldBreakFields() {
        return this.excludedFieldBreakFields;
    }

    public Set<String> getGroupBreakFields() {
        return this.groupBreakFields;
    }

    public static class SimpleBreakTypeInfo implements BreakTypeInfo {

        private String type;
        private int breakCount;
        private int excludeCount;

        public SimpleBreakTypeInfo(String type) {
            this.type = type;
            this.breakCount = 0;
            this.excludeCount = 0;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getBreakCount() {
            return this.breakCount;
        }

        public void addBreak() {
            this.breakCount++;
        }

        public int getExcludeCount() {
            return this.excludeCount;
        }

        public void addBreakExclude() {
            this.excludeCount++;
        }

        public int compareTo(BreakTypeInfo other) {
            if (this.breakCount < other.getBreakCount()) {
                return 1;
            } else if (this.breakCount > other.getBreakCount()) {
                return -1;
            } else if (this.excludeCount < other.getExcludeCount()) {
                return 1;
            } else if (this.excludeCount > other.getExcludeCount()) {
                return -1;
            } else {
                return 0;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SimpleBreakTypeInfo)) {
                return false;
            }

            SimpleBreakTypeInfo that = (SimpleBreakTypeInfo) o;

            if (breakCount != that.breakCount) {
                return false;
            }
            if (excludeCount != that.excludeCount) {
                return false;
            }
            return !(type != null ? !type.equals(that.type) : that.type != null);
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + breakCount;
            result = 31 * result + excludeCount;
            return result;
        }
    }
}
