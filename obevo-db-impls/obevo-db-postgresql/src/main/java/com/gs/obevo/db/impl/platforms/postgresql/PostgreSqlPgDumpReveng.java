package com.gs.obevo.db.impl.platforms.postgresql;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.db.impl.core.util.MultiLineStringSplitter;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

public class PostgreSqlPgDumpReveng extends AbstractDdlReveng {
    public PostgreSqlPgDumpReveng() {
        super(
                new PostgreSqlDbPlatform(),
                new MultiLineStringSplitter("GO", true),
                Lists.immutable.<Predicate<String>>of(
                        StringPredicates.contains("-- PostgreSQL database dump").and(StringPredicates.contains("-- Dumped by pg_dump"))
                ),
                getRevengPatterns(),
                new Procedure2<ChangeEntry, String>() {
                    @Override
                    public void value(ChangeEntry changeEntry, String s) {

                    }
                }
        );
        setSkipLinePredicates(Lists.immutable.<Predicate<String>>of(
                StringPredicates.startsWith("SET statement_timeout")
                , StringPredicates.startsWith("SET default_tablespace")
                , StringPredicates.startsWith("SET lock_timeout")
                , StringPredicates.startsWith("SET idle_in_transaction_session_timeout")
                , StringPredicates.startsWith("SET client_encoding")
                , StringPredicates.startsWith("SET standard_conforming_strings")
                , StringPredicates.startsWith("SET check_function_bodies")
                , StringPredicates.startsWith("SET client_min_messages")
                , StringPredicates.startsWith("SET row_security")
                , StringPredicates.startsWith("SET default_with_oids")
                , StringPredicates.startsWith("CREATE SCHEMA")
                , StringPredicates.startsWith("SET search_path")
        ));
    }

    @Override
    protected void printInstructions(AquaRevengArgs args) {

    }

    static ImmutableList<RevengPattern> getRevengPatterns() {
        String schemaNameSubPattern = "\"?(\\w+\\.)?(\\w+)\"?";
        String sequenceTablePatterm = "\"?(\\w+)\\.(\\w+)\"?";

        return Lists.immutable.with(
                new RevengPattern(ChangeType.SEQUENCE_STR, "(?i)create\\s+(?:or\\s+replace\\s+)?sequence\\s+" + schemaNameSubPattern, 2, null, null).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, "(?i)create\\s+table\\s+" + schemaNameSubPattern, 2, null, null).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
//                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + nameSubPattern + "\\s+foreign\\s+key", 2, 3, "FK").withPostProcessSql(REMOVE_QUOTES),
//                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + nameSubPattern, 2, 3, null).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+(?:only\\s+)" + schemaNameSubPattern, 2, null, null).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+sequence\\s+" + schemaNameSubPattern + "\\s+owned\\s+by\\s+" + sequenceTablePatterm, 3, 2, null).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, "(?i)create\\s+(?:unique\\s+)index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + schemaNameSubPattern, 4, 2, "INDEX").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.FUNCTION_STR, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?function\\s+" + schemaNameSubPattern, 2, null, null),
                new RevengPattern(ChangeType.VIEW_STR, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?view\\s+" + schemaNameSubPattern, 2, null, null),
                new RevengPattern(ChangeType.SP_STR, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)procedure\\s+" + schemaNameSubPattern, 2, null, null),
                new RevengPattern(ChangeType.PACKAGE_STR, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)package\\s+" + schemaNameSubPattern, 2, null, null),
                new RevengPattern(ChangeType.TRIGGER_STR, "(?i)create\\s+or\\s+replace\\s+trigger\\s+" + schemaNameSubPattern, 2, null, null)
        );
    }
}
