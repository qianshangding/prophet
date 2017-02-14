package com.qsd.prophet.agent.service.util;

import com.qsd.prophet.agent.service.common.AgentConstant;
import com.qsd.prophet.agent.service.entity.FileInfo;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.*;

public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    private static int BUFFER = 4096;

    public static String getTimeStringFormLine(String line, FileInfo fileInfo) {
        String timeFormat = fileInfo.getTimeFormat();
        String startFlag = fileInfo.getTimeStartFlag();
        int startFlagIndex = fileInfo.getTimeStartFlagIndex();

        if (StringUtils.isEmpty(line)) {
            return null;
        }
        String timeString = null;
        if (StringUtils.isEmpty(startFlag) && startFlagIndex == 0) {
            if (line.length() < timeFormat.length()) {
                return null;
            } else {
                timeString = line.substring(0, timeFormat.length());
            }
        } else {
            String[] splittedStrs = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, startFlag);
            if (ArrayUtils.isEmpty(splittedStrs) || splittedStrs.length <= startFlagIndex + 1) {
                return null;
            }
            String timeFormatString = splittedStrs[startFlagIndex + 1];
            if (timeFormatString.length() < timeFormat.length()) {
                return null;
            } else {
                timeString = timeFormatString.substring(0, timeFormat.length());
            }
        }

        if (StringUtils.isBlank(timeString)) {
            if (logger.isDebugEnabled()) {
                logger.info("timeFormat=" + timeFormat + ", startFlag=" + startFlag + ", startFlagIndex="
                        + startFlagIndex);
            }
        }

        return timeString;
    }

    public static Map<String, List<String>> getFileContent(String path) {
        Map<String, List<String>> contentMap = new HashMap<String, List<String>>();
        File file = new File(path);
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                if (subFile.isFile()) {
                    contentMap.put(subFile.getName(), readFileContent(subFile));
                }
            }
        } else {
            contentMap.put(file.getName(), readFileContent(file));
        }

        return contentMap;
    }

    public static void saveFileContent(String filePath, List<String> contents) {
        File file = new File(filePath);
        if (file.exists()) {
            File tmpFile = new File(filePath + "." + "tmp");
            writeFile(tmpFile, contents);
            if (file.delete()) {
                tmpFile.renameTo(file);
            }
        } else {
            writeFile(file, contents);
        }
    }

    private static void writeFile(File file, List<String> contents) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            for (String ctn : contents) {
                bw.write(ctn);
                bw.newLine();
            }
            bw.flush();
        } catch (Exception e) {
            logger.error("write file error! file is " + file);
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                logger.error("file  close error! file is " + file);
            }
        }
    }

    public static File getLatestRolledFile(final String parentPath, final String fileName) {
        File file = new File(parentPath);
        File[] rolledFiles = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.lastIndexOf(fileName) != -1 && name.length() > fileName.length()) {
                    if (name.lastIndexOf(".tar.gz") != -1 || name.lastIndexOf(".gz") != -1
                            || name.lastIndexOf(".zip") != -1) {
                        return false;
                    } else {
                        return true;
                    }
                }
                return false;
            }
        });

        if (rolledFiles == null || rolledFiles.length == 0) {
            return null;
        }

        File latestFile = rolledFiles[0];
        for (File rollFile : rolledFiles) {
            if (rollFile.lastModified() > latestFile.lastModified()) {
                latestFile = rollFile;
            }
        }

        return latestFile;
    }

    public static File[] getRelatedRolledFiles(File file) {
        final String fileName = file.getName();
        File dirFile = file.getParentFile();
        File[] relatedRolledFiles = dirFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.lastIndexOf(fileName) != -1 && name.length() > fileName.length()) {
                    if (name.lastIndexOf(".tar.gz") != -1 || name.lastIndexOf(".gz") != -1
                            || name.lastIndexOf(".zip") != -1) {
                        return false;
                    } else {
                        return true;
                    }
                }
                return false;
            }
        });
        return relatedRolledFiles;
    }

    public static void emptyDirectory(String dirPath) {
        File file = new File(dirPath);
        if (file.isFile()) {
            file.deleteOnExit();
        } else {
            File[] subFiles = file.listFiles();
            if (ArrayUtils.isNotEmpty(subFiles)) {
                for (File subfile : subFiles) {
                    subfile.delete();
                }
            }
        }
    }

    public static List<String> readFileContent(File subFile) {
        List<String> contents = new ArrayList<String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(subFile));
            String line = null;
            while ((line = br.readLine()) != null) {
                contents.add(line);
            }
        } catch (Exception e) {
            logger.error("readFileContent error file is " + subFile, e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                logger.error("BufferedReader close failed, file is " + subFile, e);
            }
        }
        return contents;
    }

    public static List<String> readFileContent(String absFilePath, int readLogSizePerLog) {
        List<String> contents = new ArrayList<String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(absFilePath));
            String line = null;
            while ((line = br.readLine()) != null) {
                contents.add(line);
                if (contents.size() > readLogSizePerLog) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("readFileContent error file is " + absFilePath + ", readLogSizePerLog is" + readLogSizePerLog, e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                logger.error("BufferedReader close failed, file is " + absFilePath + ", readLogSizePerLog is" + readLogSizePerLog, e);
            }
        }
        return contents;
    }

    public static int getFileType(String absFilePath) {
        if (absFilePath.endsWith("tar.gz")) {
            return AgentConstant.FILE_TYPE_TAR_GZ;
        } else if (absFilePath.endsWith(".gz")) {
            return AgentConstant.FILE_TYPE_GZ;
        } else if (absFilePath.endsWith(".zip")) {
            return AgentConstant.FILE_TYPE_ZIP;
        } else {
            return AgentConstant.FILE_TYPE_NORMAL;
        }
    }

    public static boolean extractGzipFile(String sourceGzipFile, String actualFilePath) {
        GZIPInputStream gzin = null;
        FileOutputStream fout = null;
        File outputFile = new File(actualFilePath);

        boolean result = true;
        try {
            gzin = new GZIPInputStream(new FileInputStream(sourceGzipFile));

            fout = new FileOutputStream(outputFile);

            int num;
            byte[] buf = new byte[BUFFER];
            while ((num = gzin.read(buf, 0, buf.length)) != -1) {
                fout.write(buf, 0, num);
            }
        } catch (Exception ex) {
            logger.error("unGzipFile  failed! ", ex);
            outputFile.deleteOnExit();
            result = false;
        } finally {
            try {
                if (gzin != null) {
                    gzin.close();
                }
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException e) {
                logger.error("close giz file failed! ", e);
            }
        }

        if (result) {
            logger.info("extractGzipFile sucess! sourceGzipFile is " + sourceGzipFile);
        }

        return result;
    }

    public static boolean extractTarGzFile(String sourceTarGzFile, String actualFilePath) {

        boolean result = true;
        ArchiveInputStream ais = null;
        BufferedOutputStream bufferedOutputStream = null;
        File outPutFile = new File(actualFilePath);
        try {
            ais = new ArchiveStreamFactory().createArchiveInputStream("tar", new GZIPInputStream(
                    new BufferedInputStream(new FileInputStream(sourceTarGzFile))));
            TarArchiveEntry entry = null;
            while ((entry = (TarArchiveEntry) ais.getNextEntry()) != null) {
                if (entry.getName().endsWith("/")) {
                    continue;
                }
                String[] names = entry.getName().split("/");
                String name = "";
                for (int i = 0; i < names.length; i++) {
                    String str = names[i];
                    name = name + File.separator + str;
                }

                if (actualFilePath.contains(name)) {
                    int b;
                    bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outPutFile));
                    while ((b = ais.read()) != -1) {
                        bufferedOutputStream.write(b);
                    }
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                    break;
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            logger.error("extractTarGzFile  failed! ", e);
            result = false;
            outPutFile.deleteOnExit();
        } finally {
            try {
                if (ais != null) {
                    ais.close();
                }
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
            } catch (Exception ex) {
                logger.error("close  failed! sourceTarGzFile is " + sourceTarGzFile, ex);
            }

        }
        if (result) {
            logger.info("extractTarGzFile sucess! sourceTarGzFile is " + sourceTarGzFile);
        }
        return result;
    }

    public static boolean extractZipFile(String sourceZipFile, String actualFilePath) {
        boolean result = false;
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new CheckedInputStream(new FileInputStream(sourceZipFile), new CRC32()));
            result = decompress(new File(actualFilePath), zis);
        } catch (Exception e) {
            logger.error("extractZipFile failed! sourceZipFile is " + sourceZipFile);
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    logger.error("close failed! sourceZipFile is " + sourceZipFile, e);
                }
            }
        }
        if (result) {
            logger.info("extractZipFile  sourceZipFile [ " + sourceZipFile + " ] sucess!");
        }
        return result;
    }

    private static boolean decompress(File destFile, ZipInputStream zis) throws IOException {
        boolean result = true;
        ZipEntry entry = null;
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(destFile));
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().contains((destFile.getName()))) {
                    int count;
                    byte data[] = new byte[BUFFER];
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        bos.write(data, 0, count);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("decompress failed! entry is " + entry, e);
            result = false;
            //清理垃圾文件
            destFile.deleteOnExit();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.error("close failed! entry is " + entry, e);
                }
            }
        }
        return result;
    }

    public static boolean fileIsEmpty(String path) {
        boolean result = true;
        File flie = new File(path);
        if (flie.exists() && flie.isFile()) {
            result = false;
        }
        return result;
    }
}
