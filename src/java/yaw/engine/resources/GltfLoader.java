package yaw.engine.resources;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import yaw.engine.geom.Geometry;
import yaw.engine.mesh.Texture;

import java.io.IOException;
import java.nio.file.Paths;

public class GltfLoader {

    private GltfModel model; // librairie jgltf
    private GltfModel1 gltfModel;


    public GltfLoader() {}

    public GltfModel1 getScene(){ return gltfModel; }

    public void loadGltf(String path) throws IOException {
        GltfModelReader reader = new GltfModelReader();
        System.out.println("Loading GLTF model from: " + path);
        model = reader.read(Paths.get(path));
        gltfModel = new GltfModel1("GltfModel");
        processMaterials();
        processGeometries();
        System.out.println("Finished loading GLTF model.");
    }
    private void processMaterials() {
        System.out.println("Processing materials...");
        if (model.getMaterialModels() != null) {
            for (MaterialModel materialModel : model.getMaterialModels()) {
                PBRMaterial pbrMaterial = convertToPBRMaterial(materialModel);
                gltfModel.addMaterial(materialModel.getName(), pbrMaterial);
            }
        }
    }
    private PBRMaterial convertToPBRMaterial(MaterialModel materialModel) {
        PBRMaterial material = new PBRMaterial(materialModel.getName());
        MaterialModelV2 model = (MaterialModelV2) materialModel;

        material.metallic = model.getMetallicFactor();
        material.roughness = model.getRoughnessFactor();
        material.isMetal = material.metallic > 0.5;

        TextureModel baseColorTexture = model.getBaseColorTexture();
        if (baseColorTexture != null) {
            material.basecolor = baseColorTexture.getImageModel().getUri();
        }

        TextureModel metallicRoughnessTexture = model.getMetallicRoughnessTexture();
        if (metallicRoughnessTexture != null) {
            material.metallicRoughnessTexture = metallicRoughnessTexture.getImageModel().getUri();
        }

        TextureModel normalTexture = model.getNormalTexture();
        if (normalTexture != null ) {
            material.normalTexture = normalTexture.getImageModel().getUri();
        }

        TextureModel emissiveTexture = model.getEmissiveTexture();
        if (emissiveTexture != null ) {
            material.emissiveTexture = emissiveTexture.getImageModel().getUri();
        }

        return material;
    }

    private void processGeometries() {
        System.out.println("Processing geometries...");
        int index = 0;
        for (MeshModel mesh : model.getMeshModels()) {
            String materialName = mesh.getMeshPrimitiveModels().get(0).getMaterialModel().getName();
            for (MeshPrimitiveModel primitive : mesh.getMeshPrimitiveModels()) {
                Geometry geometry = convertToGeometry(primitive);
                String geometryName = "geometry" + index++;
                gltfModel.addGeometry(geometryName, geometry);
                gltfModel.assignMaterialToGeometry(geometryName, materialName);
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
        int[] indices = indexAccessor != null ? extractIntData(indexAccessor.getAccessorData()) : new int[0];

        System.out.println("Vertices: " + vertices.length + " Normals: " + normals.length + " TexCoords: " + textCoords.length + " Indices: " + indices.length);
        return new Geometry(vertices, textCoords, normals, indices);
    }
    private int[] extractIntData(AccessorData accessorData) {
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

    private int[] extractIntFromIntData(AccessorIntData intData) {
        int totalComponents = intData.getTotalNumComponents();
        int[] data = new int[totalComponents];
        for (int i = 0; i < totalComponents; i++) {
            data[i] = intData.get(i);
        }
        return data;
    }

    private int[] extractIntFromShortData(AccessorShortData shortData) {
        int totalComponents = shortData.getTotalNumComponents();
        int[] data = new int[totalComponents];
        for (int i = 0; i < totalComponents; i++) {
            data[i] = shortData.get(i) & 0xFFFF;  // Convert short to unsigned int
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
