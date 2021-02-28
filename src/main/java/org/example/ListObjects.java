package org.example;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.util.HashMap;
import java.util.Map;

public class ListObjects {
    public Map<String, Long> listObjects(String projectId, String bucketName) {
        // The ID of your GCP project
        // String projectId = "your-project-id";

        // The ID of your GCS bucket
        // String bucketName = "your-unique-bucket-name";

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        Bucket bucket = storage.get(bucketName);
        Page<Blob> blobs = bucket.list();
        Map<String, Long> files = new HashMap<>();

        for (Blob blob : blobs.iterateAll()) {
            files.put(blob.getName(), blob.getSize());
        }
        return files;
    }
}