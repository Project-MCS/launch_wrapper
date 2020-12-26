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

package com.arcbounds.launch;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing LaunchWrapper...");
        List<String> arguments = new LinkedList<>(Arrays.asList(args));
        loadServerJar(arguments);
        arguments.add("--tweakClass=org.spongepowered.asm.launch.BukkitBootstrap");
        Launch.main(arguments.toArray(new String[0]));
    }
    
    private static void loadServerJar(List<String> arguments) {
        try {
            Optional<String> serverJarPath = getArgument(arguments, "--serverJar");
            if (!serverJarPath.isPresent()) {
                throw new IllegalArgumentException("--serverJar argument is not present");
            }
            
            File serverJarFile = new File(serverJarPath.get());
            if (!serverJarFile.exists()) throw new IOException("Failed to find server jar");
            
            URLClassLoader classLoader = (URLClassLoader) Main.class.getClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, serverJarFile.toURI().toURL());
            LogManager.getLogger("LaunchWrapper").info("Loaded Server Jar " + serverJarFile.getAbsolutePath());
        } catch (Exception ex) {
            System.err.println("Encountered an error processing BukkitBootstrap::loadServerJar");
            ex.printStackTrace();
            System.exit(1);
        }
    }
    
    private static Optional<String> getArgument(List<String> arguments, String argument) {
        for (Iterator<String> iterator = arguments.iterator(); iterator.hasNext(); ) {
            String string = iterator.next();
            if (string != null && string.equals(argument) && iterator.hasNext()) {
                iterator.remove();
                String value = iterator.next();
                iterator.remove();
                return Optional.of(value);
            }
        }
        
        return Optional.empty();
    }
}