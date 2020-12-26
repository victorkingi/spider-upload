import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

public final class SpiderUpload {

    private static final class MySpiderUpload {
        private final Map<String, Long> cache;
        private final String mainDir;
        private final ImmutableList<String> directories;
        private final Path diffDrive;

        private MySpiderUpload(final Map<String, Long> cache, final String mainDir,
                               final ImmutableList<String> directories, final Path diffDrive) throws IOException {
            this.cache = cache;
            this.mainDir = mainDir;
            this.directories = directories;
            //an extra drive incase you have one to do a spider upload
            this.diffDrive = diffDrive;
            executeSpiderUpload();
        }

        private void executeSpiderUpload() throws IOException {
            createCache();
            readCacheFile();
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
                    String usable = mainDir.concat(directory);
                    Path usePath = Paths.get(usable);
                    traverseAllSubDirectories(usePath);
                }
            }
            traverseAllSubDirectories(diffDrive);
        }

        private void traverseAllSubDirectories(final Path p) throws IOException {
            Stream<Path> stream = Files.find(p, 999999999, (path, basicFileAttributes) -> {
                File myFile = path.toFile();
                refreshCacheData(myFile);
                return basicFileAttributes.isDirectory()
                        && !myFile.getName().contentEquals("D:\\")
                        && !myFile.getName().contentEquals("E:\\");
            });
            //watch all subdirectories
            stream.forEach((dirName) -> {
                Thread myThread = new Thread(() -> spiderWatch(dirName));
                myThread.start();
            });
        }

        private void refreshCacheData(final File myFile) {
            for (Map.Entry<String, Long> val : cache.entrySet()) {
                try {
                    if (myFile.getCanonicalPath().equals(val.getKey())
                            && myFile.lastModified() != val.getValue()) {
                        cache.put(val.getKey(), myFile.lastModified());
                        updateCacheFile(val.getKey(), myFile.lastModified());
                        System.out.println("uploading...");
                        uploadToCloud(myFile.getCanonicalPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void spiderWatch(Path dir) {
            System.out.println("watching..."+dir);
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
            System.out.println("uploading...");
            uploadToCloud(key);
        }

        private void uploadToCloud(String dir) throws IOException {
            String editedDir = dir.replace(":", "");
            String finalDir = editedDir.replace("\\", "/");
            UploadObject upload = new UploadObject();
            upload.uploadObject("blender-ableton-backup", "blender-ableton", finalDir, dir);
        }

        private void createCache() {
            try {
                File myObj = new File("cache.txt");
                if (myObj.createNewFile()) {
                    System.out.println("File created: " + myObj.getName());

                } else {
                    System.out.println("File already exists.");
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }

        private void updateCacheFile(String key, Long val) {
            try {
                FileWriter myWriter = new FileWriter("cache.txt", true);
                myWriter.append(key).append("   :").append(String.valueOf(val))
                        .append("\n");
                myWriter.close();
                System.out.println("Successful write.");
            } catch (IOException e) {
                System.out.println("An error occurred.");
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
    @Nonnull public final MySpiderUpload build(final Map<String, Long> cache, final String mainDir,
                                                         final ImmutableList<String> directories,
                                                         final Path diffDrive) throws IOException {
        return new MySpiderUpload(cache, mainDir, directories, diffDrive);
    }
}
