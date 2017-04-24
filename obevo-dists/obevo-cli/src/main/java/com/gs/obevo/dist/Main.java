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
package com.gs.obevo.dist;

import java.io.File;
import java.util.Date;

import com.gs.obevo.cmdline.DeployerArgs;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.apps.reveng.AquaRevengMain;
import com.gs.obevo.db.apps.reveng.DbFileMerger;
import com.gs.obevo.db.apps.reveng.DbFileMergerArgs;
import com.gs.obevo.db.apps.reveng.TableSyncher;
import com.gs.obevo.db.cmdline.DbDeployerMain;
import com.gs.obevo.db.impl.core.compare.data.DbDataComparisonUtil;
import com.gs.obevo.util.ArgsParser;
import com.gs.obevo.util.EnumUtils;
import com.gs.obevo.util.LogUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.FileAppender;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.eclipse.collections.impl.tuple.Tuples;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;

public class Main {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Main.class);
    private static final ImmutableList<DeployCommand> COMMANDS = ArrayAdapter.adapt(DeployCommand.values()).toImmutable();
    private static final DateTimeFormatter LOG_NAME_TIME_FORMAT = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    public enum DeployCommand {
        DEPLOY,
        DBREVENG,
        NEWREVENG,
        DBREVENGMERGE,
        DBREVENGTABLEMERGE,
        MITHRAREVENG,
        INIT,
        PREVIEW,
        DBDATACOMPARE,
        ;
    }

    public static void main(String[] args) {
        new Main().execute(args);
    }

    private final ImmutableMap<String, Procedure<String[]>> commandMap;

    protected Main() {
        // use the hashing strategy to allow commands of any case to be handled
        UnifiedMapWithHashingStrategy<String, Procedure<String[]>> commandMap = new UnifiedMapWithHashingStrategy<String, Procedure<String[]>>(HashingStrategies.fromFunction(StringFunctions.toLowerCase()));
        commandMap.putAll(getCommandMap().toMap());
        this.commandMap = commandMap.toImmutable();
    }

    private void execute(String[] args) {
        Pair<String, Procedure<String[]>> commandEntry = getDeployCommand(args);

        FileAppender logAppender = getLogAppender(commandEntry.getOne());

        StopWatch changeStopWatch = new StopWatch();
        changeStopWatch.start();
        LOG.info("Starting action at time [" + new Date() + "]");
        boolean success = false;

        try {
            String[] argSubset = (String[]) ArrayUtils.subarray(args, 1, args.length);

            commandEntry.getTwo().value(argSubset);
            success = true;
        } finally {
            changeStopWatch.stop();
            long runtimeSeconds = changeStopWatch.getTime() / 1000;
            String successString = success ? "successfully" : "with errors";
            LOG.info("Action completed " + successString + " at " + new Date() + ", " +
                    "took " + runtimeSeconds + " seconds. If you need more information, " +
                    "you can see the log at: " + logAppender.getFile());
            LogUtil.closeAppender(logAppender);
        }
    }

    private static FileAppender getLogAppender(String commandName) {
        File workDir = new File(FileUtils.getTempDirectory(), ".obevo");
        final String logFileName = "obevo-" + commandName + "-" + LOG_NAME_TIME_FORMAT.print(new DateTime()) + ".log";
        return LogUtil.configureLogging(workDir, logFileName);
    }

    private Pair<String, Procedure<String[]>> getDeployCommand(String[] args) {
        if (args.length == 0) {
            usage();
            System.exit(-1);
        }

        Procedure<String[]> command = commandMap.get(args[0]);
        if (command == null) {
            System.out.println("No command w/ name " + args[0] + " has been defined in this distribution: " + commandMap.keysView().makeString("[", ", ", "]"));
            System.out.println("See the usage for more details");
            usage();
            System.exit(-1);
        }

        return Tuples.pair(args[0], command);
    }

    private static void usage() {
        System.out.println("Usage: java " + Main.class.getName() + " ["
                + COMMANDS.collect(EnumUtils.<DeployCommand>toName()).collect(StringFunctions.toLowerCase())
                .makeString("/") + "] [args]");
        System.out.println("Pick one of the commands when entering.");
        System.out.println("If you just put in the command w/ no args, you will be prompted with further options");
        System.exit(-1);
    }

    protected ImmutableMap<String, Procedure<String[]>> getCommandMap() {
        MutableMap<String, Procedure<String[]>> commandMap = Maps.mutable.empty();
        commandMap.put("deploy", new Procedure<String[]>() {
            @Override
            public void value(String[] argSubset) {
                new DbDeployerMain().start(new ArgsParser().parse(argSubset, new DeployerArgs()));
            }
        });
        commandMap.put("DBREVENG", new Procedure<String[]>() {
            @Override
            public void value(String[] argSubset) {
                AquaRevengArgs argsObj = new ArgsParser().parse(argSubset, new AquaRevengArgs());
                new AquaRevengMain().execute(argsObj);
            }
        });
        commandMap.put("NEWREVENG", new Procedure<String[]>() {
            @Override
            public void value(String[] argSubset) {
                AquaRevengArgs newArgsObj = new ArgsParser().parse(argSubset, new AquaRevengArgs());
                AbstractDdlReveng ddlReveng = newArgsObj.getDbPlatform().getDdlReveng();
                ddlReveng.reveng(newArgsObj);
            }
        });
        commandMap.put("DBREVENGMERGE", new Procedure<String[]>() {
            @Override
            public void value(String[] argSubset) {
                DbFileMergerArgs mergeArgsObj = new ArgsParser().parse(argSubset, new DbFileMergerArgs());
                new DbFileMerger().execute(mergeArgsObj);
            }
        });
        commandMap.put("DBREVENGTABLEMERGE", new Procedure<String[]>() {
            @Override
            public void value(String[] argSubset) {
                DbFileMergerArgs tableMergeArgsObj = new ArgsParser().parse(argSubset, new DbFileMergerArgs());
                new TableSyncher().execute(tableMergeArgsObj);
            }
        });
        commandMap.put("INIT", new Procedure<String[]>() {
            @Override
            public void value(String[] argSubset) {
                new DbDeployerMain().start(new ArgsParser().parse((String[]) ArrayUtils.add(argSubset, "-performInitOnly"), new DeployerArgs()));
            }
        });
        commandMap.put("PREVIEW", new Procedure<String[]>() {
            @Override
            public void value(String[] argSubset) {
                new DbDeployerMain().start(new ArgsParser().parse((String[]) ArrayUtils.add(argSubset, "-preview"), new DeployerArgs()));
            }
        });
        commandMap.put("DBDATACOMPARE", new Procedure<String[]>() {
            @Override
            public void value(String[] argSubset) {
                DbDataComparisonUtil.main(argSubset);
            }
        });

        return commandMap.toImmutable();
    }
}
