package com.nagrikHelp.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads key=value pairs from a .env or .env.local file into the Spring Environment early during startup.
 * This improves local dev ergonomics so the backend picks up values from a .env.local file placed in the repo.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String[] CANDIDATE_PATHS = new String[] {
            ".env.local",
            "./.env.local",
            "backend/.env.local",
            "./backend/.env.local",
            "../backend/.env.local",
            ".env",
            "./.env"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String p : CANDIDATE_PATHS) {
            Path path = Paths.get(p);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                try {
                    Map<String, Object> m = loadDotenv(path);
                    if (!m.isEmpty()) {
                        environment.getPropertySources().addFirst(new MapPropertySource("dotenv-" + path.getFileName(), m));
                    }
                } catch (IOException e) {
                    // ignore — best-effort only
                }
                break;
            }
        }
    }

    private Map<String, Object> loadDotenv(Path path) throws IOException {
        Map<String, Object> m = new HashMap<>();
        try (BufferedReader r = Files.newBufferedReader(path)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                    val = val.substring(1, val.length() - 1);
                }
                if (!key.isEmpty()) m.put(key, val);
            }
        }
        return m;
    }
}
