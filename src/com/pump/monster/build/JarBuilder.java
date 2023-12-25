package com.pump.monster.build;

import com.pump.io.FileTreeIterator;
import com.pump.io.IOUtils;
import com.pump.monster.ui.MonsterFrame;
import com.pump.release.Project;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class JarBuilder {
    public static void main(String[] args) throws IOException {
        File userDir = new File(System.getProperty("user.dir"));
        File libDir = FileTreeIterator.find(userDir, "lib");
        File[] jarFiles = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
        Project project = new Project(userDir, MonsterFrame.class, "monster-generator.jar");

        File versionedJarFile = project.buildJar(true, Arrays.asList(jarFiles),
                "release", "jars", MonsterFrame.VERSION);

        System.out.println("Created: " + versionedJarFile.getAbsolutePath());

        File jarDir = versionedJarFile.getParentFile().getParentFile();
        File genericJarFile = new File(jarDir, "monster-generator.jar");
        IOUtils.copy(versionedJarFile, genericJarFile);
    }
}
