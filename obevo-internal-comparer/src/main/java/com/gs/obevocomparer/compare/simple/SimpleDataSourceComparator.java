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
package com.gs.obevocomparer.compare.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gs.obevocomparer.compare.CatoComparison;
import com.gs.obevocomparer.compare.CatoDataComparator;
import com.gs.obevocomparer.compare.CatoDataSide;
import com.gs.obevocomparer.compare.CatoDataSourceComparator;
import com.gs.obevocomparer.compare.CatoProperties;
import com.gs.obevocomparer.compare.breaks.Break;
import com.gs.obevocomparer.compare.breaks.DataObjectBreak;
import com.gs.obevocomparer.compare.breaks.FieldBreak;
import com.gs.obevocomparer.compare.breaks.GroupBreak;
import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.CatoDataSource;
import com.gs.obevocomparer.sort.Sort;
import com.gs.obevocomparer.sort.SortedGroupIterator;
import com.gs.obevocomparer.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDataSourceComparator implements CatoDataSourceComparator {

    protected final CatoProperties properties;
    protected final Comparator<CatoDataObject> dataObjectComparator;
    protected final CatoDataComparator dataComparator;
    protected final Sort<CatoDataObject> sort;
    protected final Factory<Collection<Break>> breakCollectionFactory;
    protected final Factory<Collection<CatoDataObject>> dataCollectionFactory;

    protected int groupId = 1;

    private static final Logger LOG = LoggerFactory.getLogger(SimpleDataSourceComparator.class);

    public SimpleDataSourceComparator(CatoProperties properties, Comparator<CatoDataObject> dataObjectComparator,
            CatoDataComparator dataComparator, Sort<CatoDataObject> sort,
            Factory<Collection<Break>> breakCollectionFactory, Factory<Collection<CatoDataObject>> dataCollectionFactory) {
        this.properties = properties;
        this.dataObjectComparator = dataObjectComparator;
        this.dataComparator = dataComparator;
        this.sort = sort;
        this.breakCollectionFactory = breakCollectionFactory;
        this.dataCollectionFactory = dataCollectionFactory;
    }

    public CatoComparison compare(String comparisonName, CatoDataSource leftDataSource, CatoDataSource rightDataSource) {

        LOG.info("Comparing left data set '{}' to right data set '{}'",
                leftDataSource.getName(), rightDataSource.getName());

        Collection<CatoDataObject> leftData = this.dataCollectionFactory.create();
        Collection<CatoDataObject> rightData = this.dataCollectionFactory.create();

        leftDataSource.open();
        rightDataSource.open();

        Iterator<CatoDataObject> sortedLeftData = leftDataSource.isSorted() ?
                leftDataSource : this.sort.sort(leftDataSource);
        Iterator<CatoDataObject> sortedRightData = rightDataSource.isSorted() ?
                rightDataSource : this.sort.sort(rightDataSource);

        Collection<Break> breaks = this.breakCollectionFactory.create();

        SortedGroupIterator<CatoDataObject> leftGroupIter =
                new SortedGroupIterator<CatoDataObject>(sortedLeftData, this.dataObjectComparator);

        SortedGroupIterator<CatoDataObject> rightGroupIter =
                new SortedGroupIterator<CatoDataObject>(sortedRightData, this.dataObjectComparator);

        List<CatoDataObject> leftGroup = leftGroupIter.next();
        List<CatoDataObject> rightGroup = rightGroupIter.next();

        while (leftGroup.size() > 0 || rightGroup.size() > 0) {

            int keyCompResult = this.dataObjectComparator.compare(
                    leftGroup.size() > 0 ? leftGroup.get(0) : null,
                    rightGroup.size() > 0 ? rightGroup.get(0) : null);

            if (keyCompResult < 0) {
                this.processLeftOnlyGroup(leftGroup, breaks, leftData, rightData);
                leftGroup = leftGroupIter.next();
            } else if (keyCompResult > 0) {
                this.processRightOnlyGroup(rightGroup, breaks, leftData, rightData);
                rightGroup = rightGroupIter.next();
            } else {
                this.processBothGroups(leftGroup, rightGroup, breaks, leftData, rightData);
                leftGroup = leftGroupIter.next();
                rightGroup = rightGroupIter.next();
            }
        }

        leftDataSource.close();
        rightDataSource.close();

        this.processComparisonData(breaks, leftData, rightData);

        LOG.info("Completed comparison with {} breaks", breaks.size());
        return new CatoComparison(comparisonName, this.properties, breaks,
                leftDataSource, leftData,
                rightDataSource, rightData);
    }

    protected void processLeftOnlyGroup(List<CatoDataObject> leftGroup, Collection<Break> breaks,
            Collection<CatoDataObject> leftData, Collection<CatoDataObject> rightData) {
        leftData.addAll(leftGroup);

        for (CatoDataObject obj : leftGroup) {
            breaks.add(new DataObjectBreak(obj, CatoDataSide.LEFT));
        }
    }

    protected void processRightOnlyGroup(List<CatoDataObject> rightGroup, Collection<Break> breaks,
            Collection<CatoDataObject> leftData, Collection<CatoDataObject> rightData) {
        rightData.addAll(rightGroup);

        for (CatoDataObject obj : rightGroup) {
            breaks.add(new DataObjectBreak(obj, CatoDataSide.RIGHT));
        }
    }

    protected void processBothGroups(List<CatoDataObject> leftGroup, List<CatoDataObject> rightGroup,
            Collection<Break> breaks, Collection<CatoDataObject> leftData, Collection<CatoDataObject> rightData) {
        leftData.addAll(leftGroup);
        rightData.addAll(rightGroup);

        FieldBreak fieldBreak;

        if (leftGroup.size() == 1 && rightGroup.size() == 1) {
            fieldBreak = this.compareDataObjects(leftGroup.get(0), rightGroup.get(0));
            if (fieldBreak != null) {
                breaks.add(fieldBreak);
            }
            return;
        }

        if (leftGroup.size() > 100) {
            LOG.warn("Large group of size {} being compared", leftGroup.size());
        }

        List<CatoDataObject> leftCompareGroup = new ArrayList<CatoDataObject>(leftGroup);
        List<CatoDataObject> rightCompareGroup = new ArrayList<CatoDataObject>(rightGroup);
        CatoDataObject leftObj;
        CatoDataObject rightObj;

        for (Iterator<CatoDataObject> leftIter = leftCompareGroup.iterator(); leftIter.hasNext(); ) {
            leftObj = leftIter.next();
            for (Iterator<CatoDataObject> rightIter = rightCompareGroup.iterator(); rightIter.hasNext(); ) {
                rightObj = rightIter.next();
                if (this.compareDataObjects(leftObj, rightObj) == null) {
                    leftIter.remove();
                    rightIter.remove();
                    break;
                }
            }
        }

        Set<String> breakFields = new LinkedHashSet<String>();
        for (CatoDataObject leftObject : leftCompareGroup) {
            for (CatoDataObject rightObject : rightCompareGroup) {
                fieldBreak = this.compareDataObjects(leftObject, rightObject);
                breakFields.addAll(fieldBreak.getFields());
            }
        }

        for (CatoDataObject obj : leftCompareGroup) {
            breaks.add(new GroupBreak(obj, CatoDataSide.LEFT, breakFields, this.groupId));
        }

        for (CatoDataObject obj : rightCompareGroup) {
            breaks.add(new GroupBreak(obj, CatoDataSide.RIGHT, breakFields, this.groupId));
        }

        this.groupId++;
    }

    protected FieldBreak compareDataObjects(CatoDataObject leftObj, CatoDataObject rightObj) {
        if (leftObj == null || rightObj == null) {
            LOG.error("Cannot compare null DataObjects");
            throw new IllegalArgumentException("Cannot compare null DataObjects");
        }

        Object leftVal;
        Object rightVal;
        Set<String> comparedRightFields = new HashSet<String>();
        Map<String, Object> fieldBreaks = new HashMap<String, Object>();

        for (String field : leftObj.getFields()) {
            if (this.properties.getExcludeFields().contains(field)) {
                continue;
            }

            leftVal = leftObj.getValue(field);
            rightVal = rightObj.getValue(this.getRightField(field));

            if (this.properties.getKeyFields().contains(field)) {
                if (this.dataComparator.compareKeyValues(leftVal, rightVal) != 0) {
                    LOG.error("Cannot compare data objects with different keys\n{}\n{}", leftObj, rightObj);
                    throw new IllegalArgumentException("Cannot compare data objects with different keys");
                }
                continue;
            }

            if (!this.dataComparator.compareValues(leftVal, rightVal)) {
                fieldBreaks.put(field, rightVal);
            }

            comparedRightFields.add(this.getRightField(field));
        }

        for (String rightField : rightObj.getFields()) {
            if (this.properties.getKeyFields().contains(rightField)
                    || this.properties.getExcludeFields().contains(rightField)
                    || comparedRightFields.contains(rightField)
                    || this.properties.getMappedFields().containsKey(rightField)) {
                continue;
            }

            fieldBreaks.put(rightField, rightObj.getValue(rightField));
        }

        if (fieldBreaks.size() > 0) {
            return new FieldBreak(leftObj, fieldBreaks);
        } else {
            return null;
        }
    }

    protected String getRightField(String leftField) {
        if (this.properties.getMappedFields().containsKey(leftField)) {
            return this.properties.getMappedFields().get(leftField);
        } else {
            return leftField;
        }
    }

    protected void processComparisonData(Collection<Break> breaks, Collection<CatoDataObject> leftData, Collection<CatoDataObject> rightData) {
        // This method is intended to help subclass implementations
    }
}
