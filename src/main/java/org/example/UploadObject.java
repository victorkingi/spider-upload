package org.example;

import com.google.cloud.storage.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UploadObject {
    public void uploadObject(
            String projectId, String bucketName, String objectName, String filePath) {
        // The ID of your GCP project
        // String projectId = "your-project-id";

        // The ID of your GCS bucket
        // String bucketName = "your-unique-bucket-name";

        // The ID of your GCS object
        // String objectName = "your-object-name";

        // The path to your file to upload
        // String filePath = "path/to/your/file"

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        Blob uploaded = null;
        try {
            uploaded = storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            System.out.print(SpiderUpload.TEXT_RED+"❗    :"+ SpiderUpload.TEXT_RESET);
            System.out.print(" File ");
            System.out.print(SpiderUpload.TEXT_GREEN+filePath+SpiderUpload.TEXT_RESET);
            System.out.print(" failed upload and threw error ");
            System.out.println(SpiderUpload.TEXT_RED+e.toString()+SpiderUpload.TEXT_RESET);
        }
        if (uploaded == null) {
            try {
                throw new Exception("upload error!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.print(SpiderUpload.TEXT_CYAN+"i    :"+ SpiderUpload.TEXT_RESET);
            System.out.print(" File ");
            System.out.print(SpiderUpload.TEXT_GREEN+filePath+SpiderUpload.TEXT_RESET);
            System.out.print(" uploaded to bucket ");
            System.out.print(SpiderUpload.TEXT_GREEN+bucketName+SpiderUpload.TEXT_RESET);
            System.out.print(" as ");
            System.out.println(SpiderUpload.TEXT_GREEN+objectName+SpiderUpload.TEXT_RESET
                    +" size(bytes): "+ SpiderUpload.TEXT_PURPLE+uploaded.getSize()+ SpiderUpload.TEXT_RESET);
        }
    }
}