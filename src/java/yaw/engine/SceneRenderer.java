package yaw.engine;


import org.joml.Matrix4f;
import yaw.engine.camera.Camera;
import yaw.engine.items.ItemObject;
import yaw.engine.light.LightModel;
import yaw.engine.mesh.Mesh;
import yaw.engine.shader.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class representing a scene
 * we manage the rendering efficiency by splitting the meshes in two different structure
 * the first one (notInit) represent the mesh that must not be rendered ( we remove them from the gpu) unless we want to
 * and the second is a map where each mesh has a list of items
 */
public class SceneRenderer {
    //old code from a previous attempt to manage a group of scene vertex
    private boolean itemAdded = false;
    private final HashMap<Mesh, List<ItemObject>> mMeshMap;
    private final ArrayList<Mesh> notInit;

    private final LightModel lightModel;


    public SceneRenderer(LightModel lightModel) {
        mMeshMap = new HashMap<>();
        notInit = new ArrayList<>();
        this.lightModel = lightModel;
    }

    /**
     * Add the item in the map if the associated mesh is already a key
     * otherwise the association is created and the mesh is added to the nonInit List
     *
     * @param pItem the item
     */
    public synchronized void add(ItemObject pItem) {
        itemAdded = true;
        //retrieve the stored mesh in the item
        Mesh lMesh = pItem.getMesh();
        List<ItemObject> lItems = mMeshMap.get(lMesh);
        if (lItems == null) {
            lItems = new ArrayList<>();
            mMeshMap.put(lMesh, lItems);
            notInit.add(lMesh);
        }
        lItems.add(pItem);

    }

    /**
     * Remove the specified item from the mMeshMap
     *
     * @param pItem item to be removed
     */
    public void removeItem(ItemObject pItem) {
        List<ItemObject> lItems = mMeshMap.get(pItem.getMesh());
        lItems.remove(pItem);
    }

    /**
     * Invoke the method cleanup on all the active mesh
     */
    public void cleanUp(ShaderManager shaderManager) {
        for (Mesh lMesh : mMeshMap.keySet()) {
            lMesh.cleanUp();
        }
    }

    /**
     * Invoke the method init on all the mesh in the non initialize mesh collection
     */
    /*public void initMesh() {
        for (Mesh lMesh : notInit) {
            lMesh.initBuffer();
        }
        notInit.clear();
    }*/

    /**
     * Invoke the method render on all mesh with associated items
     * otherwise clean then remove mesh which has an empty list of items
     *
     * @param pCamera camera in wich that will render
     */

    public void render(Camera pCamera, ShaderManager shaderManager) {

        /* Rendering of meshes */

        //ShaderProgram shaderProgram =shaderManager.fetch("ADS");
        List<Mesh> meshesToRemove = new ArrayList<>();

        for (Mesh mesh : mMeshMap.keySet()) {
            ShaderProperties meshProps = mesh.getShaderProperties(lightModel);
            ShaderProgram meshProgram;
            if (mesh.isPBR()) {
                meshProgram = shaderManager.fetch(meshProps);
                if (meshProgram == null) {
                    meshProgram = new ShaderProgramPBR(meshProps);
                    shaderManager.register(meshProps, meshProgram);
                    meshProgram.init();
                }
            }else{
                meshProgram = shaderManager.fetch(meshProps);
                if (meshProgram == null) {
                    // create a shader program for this scene / mesh
                    meshProgram = new ShaderProgramADS(meshProps);
                    shaderManager.register(meshProps, meshProgram);
                    ((ShaderProgramADS) meshProgram).prepareMaterial(mesh.getMaterial());
                    meshProgram.init();
                }
            }


            /* Setup lights */
            lightModel.setupShader(new Matrix4f().identity(), meshProgram);

            List<ItemObject> lItems = mMeshMap.get(mesh);
            List<ItemObject> vertexHelpers = new ArrayList<>();
            List<ItemObject> normalHelpers = new ArrayList<>();
            List<ItemObject> axisHelpers = new ArrayList<>();
            if (lItems.isEmpty()) {
                meshesToRemove.add(mesh);
            } else {
                if (notInit.contains(mesh)) {
                    mesh.initBuffers();
                    notInit.remove(mesh);
                }
                mesh.renderSetup(pCamera, meshProgram);
                for (ItemObject item : lItems) {
                    mesh.renderItem(item, meshProgram);
                    if (item.showVertexHelpers()) {
                        vertexHelpers.add(item);
                    }
                    if (item.showNormalHelpers()) {
                        normalHelpers.add(item);
                    }
                    if (item.showAxisHelpers()) {
                        axisHelpers.add(item);
                    }
                }
                mesh.renderCleanup(meshProgram);

                if (!vertexHelpers.isEmpty()) {
                    mesh.renderHelperVertices(vertexHelpers, pCamera, shaderManager.fetch("VertexHelper"));
                }
                if (!normalHelpers.isEmpty()) {
                    mesh.renderHelperNormals(normalHelpers, pCamera, shaderManager.fetch("NormalHelper"));
                }
                if (!axisHelpers.isEmpty()) {
                    mesh.renderHelperAxes(axisHelpers, pCamera, shaderManager.fetch("AxisHelper"));
                }
            }
        }
        /*Clean then remove*/
        for (Mesh lMesh : meshesToRemove) {
            lMesh.cleanUp();
            mMeshMap.remove(lMesh);
        }

    }

    /**
     * Retrieve all the items of the scene
     *
     * @return the list of item
     */
    public ArrayList<ItemObject> getItemsList() {
        ArrayList<ItemObject> lItems = new ArrayList<>();
        mMeshMap.values().forEach(lItems::addAll);
        return lItems;
    }

    public boolean isItemAdded() {
        return itemAdded;
    }

    public HashMap<Mesh, List<ItemObject>> getMeshMap() {
        return mMeshMap;
    }

    public LightModel getLightModel() {
        return lightModel;
    }
}
