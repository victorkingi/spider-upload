package org.example;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.example.SpiderUpload.*;

public class Cache implements Serializable {
    private final String cacheFile;
    private Map<String, String> cache;
    private Map<String, List<String>> mapping;
    private final Map<String, List<String>> tempMapping;

    public Cache(String cacheFile) {
        this.cacheFile = cacheFile;
        this.cache = new HashMap<>();
        this.mapping = new HashMap<>();
        this.tempMapping = new HashMap<>();
    }
    public Map<String, String> getCache() {return cache; }
    public Map<String, List<String >> getMapping() { return mapping; }

    public void updateMap(String dirName, String hashKey, List<String> filesPresent, boolean isCache) {
        if (isCache) {
            if (filesPresent != null) throw new IllegalArgumentException("Illegal value!");
            cache.put(dirName, hashKey);
        } else {
            tempMapping.put(dirName, filesPresent);
        }
    }

    protected void serializeMap() {
        try {
            File obj = new File(cacheFile);
            ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(obj));

            if (cacheFile.equals("allfiles.txt")) {
                mapping = new HashMap<>(tempMapping);
                System.out.println("serializing mapping...size: "+mapping.size());
                ous.writeObject(mapping);
            } else if (cacheFile.equals("cache.txt")) {
                System.out.println("serializing cache...size: "+cache.size());
                ous.writeObject(cache);
            }
            ous.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("map serialized");
    }

    protected void createCache() {
        try {
            File myObj = new File(cacheFile);
            if (myObj.createNewFile()) {
                System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                System.out.print(" Cache created: ");
                System.out.println(TEXT_GREEN+myObj.getName()+TEXT_RESET);

            } else {
                System.out.print(TEXT_CYAN+"i    :"+TEXT_RESET);
                System.out.println(" Cache exists skipping creation."+cacheFile);
            }
        } catch (IOException e) {
            System.out.println(TEXT_RED+"An error occurred."+TEXT_RESET);
            e.printStackTrace();
        }
    }
    @SuppressWarnings("unchecked")
    protected void readMapping() throws IOException {
        File file = new File(cacheFile);
        if (file.length() > 0) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            try {
                if (cacheFile.equals("cache.txt")) {
                    System.out.println("reading cache...");
                    cache = (Map<String, String>) ois.readObject();
                    System.out.println("cache read");
                } else if (cacheFile.equals("allfiles.txt")) {
                    System.out.println("reading allfiles...");
                    mapping = (Map<String, List<String>>) ois.readObject();
                    System.out.println("allfiles read");
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            ois.close();

            new FileOutputStream(cacheFile).close();
        }
    }
}
