/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.dist;

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
import com.gs.obevo.reladomo.ReladomoSchemaConverter;
import com.gs.obevo.reladomo.ReladomoSchemaConverterArgs;
import com.gs.obevo.util.ArgsParser;
import com.gs.obevo.util.LogUtil;
import com.gs.obevo.util.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.HashingStrategyMaps;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;

@SuppressWarnings("WeakerAccess")
public class Main {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Main.class);
    private static final ImmutableList<DeployCommand> COMMANDS = ArrayAdapter.adapt(DeployCommand.values()).toImmutable();

    public enum DeployCommand {
        DEPLOY,
        DBREVENG,
        NEWREVENG,
        DBREVENGMERGE,
        DBREVENGTABLEMERGE,
        RELADOMOREVENG,
        INIT,
        PREVIEW,
        DBDATACOMPARE,;
    }

    public static void main(String[] args) {
        new Main().execute(args);
    }

    private final ImmutableMap<String, Procedure<String[]>> commandMap;

    protected Main() {
        // use the hashing strategy to allow commands of any case to be handled
        MutableMap<String, Procedure<String[]>> commandMap = HashingStrategyMaps.mutable.of(HashingStrategies.fromFunction(String::toLowerCase));
        commandMap.putAll(getCommandMap().toMap());
        this.commandMap = commandMap.toImmutable();
    }

    protected void execute(String[] args) {
        execute(args, () -> System.exit(0), () -> System.exit(-1));
    }

    /**
     * Executes the main method. This is public so that client distributions can test this method from their own
     * packages.
     */
    @VisibleForTesting
    public void execute(String[] args, Runnable exitSuccessMethod, Runnable exitFailureMethod) {
        Pair<String, Procedure<String[]>> commandEntry = getDeployCommand(args, exitFailureMethod);

        LogUtil.FileLogger logAppender = LogUtil.getLogAppender(commandEntry.getOne());

        StopWatch changeStopWatch = new StopWatch();
        changeStopWatch.start();
        LOG.info("Starting action at time [" + new Date() + "]");
        boolean success = false;

        Throwable processException = null;
        try {
            String[] argSubset = (String[]) ArrayUtils.subarray(args, 1, args.length);

            commandEntry.getTwo().value(argSubset);
            success = true;
        } catch (Throwable t) {
            processException = t;
        } finally {
            // We handle the exception and do system.exit in the finally block as we want a clean message to go out to users.
            // If we just threw the runtime exception, then that would be the last thing that appears to users, which
            // was confusing for users.

            changeStopWatch.stop();
            long runtimeSeconds = changeStopWatch.getTime() / 1000;
            String successString = success ? "successfully" : "with errors";
            LOG.info("");
            LOG.info("Action completed {} at {}, took {} seconds.", successString, new Date(), runtimeSeconds);
            LOG.info("");
            if (processException != null) {
                LOG.info("*** Exception stack trace ***", processException);
                LOG.info("");
            }
            LOG.info("Detailed Log File is available at: {}", logAppender.getLogFile());
            LOG.info("");
            LOG.info("Exiting {}!", successString);
            IOUtils.closeQuietly(logAppender);
            if (processException != null) {
                exitFailureMethod.run();
            } else {
                exitSuccessMethod.run();
            }
        }
    }

    private Pair<String, Procedure<String[]>> getDeployCommand(String[] args, Runnable exitFailureMethod) {
        if (args.length == 0) {
            usage();
            exitFailureMethod.run();
        }

        Procedure<String[]> command = commandMap.get(args[0]);
        if (command == null) {
            System.out.println("No command w/ name " + args[0] + " has been defined in this distribution: " + commandMap.keysView().makeString("[", ", ", "]"));
            System.out.println("See the usage for more details");
            usage();
            exitFailureMethod.run();
        }

        return Tuples.pair(args[0], command);
    }

    private static void usage() {
        System.out.println("Usage: java " + Main.class.getName() + " ["
                + COMMANDS.collect(DeployCommand::name).collect(StringFunctions.toLowerCase())
                .makeString("/") + "] [args]");
        System.out.println("Pick one of the commands when entering.");
        System.out.println("If you just put in the command w/ no args, you will be prompted with further options");
    }

    protected ImmutableMap<String, Procedure<String[]>> getCommandMap() {
        MutableMap<String, Procedure<String[]>> commandMap = Maps.mutable.empty();
        commandMap.put("deploy", argSubset -> new DbDeployerMain().start(new ArgsParser().parse(argSubset, new DeployerArgs(), object -> {
            if (object.getSourcePath() == null) {
                return "-sourcePath argument must be passed in";
            }
            return null;
        })));
        commandMap.put("DBREVENG", argSubset -> {
            AquaRevengArgs argsObj = new ArgsParser().parse(argSubset, new AquaRevengArgs());
            new AquaRevengMain().execute(argsObj);
        });
        commandMap.put("NEWREVENG", argSubset -> {
            AquaRevengArgs newArgsObj = new ArgsParser().parse(argSubset, new AquaRevengArgs());
            AbstractDdlReveng ddlReveng = newArgsObj.getDbPlatform().getDdlReveng();
            ddlReveng.reveng(newArgsObj);
        });
        commandMap.put("DBREVENGMERGE", argSubset -> {
            DbFileMergerArgs mergeArgsObj = new ArgsParser().parse(argSubset, new DbFileMergerArgs());
            new DbFileMerger().execute(mergeArgsObj);
        });
        commandMap.put("DBREVENGTABLEMERGE", argSubset -> {
            DbFileMergerArgs tableMergeArgsObj = new ArgsParser().parse(argSubset, new DbFileMergerArgs());
            new TableSyncher().execute(tableMergeArgsObj);
        });
        commandMap.put("INIT", argSubset -> new DbDeployerMain().start(new ArgsParser().parse((String[]) ArrayUtils.add(argSubset, "-performInitOnly"), new DeployerArgs())));
        commandMap.put("PREVIEW", argSubset -> new DbDeployerMain().start(new ArgsParser().parse((String[]) ArrayUtils.add(argSubset, "-preview"), new DeployerArgs())));
        commandMap.put("DBDATACOMPARE", argSubset -> DbDataComparisonUtil.main(argSubset));
        commandMap.put(DeployCommand.RELADOMOREVENG.name(), argSubset -> {
            ReladomoSchemaConverter schemaConverter = new ReladomoSchemaConverter();
            ReladomoSchemaConverterArgs reladomoArgs = new ArgsParser().parse(argSubset, new ReladomoSchemaConverterArgs());
            schemaConverter.convertDdlsToDaFormat(reladomoArgs);
        });

        return commandMap.toImmutable();
    }
}
