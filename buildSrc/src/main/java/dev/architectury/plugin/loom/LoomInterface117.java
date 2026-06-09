/*
 * This file is part of Architectury.
 *
 * This file is a modified version of the original file:
 * https://github.com/architectury/architectury-plugin/blob/3.5/src/loom117/kotlin/dev/architectury/plugin/loom/LoomInterface117.kt
 *
 * Licensed under the MIT License.
 */
package dev.architectury.plugin.loom;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.RunConfiguration;
import net.fabricmc.loom.build.mixin.AnnotationProcessorInvoker;
import net.fabricmc.loom.task.RemapJarTask;
import net.fabricmc.loom.util.gradle.GradleUtils;
import net.fabricmc.loom.util.gradle.SourceSetHelper;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class LoomInterface117 implements LoomInterface {

    private final Project project;

    LoomInterface117(Project project) {
        this.project = project;
    }

    private LoomGradleExtension getExtension() {
        return LoomGradleExtension.get(project);
    }

    @Override
    public Collection<File> getAllMixinMappings() {
        List<File> files = new ArrayList<>();

        GradleUtils.allLoomProjects(project.getGradle(), proj -> {
            LoomGradleExtension ext = LoomGradleExtension.get(proj);

            boolean thisDisable = this.getExtension().disableObfuscation();
            boolean otherDisable = ext.disableObfuscation();

            if (thisDisable != otherDisable) {
                return;
            }

            if (!thisDisable) {
                String thisId = this.getExtension()
                        .getMappingConfiguration()
                        .mappingsIdentifier();

                String otherId = ext
                        .getMappingConfiguration()
                        .mappingsIdentifier();

                if (!thisId.equals(otherId)) {
                    return;
                }
            }

            for (var sourceSet : SourceSetHelper.getSourceSets(proj)) {
                File mixinMappings = AnnotationProcessorInvoker
                        .getMixinMappingsForSourceSet(proj, sourceSet);

                if (!mixinMappings.exists()) {
                    continue;
                }

                files.add(mixinMappings);
            }
        });

        return files;
    }

    @Override
    public Path getTinyMappingsWithSrg() {
        return getExtension().getMappingConfiguration().tinyMappingsWithSrg;
    }

    @Override
    public String getRefmapName() {
        return getExtension().getMixin().getDefaultRefmapName().get();
    }

    @Override
    public boolean getGenerateSrgTiny() {
        return getExtension().shouldGenerateSrgTiny();
    }

    @Override
    public void setGenerateSrgTiny(boolean value) {
        getExtension().setGenerateSrgTiny(value);
    }

    @Override
    public boolean getLegacyMixinApEnabled() {
        return getExtension().getMixin().getUseLegacyMixinAp().get();
    }

    @Override
    public boolean getAddRefmapForForge() {
        return !getExtension()
                .getMinecraftProvider()
                .getVersionInfo()
                .isVersionOrNewer("2024-04-23T00:00:00+00:00");
    }

    @Override
    public boolean getGenerateTransformerPropertiesInTask() {
        return true;
    }

    @Override
    public boolean getDisableObfuscation() {
        return getExtension().disableObfuscation();
    }

    @Override
    public void settingsPostEdit(Function1<? super LoomInterface.LoomRunConfig, Unit> action) {
        getExtension().getSettingsPostEdit().add(c ->
                action.invoke(new LoomRunConfigImpl(c))
        );
    }

    @Override
    public void setIdeConfigGenerated() {
        getExtension().getRunConfigs().forEach(it ->
                it.getGenerateRunConfig().set(true)
        );

        getExtension().getRunConfigs().whenObjectAdded(it ->
                it.getGenerateRunConfig().set(true)
        );

        getExtension().addTaskBeforeRun(
                "$PROJECT_DIR$/" + project.getName() + ":classes"
        );
    }

    @Override
    public void setRemapJarInput(Jar task, Provider<RegularFile> archiveFile) {
        RemapJarTask remap = (RemapJarTask) task;
        remap.getInputFile().set(archiveFile);
    }

    class LoomRunConfigImpl implements LoomInterface.LoomRunConfig {

        private final RunConfiguration config;

        LoomRunConfigImpl(RunConfiguration config) {
            this.config = config;
        }

        @Override
        public String getMainClass() {
            return config.getMainClass().get();
        }

        @Override
        public void setMainClass(String value) {
            config.getMainClass().set(value);
        }

        @Override
        public void addVmArg(String vmArg) {
            config.getJvmArguments().add(vmArg);
        }

        @Override
        public String escape(String arg) {
            if (arg.chars().anyMatch(Character::isWhitespace)) {
                return "\"" + arg + "\"";
            }
            return arg;
        }
    }
}