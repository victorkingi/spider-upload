package org.example;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MutexLock {
    private final Log LOG = LogFactory.getLog(MutexLock.class);
    private final FileChannel fc;
    private final RandomAccessFile randomAccessFile;

    MutexLock(String fileName) throws FileNotFoundException {
        randomAccessFile = new RandomAccessFile(fileName, "rw");
        fc = randomAccessFile.getChannel();
    }
    public FileLock getLock() {
        try (fc) {
            try (randomAccessFile) {
                try (FileLock fileLock = fc.lock()) {
                    return fileLock;
                }
            }
        } catch (OverlappingFileLockException | IOException ex) {
            LOG.error("Exception occurred while trying to get a lock on File... " + ex.getMessage());
        }
        return null;
    }
}
