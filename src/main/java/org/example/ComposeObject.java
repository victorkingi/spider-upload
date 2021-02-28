package org.example;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class ComposeObject {
    public void composeObject(
            String bucketName,
            String[] objects,
            String targetObjectName,
            String projectId) {

        if (objects.length > 32) {
            try {
                throw new Exception("Object number greater than 32!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

        Storage.ComposeRequest composeRequest =
                Storage.ComposeRequest.newBuilder()
                        // addSource takes varargs, so you can put as many objects here as you want, up to the
                        // max of 32
                        .addSource(objects)
                        .setTarget(BlobInfo.newBuilder(bucketName, targetObjectName).build())
                        .build();

        Blob compositeObject = storage.compose(composeRequest);

        System.out.print(SpiderUpload.TEXT_CYAN+"i    :"+ SpiderUpload.TEXT_RESET);
        System.out.print(" New composite object ");
        System.out.print(SpiderUpload.TEXT_GREEN+compositeObject.getName()+SpiderUpload.TEXT_RESET+" size(bytes): "
                + SpiderUpload.TEXT_PURPLE+compositeObject.getSize()+SpiderUpload.TEXT_RESET);
        System.out.print(" was created by combining ");
        for (String str : objects) {
            System.out.println(SpiderUpload.TEXT_GREEN+str+SpiderUpload.TEXT_RESET+", ");
        }
    }
}