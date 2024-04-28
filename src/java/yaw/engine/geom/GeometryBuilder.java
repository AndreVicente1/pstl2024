package yaw.engine.geom;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

public class GeometryBuilder {
    private List<Vector3f> vertices;
    private List<Vector3i> triangles;

    private List<Vector2f> textCoords;

    private List<Vector3f> normals;

    private List<Vector3f> tangents; // tangentes

    public GeometryBuilder() {
        vertices = new ArrayList<>();
        triangles = new ArrayList<>();
        textCoords = new ArrayList<>();
        normals = new ArrayList<>();
        tangents = new ArrayList<>();
    }

    public int addVertex(float x, float y, float z) {
        vertices.add(new Vector3f(x, y, z));
        return vertices.size() - 1;
    }

    public void addVertices(float... coords) {
        if (coords.length % 3 != 0) {
            throw new Error("Vertex coordinates must be triples.");
        }
        for(int i=0; i<coords.length; i+=3) {
            addVertex(coords[i], coords[i+1], coords[i+2]);
        }
    }

    private void checkVertex(String vrefStr, int vref) {
        if (!(0 <= vref) && (vref < vertices.size())) {
            throw new Error("Wrong vertex " + vrefStr + " = " + vref);
        }
    }

    public void addTriangle(int v1ref, int v2ref, int v3ref) {
        checkVertex("v1ref", v1ref);
        checkVertex("v2ref", v2ref);
        checkVertex("v3ref", v3ref);
        triangles.add(new Vector3i(v1ref, v2ref, v3ref));
    }

    public void addTriangles(int... vrefs) {
        if (vrefs.length %3 != 0) {
            throw new Error("Vertex indices must be triples.");
        }
        for(int i=0; i<vrefs.length; i+=3) {
            addTriangle(vrefs[i], vrefs[i+1], vrefs[i+2]);
        }
    }

    public float[] getVertices() {
        if(vertices.isEmpty()) {
            throw new Error("No vertices in geometry");
        }
        float[] verts = new float[vertices.size()*3];
        int i = 0;
        for(Vector3f v : vertices) {
            verts[i] = v.x;
            verts[i+1] = v.y;
            verts[i+2] = v.z;
            i += 3;
        }
        return verts;
    }

    public int[] getIndices() {
        if(triangles.isEmpty()) {
            throw new Error("No triangles in geometry");
        }
        int[] indices = new int[triangles.size()*3];
        int i = 0;
        for(Vector3i tri : triangles) {
            indices[i] = tri.get(0);
            indices[i+1] = tri.get(1);
            indices[i+2] = tri.get(2);
            i += 3;
        }
        return indices;
    }

    public int addNormal(float x, float y, float z) {
        normals.add(new Vector3f(x, y, z));
        return normals.size() - 1;
    }

    public void addNormals(float... coords) {
        if (coords.length % 3 != 0) {
            throw new Error("Normal coordinates must be triples.");
        }
        for(int i=0; i<coords.length; i+=3) {
            addNormal(coords[i], coords[i+1], coords[i+2]);
        }
    }

    public void generateNormals() {
        if(!normals.isEmpty()) {
            throw new Error("Cannot generate: normals already set");
        }
        normals = new ArrayList<>(vertices.size());
        for(int i=0; i<vertices.size();i++) {
            normals.add(new Vector3f(0, 0, 0));
        }

        for(Vector3i tri : triangles) {
            int i1 = tri.get(0);
            int i2 = tri.get(1);
            int i3 = tri.get(2);
            Vector3f v1 = vertices.get(i1);
            Vector3f v2 = vertices.get(i2);
            Vector3f v3 = vertices.get(i3);

            Vector3f n1 = normals.get(i1);
            Vector3f n2 = normals.get(i2);
            Vector3f n3 = normals.get(i3);

            Vector3f trinorm = v2.sub(v1).cross(v3.sub(v1)).normalize();

            normals.set(i1, n1.add(trinorm));
            normals.set(i2, n2.add(trinorm));
            normals.set(i3, n3.add(trinorm));
        }

        // normalize everything
        for(Vector3f normal : normals) {
            normal.normalize();
        }

    }

    public float[] getNormals() {
        if (normals.isEmpty()) {
            throw new Error("No normals in geometry");
        }

        float[] norms = new float[normals.size() * 3];
        int i = 0;
        for (Vector3f v : normals) {
            norms[i] = v.x;
            norms[i + 1] = v.y;
            norms[i + 2] = v.z;
            i += 3;
        }
        return norms;
    }

    public void addTextCoord(float tx, float ty) {
        textCoords.add(new Vector2f(tx, ty));
    }

    public float[] getTextCoords() {
        if (textCoords.isEmpty()) {
            throw new Error("No texture coordinates in geometry");
        }
        float[] tcoords = new float[textCoords.size()*2];
        int i = 0;
        for(Vector2f t : textCoords) {
            tcoords[i] = t.x;
            tcoords[i+1] = t.y;
            i += 2;
        }
        return tcoords;
    }

    public void generateTangents() {
        if (tangents.size() != vertices.size()) {
            tangents.clear();
            for (int i = 0; i < vertices.size(); i++) {
                tangents.add(new Vector3f());
            }
        }

        for (int i = 0; i < triangles.size(); i++) {
            Vector3i tri = triangles.get(i);

            int idxV1 = tri.x;
            int idxV2 = tri.y;
            int idxV3 = tri.z;

            Vector3f p1 = vertices.get(idxV1);
            Vector3f p2 = vertices.get(idxV2);
            Vector3f p3 = vertices.get(idxV3);

            Vector2f uv1 = textCoords.get(idxV1);
            Vector2f uv2 = textCoords.get(idxV2);
            Vector2f uv3 = textCoords.get(idxV3);

            Vector3f edge1 = new Vector3f();
            Vector3f edge2 = new Vector3f();
            p2.sub(p1, edge1);
            p3.sub(p1, edge2);

            Vector2f deltaUV1 = new Vector2f();
            Vector2f deltaUV2 = new Vector2f();
            uv2.sub(uv1, deltaUV1);
            uv3.sub(uv1, deltaUV2);

            float f = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV2.x * deltaUV1.y);
            Vector3f tangent = new Vector3f();
            tangent.set(
                    f * (deltaUV2.y * edge1.x - deltaUV1.y * edge2.x),
                    f * (deltaUV2.y * edge1.y - deltaUV1.y * edge2.y),
                    f * (deltaUV2.y * edge1.z - deltaUV1.y * edge2.z)
            );

            tangent.normalize();
            tangents.set(idxV1, tangents.get(idxV1).add(tangent));
            tangents.set(idxV2, tangents.get(idxV2).add(tangent));
            tangents.set(idxV3, tangents.get(idxV3).add(tangent));
        }

        for (Vector3f tan : tangents) {
            tan.normalize();
        }
    }
    public Geometry build() {
        float[] vertices = getVertices();
        float[] uvCoords = null;
        if(!textCoords.isEmpty()) {
            uvCoords = getTextCoords();
        }
        if (normals.isEmpty()) {
            generateNormals();
        }
        float[] normals = getNormals();

        int[] indices = getIndices();

        Geometry geom;
        if(!textCoords.isEmpty()) {
            geom = new Geometry(vertices, uvCoords, normals, indices);
        } else {
            geom = new Geometry(vertices, normals, indices);
        }
        return geom;
    }

}
