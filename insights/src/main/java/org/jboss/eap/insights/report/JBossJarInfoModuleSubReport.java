/*
 * Copyright 2023 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.eap.insights.report;

import com.redhat.insights.jars.JarAnalyzer;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.jars.JarInfoSubreport;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.jboss.eap.insights.report.logging.InsightsReportLogger;
import org.jboss.eap.insights.report.logging.JbossLoggingInsightsLogger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
class JBossJarInfoModuleSubReport extends JarInfoSubreport {

    private final JarAnalyzer analyzer;
    private final Set<String> processedJars;

    JBossJarInfoModuleSubReport() {
        super(JbossLoggingInsightsLogger.INSTANCE);
        this.analyzer = new JarAnalyzer(JbossLoggingInsightsLogger.INSTANCE, true);
        this.processedJars = new HashSet<>();
    }

    @Override
    public void generateReport() {
        String[] modulesRoots = System.getProperty("module.path", "").split(File.pathSeparator);
        for (String modulesRoot : modulesRoots) {
            Path modulePath = new File(modulesRoot).toPath();
            InsightsReportLogger.ROOT_LOGGER.startProcessingModulePath(modulePath);
            try {
                Files.walkFileTree(modulePath, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!processedJars.contains(file.toAbsolutePath().toString())) {
                            processedJars.add(file.toAbsolutePath().toString());
                            if (isArchive(file)) {
                                try {
                                    Optional<JarInfo> info = analyzer.process(file.toUri().toURL());
                                    if (info.isPresent()) {
                                        JarInfo jarInfo = info.get();
                                        jarInfo.attributes().put("path", formatPath(modulePath.relativize(file)));
                                        jarInfo.attributes().put("overridden", "unknown");
                                        InsightsReportLogger.ROOT_LOGGER.addingAnalyzedJar(jarInfo);
                                        jarInfos.add(jarInfo);
                                    }
                                } catch (URISyntaxException ex) {
                                    InsightsReportLogger.ROOT_LOGGER.errorAnalyzingJar(file, ex);
                                }
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    private String formatPath(Path path) {
                        return path.toString().replace(File.separatorChar, '/');
                    }
                });
            } catch (IOException ex) {
                throw InsightsReportLogger.ROOT_LOGGER.failedToReadModules(ex);
            }
        }
        InsightsReportLogger.ROOT_LOGGER.endProcessingModules();
        Collections.sort((List<JarInfo>)jarInfos, Comparator.nullsFirst(
                Comparator.comparing(JarInfo::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Comparator.comparing(JarInfo::version, String.CASE_INSENSITIVE_ORDER))));
    }

    /**
     * Test if the target path is an archive.
     *
     * @param path path to the file.
     * @return true if the path points to a zip file - false otherwise.
     * @throws IOException
     */
    public static final boolean isArchive(Path path) throws IOException {
        if (Files.exists(path) && Files.isRegularFile(path)) {
            String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (fileName.endsWith(".zip") || fileName.endsWith(".jar") || fileName.endsWith(".war") || fileName.endsWith(".rar") || fileName.endsWith(".ear")) {
                try (ZipFile zip = new ZipFile(path.toFile())) {
                    return true;
                } catch (ZipException e) {
                    return false;
                }
            }
        }
        return false;
    }
}
