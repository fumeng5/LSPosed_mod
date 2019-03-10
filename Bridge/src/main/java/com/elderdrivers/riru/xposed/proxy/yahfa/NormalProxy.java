package com.elderdrivers.riru.xposed.proxy.yahfa;

import com.elderdrivers.riru.xposed.Main;
import com.elderdrivers.riru.xposed.config.ConfigManager;
import com.elderdrivers.riru.xposed.entry.Router;
import com.elderdrivers.riru.xposed.util.PrebuiltMethodsDeopter;

import static com.elderdrivers.riru.xposed.util.FileUtils.getDataPathPrefix;

public class NormalProxy {

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir) {
        // mainly for secondary zygote
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        // call this to ensure the flag is set to false ASAP
        Router.prepare(false);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
        // install bootstrap hooks for secondary zygote
        Router.installBootstrapHooks(false);
        if (Main.closedFdTable == 0) {
            // only load modules for secondary zygote
            Router.loadModulesSafely(true);
        }
    }

    public static void forkAndSpecializePost(int pid, String appDataDir, String niceName) {
        // TODO consider processes without forkAndSpecializePost called
        Main.appDataDir = appDataDir;
        Main.niceName = niceName;
        Router.prepare(false);
        Router.reopenFilesIfNeeded();
        Router.onEnterChildProcess();
        // load modules for each app process on its forked if dynamic modules mode is on
        Router.loadModulesSafely(false);
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities) {
        final boolean isDynamicModulesMode = Main.isDynamicModulesEnabled();
        ConfigManager.setDynamicModulesMode(isDynamicModulesMode);
        // set startsSystemServer flag used when loadModules
        Router.prepare(true);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for main zygote
        // install bootstrap hooks for main zygote as early as possible
        // in case we miss some processes not forked via forkAndSpecialize
        // for instance com.android.phone
        Router.installBootstrapHooks(true);
        // loadModules have to be executed in zygote even isDynamicModules is false
        // because if not global hooks installed in initZygote might not be
        // propagated to processes not forked via forkAndSpecialize
        Router.loadModulesSafely(true);
    }

    public static void forkSystemServerPost(int pid) {
        // in system_server process
        Main.appDataDir = getDataPathPrefix() + "android";
        Main.niceName = "system_server";
        Router.prepare(true);
        Router.reopenFilesIfNeeded();
        Router.onEnterChildProcess();
        // reload module list if dynamic mode is on
        Router.loadModulesSafely(false);
    }

}
