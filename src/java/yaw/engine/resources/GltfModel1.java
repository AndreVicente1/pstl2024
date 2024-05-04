package yaw.engine.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import yaw.engine.geom.Geometry;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.Mesh;

public class GltfModel1 {
    private String modelName;
    private Map<String, Geometry> geometries;
    private List<String> geometryNames;

    private Map<String, PBRMaterial> materials;
    private List<String> materialNames;
    private Map<String, String> materialAssignments;

    public GltfModel1(String modelName) {
        this.modelName = modelName;
        geometries = new HashMap<>();
        geometryNames = new ArrayList<>();
        materials = new HashMap<>();
        materialNames = new ArrayList<>();
        materialAssignments = new HashMap<>();
    }

    public void addGeometry(String name, Geometry geometry) {
        if (geometries.containsKey(name)) {
            throw new IllegalStateException("Geometry already exists: " + name);
        }
        geometries.put(name, geometry);
        geometryNames.add(name);
    }

    public void addMaterial(String name, PBRMaterial material) {
        if (materials.containsKey(name)) {
            throw new IllegalStateException("Material already exists: " + name);
        }
        materials.put(name, material);
        materialNames.add(name);
    }

    public void assignMaterialToGeometry(String geometryName, String materialName) {
        if (!geometries.containsKey(geometryName) || !materials.containsKey(materialName)) {
            throw new IllegalArgumentException("Geometry or Material does not exist.");
        }
        materialAssignments.put(geometryName, materialName);
    }

    public Mesh[] buildMeshes(boolean withShadows) {
        List<Mesh> meshes = new ArrayList<>();
        for (String geometryName : geometryNames) {
            Geometry geom = geometries.get(geometryName);
            String materialName = materialAssignments.get(geometryName);
            Material mat = materials.get(materialName).getMaterial(withShadows);

            Mesh mesh = new Mesh(geom, mat,true);
            meshes.add(mesh);
        }
        return meshes.toArray(new Mesh[0]);
    }
}