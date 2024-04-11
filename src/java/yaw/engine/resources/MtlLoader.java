package yaw.engine.resources;

import org.joml.Vector3f;

import java.io.*;

public class MtlLoader {
    private LoadMode loadMode = LoadMode.LOAD_UNDEFINED;
    private String loadPath = null;
    private MtlMaterial currentMaterial = null;
    private ObjModel objModel;

    public MtlLoader(ObjModel objModel) {
        this.objModel = objModel;
    }

    public MtlLoader() {
    }

    public MtlMaterial getMat() {
        return currentMaterial;
    }

    public void parseFromFile(String filename) throws IOException {
        loadMode = LoadMode.LOAD_FROM_FILE;
        loadPath = filename;
        parseFromBufferedReader(new BufferedReader(new FileReader(filename)));
    }

    public void parseFromResource(String name) throws IOException {
        InputStream istream = ObjLoader.class.getResourceAsStream(name);
        if (istream == null) {
            throw new ObjLoader.ParseError("Cannot find resource: " + name);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
        loadMode = LoadMode.LOAD_FROM_RESOURCE;
        loadPath = name;
        parseFromBufferedReader(reader);
    }

    public void parseFromBufferedReader(BufferedReader reader) throws IOException {
        String[] lines = Utils.readLines(reader);
        parseFromLines(lines);
    }

    public void parseFromLines(String[] lines) {
        for (int linepos = 1; linepos <= lines.length; linepos++) {
            parseLine(linepos, lines[linepos - 1]);
        }
    }

    public void parseLine(int linepos, String line) {
        if (line.isEmpty()) {
            return;
        }
        if (line.charAt(0) == '#') {
            return;
        }


        String[] parts = line.split("\\s+");
        if (parts.length == 0) {
            return;
        }

        if (parts[0].equals("newmtl")) {
            // new material
            if (parts.length < 2) {
                throw new ObjLoader.ParseError("Missing material name", linepos);
            }
            String matName = parts[1];
            MtlMaterial material = new MtlMaterial(matName);
            //System.out.println("MTL material: " + material);
            objModel.addMaterial(matName, material);
            currentMaterial = material;
        } else if (parts[0].equals("Ns")) {
            float shineness = parseMaterialFloat("shineness", parts, linepos);
            currentMaterial.shineness = shineness;
        } else if (parts[0].equals("Ka")) {
            Vector3f ambient = parseMaterialVector3f("ambient", parts, linepos);
            currentMaterial.ambient = ambient;
        } else if (parts[0].equals("Kd")) {
            Vector3f diffuse = parseMaterialVector3f("diffuse", parts, linepos);
            currentMaterial.diffuse = diffuse;
        } else if (parts[0].equals("Ks")) {
            Vector3f specular = parseMaterialVector3f("specular", parts, linepos);
            currentMaterial.specular = specular;
        } else if (parts[0].equals("Ke")) {
            Vector3f emissive = parseMaterialVector3f("emissive", parts, linepos);
            currentMaterial.emissive = emissive;
        } else if (parts[0].equals("d")) {
            float opacity = parseMaterialFloat("opacity", parts, linepos);
            currentMaterial.opacity = opacity;
        }
        // gestion des textures
        //TODO : gerer si ces lignes contiennent des options, avec une methode auxiliaire
        else if (parts[0].equals("map_Kd")) {
            currentMaterial.map_Kd = parts[1];
        } else if (parts[0].equals("map_bump")) {
            currentMaterial.map_Bump = parts[1];
        } else if (parts[0].equals("map_Ns")) {
            System.out.println("map_Ns : " + parts[1]);
            currentMaterial.map_Ns = parts[1];
        } else {
            // unsupported entry : emit warning ?
        }
    }

    public float parseMaterialFloat(String desc, String[] parts, int linepos) {
        if (parts.length < 2) {
            throw new ObjLoader.ParseError("Missing " + desc + " value", linepos);
        }
        if (currentMaterial == null) {
            throw new ObjLoader.ParseError("Material name is not specified", linepos);
        }
        float value = 0;
        try {
            value = Float.parseFloat(parts[1]);
        } catch (NumberFormatException e) {
            throw new ObjLoader.ParseError("Cannot parse " + desc + " value", linepos, e);
        }
        return value;
    }

    public Vector3f parseMaterialVector3f(String desc, String[] parts, int linepos) {
        if (parts.length < 4) {
            throw new ObjLoader.ParseError("Missing " + desc + " values", linepos);
        }
        if (currentMaterial == null) {
            throw new ObjLoader.ParseError("Material name is not specified", linepos);
        }
        float x = 0, y = 0, z = 0;
        try {
            x = Float.parseFloat(parts[1]);
            y = Float.parseFloat(parts[2]);
            z = Float.parseFloat(parts[3]);
        } catch (NumberFormatException e) {
            throw new ObjLoader.ParseError("Cannot parse " + desc + " value", linepos, e);
        }
        return new Vector3f(x, y, z);
    }

    public enum LoadMode {
        LOAD_UNDEFINED, LOAD_FROM_FILE, LOAD_FROM_RESOURCE
    }
}
