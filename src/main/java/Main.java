import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class Main {

    private static class LastModified {
        // public member variable
        public Map<String, Long> lastModifiedCache;

        // default constructor
        public LastModified() {
            lastModifiedCache = new HashMap<>();
        }
    }


    //we should have a least of all files that have changed within a session
    public static void main(String... args) throws IOException {
        LastModified last = new LastModified();
        final String mainDir = "D:\\";
        File file = new File(mainDir);
        createCache();
        readCacheFile(last);
        String[] directories = file.list((current, name) -> {
            File test =  new File(current, name);
            return test.isDirectory()
                    && !test.isHidden()
                    && !test.getName().contentEquals("Program Files")
                    && !test.getName().contentEquals("WindowsApps")
                    && !test.getName().contentEquals("WpSystem");
        });
        assert directories != null;
        for (String directory : directories) {
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
                findAllSubs(last, usePath);
            }
        }
        Path otherPath = Paths.get("E:\\virtualbox");
        findAllSubs(last, otherPath);
    }

    private static void findAllSubs(LastModified last, Path mainPath) throws IOException {
        Stream<Path> stream = Files.find(mainPath, 999999999, (path, basicFileAttributes) -> {
            File myFile = path.toFile();
            //if last modified not equal to last
            for (Map.Entry<String, Long> val : last.lastModifiedCache.entrySet()) {
                try {
                    if (myFile.getCanonicalPath().equals(val.getKey())
                            && myFile.lastModified() != val.getValue()) {
                        last.lastModifiedCache.put(val.getKey(), myFile.lastModified());
                        updateCacheFile(val.getKey(), myFile.lastModified());
                        System.out.println("uploading...");
                        uploadToCloud(myFile.getCanonicalPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return basicFileAttributes.isDirectory()
                    && !myFile.getName().contentEquals("D:\\");
        });

        //watch all subdirectories
        stream.forEach((dirName) -> {
            Thread myThread = new Thread(() -> spiderWatch(dirName, last));
            myThread.start();
        });
    }

    private static void spiderWatch(Path dir, LastModified last) {
        System.out.println("watching..."+dir);
        try (WatchService service = FileSystems.getDefault().newWatchService()) {
            Map<WatchKey, Path> keyMap = new HashMap<>();
            keyMap.put(dir.register(service,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY),
                    dir);
            WatchKey watchKey;

            do {
                watchKey = service.take();
                Path eventDir = keyMap.get(watchKey);

                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path eventPath = (Path)event.context();
                    File current = new File(eventDir.toString()+'/'+eventPath.toString());
                    if (current.exists() && !current.isDirectory()) {
                        //only files
                        boolean set = false;

                        for (Map.Entry<String, Long> val : last.lastModifiedCache.entrySet()) {
                            if (current.getCanonicalPath().equals(val.getKey())) {
                                    set = true;
                            }
                            //if not in map then never updated so set it in map, cache and upload new file
                            if (current.getCanonicalPath().equals(val.getKey())
                                    && current.lastModified() != val.getValue()) {
                                last.lastModifiedCache.put(val.getKey(), current.lastModified());
                                updateCacheFile(val.getKey(), current.lastModified());
                                System.out.println(eventDir + ": " + kind + ": " + eventPath);
                                System.out.println("uploading...");
                                uploadToCloud(val.getKey());
                                set = true;
                            }
                        }

                        //we are now sure that either it is a new file or it was never edited before
                        if (!set) {
                            last.lastModifiedCache.put(current.getCanonicalPath(), current.lastModified());
                            updateCacheFile(current.getCanonicalPath(), current.lastModified());
                            System.out.println(eventDir + ": " + kind + ": " + eventPath);
                            System.out.println("uploading...");
                            uploadToCloud(current.getCanonicalPath());
                        }
                    }
                }

            } while (watchKey.reset());

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void uploadToCloud(String dir) throws IOException {
        String editedDir = dir.replace(":", "");
        String finalDir = editedDir.replace("\\", "/");
        UploadObject upload = new UploadObject();
        upload.uploadObject("blender-ableton-backup", "blender-ableton", finalDir, dir);
    }

    private static void createCache() {
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

    private static void updateCacheFile(String key, Long val) {
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
    private static void readCacheFile(LastModified last) {
        try {
            File myObj = new File("cache.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] arr = data.split(" {3}:");
                last.lastModifiedCache.put(arr[0], Long.valueOf(arr[1]));
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
