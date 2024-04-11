package yaw.engine.resources;

import yaw.engine.geom.GeometryBuilder;

import java.io.*;
import java.util.*;

public class ObjLoader {
    private LoadMode loadMode = LoadMode.LOAD_UNDEFINED;
    private String loadPath = null;
    private ObjModel objModel;
    private String currentObject = null;

    private int vertexCount;
    private int textCount;
    private int normalCount;

    public ObjLoader() {
        objModel = null;
    }

    public ObjModel getScene() {
        return objModel;
    }

    public void parseFromFile(String filename) throws IOException {
        loadMode = LoadMode.LOAD_FROM_FILE;
        loadPath = filename;
        objModel = new ObjModel(filename);
        currentObject = null;
        parseFromBufferedReader(new BufferedReader(new FileReader(filename)));
    }

    public void parseFromResource(String name) throws IOException {
        InputStream istream = ObjLoader.class.getResourceAsStream(name);
        if (istream == null) {
            throw new ParseError("Cannot find resource: " + name);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
        loadMode = LoadMode.LOAD_FROM_RESOURCE;
        loadPath = name;
        objModel = new ObjModel(name);
        currentObject = null;
        parseFromBufferedReader(reader);
    }

    public void parseFromBufferedReader(BufferedReader reader) throws IOException {
        String[] lines = Utils.readLines(reader);
        parseFromLines(lines);
    }

    public void parseFromLines(String[] lines) {
        // In OBJ files vertices, texture coordinates and normals
        // are counted globally and are not reset before each object
        vertexCount = 0;
        textCount = 0;
        normalCount = 0;

        int linepos = 0;

        while (linepos < lines.length - 1) {
            linepos++;
            ObjEntry entry = parseLine(linepos, lines[linepos - 1]);
            System.out.println("entry type: " + entry.getType());
            switch (entry.getType()) {
                case MTLLIB:
                    MtlLoader mtlLoader = new MtlLoader(objModel);
                    switch (loadMode) {
                        case LOAD_FROM_FILE: {
                            String mtlName = Utils.fetchRelativeName(loadPath, ((MtlLibEntry) entry).mtllib);
                            try {
                                mtlLoader.parseFromFile(mtlName);
                            } catch (IOException e) {
                                throw new ParseError("Cannot parse MTL file '" + mtlName + "'", linepos, e);
                            }
                            break;
                        }
                        case LOAD_FROM_RESOURCE: {
                            String mtlName = Utils.fetchRelativeName(loadPath, ((MtlLibEntry) entry).mtllib);
                            try {
                                mtlLoader.parseFromResource(mtlName);
                            } catch (IOException e) {
                                throw new ParseError("Cannot parse MTL resource '" + mtlName + "'", linepos, e);
                            }
                            break;
                        }
                        default:
                            throw new Error("Undefined load mode (please report)");
                    }
                    break;
                case LINE_COMMENT:
                case NO_ENTRY:
                    continue;
                default:
                    // let's parse a new geometry
                    linepos = parseGeometry(lines, linepos);
            }
        }
    }

    public int parseGeometry(String[] lines, int startline) {
        int linepos = startline - 1;
        String objName = null;
        String matName = null;
        List<VertexEntry> vertexEntries = new ArrayList<>();
        List<TextureEntry> textureEntries = new ArrayList<>();
        List<NormalEntry> normalEntries = new ArrayList<>();
        List<FaceEntry> faceEntries = new ArrayList<>();
        boolean cont = true;
        while (cont) {
            linepos++;
            if (linepos - 1 >= lines.length) {
                // no more line, let's build the geometry
                break;
            }
            ObjEntry entry = parseLine(linepos, lines[linepos - 1]);
            switch (entry.getType()) {
                case VERTEX:
                    vertexEntries.add(entry.asVertex());
                    break;
                case TEXT_COORD:
                    textureEntries.add(entry.asTexture());
                    break;
                case NORMAL:
                    normalEntries.add(entry.asNormal());
                    break;
                case FACE:
                    faceEntries.add(entry.asFace());
                    break;
                case NO_ENTRY:
                case UNSUPPORTED:
                    break;
                case USEMTL:
                    if (matName == null) {
                        matName = ((UseMtlEntry) entry).matName;
                    } else {
                        // a new material is specified, we end the geometry
                        linepos--;
                        cont = false;
                    }
                    break;
                case MTLLIB:
                    // somewhat unexpected, we end the geometry
                    linepos--;
                    cont = false;
                    break;
                case OBJNAME:
                    if (objName == null) {
                        objName = ((ObjNameEntry) entry).objName;
                    } else {
                        // a new object will start, we end the geometry
                        linepos--;
                        cont = false;
                    }
                    break;
                default:
                    throw new Error("Unexpected entry: " + entry);
            }
        }

        // Building the mesh components
        Map<GLVertex, Integer> vertexMap = new HashMap<>();
        List<GLVertex> glVertices = new ArrayList<>();
        List<GLTriangle> glTriangles = new ArrayList<>();
        boolean hasTextCoords = false;
        boolean hasNormals = false;

        for (FaceEntry faceEntry : faceEntries) {
            int[] triIndices = new int[faceEntry.face.length];
            for (int i = 0; i < faceEntry.face.length; i++) {
                FaceVertex faceVert = faceEntry.face[i];
                VertexEntry vertexEntry = vertexEntries.get(faceVert.vertexId - vertexCount - 1);
                TextureEntry textureEntry = null;
                if (faceVert.textId != 0) {
                    textureEntry = textureEntries.get(faceVert.textId - textCount - 1);
                }
                NormalEntry normalEntry = null;
                if (faceVert.normId != 0) {
                    normalEntry = normalEntries.get(faceVert.normId - normalCount - 1);
                }
                float[] positions = new float[3];
                positions[0] = vertexEntry.x;
                positions[1] = vertexEntry.y;
                positions[2] = vertexEntry.z;

                float[] textCoords = null;
                if (textureEntry != null) {
                    textCoords = new float[2];
                    textCoords[0] = textureEntry.tx;
                    textCoords[1] = textureEntry.ty;
                    hasTextCoords = true;
                }

                float[] normals = null;
                if (normalEntry != null) {
                    normals = new float[3];
                    normals[0] = normalEntry.nx;
                    normals[1] = normalEntry.ny;
                    normals[2] = normalEntry.nz;
                    hasNormals = true;
                }
                GLVertex vertex = new GLVertex(positions, textCoords, normals);

                int vertIndex;
                Integer vertId = vertexMap.get(vertex);
                if (vertId != null) {
                    vertIndex = vertId;
                } else {
                    // new vertex
                    glVertices.add(vertex);
                    vertIndex = glVertices.size() - 1;
                    vertexMap.put(vertex, vertIndex);
                }
                triIndices[i] = vertIndex;
            }
            // Here we have the vertex indices, we now apply a dumb triangulation
            // algorithm for faces with more than 3 points
            for (int i = 1; i < triIndices.length - 1; i++) {
                GLTriangle tri = new GLTriangle(triIndices[0], triIndices[i], triIndices[i + 1]);
                glTriangles.add(tri);
            }
        }

        // update counts
        vertexCount += vertexEntries.size();
        textCount += textureEntries.size();
        normalCount += normalEntries.size();

        GeometryBuilder geom = new GeometryBuilder();

        // we may now build the Mesh geometry
        for (GLVertex glVertex : glVertices) {
            geom.addVertices(glVertex.positions[0], glVertex.positions[1], glVertex.positions[2]);
            if (hasTextCoords) {
                geom.addTextCoord(glVertex.textCoords[0], glVertex.textCoords[1]);
            }
            if (hasNormals) {
                geom.addNormal(glVertex.normals[0], glVertex.normals[1], glVertex.normals[2]);
            }
        }

        for (GLTriangle glTriangle : glTriangles) {
            geom.addTriangle(glTriangle.indice1, glTriangle.indice2, glTriangle.indice3);
        }

        if (objName == null) {
            objName = objModel.getFreshGeomName();
        }
        objModel.addGeom(objName, geom);
        if (matName != null) {
            objModel.assignMaterial(objName, matName);
        }
        return linepos;
    }

    public ObjEntry parseLine(int linepos, String line) {
        if (line.isEmpty()) {
            return new NoEntry(linepos);
        }
        if (line.charAt(0) == '#') {
            return new LineComment(line.substring(1).trim(), linepos);
        }

        String[] parts = line.split("\\s+");
        if (parts.length == 0) {
            return new NoEntry(linepos);
        }

        if (parts[0].equals("o")) {
            if (parts.length < 2) {
                throw new ParseError("Missing object name", linepos);
            }
            return new ObjNameEntry(parts[1], linepos);
        } else if (parts[0].equals("mtllib")) {
            if (parts.length < 2) {
                throw new ParseError("Missing MTL library name", linepos);
            }
            return new MtlLibEntry(parts[1], linepos);
        } else if (parts[0].equals("usemtl")) {
            if (parts.length < 2) {
                throw new ParseError("Missing MTL material name", linepos);
            }
            return new UseMtlEntry(parts[1], linepos);
        } else if (parts[0].equals("v")) {
            // parse a vertex
            if (parts.length < 4) {
                throw new ParseError("Not enough coordinates for vertex", linepos);
            }
            float x, y, z;
            try {
                x = Float.parseFloat(parts[1]);
                y = Float.parseFloat(parts[2]);
                z = Float.parseFloat(parts[3]);
            } catch (NumberFormatException e) {
                throw new ParseError("Cannot parse vertex coordinates", linepos, e);
            }

            return new VertexEntry(x, y, z, linepos);
        } else if (parts[0].equals("vn")) {
            // parse a normal
            if (parts.length < 4) {
                throw new ParseError("Not enough coordinates for normal", linepos);
            }
            float nx, ny, nz;
            try {
                nx = Float.parseFloat(parts[1]);
                ny = Float.parseFloat(parts[2]);
                nz = Float.parseFloat(parts[3]);
            } catch (NumberFormatException e) {
                throw new ParseError("Cannot parse normal coordinates", linepos, e);
            }

            return new NormalEntry(nx, ny, nz, linepos);
        } else if (parts[0].equals("vt")) {
            // parse a texture coordinates
            if (parts.length < 3) {
                throw new ParseError("Not enough coordinates for texture", linepos);
            }
            float tx, ty;
            try {
                tx = Float.parseFloat(parts[1]);
                ty = 1.0f - Float.parseFloat(parts[2]); // on a inversé les coordonnées de textures car en OpenGL, l'origine (0,0) est en haut à droite
                                                        // dans notre cas, l'origine est en bas à gauche
                                                        // le mapping est correcte maintenant
            } catch (NumberFormatException e) {
                throw new ParseError("Cannot parse texture coordinates", linepos, e);
            }

            return new TextureEntry(tx, ty, linepos);
        } else if (parts[0].equals("f")) {
            // Parse a face
            List<FaceVertex> faces = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                String[] sindices = parts[i].split("/");
                if (sindices.length == 0 || sindices.length > 3) {
                    throw new ParseError("Cannot parse face : wrong indices", linepos);
                }
                int[] indices = new int[sindices.length];
                for (int j = 0; j < sindices.length; j++) {
                    try {
                        if (sindices[j].equals("")) {
                            indices[j] = 0;
                        } else {
                            indices[j] = Integer.parseInt(sindices[j]);
                        }
                    } catch (NumberFormatException e) {
                        throw new ParseError("Cannot parse face index", linepos, e);
                    }
                }
                switch (indices.length) {
                    case 1:
                        faces.add(new FaceVertex(indices[0], 0, 0));
                        break;
                    case 2:
                        faces.add(new FaceVertex(indices[0], indices[1], 0));
                        break;
                    case 3:
                        faces.add(new FaceVertex(indices[0], indices[1], indices[2]));
                        break;
                    default:
                        throw new Error("Should not be reachable (please report)");
                }
            }
            return new FaceEntry(faces.toArray(new FaceVertex[0]), linepos);
        } else {
            return new UnsupportedEntry(parts[0], line, linepos);
        }

    }

    public enum LoadMode {
        LOAD_UNDEFINED, LOAD_FROM_FILE, LOAD_FROM_RESOURCE
    }

    public static class ParseError extends Error {
        public ParseError(String message) {
            super("Parse error ==> " + message);
        }

        public ParseError(String message, int linepos) {
            super("Parse error at line " + linepos + "\n ==> " + message);
        }

        public ParseError(String message, Throwable cause) {
            super("Parse error ==> " + message, cause);
        }

        public ParseError(String message, int linepos, Throwable cause) {
            super("Parse error at line " + linepos + "\n ==> " + message, cause);
        }
    }

}

class GLVertex {
    public float[] positions;
    public float[] textCoords;
    public float[] normals;

    public GLVertex(float[] positions, float[] textCoords, float[] normals) {
        this.positions = positions;
        this.textCoords = textCoords;
        this.normals = normals;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(positions)
                + (textCoords == null ? 4242 : Arrays.hashCode(textCoords)
                + (normals == null ? 66666 : Arrays.hashCode(normals)));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        GLVertex other = (GLVertex) obj;
        if (!Arrays.equals(other.positions, positions)) {
            return false;
        }
        if (other.textCoords == null) {
            return textCoords == null;
        }
        if (!Arrays.equals(other.textCoords,textCoords)) {
            return false;
        }
        if (other.normals == null) {
            return normals == null;
        }
        return Arrays.equals(other.normals, normals);
    }
}

class GLTriangle {
    public int indice1;
    public int indice2;
    public int indice3;

    public GLTriangle(int indice1, int indice2, int indice3) {
        this.indice1 = indice1;
        this.indice2 = indice2;
        this.indice3 = indice3;
    }
}
