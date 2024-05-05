package yaw.engine.resources;

import org.joml.Vector3f;
import yaw.engine.geom.Geometry;
import yaw.engine.geom.GeometryBuilder;
import yaw.engine.mesh.Material;
import yaw.engine.mesh.MaterialADS;
import yaw.engine.mesh.Mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjModel {
    private String sceneName;
    private Map<String, GeometryBuilder> geometries;
    private List<String> geometryIds;

    private Map<String, MtlMaterial> materials;
    private List<String> materialIds;
    private Map<String, String> materialMap;


    public ObjModel(String sceneName) {
        geometries = new HashMap<>();
        geometryIds = new ArrayList<>();
        materials = new HashMap<>();
        materialIds = new ArrayList<>();
        materialMap = new HashMap<>();
        this.sceneName = sceneName;
    }

    public String getFreshGeomName() {
        return "Geom-" + (geometries.size() + 1);
    }

    public void addGeom(String objName, GeometryBuilder geom) {
        if (geometries.containsKey(objName)) {
            throw new Error("Object '" + objName + "' already added.");
        }
        geometryIds.add(objName);
        geometries.put(objName, geom);
    }

    public void addMaterial(String matName, MtlMaterial material) {
        if (materials.containsKey(matName)) {
            throw new Error("Material '" + matName + "' already added.");
        }
        materialIds.add(matName);
        materials.put(matName, material);
    }

    public void assignMaterial(String objName, String matName) {
        materialMap.put(objName, matName);
    }

    public int nbGeometries() {
        return geometryIds.size();
    }

    public GeometryBuilder getGeometryByIndex(int index) {
        String geomName = geometryIds.get(index);
        return geometries.get(geomName);
    }

    public MtlMaterial getMaterialByIndex(int index) {
        String matName = materialIds.get(index);
        return materials.get(matName);
    }

    /**
     * Create a list of Meshes from the OBJ/MTL objects, based on the following heuristics.
     * each "object" of the OBJ becomes a Mesh in the Yaw glossary
     * The assignment object/material  (as a Mesh) is also done in an
     * heuristic way.
     *
     * If more control is needed, then the meshes must be created manually
     *
     * @return The Meshes corresponding to the OBJ/MTL model.
     */
    public Mesh[] buildMeshes(boolean withShadows) {
        List<Mesh> meshes = new ArrayList<>();
        for (String objName : geometryIds) {
            Geometry geom = geometries.get(objName).build();
            Material mat;
            String matName = materialMap.get(objName);
            if (matName != null) {
                mat = materials.get(matName).getMaterial(withShadows);
            } else {
                // use white if material not provided (vertex colors ?)
                mat = new MaterialADS(new Vector3f(1.0f, 1.0f, 1.0f));
            }
            /* DEBUG
            mat = new Material();
            mat.setColor(new Vector3f(0.1f , 0.7f, 0.9f));
            */
            Mesh mesh = new Mesh(geom, mat,false);
            meshes.add(mesh);
        }
        Mesh[] witness = new Mesh[1];
        return meshes.toArray(witness);
    }

}
