package org.example;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.math.BigInteger;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class SpiderUpload {
    public static final String TEXT_RESET = "\u001B[0m";
    public static final String TEXT_GREEN = "\u001B[32m";
    public static final String TEXT_YELLOW = "\u001B[33m";
    public static final String TEXT_BLUE = "\u001B[34m";
    public static final String TEXT_PURPLE = "\u001B[35m";
    public static final String TEXT_CYAN = "\u001B[36m";
    public static final String TEXT_WHITE = "\u001B[37m";
    public static final String TEXT_RED = "\u001B[31m";
    public static final String PROJECT_ID = "blender-ableton-backup";
    public static final String BUCKET_NAME = "blender-ableton";

    private static final class MySpiderUpload {
        private Map<String, String> cache;
        private final String mainDir;
        private final List<String> directories;
        private final Path diffDrive;
        private int directoryCount;
        private int divider;
        private File previousFile;
        private final Cache cache1;
        private final Cache cache2;

        private MySpiderUpload(final String mainDir,
                               final List<String> directories, final Path diffDrive) {
            this.mainDir = mainDir;
            this.directories = directories;
            //an extra drive in case you have one to do a spider upload
            this.diffDrive = diffDrive;
            this.directoryCount = 0;
            this.divider = 32;
            this.previousFile = new File("D:\\");
            this.cache1 = new Cache("allfiles.txt");
            this.cache2 = new Cache("cache.txt");
            this.cache1.createCache();
            this.cache2.createCache();
            executeSpiderUpload();
        }

        private void executeSpiderUpload() {
            try {
                cache1.readMapping();
                cache2.readMapping();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.cache = cache2.getCache();
            List<String> selected = new ArrayList<>();
            for (String directory : directories) {
                //i only want to backup these folders and their sub directories
                boolean accept = directory.contentEquals("adobe after effects")
                        || directory.contentEquals("adobe character animation")
                        || directory.contentEquals("adobe pdf")
                        || directory.contentEquals("adobe photoshop")
                        || directory.contentEquals("adobe premiere")
                          || directory.contentEquals("blender")
                        || directory.contentEquals("Ableton")
                        || directory.contentEquals("patron")
                        || directory.contentEquals("year 2")
                        || directory.contentEquals("zbrush");
                if (accept) {
                    selected.add(directory);
                    String usable = mainDir.concat(directory);
                    Path usePath = Paths.get(usable);
                    try {
                        traverseAllSubDirectories(usePath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                traverseAllSubDirectories(diffDrive);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cache1.serializeMap();
            cache2.serializeMap();
            System.out.println(TEXT_GREEN+"✅   : Done!"+TEXT_RESET);

            for (String select : selected) {
                String dir = mainDir+""+select;
                System.out.print(TEXT_GREEN+"✅   :"+TEXT_RESET);
                System.out.print(" watching... ");
                System.out.print(TEXT_GREEN+dir+TEXT_RESET);
                System.out.println(" and all sub directories");
            }
            System.out.print(TEXT_GREEN+"✅   :"+TEXT_RESET);
            System.out.print(" watching... ");
            System.out.print(TEXT_GREEN+diffDrive+TEXT_RESET);
            System.out.println(" and all sub directories");
        }

        private int countSubDir(String dirPath) {
            directoryCount++;
            int count = 0;
            File f = new File(dirPath);
            File[] files = f.listFiles();

            if (files != null)
                for (File file : files) {
                    if (file.isDirectory()) {
                        count = countSubDir(file.getAbsolutePath());
                        directoryCount += count;
                    }
                }
            return count;
        }

        private void traverseAllSubDirectories(final Path p) throws Exception {
            directoryCount = 0;
            System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
            System.out.print(" Counting subdirectories of ");
            System.out.println(TEXT_GREEN+p+TEXT_RESET);
            countSubDir(p.toString());
            System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
            if (directoryCount > 1) {
                System.out.print(" "+directoryCount+" folders in ");
            } else {
                System.out.print(" "+directoryCount+" folder in ");
            }
            System.out.print(TEXT_GREEN+p+TEXT_RESET);
            System.out.println(" will receive spiders...");
            System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
            System.out.print(" Refreshing cache for ");
            System.out.println(TEXT_GREEN+p+TEXT_RESET+"...");

            Stream<Path> stream = walkDirectoryTree(p);

            //watch all subdirectories
            System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
            System.out.print(" Dispersing spiders for ");
            System.out.print(TEXT_GREEN+p+TEXT_RESET);
            System.out.println(" might take a while...");
            if (stream == null) {
                throw new Exception("No directories returned after walking!");
            }

            //FOR each directory have a map of directory and list all files present
            //get another file to contain this if new file then backup
            stream.forEach((dirName) -> {
                //first upload new files
                try {
                    checkForNewFiles(dirName.toFile().getCanonicalPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //then upload changed files
                Thread myThread = new Thread(() -> spiderWatch(dirName));
                myThread.start();
            });

            System.out.println(TEXT_GREEN+"✅   : Cache refreshed... "+p+TEXT_RESET);
            System.out.println(TEXT_GREEN+"✅   : Spiders dispersed... "+p+TEXT_RESET);
        }

        private void checkForNewFiles(String dirName) throws Exception {
            Map<String, List<String>> mapping = cache1.getMapping();
            System.out.println("Checking for new files in: "+dirName+"...");

            for (Map.Entry<String, List<String>> val : mapping.entrySet()) {
                if (dirName.equals(val.getKey())) {
                    //check if a new file exists
                    File dir = new File(val.getKey());
                    if (!dir.isDirectory()) throw new IllegalArgumentException("not a directory!");
                    //current files in the directory
                    File[] files = dir.listFiles();
                    if (files != null) {
                        boolean newFile = true;
                        for (File current : files) {
                            for (String previous : val.getValue()) {
                                if (previous.equals(current.getCanonicalPath())) {
                                    newFile = false;
                                    break;
                                }
                            }

                            if (newFile) {
                                Hashcode hashcode = new Hashcode(current.getCanonicalPath());
                                uploadToCloud(current.getCanonicalPath());
                                String fileHash = hashcode.calculateFileKey();
                                cache2.updateMap(current.getCanonicalPath(), fileHash, null, true);
                            }
                        }
                    }
                    System.out.println("Checked files if new in..."+dirName);
                    break;
                }
            }
            File dir = new File(dirName);
            if (!dir.isDirectory()) throw new IllegalArgumentException("not a directory!");
            //current files in the directory
            File[] files = dir.listFiles();
            List<String> allFiles = new ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    allFiles.add(file.getCanonicalPath());
                }
                cache1.updateMap(dir.getCanonicalPath(), null, allFiles, false);
            }
        }

        private Stream<Path> walkDirectoryTree(Path p) {
            Stream<Path> stream = null;

            try {
                stream = Files.find(p, 999999999, (path, basicFileAttributes) -> {
                    File myFile = path.toFile();
                    if (!basicFileAttributes.isDirectory() && !myFile.getName().contains(".part")) {
                        try {
                            refreshCacheData(myFile);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return basicFileAttributes.isDirectory()
                            && !myFile.getName().contentEquals("D:\\")
                            && !myFile.getName().contentEquals("E:\\") && myFile.canRead();
                });
            } catch (IOException e) {
                System.out.print(SpiderUpload.TEXT_RED+"❗    :"+ SpiderUpload.TEXT_RESET);
                System.out.print(" File ");
                System.out.print(SpiderUpload.TEXT_GREEN+ p.toString()+SpiderUpload.TEXT_RESET);
                System.out.print(" failed access and threw error ");
                System.out.println(SpiderUpload.TEXT_RED+e.toString()+SpiderUpload.TEXT_RESET);
            }
            return stream;
        }

        private void spiderWatch(Path dir) {
            try (WatchService service = FileSystems.getDefault().newWatchService()) {
                Map<WatchKey, Path> keyMap = new HashMap<>();
                keyMap.put(dir.register(service,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY),
                        dir);
                listenForEvents(service, keyMap);

            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }

        private void listenForEvents(final WatchService service,
                                     final Map<WatchKey, Path> keyMap) throws Exception {
            WatchKey watchKey;
            do {
                watchKey = service.take();
                Path eventDir = keyMap.get(watchKey);

                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path eventPath = (Path) event.context();
                    File current = new File(eventDir.toString() + '/' + eventPath.toString());
                    //sometimes a file will register as exists then be deleted in the next microsecond
                    //by a process, hence causing this method to throw Null Pointer Exception
                    //sleeping for 2 seconds will prevent this

                    //noinspection BusyWait
                    Thread.sleep(2000);

                    boolean newBlenderDir = current.isDirectory()
                            && eventDir.toString().equals("D:\\blender\\projects")
                            && kind.toString().equals("ENTRY_CREATE");

                    if (current.exists() && !current.isDirectory()
                            && !previousFile.equals(current)) {
                        System.out.println("Acquiring file lock...");
                        MutexLock mutexLock = new MutexLock(current.getCanonicalPath());
                        FileLock lock = mutexLock.getLock();

                        if (!lock.isShared() && !eventPath.toString().contains(".testfile")
                                && !eventPath.toString().contains(".fdmdownload")) {
                            System.out.println("lock acquired!");
                            //you got a lock, other process is done writing
                            if (!eventPath.toString().contains(".part")
                                    && !eventPath.toString().contains(".my.zip")) {
                                String dest = "D:\\blender\\projects\\all blends\\".concat(current.getName());

                                if (current.getName().endsWith(".blend")
                                        && !current.equals(new File(dest))) {
                                    isBlendThenCopy(current, new File(dest));
                                    handleAllUpdates(eventDir, kind, eventPath, current.getCanonicalPath());

                                } else if (!current.isDirectory()) {
                                    handleAllUpdates(eventDir, kind, eventPath, current.getCanonicalPath());

                                }
                            }
                            previousFile = current;
                        } else {
                            System.out.println("lock in use by another process");
                        }
                    } else if (newBlenderDir) {
                        Thread myThread = new Thread(() -> spiderWatch(current.toPath()));
                        myThread.start();
                    }
                }

            } while (watchKey.reset());
        }

        private void handleAllUpdates(final Path eventDir, final WatchEvent.Kind<?> kind,
                                      final Path eventPath, String key) throws Exception {
            Hashcode hashcode = new Hashcode(key);
            String fileHash = hashcode.calculateFileKey();

            if (!eventPath.toString().endsWith(".blend@")) {
                cache2.updateMap(key, fileHash, null, true);
                System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                System.out.println(TEXT_GREEN+" "+eventDir + ": "+TEXT_RESET+TEXT_PURPLE + kind + ": "+ TEXT_RESET +TEXT_GREEN+ eventPath+TEXT_RESET);
                uploadToCloud(key);
            } else {
                System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                System.out.println(TEXT_GREEN+" "+eventDir + ": "+TEXT_RESET+TEXT_PURPLE + eventPath + ": "+ TEXT_RESET +TEXT_YELLOW+ "temp blend file skipped"+TEXT_RESET);
            }
        }

        //finds suitable divisions for parallel upload
        private void findPerfectDivider(long length) {
            while ((length/divider) > 2000000000) {
                divider += 32;
            }
        }

        private void uploadToCloud(String dir) throws Exception {
            String finalZip = "";
            String originalDir = dir;
            File file = new File(dir);
            boolean isDir = file.isDirectory();
            if (isDir) {
                throw new Exception("object is a directory!");
            }
            String editedDir = dir.replace(":", "");
            String finalDir = editedDir.replace("\\", "/");
            BigInteger MAX = new BigInteger("100000000000");
            ZipFile.ZipResult isZippedData;
            long fileSize = file.length();

            if (fileSize > 2000000000) {
                ZipFile zipFile = new ZipFile();
                isZippedData = zipFile.zipFile(file.getCanonicalPath());

                //if after zipping file is less than 2GB execute normal upload, else delete the zip created
                //and just upload the original big file
                if (isZippedData.getPath() != null && isZippedData.getSize() < 2000000000) {
                    dir = isZippedData.getPath();
                    fileSize = isZippedData.getSize();
                    String _d = originalDir.replace(":", "");
                    String objectName = _d.replace("\\", "/");
                    finalDir = objectName.concat(".my.zip");

                } else if (isZippedData.getPath() != null) {
                    finalZip = isZippedData.getPath();

                    File zipTooBig = new File(finalZip);
                    boolean deleted = zipTooBig.delete();
                    if (!deleted) {
                        try {
                            throw new Exception("Zip file failed deletion");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
            if (fileSize > 2000000000) {
                //confirm if upload of above 100 GB will take place
                if (fileSize > MAX.longValueExact()) {
                    Scanner myObj = new Scanner(System.in);
                    System.out.println("File bigger than 100 GB, are you sure you wish to continue?"+TEXT_GREEN+"TYPE Y/N"+TEXT_RESET);
                    String prompt = myObj.nextLine();

                    if (prompt.equals("Y")) {
                        parallelUpload(dir, finalDir);
                    } else {
                        System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                        System.out.println(TEXT_YELLOW+" user skipped upload of "+TEXT_RESET+TEXT_GREEN+dir+TEXT_RESET);
                    }
                } else {
                    parallelUpload(dir, finalDir);
                }
            } else {
                if (finalZip.contains(".my.zip")) {
                    System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                    System.out.println(TEXT_PURPLE+" new file size after zipping: "
                            +TEXT_RESET+fileSize+" bytes");
                }
                System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                System.out.println(TEXT_PURPLE+" uploading..."+TEXT_RESET);
                UploadObject upload = new UploadObject();
                upload.uploadObject(PROJECT_ID, BUCKET_NAME, finalDir, dir);
            }
            if (dir.contains(".my.zip")) {
                File uploaded = new File(dir);
                boolean deleted = uploaded.delete();
                if (!deleted) {
                    try {
                        throw new Exception("Zip file failed deletion");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //critical section assertions made to confirm state of cloud storage and local
        private void parallelUpload(String dir, String finalDir) throws IOException {
            File file = new File(dir);
            System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
            System.out.println(TEXT_PURPLE+" uploading..."+TEXT_RESET);
            System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
            System.out.println(" "+TEXT_GREEN+ dir +TEXT_RESET+" is above 2GB, proceeding with parallel upload...");

            FileInputStream is = new FileInputStream(file);
            UploadObject upload = new UploadObject();

            findPerfectDivider(file.length());
            System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
            System.out.println(" file will be divided "+divider+" times...");
            String[] objects = writePartToTempFile(dir, file, is, upload, finalDir);

            //compose the uploads
            if (objects.length > 32) {
                double requestNo = Math.ceil(objects.length/32.0);
                String[] finalCombine = new String[(int) requestNo];
                boolean isOdd = false;
                if (objects.length % 32 != 0) {
                    isOdd = true;
                }
                for (int i = 0; i < requestNo; i++) {
                    String[] temp = Arrays.copyOfRange(objects, i*32, (i+1)*32);
                    if (isOdd && i == requestNo - 1) {
                        temp = Arrays.copyOfRange(objects, i*32, objects.length);
                    }
                    ComposeObject compose = new ComposeObject();
                    compose.composeObject(BUCKET_NAME, temp, finalDir +".final"+i, PROJECT_ID);
                    finalCombine[i] = finalDir +".final"+i;
                    deleteAllTempObjects(temp);
                }
                if (finalCombine.length > 32) {
                    finalCombine = recurseComposing(finalDir, finalCombine);
                }
                //final composition
                ComposeObject compose = new ComposeObject();
                compose.composeObject(BUCKET_NAME, finalCombine, finalDir, PROJECT_ID);

                //delete temp files in cloud
                deleteAllTempObjects(finalCombine);

            } else {
                ComposeObject compose = new ComposeObject();
                compose.composeObject(BUCKET_NAME, objects, finalDir, PROJECT_ID);

                //delete temp files in cloud
                deleteAllTempObjects(objects);
            }
            divider = 32;
        }

        private void deleteAllTempObjects(String[] finalCombine) {
            for (String str : finalCombine) {
                DeleteObject deleteObject = new DeleteObject();
                deleteObject.deleteObject(PROJECT_ID, BUCKET_NAME, str);
            }
        }

        private String[] writePartToTempFile(String dir, File file, FileInputStream is,
                                             UploadObject upload, String finalDir)
                throws IOException {
            byte[] buf = new byte[(int)(file.length()/divider)];
            String[] objects = new String[divider+1];
            int read = 0;
            while((is.read(buf)) > 0) {
                String newDir = dir +".part"+read;
                String finalPart = finalDir.concat(".part"+read);
                System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                System.out.println(" writing to... "+TEXT_GREEN+newDir+TEXT_RESET);
                FileOutputStream os = new FileOutputStream(newDir);
                os.write(buf);
                os.close();
                objects[read] = finalPart;
                read++;

                //upload the parts
                File temp = new File(newDir);
                System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                System.out.println(" done writing, uploading... "+TEXT_GREEN+newDir+TEXT_RESET+" size(bytes): "+TEXT_PURPLE+temp.length()+TEXT_RESET);
                upload.uploadObject(PROJECT_ID, BUCKET_NAME, finalPart, newDir);
                boolean deleted = temp.delete();
                if (!deleted) {
                    try {
                        throw new Exception("Deletion had an error!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                System.out.println(" done uploading and cleaned directory... "+TEXT_GREEN+newDir+TEXT_RESET);
            }
            is.close();
            return objects;
        }

        private String[] recurseComposing(String finalDir, String[] finalCombine) {
            //if final length is 64 then 64/32 2 more requests
            int check = finalCombine.length/32;
            String[] finalArr = new String[check];
            int count = 0;

            while (check > 0) {
                String[] temp = Arrays.copyOfRange(finalCombine, count*32, (count+1)*32);
                ComposeObject compose = new ComposeObject();
                compose.composeObject(BUCKET_NAME, temp, finalDir +".final"+count, PROJECT_ID);

                for (int i = 0; i < finalCombine.length; i++) {
                    //if the array value has already been used, then delete it from database
                    if (i < (count+1)*32) {
                        //creating final0 will overwrite previous values hence no need
                        // for deletion
                        if (i != 0) {
                            DeleteObject deleteObject = new DeleteObject();
                            deleteObject.deleteObject(PROJECT_ID, BUCKET_NAME, finalCombine[i]);
                        }
                    } else {
                        break;
                    }
                }
                finalArr[count] = finalDir +".final"+count;
                check--;
                count++;
            }

            if (finalArr.length > 32) {
                finalArr = recurseComposing(finalDir, finalArr);
            }

            return finalArr;
        }

        private void refreshCacheData(final File myFile) throws Exception {
            Map<String, String> toRemove = new HashMap<>();
            boolean cacheEmpty = true;
            Hashcode hashcode = new Hashcode(myFile.getCanonicalPath());
            String fileHash = hashcode.calculateFileKey();

            for (Map.Entry<String, String> val : cache.entrySet()) {
                try {
                    toRemove.put(val.getKey(), fileHash);
                    cache2.updateMap(val.getKey(), fileHash, null, true);
                    if (myFile.getCanonicalPath().equals(val.getKey())
                            && !fileHash.equals(val.getValue())) {
                        cacheEmpty = false;
                        uploadToCloud(myFile.getCanonicalPath());
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (cacheEmpty) {
                cache2.updateMap(myFile.getCanonicalPath(), fileHash, null, true);
            }

            //prevents java.util.ConcurrentModificationException
            for (Map.Entry<String, String> val : toRemove.entrySet()) {
                Map<String, String> temp = new HashMap<>(cache);
                cache.remove(val.getKey());
                if (cache.size() != temp.size()-1) throw new ArrayIndexOutOfBoundsException("cache wasn't reduced");
            }
        }

        private void isBlendThenCopy(File source, File dest) throws IOException {
            FileUtils.copyFile(source, dest);
        }

    }

    public final void build(final String mainDir,
                            final List<String> directories,
                            final Path diffDrive) {
        new MySpiderUpload(mainDir, directories, diffDrive);
    }
}
