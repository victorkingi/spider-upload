import java.io.*;
import java.util.zip.*;

public class ZipFile {
    public long zipFile(String filePath) {
        try {
            File file = new File(filePath);
            String zipFileName = file.getName().concat(".zip");

            FileOutputStream fos = new FileOutputStream(zipFileName);
            FileInputStream fis = new FileInputStream(file);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zos.setLevel(9);

            zos.putNextEntry(new ZipEntry(file.getName()));
            int divider = 2;
            while ((file.length()/divider) > 2000000000) {
                divider += 2;
            }
            byte[] buf = new byte[(int)(file.length()/divider)];
            while((fis.read(buf)) > 0) {
                zos.write(buf, 0, buf.length);
            }
            zos.closeEntry();
            fis.close();
            zos.close();

        } catch (FileNotFoundException ex) {
            System.err.format("The file %s does not exist", filePath);
        } catch (IOException ex) {
            System.err.println("I/O error: " + ex);
        }
        File zipped = new File(filePath.concat(".zip"));
        return zipped.length();
    }
}
