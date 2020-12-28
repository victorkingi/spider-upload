import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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

    private static final class MySpiderUpload {
        private final Map<String, Long> cache;
        private final String mainDir;
        private final ImmutableList<String> directories;
        private final Path diffDrive;
        private int directoryCount;

        private MySpiderUpload(final Map<String, Long> cache, final String mainDir,
                               final ImmutableList<String> directories, final Path diffDrive) throws IOException {
            this.cache = cache;
            this.mainDir = mainDir;
            this.directories = directories;
            //an extra drive in case you have one to do a spider upload
            this.diffDrive = diffDrive;
            this.directoryCount = 0;
            executeSpiderUpload();
        }

        private void executeSpiderUpload() throws IOException {
            createCache();
            readCacheFile();
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
                    traverseAllSubDirectories(usePath);
                }
            }
            traverseAllSubDirectories(diffDrive);
            System.out.println(TEXT_GREEN+"✅   : Done!"+TEXT_RESET);

            for (String select : selected) {
                String dir = mainDir+""+select;
                System.out.print(TEXT_GREEN+"✅    :"+TEXT_RESET);
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

        private void traverseAllSubDirectories(final Path p) throws IOException {
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
            System.out.println(TEXT_GREEN+p+TEXT_RESET);
            Stream<Path> stream = Files.find(p, 999999999, (path, basicFileAttributes) -> {
                File myFile = path.toFile();
                try {
                    refreshCacheData(myFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return basicFileAttributes.isDirectory()
                        && !myFile.getName().contentEquals("D:\\")
                        && !myFile.getName().contentEquals("E:\\") && myFile.canRead();
            });
            System.out.println(TEXT_GREEN+"✅   : Cache refreshed... "+p+TEXT_RESET);


            //watch all subdirectories
            System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
            System.out.print(" Dispersing spiders for ");
            System.out.print(TEXT_GREEN+p+TEXT_RESET);
            System.out.println(" might take a while");
            stream.forEach((dirName) -> {
                Thread myThread = new Thread(() -> spiderWatch(dirName));
                myThread.start();
            });
            System.out.println(TEXT_GREEN+"✅   : Spiders dispersed... "+p+TEXT_RESET);
        }

        private void spiderWatch(Path dir) {
            try (WatchService service = FileSystems.getDefault().newWatchService()) {
                Map<WatchKey, Path> keyMap = new HashMap<>();
                keyMap.put(dir.register(service,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY),
                        dir);
                listenForEvents(service, keyMap);

            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }

        private void listenForEvents(final WatchService service,
                                     final Map<WatchKey, Path> keyMap) throws InterruptedException, IOException {
            WatchKey watchKey;
            do {
                watchKey = service.take();
                Path eventDir = keyMap.get(watchKey);

                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path eventPath = (Path)event.context();
                    File current = new File(eventDir.toString()+'/'+eventPath.toString());
                    if (current.exists() && !current.isDirectory()) {
                        checkForChanges(eventDir, kind, eventPath, current);
                    }
                }

            } while (watchKey.reset());
        }

        private void checkForChanges(final Path eventDir, final WatchEvent.Kind<?> kind,
                                     final Path eventPath, final File current) throws IOException {
            boolean set = false;

            for (Map.Entry<String, Long> val : cache.entrySet()) {
                if (current.getCanonicalPath().equals(val.getKey())) {
                    set = true;
                }
                if (current.getCanonicalPath().equals(val.getKey())
                        && current.lastModified() != val.getValue()) {
                    handleAllUpdates(eventDir, kind, eventPath, current, val.getKey());
                    set = true;
                }
            }
            if (!set) {
                handleAllUpdates(eventDir, kind, eventPath, current, current.getCanonicalPath());
            }
        }

        private void handleAllUpdates(final Path eventDir, final WatchEvent.Kind<?> kind,
                                      final Path eventPath, final File current, String key) throws IOException {
            cache.put(key, current.lastModified());
            updateCacheFile(key, current.lastModified());
            System.out.println(eventDir + ": " + kind + ": " + eventPath);
            System.out.println("\nuploading...");
            uploadToCloud(key);
        }

        private void uploadToCloud(String dir) throws IOException {
            String editedDir = dir.replace(":", "");
            String finalDir = editedDir.replace("\\", "/");
            UploadObject upload = new UploadObject();
            upload.uploadObject("blender-ableton-backup", "blender-ableton", finalDir, dir);
        }

        private void refreshCacheData(final File myFile) throws IOException {
            boolean containsFile = false;
            for (Map.Entry<String, Long> val : cache.entrySet()) {
                try {
                    if (myFile.getCanonicalPath().equals(val.getKey())
                            && myFile.lastModified() != val.getValue()) {
                        cache.put(val.getKey(), myFile.lastModified());
                        updateCacheFile(val.getKey(), myFile.lastModified());
                        System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                        System.out.println(TEXT_PURPLE+"uploading..."+TEXT_RESET);
                        uploadToCloud(myFile.getCanonicalPath());
                    }
                    if (myFile.getCanonicalPath().equals(val.getKey())) {
                        containsFile = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (!containsFile) {
                cache.put(myFile.getCanonicalPath(), myFile.lastModified());
                updateCacheFile(myFile.getCanonicalPath(), myFile.lastModified());
            }
        }
        private void createCache() {
            try {
                File myObj = new File("cache.txt");
                if (myObj.createNewFile()) {
                    System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                    System.out.print(" Cache created: ");
                    System.out.println(TEXT_GREEN+myObj.getName()+TEXT_RESET);

                } else {
                    System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                    System.out.println(" Cache exists skipping creation.");
                }
            } catch (IOException e) {
                System.out.println(TEXT_RED+"An error occurred."+TEXT_RESET);
                e.printStackTrace();
            }
        }
        private void updateCacheFile(String key, Long val) {
            try {
                FileWriter myWriter = new FileWriter("cache.txt", true);
                myWriter.append(key).append("   :").append(String.valueOf(val))
                        .append("\n");
                myWriter.close();
            } catch (IOException e) {
                System.out.println(TEXT_RED+"An error occurred."+TEXT_RESET);
                e.printStackTrace();
            }
        }
        private void readCacheFile() {
            try {
                File myObj = new File("cache.txt");
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    String[] arr = data.split(" {3}:");
                    cache.put(arr[0], Long.valueOf(arr[1]));
                }
                myReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }

    }
    public final void build(final Map<String, Long> cache, final String mainDir,
                                     final ImmutableList<String> directories,
                                     final Path diffDrive) throws IOException {
        new MySpiderUpload(cache, mainDir, directories, diffDrive);
    }
}
