import java.io.*;
import java.util.zip.*;

public class ZipFile {
    public long zipFile(String filePath) {
        try {
            File file = new File(filePath);
            String zipFileName = file.getPath().concat(".zip");

            FileOutputStream fos = new FileOutputStream(zipFileName);
            FileInputStream fis = new FileInputStream(file);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zos.setLevel(9);
            System.out.print(SpiderUpload.TEXT_CYAN+"i    :"+ SpiderUpload.TEXT_RESET);
            System.out.println(SpiderUpload.TEXT_PURPLE+" file bigger than 2GB, zipping to ..."
                    +SpiderUpload.TEXT_RESET+SpiderUpload.TEXT_GREEN+zipFileName+SpiderUpload.TEXT_RESET+" size: "+file.length()+" bytes");
            zos.putNextEntry(new ZipEntry(filePath));
            byte[] buf = new byte[1024];
            int length;
            while((length = fis.read(buf)) >= 0) {
                zos.write(buf, 0, length);
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
