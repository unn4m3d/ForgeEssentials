package com.forgeessentials.core.preloader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.launch.MixinLaunchPlugin;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.mixin.MixinEnvironment;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IEnvironment.Keys;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import joptsimple.OptionSpecBuilder;

public class FELaunchHandler implements ITransformationService
{
    public static final String NAME = "felaunchhandler";

    protected static final Logger launchLog = LogManager.getLogger("ForgeEssentials");

    public static final String FE_DIRECTORY = "ForgeEssentials";

    public static final String FE_LIB_VERSION = "3";

    public static final FilenameFilter JAR_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".jar");
        }
    };

    /* ------------------------------------------------------------ */

    private static File gameDirectory;

    private static File feDirectory;

    private static File libDirectory;

    private static File moduleDirectory;

    private static File jarLocation;

    /* ------------------------------------------------------------ */

    public boolean shouldExtractLibraries()
    {
        // boolean runtimeDeobfEnabled = (!(boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment"));
        if (!libDirectory.exists())
            return true;
        try
        {
            File versionFile = new File(libDirectory, "version.txt");
            String version = FileUtils.readFileToString(versionFile);
            return !FE_LIB_VERSION.equals(version);
        }
        catch (IOException e)
        {
            return true;
        }
    }

    public void extractLibraries()
    {
        try
        {
            FileUtils.deleteDirectory(libDirectory);
            libDirectory.mkdirs();
            // TODO Check for other stuff like WorldEdit!

            InputStream libArchive = getClass().getResourceAsStream("/libraries.zip");
            if (libArchive == null)
            {
                launchLog.warn("Could not find libraries.zip. Running in dev env?");
                return;
            }

            launchLog.info("Extracting libraries");
            try (ZipInputStream zIn = new ZipInputStream(libArchive))
            {
                ZipEntry zEntry;
                while ((zEntry = zIn.getNextEntry()) != null)
                {
                    File file = new File(gameDirectory, zEntry.getName());
                    if (zEntry.isDirectory())
                    {
                        file.mkdirs();
                    }
                    else
                    {
                        file.getParentFile().mkdirs();
                        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file)))
                        {
                            IOUtils.copy(zIn, out);
                        }
                    }
                }
            }

            File versionFile = new File(libDirectory, "version.txt");
            try (FileWriter out = new FileWriter(versionFile))
            {
                out.write(FE_LIB_VERSION);
            }
        }
        catch (IOException e)
        {
            launchLog.error("Error extraction libraries!");
            e.printStackTrace();
        }
    }

    /* ------------------------------------------------------------ */

    private void loadLibraries(URLClassLoader classLoader) throws IncompatibleEnvironmentException
    {
        File[] files = libDirectory.listFiles(JAR_FILTER);
        if (files == null)
            return;
        for (File f : files)
        {
            try
            {
                addURL(classLoader, f.toURI().toURL());
                launchLog.info(String.format("Added library %s to classpath", f.getAbsolutePath()));
            }
            catch (MalformedURLException e)
            {
                throw new RuntimeException(String.format("[ForgeEssentials] Error adding library %s to classpath: %s", f.getAbsolutePath(), e.getMessage()));
            }
        }
    }

    private void addURL(URLClassLoader classLoader, URL toURL) throws IncompatibleEnvironmentException
    {
        try {
            Method addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURLMethod.setAccessible(true);
            addURLMethod.invoke(classLoader, getClass().getProtectionDomain().getCodeSource().getLocation().toURI().toURL());
        } catch (Throwable ex) {
            throw new IncompatibleEnvironmentException("Failed to invoke URLClassLoader::addURL");
        }
    }

    private void loadModules(URLClassLoader classLoader) throws IncompatibleEnvironmentException
    {
        for (File f : moduleDirectory.listFiles(JAR_FILTER))
        {
            try
            {
                addURL(classLoader, f.toURI().toURL());
                launchLog.info(String.format("Added module %s to classpath", f.getAbsolutePath()));
            }
            catch (MalformedURLException e)
            {
                throw new RuntimeException(String.format("[ForgeEssentials] Error adding module %s to classpath: %s", f.getAbsolutePath(), e.getMessage()));
            }
        }
    }

    /* ------------------------------------------------------------ */

    public static File getGameDirectory()
    {
        return gameDirectory;
    }

    public static File getJarLocation()
    {
        return jarLocation;
    }

    public static File getFeDirectory()
    {
        return feDirectory;
    }

    public static File getModuleDirectory()
    {
        return moduleDirectory;
    }

    @Override public String name()
    {
        return NAME;
    }

    @Override public void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder)
    {

    }

    @Override public void argumentValues(OptionResult option)
    {

    }

    @Override public void initialize(IEnvironment environment)
    {
        Optional<ILaunchPluginService> plugin = environment.findLaunchPlugin(MixinLaunchPlugin.NAME);
        if (!plugin.isPresent()) {
            throw new MixinInitialisationError("Mixin Launch Plugin Service could not be located");
        }


        try {
            URI uri = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            MixinBootstrap.getPlatform().addContainer(new ContainerHandleURI(uri));
        } catch (URISyntaxException e) {
            launchLog.error(e);
        }

        // Fix CoFH compatibility. Fixes #1903
        MixinEnvironment.getEnvironment(MixinEnvironment.Phase.PREINIT).addTransformerExclusion("cofh.asm.CoFHAccessTransformer");

        // Enable FastCraft compatibility mode
        System.setProperty("fastcraft.asm.permissive", "true");

    }

    @Override public void beginScanning(IEnvironment environment)
    {

    }

    @Override public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException
    {
        // Setup directories
        Optional<Path> path = env.getProperty(Keys.GAMEDIR.get());
        if (path.isPresent())
        {
            gameDirectory = path.get().toFile();

            feDirectory = new File(gameDirectory, FE_DIRECTORY);
            feDirectory.mkdirs();

            moduleDirectory = new File(feDirectory, "modules");
            moduleDirectory.mkdirs();

            libDirectory = new File(feDirectory, "lib");

            try
            {
                jarLocation = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            }
            catch (URISyntaxException ex)
            {
                launchLog.error("Could not get JAR location");
                ex.printStackTrace();
            }

            URLClassLoader classLoader = (URLClassLoader) Launcher.class.getClassLoader();
            if (shouldExtractLibraries())
                extractLibraries();
            loadLibraries(classLoader);
            loadModules(classLoader);
        } else {
            throw new RuntimeException("Unable to find GAMEDIR!!  Unable to continue!");
        }
    }

    @Nonnull @Override public List<ITransformer> transformers()
    {
        return Stream.of(new EventTransformer(
                new String[] {
                        "com.forgeessentials.core.preloader.injections.MixinEntity",
                        "com.forgeessentials.core.preloader.injections.MixinBlock" },
                new String[] {

                }
        )).collect(Collectors.toList());
    }
}
