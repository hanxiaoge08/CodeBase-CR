package com.hxg.service.impl;

import com.hxg.service.IFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author hxg
 * @description: 文件服务实现类
 * @date 2025/7/20 19:23
 */
@Slf4j
@Service
public class FileServiceImpl implements IFileService {
    @Override
    public String getFileTree(String localPath) {
        //1.读取gitignore文件
        File gitignoreFile = new File(localPath, ".gitignore");
        List<String> ignorePatterns = new ArrayList<>();
        if (gitignoreFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(gitignoreFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        ignorePatterns.add(line);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("读取.gitignore文件失败" + e.getMessage(), e);
            }
        }
        // 2. 递归读取localPath下的所有文件，包括文件夹，并过滤.gitignore内包含的文件
        StringBuilder mdTree = new StringBuilder();
        buildFileTree(new File(localPath), "", mdTree, ignorePatterns, localPath);
        // 3. 直接返回md格式内容
        return mdTree.toString();
    }

    @Override
    public String unzipToProjectDir(MultipartFile file, String userName, String projectName) {
        log.info("开始解压文件，文件名：{}，大小：{} bytes", file.getOriginalFilename(), file.getSize());
        String baseDir = System.getProperty("user.dir") + File.separator + "repository";
        String destDir = baseDir + File.separator + userName + File.separator + projectName;
        log.info("解压目录：{}", destDir);

        File destDirFile = new File(destDir);
        if (!destDirFile.exists()) {
            destDirFile.mkdirs();
            log.info("创建目录：{}", destDir);
        }
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry zipEntry;
            int fileCount = 0;
            while ((zipEntry = zip.getNextEntry()) != null) {
                log.info("开始解压文件：{}", file.getOriginalFilename());
                File newFile = newFile(destDir, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                    log.info("创建目录：{}", newFile.getAbsolutePath());
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        long totalBytes = 0;
                        while ((len = zip.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                            totalBytes += len;
                        }
                        log.info("解压文件 {} 成功，大小：{} bytes", newFile.getAbsolutePath(), totalBytes);
                        fileCount++;
                    }
                }
                zip.closeEntry();
            }
            log.info("解压完成，共解压 {} 个文件", fileCount);
        } catch (Exception e) {
            log.error("解压文件时发生错误", e);
            throw new RuntimeException("解压文件时发生错误: " + e.getMessage(), e);
        }
        return destDir;
    }

    @Override
    public String getRepositoryPath(String userName, String projectName) {
        String baseDir = System.getProperty("user.dir") + File.separator + "repository";
        String localPath = baseDir + File.separator + userName + File.separator + projectName;
        File baseDirFile = new File(baseDir);
        if (!baseDirFile.exists()) {
            baseDirFile.mkdirs();
        }
        //如果localpath已存在，则删除/projectName目录
        File projectDir = new File(localPath);
        if (projectDir.exists()) {
            try {
                log.info("项目目录 {} 已存在，正在删除...", localPath);
                FileUtils.deleteDirectory(projectDir);
                log.info("项目目录 {} 删除成功", localPath);
            } catch (IOException e) {
                log.error("删除项目目录 {} 失败: {}", localPath, e.getMessage(), e);
                throw new RuntimeException("删除已存在的项目目录失败: " + e.getMessage(), e);
            }
        }

        // 确保目录存在
        projectDir.mkdirs();
        return localPath;
    }

    @Override
    public void deleteProjectDirectory(String userName, String projectName) {
        if (userName == null || projectName == null) {
            log.warn("无法删除项目目录，用户名或项目名为空");
            return;
        }
        String baseDir = System.getProperty("user.dir") + File.separator + "repository";
        String projectPath = baseDir + File.separator + userName + File.separator + projectName;
        File projectDir = new File(projectPath);

        if (projectDir.exists()) {
            try {
                log.info("正在删除项目目录：{}", projectPath);
                FileUtils.deleteDirectory(projectDir);
                log.info("项目目录删除成功：{}", projectPath);
            } catch (IOException e) {
                log.error("项目目录{}删除失败：{}", projectPath, e.getMessage(), e);
                throw new RuntimeException("项目目录删除失败：" + e.getMessage(), e);
            }
        } else {
            log.info("项目目录{}不存在,无需删除", projectPath);
        }

    }

    /**
     * 构建文件树
     *
     * @param dir            文件夹
     * @param prefix         前缀
     * @param mdTree         md树
     * @param ignorePatterns 忽略模式
     * @param rootPath       根路径
     */
    private void buildFileTree(File dir, String prefix, StringBuilder mdTree, List<String> ignorePatterns, String rootPath) {

        String name = dir.getName();
        if (!dir.exists() || name.startsWith(".") || isIgnored(dir, ignorePatterns, rootPath)) {
            return;
        }
        if (!dir.getAbsolutePath().equals(rootPath)) {
            if (dir.isDirectory()) {
                mdTree.append(prefix).append("- ").append(name).append("/").append("\n");
            } else {
                mdTree.append(prefix).append("- ").append(name).append("\n");
            }
        }
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (File file : files) {
                    buildFileTree(file, prefix + "  ", mdTree, ignorePatterns, rootPath);
                }
            }
        }
    }

    /**
     * 判断文件是否被忽略
     *
     * @param file           文件
     * @param ignorePatterns 忽略模式
     * @param rootPath       根路径
     * @return 是否被忽略
     */
    private boolean isIgnored(File file, List<String> ignorePatterns, String rootPath) {

        // 如果文件是根目录本身，则不应被.gitignore模式忽略
        if (file.getAbsolutePath().equals(rootPath)) {
            return false;
        }

        // 确保文件路径确实是rootPath的子路径，避免IndexOutOfBoundsException
        if (!file.getAbsolutePath().startsWith(rootPath + File.separator)) {
            // 如果文件不在rootPath下，也视为不忽略（或根据实际业务调整）
            return false;
        }
        String relativePath = file.getAbsolutePath().substring(rootPath.length() + 1).replace("\\", "/");
        for (String pattern : ignorePatterns) {
            String regexPattern;
            if (pattern.endsWith("/")) {
                // 对于目录模式，精确匹配目录名，后跟 / 或字符串结束，然后是任何字符。
                // 例如，"foo/" 应匹配 "foo/" 或 "foo/bar"
                String dirName = pattern.substring(0, pattern.length() - 1);
                regexPattern = "^" + Pattern.quote(dirName) + "(/.*)?$";
            } else {
                // 对于文件或通用模式，精确匹配模式，后跟 / 或字符串结束，然后是任何字符。
                // 例如，"foo" 应匹配 "foo" 或 "foo/bar"
                regexPattern = "^" + Pattern.quote(pattern) + "(/.*)?$";
            }
            Pattern regex = Pattern.compile(regexPattern);
            Matcher matcher = regex.matcher(relativePath);

            if (matcher.matches()) {
                // 如果模式专门针对目录（以 '/' 结尾）
                // 那么只有当当前 'file' 确实是目录时才应被忽略。
                // 如果是文件但匹配目录模式，则不应被忽略。
                if (pattern.endsWith("/") && !file.isDirectory()) {
                    // 此模式适用于目录，但 'file' 不是目录。继续下一个模式。
                    continue;
                }
                // 找到匹配项且条件满足。
                return true;
            }
        }
        return false;

    }

    /**
     * 创建文件
     *
     * @param destinationDir 目标目录
     * @param entryName      条目名称
     * @return 创建的文件
     * @throws IOException 创建文件时发生IO异常
     */
    private File newFile(String destinationDir, String entryName) throws IOException {
        File destFile = new File(destinationDir, entryName);
        String destDirPath = new File(destinationDir).getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("压缩文件中包含着非法的文件路径：" + entryName);
        }
        return destFile;
    }
}
