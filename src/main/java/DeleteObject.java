import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class DeleteObject {
    public void deleteObject(String projectId, String bucketName, String objectName) {
        // The ID of your GCP project
        // String projectId = "your-project-id";

        // The ID of your GCS bucket
        // String bucketName = "your-unique-bucket-name";

        // The ID of your GCS object
        // String objectName = "your-object-name";

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        storage.delete(bucketName, objectName);

        System.out.print(SpiderUpload.TEXT_CYAN+"i    :"+ SpiderUpload.TEXT_RESET);
        System.out.print(" Object ");
        System.out.print(SpiderUpload.TEXT_GREEN+objectName+SpiderUpload.TEXT_RESET);
        System.out.print(" was deleted from ");
        System.out.print(SpiderUpload.TEXT_GREEN+bucketName+SpiderUpload.TEXT_RESET);
    }
}
