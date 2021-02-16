import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashcode {
    private final String fileLocation;

    public Hashcode(final String fileLocation) {
        this.fileLocation = fileLocation;

    }

    private String bytesToHex(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String calculateFileKey() throws IOException {
        int buff = 8192;
        try {
            RandomAccessFile file = new RandomAccessFile(fileLocation, "r");
            MessageDigest hashSum = MessageDigest.getInstance("SHA-1");

            byte[] buffer = new byte[buff];
            long read = 0;
            long offset = file.length();

            int unitsize;
            while (read < offset) {
                unitsize = (int) (((offset - read) >= buff) ? buff : (offset - read));
                file.read(buffer, 0, unitsize);
                hashSum.update(buffer, 0, unitsize);
                read += unitsize;
            }
            file.close();
            byte[] partialHash = hashSum.digest();
            return bytesToHex(partialHash);

        } catch (FileNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
