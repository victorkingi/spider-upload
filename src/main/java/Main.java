import com.google.common.collect.ImmutableList;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class Main {

    //we should have a least of all files that have changed within a session
    public static void main(String... args) throws IOException {
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
        assert tempDirectories != null;
        List<String> temp = Arrays.asList(tempDirectories);
        Map<String, Long> cache = new HashMap<>();
        ImmutableList<String> directories = ImmutableList.<String>builder()
                .addAll(temp).build();
        SpiderUpload start = new SpiderUpload();
        start.build(cache, mainDir, directories, extraPath);
    }
}
