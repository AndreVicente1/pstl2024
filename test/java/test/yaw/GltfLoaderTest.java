package test.yaw;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.json.JSONArray;
import org.json.JSONObject;
import yaw.engine.geom.Geometry;
import yaw.engine.mesh.Texture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GltfLoaderTest {
    private static GltfModel model;
    private static List<Geometry> geometries = new ArrayList<>();
    public static void main(String[] args) throws IOException {
        Path path = Paths.get("src/java/resources/models/Cake_Pop.gltf");
        loadGltf(String.valueOf(path));
    }

    public static void loadGltf(String path) throws IOException {
        GltfModelReader reader = new GltfModelReader();
        System.out.println("Loading GLTF model from: " + path);
        model = reader.read(Paths.get(path));
        processGeometries();
        System.out.println("Finished processing geometries. Total loaded: " + geometries.size());
    }

    private static void processGeometries() {
        System.out.println("Processing geometries...");
        for (MeshModel mesh : model.getMeshModels()) {
            for (MeshPrimitiveModel primitive : mesh.getMeshPrimitiveModels()) {
                Geometry geometry = convertToGeometry(primitive);
                geometries.add(geometry);
                System.out.println("Processed a geometry with " + geometry.getVertices().length / 3 + " vertices.");
            }
        }
    }

    private static Geometry convertToGeometry(MeshPrimitiveModel meshPrimitive) {
        System.out.println("Converting mesh primitive to geometry...");
        AccessorModel vertexAccessor = meshPrimitive.getAttributes().get("POSITION");
        AccessorModel normalAccessor = meshPrimitive.getAttributes().get("NORMAL");
        AccessorModel texCoordAccessor = meshPrimitive.getAttributes().get("TEXCOORD_0");
        AccessorModel indexAccessor = meshPrimitive.getIndices();

        float[] vertices = vertexAccessor != null ? extractFloatData((AccessorFloatData) vertexAccessor.getAccessorData()) : new float[0];
        float[] normals = normalAccessor != null ? extractFloatData((AccessorFloatData) normalAccessor.getAccessorData()) : new float[0];
        float[] textCoords = texCoordAccessor != null ? extractFloatData((AccessorFloatData) texCoordAccessor.getAccessorData()) : new float[0];
        int[] indices = indexAccessor != null ? extractIntData(indexAccessor.getAccessorData()) : new int[0];

        System.out.println("Vertices: " + vertices.length + " Normals: " + normals.length + " TexCoords: " + textCoords.length + " Indices: " + indices.length);
        return new Geometry(vertices, textCoords, normals, indices);
    }

    private static int[] extractIntData(AccessorData accessorData) {
        if (accessorData instanceof AccessorIntData) {
            AccessorIntData intData = (AccessorIntData) accessorData;
            return extractIntFromIntData(intData);
        } else if (accessorData instanceof AccessorShortData) {
            AccessorShortData shortData = (AccessorShortData) accessorData;
            return extractIntFromShortData(shortData);
        } else {
            throw new IllegalArgumentException("Unsupported accessor data type for indices: " + accessorData.getClass().getName());
        }
    }

    private static int[] extractIntFromIntData(AccessorIntData intData) {
        int totalComponents = intData.getTotalNumComponents();
        int[] data = new int[totalComponents];
        for (int i = 0; i < totalComponents; i++) {
            data[i] = intData.get(i);
        }
        return data;
    }

    private static int[] extractIntFromShortData(AccessorShortData shortData) {
        int totalComponents = shortData.getTotalNumComponents();
        int[] data = new int[totalComponents];
        for (int i = 0; i < totalComponents; i++) {
            data[i] = shortData.get(i) & 0xFFFF;  // Convert short to unsigned int
        }
        return data;
    }


    private static float[] extractFloatData(AccessorFloatData accessorData) {
        int totalComponents = accessorData.getTotalNumComponents();
        float[] data = new float[totalComponents];
        for (int i = 0; i < totalComponents; i++) {
            data[i] = accessorData.get(i);
        }
        return data;
    }

}
