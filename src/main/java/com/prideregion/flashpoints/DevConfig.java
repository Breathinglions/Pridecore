package com.prideregion.flashpoints;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Explicit dimension allowlist and opt-in for a disposable staging world. */
public record DevConfig(boolean enabled,String dimension,int scanBudget,int writeBudget,boolean testEncounters) {
    public static DevConfig load() throws IOException {
        Path file=FabricLoader.getInstance().getConfigDir().resolve("pride_flashpoint_dev.properties");
        Properties props=new Properties();
        if(!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            props.setProperty("enabled","false");
            props.setProperty("allowed_dimension","pride:staging_unconfigured");
            props.setProperty("scan_budget_per_tick","3000");
            props.setProperty("write_budget_per_tick","250");
            props.setProperty("enable_test_encounters","false");
            try(OutputStream out=Files.newOutputStream(file)) { props.store(out,"Staging only; back up your world."); }
        }
        try(InputStream in=Files.newInputStream(file)) { props.load(in); }
        return new DevConfig(Boolean.parseBoolean(props.getProperty("enabled","false")),
            props.getProperty("allowed_dimension","pride:staging_unconfigured").trim(),
            bound(props.getProperty("scan_budget_per_tick","3000"),300,5000),
            bound(props.getProperty("write_budget_per_tick","250"),25,500),
            Boolean.parseBoolean(props.getProperty("enable_test_encounters","false")));
    }
    private static int bound(String raw,int min,int max) {
        try { return Math.max(min,Math.min(max,Integer.parseInt(raw))); }
        catch(NumberFormatException ex) { return min; }
    }
}
