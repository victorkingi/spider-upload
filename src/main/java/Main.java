import com.google.common.collect.ImmutableList;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Path extraPath = Paths.get("E:\\virtualbox");
        final String mainDir = "D:\\";
        File file = new File(mainDir);
        String[] tempDirectories = file.list((current, name) -> {
            File test =  new File(current, name);
            //exclude system directories
            return test.isDirectory()
                    && !test.isHidden()
                    && !test.getName().contentEquals("Program Files")
                    && !test.getName().contentEquals("WindowsApps")
                    && !test.getName().contentEquals("WpSystem");
        });
        if (tempDirectories == null) {
            try {
                throw new Exception("No directories found");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            List<String> temp = Arrays.asList(tempDirectories);
            Map<String, Long> cache = new HashMap<>();
            ImmutableList<String> directories = ImmutableList.<String>builder()
                    .addAll(temp).build();
            SpiderUpload start = new SpiderUpload();
            start.build(cache, mainDir, directories, extraPath);
        }
    }
}
