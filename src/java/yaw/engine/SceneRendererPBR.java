package yaw.engine;


import org.joml.Matrix4f;
import yaw.engine.camera.Camera;
import yaw.engine.items.ItemObject;
import yaw.engine.light.LightModel;
import yaw.engine.mesh.Mesh;
import yaw.engine.mesh.MeshPBR;
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
public class SceneRendererPBR extends SceneRenderer{
    //private final HashMap<MeshPBR, List<ItemObject>> mMeshMap;

    public SceneRendererPBR(LightModel lightModel) {
        super(lightModel);
        //mMeshMap = new HashMap<>();
    }

    public void render(Camera pCamera, ShaderManager shaderManager) {

        /* Rendering of meshes */

        //ShaderProgram shaderProgram =shaderManager.fetch("ADS");
        List<Mesh> meshesToRemove = new ArrayList<>();

        for (Mesh mesh : super.getMeshMap().keySet()) {
            ShaderProperties meshProps = mesh.getShaderProperties(super.getLightModel());
            // TODO : ugly cast, fix when support for e.g. PBR materials
            ShaderProgramPBR meshProgram = (ShaderProgramPBR) shaderManager.fetch(meshProps);
            //System.out.println("meshProgram : "+meshProgram);
            if (meshProgram == null) {
                // create a shader program for this scene / mesh
                meshProgram = new ShaderProgramPBR(meshProps);
                shaderManager.register(meshProps, meshProgram);
                meshProgram.init();
            }
            /* Setup lights */
            super.getLightModel().setupShader(new Matrix4f().identity(), meshProgram);

            List<ItemObject> lItems = super.getMeshMap().get(mesh);
            List<ItemObject> vertexHelpers = new ArrayList<>();
            List<ItemObject> normalHelpers = new ArrayList<>();
            List<ItemObject> axisHelpers = new ArrayList<>();
            if (lItems.isEmpty()) {
                meshesToRemove.add(mesh);
            } else {
                if (super.getNotInit().contains(mesh)) {
                    mesh.initBuffers();
                    super.getNotInit().remove(mesh);
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
            super.getMeshMap().remove(lMesh);
        }

    }
}
