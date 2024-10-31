package me.n1ar4.jar.analyzer.engine.index;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.StrUtil;
import me.n1ar4.jar.analyzer.engine.DecompileEngine;
import me.n1ar4.jar.analyzer.engine.index.entity.Result;
import me.n1ar4.jar.analyzer.gui.util.LogUtil;
import me.n1ar4.jar.analyzer.starter.Const;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class IndexPluginsSupport {
    public final static String VERSION = "0.1";
    public final static String CurrentPath = System.getProperty("user.dir");
    public final static String DocumentPath = CurrentPath + FileUtil.FILE_SEPARATOR + Const.indexDir;
    public final static String TempPath = CurrentPath + FileUtil.FILE_SEPARATOR + Const.tempDir;


    private static final Integer MAX_CORE = Runtime.getRuntime().availableProcessors();
    private static final Integer MAX_SIZE_GROUP = 40;
    private static final ExecutorService executorService = ExecutorBuilder.create()
            .setCorePoolSize(MAX_CORE * 2)
            .setMaxPoolSize(MAX_CORE * 3)
            .setWorkQueue(new LinkedBlockingQueue<>())
            .build();


    static {
        Path curPath = Paths.get(CurrentPath);
        Path indexPath = curPath.resolve(Const.indexDir);
        Path tempPath = curPath.resolve(Const.tempDir);
        // MKDIR TEMP DIR
        if (!Files.exists(tempPath)) {
            try {
                Files.createDirectories(tempPath);
            } catch (Exception ignored) {
            }
        }
        // MKDIR INDEX DIR
        if (!Files.exists(indexPath)) {
            try {
                Files.createDirectories(indexPath);
            } catch (Exception ignored) {
            }
        }
    }

    public static List<File> getJarAnalyzerPluginsSupportAllFiles() {
        return FileUtil.loopFiles(TempPath, pathname -> pathname.getName().endsWith(".class"));
    }

    /**
     * 清除生成标识
     *
     * @param file jar文件
     * @return code
     */
    public static String getCode(File file) {
        String decompile = null;
        try {
            decompile = DecompileEngine.decompile(file.toPath());
        } catch (Exception e) {
            LogUtil.error(e.getMessage());
        }
        return StrUtil.isNotBlank(decompile) ? decompile.replace(DecompileEngine.getFERN_PREFIX(), "") : null;
    }

    public static boolean initIndex() throws IOException, InterruptedException {
        FileUtil.del(DocumentPath);
        int size = MAX_SIZE_GROUP;
        List<File> jarAnalyzerPluginsSupportAllFiles = getJarAnalyzerPluginsSupportAllFiles();
        if (jarAnalyzerPluginsSupportAllFiles.isEmpty()) {
            LogUtil.info("未找到任何 class 文件 无法搜索");
            return false;
        }
        IndexEngine.createIndex(DocumentPath);
        List<List<File>> split = CollUtil.split(jarAnalyzerPluginsSupportAllFiles, size);
        CountDownLatch latch = new CountDownLatch(split.size());
        for (List<File> files : split) {
            executorService.execute(() -> {
                Map<String, String> codeMap = new HashMap<>();
                files.forEach(file -> {
                    String code = getCode(file);
                    if (StrUtil.isNotBlank(code)) {
                        codeMap.put(file.getPath(), code);
                    }
                });
                try {
                    IndexEngine.addIndexCollection(DocumentPath, codeMap);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        return true;
    }

    public static Result search(String keyword) throws IOException, ParseException {
        return IndexEngine.search(keyword);
    }
}