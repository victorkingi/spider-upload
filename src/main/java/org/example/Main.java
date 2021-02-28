package org.example;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        final Path extraPath = Paths.get("E:\\virtualbox");
        final String mainDir = "D:\\";
        final File file = new File(mainDir);
        final String[] tempDirectories = file.list((current, name) -> {
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
            final List<String> temp = Arrays.asList(tempDirectories);
            final SpiderUpload start = new SpiderUpload();
            start.build(mainDir, temp, extraPath);
        }
    }
}
