package yaw.engine.resources;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import yaw.engine.geom.Geometry;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GltfLoader {

    private GltfModel model;
    private List<Geometry> geometries = new ArrayList<>();

    public GltfLoader() {}

    public void loadGltf(String path) throws IOException {
        GltfModelReader reader = new GltfModelReader();
        System.out.println("Loading GLTF model from: " + path);
        model = reader.read(Paths.get(path));
        processGeometries();
        System.out.println("Finished processing geometries. Total loaded: " + geometries.size());
    }

    private void processGeometries() {
        System.out.println("Processing geometries...");
        for (MeshModel mesh : model.getMeshModels()) {
            for (MeshPrimitiveModel primitive : mesh.getMeshPrimitiveModels()) {
                Geometry geometry = convertToGeometry(primitive);
                geometries.add(geometry);
                System.out.println("Processed a geometry with " + geometry.getVertices().length / 3 + " vertices.");
            }
        }
    }

    private Geometry convertToGeometry(MeshPrimitiveModel meshPrimitive) {
        System.out.println("Converting mesh primitive to geometry...");
        AccessorModel vertexAccessor = meshPrimitive.getAttributes().get("POSITION");
        AccessorModel normalAccessor = meshPrimitive.getAttributes().get("NORMAL");
        AccessorModel texCoordAccessor = meshPrimitive.getAttributes().get("TEXCOORD_0");
        AccessorModel indexAccessor = meshPrimitive.getIndices();

        float[] vertices = vertexAccessor != null ? extractFloatData((AccessorFloatData) vertexAccessor.getAccessorData()) : new float[0];
        float[] normals = normalAccessor != null ? extractFloatData((AccessorFloatData) normalAccessor.getAccessorData()) : new float[0];
        float[] textCoords = texCoordAccessor != null ? extractFloatData((AccessorFloatData) texCoordAccessor.getAccessorData()) : new float[0];
        int[] indices = indexAccessor != null ? extractIntData((AccessorIntData) indexAccessor.getAccessorData()) : new int[0];

        System.out.println("Vertices: " + vertices.length + " Normals: " + normals.length + " TexCoords: " + textCoords.length + " Indices: " + indices.length);
        return new Geometry(vertices, textCoords, normals, indices);
    }

    private int[] extractIntData(AccessorIntData accessorData) {
        int totalComponents = accessorData.getTotalNumComponents();
        int[] data = new int[totalComponents];
        for (int i = 0; i < totalComponents; i++) {
            data[i] = accessorData.get(i);
        }
        return data;
    }

    private float[] extractFloatData(AccessorFloatData accessorData) {
        int totalComponents = accessorData.getTotalNumComponents();
        float[] data = new float[totalComponents];
        for (int i = 0; i < totalComponents; i++) {
            data[i] = accessorData.get(i);
        }
        return data;
    }
}
