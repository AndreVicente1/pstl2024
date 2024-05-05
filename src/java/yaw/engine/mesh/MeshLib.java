package yaw.engine.mesh;

import org.joml.Vector3f;
import yaw.engine.geom.GeomLib;
import yaw.engine.geom.Geometry;
import yaw.engine.mesh.strategy.DefaultDrawingStrategy;

public class MeshLib {

    public static Mesh makeSolidCube(float length, Vector3f color) {
        return makeSolidCuboid(length, length, length, color);
    }
    public static Mesh makeSolidCuboid(float xLength, float yLength, float zLength, Vector3f color) {
        float x = xLength / 2f;
        float y = yLength / 2f;
        float z = zLength / 2f;
        float[] vertices = new float[]{
                //Front face
                x, y, z, -x, y, z, -x, -y, z, x, -y, z,
                //Top face
                x, y, z, -x, y, z, x, y, -z, -x, y, -z,
                //Back face
                x, y, -z, -x, y, -z, -x, -y, -z, x, -y, -z,
                //Bottom face
                x, -y, z, -x, -y, z, x, -y, -z, -x, -y, -z,
                //Left face
                -x, y, z, -x, y, -z, -x, -y, z, -x, -y, -z,
                //Right face
                x, y, z, x, y, -z, x, -y, z, x, -y, -z};
        //for light
        float[] normals = {
                //Front face
                0, 0, 1f, 0, 0, 1f, 0, 0, 1f, 0, 0, 1f,
                //Top face
                0, 1f, 0, 0, 1f, 0, 0, 1f, 0, 0, 1f, 0,
                //Back face
                0, 0, -1f, 0, 0, -1f, 0, 0, -1f, 0, 0, -1f,
                //Bottom face
                0, -1f, 0, 0, -1f, 0, 0, -1f, 0, 0, -1f, 0,
                //Left face
                -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0,
                //Right face
                1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0};
        //mapping for the texture
        float[] textCoord2 = new float[]{
                0.0f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.5f, 0.0f,

                0.0f, 0.0f,
                0.5f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,

                // For text coords in top face
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.0f, 1.0f,
                0.5f, 1.0f,

                // For text coords in right face
                0.0f, 0.0f,
                0.0f, 0.5f,

                // For text coords in left face
                0.5f, 0.0f,
                0.5f, 0.5f,

                // For text coords in bottom face
                0.5f, 0.0f,
                1.0f, 0.0f,
                0.5f, 0.5f,
                1.0f, 0.5f,
        };

        float[] textCoord = new float[]{
                0.5f,0,
                0.5f,0.25f,
                0.75f,0.25f,
                0.75f,0,

                0.25f,0.25f,
                0.25f,0.5f,
                0.5f,0.5f,
                0.5f,0.25f,

                0.5f,0.25f,
                0.5f,0.5f,
                0.75f,0.5f,
                0.75f,0.25f,

                0.75f,0.25f,
                0.75f,0.5f,
                1,0.5f,
                1,0.25f,

                0.5f,0.5f,
                0.5f,0.75f,
                0.75f,0.75f,
                0.75f,0.5f,

                0.5f,0.75f,
                0.5f,1,
                0.75f,1,
                0.75f,0.75f
        };

        //mapping the triangle for each face
        int[] indices = new int[]{
                //Front face
                0, 1, 2, 0, 2, 3,
                //Top face
                4, 6, 5, 6, 7, 5,
                //Back face
                8, 11, 10, 8, 10, 9,
                //Bottom Face
                14, 12, 13, 14, 13, 15,
                //Left face
                16, 19, 18, 16, 17, 19,
                //Right face
                20, 22, 21, 22, 23, 21};

        Geometry geom = GeomLib.makeCuboid(xLength, yLength, zLength).build();
        Mesh lMesh = new Mesh(geom, new MaterialADS(color));
        lMesh.setDrawingStrategy(new DefaultDrawingStrategy());
        return lMesh;
    }

}
