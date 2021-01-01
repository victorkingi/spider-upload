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
        // The ID of your GCP project
        // String projectId = "your-project-id";

        // The ID of your GCS bucket
        // String bucketName = "your-unique-bucket-name";

        // The ID of the first GCS object to compose
        // String firstObjectName = "your-first-object-name";

        // The ID of the second GCS object to compose
        // String secondObjectName = "your-second-object-name";

        // The ID to give the new composite object
        // String targetObjectName = "new-composite-object-name";
        assert objects.length == 32;
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
        System.out.print(SpiderUpload.TEXT_GREEN+compositeObject.getName()+SpiderUpload.TEXT_RESET);
        System.out.print(" was created by combining ");
        for (String str : objects) {
            System.out.println(SpiderUpload.TEXT_GREEN+str+SpiderUpload.TEXT_RESET+", ");
        }
    }
}