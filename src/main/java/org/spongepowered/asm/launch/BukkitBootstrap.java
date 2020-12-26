/*
 * Copyright 2017 Alex Thomson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.spongepowered.asm.launch;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Modified version of {@link org.spongepowered.asm.launch.MixinTweaker MixinTweaker} in order to launch {@link org.bukkit.craftbukkit.Main Main}.
 */
public class BukkitBootstrap implements ITweaker {

    private String[] launchArguments;

    public BukkitBootstrap() {
        setLaunchArguments(new String[]{});
        MixinBootstrap.start();

        loadPluginsWithMixinsToClasspath();
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        if (args != null && !args.isEmpty()) {
            setLaunchArguments(args.toArray(new String[args.size()]));
        }

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("IReallyKnowWhatIAmDoingISwear", "true"); // Don't check if build is outdated.
        MixinBootstrap.doInit(args);


    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.addClassLoaderExclusion("com.mojang.util.QueueLogAppender");
        classLoader.addClassLoaderExclusion("jline.");
        classLoader.addClassLoaderExclusion("org.fusesource.");
        MixinBootstrap.inject();
    }

    @Override
    public String getLaunchTarget() {
        return "org.bukkit.craftbukkit.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return launchArguments;
    }

    private void setLaunchArguments(String[] launchArguments) {
        this.launchArguments = launchArguments;
    }

    private void loadPluginsWithMixinsToClasspath() {
        Path plugins = Paths.get(".").resolve("mixin");

        Multimap<Path, String> jarsToLoad = ArrayListMultimap.create();

        try {
            Files.list(plugins)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(jarFile -> {
                        try {
                            JarFile jar = new JarFile(jarFile.toFile());
                            Enumeration<JarEntry> entries = jar.entries();

                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();

                                if (entry.isDirectory()) continue;
                                if (!entry.getName().startsWith("mixins.") ||
                                    !entry.getName().endsWith(".json")) continue;

                                jarsToLoad.put(jarFile, entry.getName());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Map.Entry<Path, Collection<String>> entry : jarsToLoad.asMap().entrySet()) {
            try {
                System.out.println("Trying to load mixins from " + entry.getKey());
                URLClassLoader classLoader = Launch.classLoader;
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, entry.getKey().toFile().toURI().toURL());

                for (String mixin : entry.getValue())
                    Mixins.addConfiguration(mixin);
            } catch (Exception ex) {
                System.out.println("Failed to load mixins for " + entry.getKey());
                ex.printStackTrace();
            }
        }
    }
}