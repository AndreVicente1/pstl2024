package test.yaw;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class GltfLoaderTest {
    // JSON dependency: org.json dans pom.xml
    public static void main(String[] args) {
        // format .gltf
        Path path = Paths.get("src/java/resources/models/Cake_Pop.gltf");
        try {
            String text = new String(Files.readAllBytes(path));
            JSONObject gltfJson = new JSONObject(text);

            JSONArray materials = gltfJson.getJSONArray("materials");
            for (int i = 0; i < materials.length(); i++) {
                JSONObject material = materials.getJSONObject(i);
                System.out.println("Material name: " + material.getString("name"));
                if (material.has("pbrMetallicRoughness")) {
                    JSONObject pbrMetallicRoughness = material.getJSONObject("pbrMetallicRoughness");

                    // Base Color Texture test
                    if (pbrMetallicRoughness.has("baseColorTexture")) {
                        int baseColorTextureIndex = pbrMetallicRoughness.getJSONObject("baseColorTexture").getInt("index");
                        System.out.println("Base Color Texture Index: " + baseColorTextureIndex);
                    }

                    // Metallic-Roughness Texture test
                    if (pbrMetallicRoughness.has("metallicRoughnessTexture")) {
                        int metallicRoughnessTextureIndex = pbrMetallicRoughness.getJSONObject("metallicRoughnessTexture").getInt("index");
                        System.out.println("Metallic Roughness Texture Index: " + metallicRoughnessTextureIndex);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // gltf model reader
        path = Paths.get("src/java/resources/models/Cake_Pop.gltf");
        try {
            GltfModelReader reader = new GltfModelReader();
            GltfModel gltfModel = reader.read(path);

            for (MeshModel materialModel : gltfModel.getMeshModels()) {
                for (MeshPrimitiveModel mat : materialModel.getMeshPrimitiveModels()) {
                    MaterialModel mt = mat.getMaterialModel();
                    System.out.println("mat : " + mt.getName() + " extensions: " + mt.getExtensions());
                }
            }

//            for (MaterialModel materialModel : gltfModel.getMaterialModels()) {
//                System.out.println("Material name: " + materialModel.getName());
//                materialModel.getExtensions().forEach((key, value) -> {
//                    System.out.println("Key: " + key + ", Value: " + value);
//                });
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadTexture(GltfModel material, String textureKey) {
        TextureModel textureModel = (TextureModel) material.getExtensions().get(textureKey);
        if (textureModel != null) {
            System.out.println("Loading texture: " + textureModel.getImageModel().getUri());
        }
    }
}
