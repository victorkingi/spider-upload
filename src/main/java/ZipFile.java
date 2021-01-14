import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.*;

public class ZipFile {
    static final class ZipResult {
        private final long size;
        private final String path;

        private ZipResult(final long size, final String path) {
            this.size = size;
            this.path = path;
        }

        public String getPath() { return path; }

        public long getSize() { return size; }
    }
    public ZipResult zipFile(String filePath) {
        try {
            File file = new File(filePath);
            File[] paths;
            FileSystemView fsv = FileSystemView.getFileSystemView();

            // returns pathnames for files and directory
            paths = File.listRoots();
            Map<Integer, String> availableDrives = new HashMap<>();
            String zipFileName = null;
            int count = 0;
            for(File path:paths) {
                // prints file and directory paths
                if (fsv.getSystemTypeDescription(path).equals("Local Disk")) {
                    if (path.getFreeSpace() > file.length()) {
                        count++;
                        availableDrives.put(count, path.getPath());
                    }
                }
            }

            if (availableDrives.isEmpty()) {
                System.out.print(SpiderUpload.TEXT_CYAN+"i    :"+ SpiderUpload.TEXT_RESET);
                System.out.println(SpiderUpload.TEXT_YELLOW+" no available disk space for zipping ..."
                        +SpiderUpload.TEXT_RESET+SpiderUpload.TEXT_GREEN+filePath
                        +SpiderUpload.TEXT_RESET+" too big size: "+file.length()/1073741824+" GB");

            } else {
                Scanner myObj = new Scanner(System.in);
                System.out.println("Choose preferred temp drive? (Enter a number)");
                for (Map.Entry<Integer, String> drive : availableDrives.entrySet()) {
                    System.out.println(drive.getKey()+") "+drive.getValue());
                }
                String prompt = myObj.nextLine();
                boolean found = false;
                for (Map.Entry<Integer, String> drive : availableDrives.entrySet()) {
                    if (prompt.equals(drive.getKey().toString())) {
                        found = true;
                        File newDir = new File(drive.getValue().concat("\\backup-temp"));
                        boolean created;
                        if (!newDir.exists()) {
                            created = newDir.mkdirs();
                        } else {
                            created = true;
                        }
                        if (created) {
                            zipFileName = newDir.getCanonicalPath().concat("\\").concat(file.getName()).concat(".my.zip");
                        }
                    }
                }
                var defaultDrive = availableDrives.entrySet().stream().findFirst();
                if (!found && defaultDrive.isPresent()) {
                    System.out.println("No drive selected, proceeding with (default) "+defaultDrive.get().getValue());
                    File newDir = new File(defaultDrive.get().getValue().concat("\\backup-temp"));
                    boolean created;
                    if (!newDir.exists()) {
                        created = newDir.mkdirs();
                    } else {
                        created = true;
                    }
                    if (created) {
                        zipFileName = newDir.getCanonicalPath().concat("\\").concat(file.getName()).concat(".my.zip");
                    }
                }

                if (zipFileName != null) {
                    FileOutputStream fos = new FileOutputStream(zipFileName);
                    FileInputStream fis = new FileInputStream(file);
                    ZipOutputStream zos = new ZipOutputStream(fos);
                    zos.setLevel(9);
                    System.out.print(SpiderUpload.TEXT_CYAN+"i    :"+ SpiderUpload.TEXT_RESET);
                    System.out.println(SpiderUpload.TEXT_PURPLE+" file bigger than 2GB, zipping to ..."
                            +SpiderUpload.TEXT_RESET+SpiderUpload.TEXT_GREEN+zipFileName+SpiderUpload.TEXT_RESET+" size: "+file.length()+" bytes");
                    zos.putNextEntry(new ZipEntry(filePath));
                    byte[] buf = new byte[8192];
                    int length;
                    double percent;
                    double counter = 0;
                    DecimalFormat df = new DecimalFormat("###.###");
                    while((length = fis.read(buf)) >= 0) {
                        zos.write(buf, 0, length);
                        counter += 8192;
                        percent = (counter/file.length()) * 100.000;
                        System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
                        System.out.print(df.format(percent)+" % complete");
                    }
                    System.out.println();
                    zos.closeEntry();
                    fis.close();
                    zos.close();
                    File zipped = new File(zipFileName);
                    return new ZipResult(zipped.length(), zipped.getCanonicalPath());
                }
            }

        } catch (FileNotFoundException ex) {
            System.err.format("The file %s does not exist", filePath);
        } catch (IOException ex) {
            System.err.println("I/O error: " + ex);
        }
        return new ZipResult(0L, null);
    }
}
