package com.keyesit.graphcli;

import java.nio.file.Path;

public class BaselineMain {

    public static void main(String[] args) throws Exception {
        Path configPath = (args.length > 0) ? Path.of(args[0]) : Path.of("config.ini");

        System.out.println("Loading config: " + configPath.toAbsolutePath());

        IniConfig ini = IniConfig.load(configPath);

        AppConfig cfg;
        try {
            cfg = AppConfig.fromIni(ini);
        } catch (Exception e) {
            System.err.println("CONFIG ERROR: " + e.getMessage());
            System.exit(2);
            return;
        }

        System.out.println(cfg.prettySummary());

        // Baseline exit: no Graph calls yet.
        System.out.println("Baseline validation OK. (No Graph operations executed.)");
    }
}
