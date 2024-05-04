package yaw.engine.shader;

import org.joml.Vector3f;
import yaw.engine.mesh.Material;

public class ShaderProgramPBR extends ShaderProgram {
    private final String glVersion;
    private final boolean glCoreProfile;

    private final ShaderProperties shaderProperties;

    public ShaderProgramPBR(String glVersion, boolean glCoreProfile, ShaderProperties shaderProperties) {
        this.glVersion = glVersion;
        this.glCoreProfile = glCoreProfile;
        this.shaderProperties = shaderProperties;
    }

    public ShaderProgramPBR(ShaderProperties shaderProperties) {
        this("330", true, shaderProperties);
    }

    public static ShaderCode computeShadow(ShaderCode code) {
        code.function("Computation of shadowmap (only for directional lights, for now ...)",
                "float", "computeShadow", new String[][] {{"vec4", "lightSpace"}, {"vec3", "to_light_dir"}, {"vec3", "normal"}});

        code.l().l("vec3 projCoords = lightSpace.xyz / lightSpace.w")
                .l("projCoords = projCoords * 0.5 + 0.5")
                .l("float currentDepth = projCoords.z");

        code.l().l("if(currentDepth > 1.0) currentDepth = 0.0");

        code.l().l("float cosTheta = clamp(dot(normal, to_light_dir), 0, 1)")
                .l("float rbias = shadowBias*tan(acos(cosTheta))")
                .l("rbias = clamp(rbias, 0,0.01)");

        code.l().l("float shadow = 0.0")
                .l("vec2 texelSize = 1.0 / textureSize(shadowMapSampler, 0")
                .beginFor("int x = -1", "x <= 1", "++x")
                .beginFor("int y = -1", "y <= 1", "++y")
                .l("float pcfDepth = texture(shadowMapSampler, projCoords.xy + vec2(x, y) * texelSize).r")
                .l("shadow += currentDepth-rbias > pcfDepth ? 1.0 : 0.0")
                .endFor()
                .endFor()
                .l("shadow /= 9.0");

        code.l().l("return shadow");

        return code.endFunction();
    }

    public ShaderCode vertexShader(boolean withShadows) {
        ShaderCode code = new ShaderCode(glVersion, glCoreProfile)
                .l()
                .cmt("Input buffer components")
                .l("layout(location = 0) in vec3 position")
                .l("layout(location = 1) in vec2 texCoord")
                .l("layout(location = 2) in vec3 normal")
                .l("layout(location = 3) in vec4 color")
                .l("layout(location = 4) in vec3 tangent")
                .l()
                .cmt("Output values")
                .l("out vec3 vPos")
                .l("out vec2 vTexCoord")
                .l("out vec3 vNorm")
                .l("out vec4 vColor")
                .l("out vec3 vTangent");

        if (withShadows) {
            code.l().cmt("Shadow properties")
                    .l("out vec4 vDirectionalShadowSpace");
        }

        code.l().cmt("Camera-level uniforms")
                .l("uniform mat4 worldMatrix")
                .l()
                .cmt("Model-level uniforms")
                .l("uniform mat4 modelMatrix")
                .l("uniform mat3 normalMatrix");

        if (withShadows) {
            code.l().cmt("Shadow uniforms")
                    .l("uniform mat4 directionalShadowMatrix");
        }

        code.l().beginMain()
                .cmt("World vertex position")
                .l("vec4 mvPos = modelMatrix * vec4(position, 1.0)")
                .cmt("Projected position")
                .l("gl_Position = worldMatrix * mvPos")
                .cmt("Output copy of position")
                .l("vPos = mvPos.xyz;")
                .cmt("Computation of normal vector")
                .l("vNorm = normalize(mat3(normalMatrix) * normal)")
                .cmt("Texture coordinates")
                .l("vTexCoord = texCoord")

                .cmt("Vertexes Color")
                .l("vColor = color")
                .cmt("Normal map tangents")
                .l("vTangent = normalize(mat3(normalMatrix) * tangent)");

        if (withShadows) {
            code.l().cmt("Shadow output")
                    .l("vDirectionalShadowSpace = directionalShadowMatrix * mvPos");
        }

        code.endMain();

        return code;
    }

    public ShaderCode fragmentShader(boolean hasDirectionalLight, int maxPointLights, int maxSpotLights, boolean hasTexture, boolean withShadows) {
        ShaderCode code = new ShaderCode(glVersion, glCoreProfile)
                .cmt("Fragment shader for Physically Based Rendering")
                .l();

        code = ggxDistribution(code);
        code = schlickFresnel(code);
        code = geomSmith(code);
        code = calcPBRLight(code);

        if (maxPointLights > 0) {
            code.l("const int MAX_POINT_LIGHTS = " + maxPointLights);
        }
        if (maxSpotLights > 0) {
            code.l("const int MAX_SPOT_LIGHTS = " + maxSpotLights);
        }

        //TODO: verifier LocalPos0 et cameraPos

        code.l("in vec3 vPos")
                .l("in vec2 vTexCoord")
                .l("in vec3 vNorm")
                .l("in vec4 vColor")
                .l("in vec3 vTangent");

        // Uniforms
        code.l("uniform Material material")
                .l("uniform vec3 ambientLight")
                .l("uniform vec3 cameraPos");


        if (withShadows) {
            code.l("in vec4 vDirectionalShadowSpace");
        }

        // Output color
        code.l("out vec4 fragColor");

        // Material struct
        code.beginStruct("Material")
                .item("sampler2D", "metallicRoughnessTex")
                .item("sampler2D", "normalTex")
                .item("sampler2D", "emissiveTex")
                .item("float", "metallic")
                .item("float", "roughness")
                .item("int", "isMetal");

        if (hasTexture) {
            code.item("sampler2D", "texture_sampler");
        } else {
            code.item("vec3","color");
        }
        code.endStruct()
                .l();

        code.beginStruct("BaseLight")
                .item("vec3", "color")
                .item("float", "ambientIntensity")
                .item("float", "diffuseIntensity")
                .endStruct();

        if (hasDirectionalLight) {
            code.beginStruct("DirectionalLight")
                    .item("BaseLight", "base")
                    .item("vec3", "direction")
                    .endStruct()
                    .l("uniform DirectionalLight directionalLight");
        }

        if (maxPointLights > 0) {
            code.beginStruct("PointLight")
                    .item("BaseLight", "base")
                    .item("vec3", "localPos")
                    .item("vec3","worldPos")
                    .item("float", "att_constant")
                    .item("float", "att_linear")
                    .item("float", "att_quadratic")
                    .endStruct()
                    .l("uniform PointLight pointLights[MAX_POINT_LIGHTS]")
                    .l("uniform int nbPointLights");
        }

        if (maxSpotLights > 0) {
            code.beginStruct("SpotLight")
                    .item("PointLight", "pl")
                    .item("vec3", "conedir")
                    .item("float", "cutoff")
                    .endStruct()
                    .l("uniform SpotLight spotLights[MAX_SPOT_LIGHTS]")
                    .l("uniform int nbSpotLights");
        }

        if (withShadows) {
            code.l("uniform sampler2D shadowMap")
                    .l("uniform float shadowBias");
            code = computeShadow(code);
        }

        code.beginMain()
                .l("vec3 accumLight = vec3(0.0);");  // Accumulateur de lumière

        if (hasDirectionalLight) {
            code.l("vec3 lightEffect = calcPBRLight(directionalLight.base, vPos, 1, vNorm, material);")
                    .l("accumLight += lightEffect;");
        }

        if (maxPointLights > 0) {
            code.beginFor("int i = 0", "i < nbPointLights", "i++")
                    .l("vec3 pointLightEffect = calcPBRLight(pointLights[i].base, vPos, 0, vNorm, material);")
                    .l("accumLight += pointLightEffect;")
                    .endFor();
        }

        if (maxSpotLights > 0) {
            code.beginFor("int i = 0", "i < nbSpotLights", "i++")
                    .l("vec3 spotLightEffect = calcPBRLight(spotLights[i].pl.base, vPos, 0, vNorm, material);")
                    .l("accumLight += spotLightEffect;")
                    .endFor();
        }

        // HDR tone mapping
        code.l("totalLight = accumLight / (accumLight + vec3(1.0));");

        // Gamma correction
        code.l("vec4 finalLight = vec4(pow(totalLight, vec3(1.0/2.2)), 1.0);");

        code.beginIf("material.roughness > 0.5")
                .l("material.isMetal = 1");
        code.endIf().beginElse()
                .l("material.isMetal = 0");
        code.endElse();

        code.l("vec3 ambientComponent = ambientLight * material.color;")
                .l("vec3 finalColor = ambientComponent + finalLight;")
                .l("fragColor = vec4(finalColor, 1.0);");

        code.endMain();

        return code;
    }


    /**
     * Create uniform for each attribute of the PBR material
     *
     * @param uniformName uniform name
     */
    public void createMaterialUniform(String uniformName, boolean textured) {

        createUniform(uniformName + ".texture_sampler");
        createUniform(uniformName + ".color");

        createUniform(uniformName + ".metallicFactor");
        createUniform(uniformName + ".roughnessFactor");
    }


    /**
     * Modifies the value of a uniform material with the specified material
     *
     * @param uniformName the uniform name
     * @param material    the PBR material
     */
    public void setUniform(String uniformName, Material material) {

        setUniform(uniformName + ".texture_sampler", 0); // TODO : assign sampler slots more dynamically
        setUniform(uniformName + ".color", material.getBaseColor());

        if (material.hasMetallicRoughnessTexture()) {
            createUniform(uniformName + ".metallicRoughnessTex");
            setUniform(uniformName + ".metallicRoughnessTex", 1);
        }
        if (material.hasPbrNormalTexture()){
            createUniform(uniformName + ".normalTex");
            setUniform(uniformName + ".normalTex", 2);
        }
        if (material.hasEmissiveTexture()){
            createUniform(uniformName + ".emissiveTex");
            setUniform(uniformName + ".emissiveTex", 3);
        }
        setUniform(uniformName + ".metallicFactor", material.getMetallic());
        setUniform(uniformName + ".roughnessFactor", material.getRoughness());
    }

    public static ShaderCode calcPBRLight(ShaderCode code) {
        code.function("Compute PBR-based direct lighting",
                "vec3", "calcPBRLight",
                new String[][]{{"BaseLight", "light"},
                        {"vec3", "posDir"},
                        {"int", "isDirLight"},
                        {"vec3", "normal"},
                        {"Material", "material"}});

        code.l().l("vec3 lightIntensity = light.color * light.DiffuseIntensity;")
                .l("vec3 l = vec3(0.0);");
        code.beginIf("isDirLight == 1")
                .l("l = -posDir.xyz;");
        code.endIf().beginElse()
                .l().l("l = posDir - position;") //CHECKME
                .l("float lightToPixelDist = length(l);")
                .l("l = normalize(l);")
                .l("lightIntensity /= (lightToPixelDist * lightToPixelDist);");
        code.endElse();

        code.l().l("vec3 n = normal")
                .l("vec3 v = normalize(gl_Position - position);") //CHECKME
                .l("vec3 h = normalize(v + l);");

        code.l().l("float nDotH = max(dot(n, h), 0.0);")
                .l("float vDotH = max(dot(v, h), 0.0);")
                .l("float nDotL = max(dot(n, l), 0.0);")
                .l("float nDotV = max(dot(n, v), 0.0);");

        code.l().l("vec3 F = schlickFresnel(vDotH, material);")
                .l("vec3 kS = F;")
                .l("vec3 kD = vec3(1.0) - kS;");

        code.l().l("vac 3 specBRDF_nom = ggxDistribution(nDotH, material.roughness) * F * geomSmith(nDotL, material.roughness) * geomSmith(nDotV, material.roughness);")
                .l("float specBRDF_denom = 4.0 * nDotV * nDotL + 0.0001;")
                .l("vec3 specBRDF = specBRDF_nom / specBRDF_denom;");

        code.l().l("vec3 flambert = vec3(0.0);");
        code.beginIf("material.isMetal == 0")
                .l("flambert = material.color;") //CHECKME: fix
                .endIf();

        code.l().l("vec3 diffuseBRDF = kD * flambert / PI;")
                .l("vec3 finalColor = (diffuseBRDF + specBRDF) * lightIntensity * nDotL;");

        code.l().l("return finalColor;");

        return code.endFunction();
    }

    public static ShaderCode geomSmith(ShaderCode code) {
        code.function("Geometry function using Smith's method",
                "float", "geomSmith",
                new String[][]{{"float", "dp"},
                        {"float", "roughness"}});

        code.l("float k = (roughness + 1.0) * (roughness + 1.0) / 8.0;")
                .l("float denom = dp * (1 - k) + k;");

        code.l().l("return dp / denom;");
        return code.endFunction();
    }

    public static ShaderCode ggxDistribution(ShaderCode code) {
        code.function("GGX distribution function",
                "float", "ggxDistribution",
                new String[][]{{"vec3", "nDotH"},
                        {"float", "roughness"}});

        code.l("float alpha2 = roughness * roughness * roughness * roughness;")
                .l("float d = nDotH * nDotH * (alpha2 - 1.0) + 1.0;");

        code.l().l("return alpha2 / (PI * d * d);");
        return code.endFunction();
    }

    public static ShaderCode schlickFresnel(ShaderCode code){
        code.function("Schlick's approximation for Fresnel effect",
                "vec3", "schlickFresnel",
                new String[][]{{"float", "vDotH"},
                        {"Material","material"}});

        code.l().l("vec3 f0 = vec3(0.04);"); // actual F0 for the metals
        code.l().beginIf("material.isMetal == 1")
                .l("f0 = material.color;"); //FIXME: fix
        code.endIf();

        code.l().l("vec3 ret = f0 + (1 - f0) * pow(clamp(1.0 - vDotH, 0.0, 1.0), 5);")
                .l("return ret;");

        return code.endFunction();
    }

    public void init() {
        /* Initialization of the shader program. */
        ShaderCode vertexCode = vertexShader(shaderProperties.withShadows);
        //System.out.println("Vertex shader:\n" + vertexCode);
        createVertexShader(vertexCode);

        ShaderCode fragmentCode = fragmentShader(shaderProperties.hasDirectionalLight,
                shaderProperties.maxPointLights,
                shaderProperties.maxSpotLights,
                shaderProperties.hasTexture,
                shaderProperties.withShadows);
        //System.out.println("Fragment shader:\n" + fragmentCode);
        createFragmentShader(fragmentCode);

        /* Binds the code and checks that everything has been done correctly. */
        link();

        createUniform("worldMatrix");
        createUniform("modelMatrix");
        createUniform("normalMatrix");

        /* Initialization of the shadow map matrix uniform. */
        if (shaderProperties.withShadows) {
            createUniform("directionalShadowMatrix");
        }

        /* Create uniform for material. */
        createMaterialUniform("material", shaderProperties.hasTexture);

        /* Initialization of the light's uniform. */
        createUniform("camera_pos");

        createUniform("ambientLight");
        if (shaderProperties.hasDirectionalLight) {
            createDirectionalLightUniform("directionalLight");
        }

        if (shaderProperties.maxPointLights > 0) {
            createPointLightListUniform("pointLights", shaderProperties.maxPointLights);
        }

        if (shaderProperties.maxSpotLights > 0) {
            createSpotLightUniformList("spotLights", shaderProperties.maxSpotLights);
        }

        if (shaderProperties.withShadows) {
            createUniform("shadowMapSampler");
            createUniform("shadowBias");
        }
    }
}
