package com.arcbounds.launch.ext;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ExternalMixinLoader {
    private static final Logger logger = LogManager.getLogger("LaunchWrapper");
    private static Method CLASSPATH_ADD_URL;

    static {
        try {
            CLASSPATH_ADD_URL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            CLASSPATH_ADD_URL.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private final List<ExternalMixin> mixins = new ArrayList<>();
    private final List<Path> loaded = new ArrayList<>();

    public void discover(Path directory) throws IOException {
        for (Path file : Files.list(directory).collect(Collectors.toSet())) {
            if (Files.isDirectory(file) || !file.toString().endsWith(".jar")) return;

            try (JarFile jar = new JarFile(file.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (entry.isDirectory() || !isValidMixin(entry.getName())) continue;

                    mixins.add(new ExternalMixin(file, entry.getName()));
                }
            } catch (IOException ignored) {
                logger.warn("Failed to read jar file " + file);
            }
        }
    }

    public void load() {
        for (ExternalMixin mixin : this.mixins) {
            logger.info("Trying to load " + mixin.getMixin() + ".");
            try {
                if (!loaded.contains(mixin.getJar()))
                    loadJarToClasspath(mixin.getJar());

                Mixins.addConfiguration(mixin.getMixin());
            } catch (Exception e) {
                logger.warn("Failed to load " + mixin.getMixin() + "!");
                e.printStackTrace();
            }
        }
    }

    public int count() {
        return mixins.size();
    }

    private boolean isValidMixin(String mixin) {
        return mixin.startsWith("mixins.") && mixin.endsWith(".json") && !mixin.endsWith(".refmap.json");
    }

    private void loadJarToClasspath(Path jar) throws Exception {
        URLClassLoader classLoader = Launch.classLoader;
        CLASSPATH_ADD_URL.invoke(classLoader, jar.toUri().toURL());
        loaded.add(jar);
    }

    private static class ExternalMixin {
        private final Path jar;
        private final String mixin;

        public ExternalMixin(Path jar, String mixin) {
            this.jar = jar;
            this.mixin = mixin;
        }

        public Path getJar() {
            return jar;
        }

        public String getMixin() {
            return mixin;
        }
    }
}
