package com.gs.obevo.db.apps.reveng;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;

public class RevengPattern {
    private final String changeType;
    private final NamePatternType namePatternType;
    private final Pattern pattern;
    private final int primaryNameIndex;
    private final Integer secondaryNameIndex;
    private final String annotation;
    private final MutableList<Function<String, LineParseOutput>> postProcessSqls = Lists.mutable.empty();
    private Integer suggestedOrder;
    private boolean shouldBeIgnored;

    public static final Function<RevengPattern, String> TO_CHANGE_TYPE = new Function<RevengPattern, String>() {
        @Override
        public String valueOf(RevengPattern revengPattern) {
            return revengPattern.getChangeType();
        }
    };
    private Function<String, String> remapObjectName = Functions.getStringPassThru();

    public RevengPattern(String changeType, NamePatternType namePatternType, String pattern) {
        this(changeType, namePatternType, pattern, 1);
    }

    private RevengPattern(String changeType, NamePatternType namePatternType, String pattern, int primaryNameIndex) {
        this(changeType, namePatternType, pattern, primaryNameIndex, null, null);
    }

    public RevengPattern(String changeType, NamePatternType namePatternType, String pattern, int primaryNameIndex, Integer secondaryNameIndex, String annotation) {
        this.changeType = changeType;
        this.namePatternType = namePatternType;
        this.pattern = Pattern.compile(pattern, Pattern.DOTALL);
        this.primaryNameIndex = primaryNameIndex;
        this.secondaryNameIndex = secondaryNameIndex;
        this.annotation = annotation;
    }

    String getChangeType() {
        return changeType;
    }

    public NamePatternType getNamePatternType() {
        return namePatternType;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public int getPrimaryNameIndex() {
        return primaryNameIndex;
    }

    public Integer getSecondaryNameIndex() {
        return secondaryNameIndex;
    }

    String getAnnotation() {
        return annotation;
    }

    MutableList<Function<String, LineParseOutput>> getPostProcessSqls() {
        return postProcessSqls;
    }

    /**
     * See {@link #withSuggestedOrder(Integer)}.
     */
    Integer getSuggestedOrder() {
        return suggestedOrder;
    }

    public RevengPattern withPostProcessSql(Function<String, LineParseOutput> postProcessSql) {
        this.postProcessSqls.add(postProcessSql);
        return this;
    }

    /**
     * A hint to the reverse-engineering where the resultant change should be ordered, relative to other changes.
     * The default order is 0. This is needed for cases where changes for a particular object are spread across
     * files in the input.
     */
    public RevengPattern withSuggestedOrder(Integer suggestedOrder) {
        this.suggestedOrder = suggestedOrder;
        return this;
    }

    boolean isShouldBeIgnored() {
        return shouldBeIgnored;
    }

    public void setShouldBeIgnored(boolean shouldBeIgnored) {
        this.shouldBeIgnored = shouldBeIgnored;
    }

    public RevengPattern withShouldBeIgnored(boolean shouldBeIgnored) {
        this.shouldBeIgnored = shouldBeIgnored;
        return this;
    }

    /**
     * Remaps the object name in case users want to group objects into files together. By default, this is a passthrough
     * function, but users can override the behavior.
     */
    public String remapObjectName(String candidateObject) {
        return remapObjectName.valueOf(candidateObject);
    }

    public RevengPattern withRemapObjectName(Function<String, String> remapObjectName) {
        this.remapObjectName = remapObjectName != null ? remapObjectName : Functions.getStringPassThru();
        return this;
    }

    private String getMatcherGroup(Matcher matcher, Integer index) {
        if (index == null) {
            return null;
        }
        return matcher.group(index);
    }

    public RevengPatternOutput evaluate(String input) {
        final Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String primaryName = matcher.group(namePatternType.getObjectIndex(primaryNameIndex));
            String schema = getMatcherGroup(matcher, namePatternType.getSchemaIndex(primaryNameIndex));
            String subSchema = getMatcherGroup(matcher, namePatternType.getSubSchemaIndex(primaryNameIndex));

            // If we are looking for a subschema and only see one schema prefix, then assume it belongs to the subschema, not schema
            if (namePatternType.getSubSchemaIndex(primaryNameIndex) != null && schema != null && subSchema == null) {
                subSchema = schema;
                schema = null;
            }

            String secondaryName = null;
            if (secondaryNameIndex != null) {
                secondaryName = matcher.group(namePatternType.getObjectIndex(secondaryNameIndex));
                if (schema == null) {
                    schema = getMatcherGroup(matcher, namePatternType.getSchemaIndex(secondaryNameIndex));
                }
                if (subSchema == null) {
                    subSchema = getMatcherGroup(matcher, namePatternType.getSubSchemaIndex(secondaryNameIndex));
                }

                // Same check as above for subschema
                if (namePatternType.getSubSchemaIndex(secondaryNameIndex) != null && schema != null && subSchema == null) {
                    subSchema = schema;
                    schema = null;
                }
            }

            return new RevengPatternOutput(this, primaryName, secondaryName, schema, subSchema, input);
        }

        return null;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("changeType", changeType)
                .append("namePatternType", namePatternType)
                .append("pattern", pattern)
                .append("primaryNameIndex", primaryNameIndex)
                .append("secondaryNameIndex", secondaryNameIndex)
                .append("annotation", annotation)
                .append("postProcessSqls", postProcessSqls)
                .toString();
    }

    public enum NamePatternType {
        ONE(1),
        TWO(2),
        THREE(3),;

        private final int numParts;

        NamePatternType(int numParts) {
            this.numParts = numParts;
        }

        Integer getSchemaIndex(int groupIndex) {
            switch (numParts) {
            case 2:
                return groupIndex * numParts - 1;
            case 3:
                return groupIndex * numParts - 2;
            default:
                return null;
            }
        }

        Integer getSubSchemaIndex(int groupIndex) {
            switch (numParts) {
            case 3:
                return groupIndex * numParts - 1;
            default:
                return null;
            }
        }

        int getObjectIndex(int groupIndex) {
            return groupIndex * numParts;
        }
    }
}
