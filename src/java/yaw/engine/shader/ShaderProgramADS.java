package yaw.engine.shader;

import org.joml.Vector3f;
import yaw.engine.light.LightModel;
import yaw.engine.mesh.Material;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;

public class ShaderProgramADS extends ShaderProgram {
    private final String glVersion;
    private final boolean glCoreProfile;

    private final ShaderProperties shaderProperties;

    public ShaderProgramADS(String glVersion, boolean glCoreProfile, ShaderProperties shaderProperties) {
        this.glVersion = glVersion;
        this.glCoreProfile = glCoreProfile;
        this.shaderProperties = shaderProperties;
    }

    public ShaderProgramADS(ShaderProperties shaderProperties) {
        this("330", true, shaderProperties);
    }

    public static ShaderCode computeLight(ShaderCode code) {
        code.function("Compute diffuse and specular components of lights.",
                "vec4", "computeLight", new String[][]{{"vec3", "light_color"},
                        {"float", "light_intensity"},
                        {"vec3", "position"},
                        {"vec3", "to_light_dir"},
                        {"vec3", "normal"}});

        code.l("vec4 diffusecolor = vec4(0, 0, 0, 0)")
                .l("vec4 speccolor = vec4(0, 0, 0, 0)");

        code.l().cmt("Diffuse color")
                .l("float diffuseFactor = max(dot(normal, to_light_dir), 0.0)")
                .l("diffusecolor = vec4(light_color, 1.0) * light_intensity * diffuseFactor * vec4(material.diffuse, 1.0)");

        code.l().cmt("Specular color")
                .l("vec3 camera_direction = normalize(camera_pos - position)")
                .l("vec3 from_light_dir = -to_light_dir")
                .l("vec3 reflected_light = normalize(reflect(from_light_dir , normal))")
                .l("float specularFactor = max( dot(camera_direction, reflected_light), 0.0)")
                .l("specularFactor = pow(specularFactor, material.shineness)")
                .l("speccolor = light_intensity  * specularFactor * vec4(material.specular, 1.0) * vec4(light_color, 1.0)");

        code.l().l("return (diffusecolor + speccolor)");

        return code.endFunction();
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

    public static ShaderCode computeDirectionalLight(ShaderCode code, boolean withShadows) {
        code.function("vec4", "computeDirectionalLight",
                new String[][]{{"DirectionalLight", "light"},
                        {"vec3", "position"},
                        {"vec3", "normal"}});

        if (withShadows) {
            code.l("float shadow = computeShadow(vDirectionalShadowSpace, normalize(-light.direction), normal)")
                    .l("return (1.0 - shadow) * computeLight(light.color, light.intensity, position, normalize(-light.direction), normal)");
        } else {
            code.l("return computeLight(light.color, light.intensity, position, normalize(-light.direction), normal)");
        }
        return code.endFunction();
    }

    public static ShaderCode computePointLight(ShaderCode code) {
        code.function("vec4", "computePointLight", new String[][]{{"PointLight", "light"}
                , {"vec3", "position"}
                , {"vec3", "normal"}});

        code.l().l("vec3 light_direction = light.position - position")
                .l("vec3 to_light_dir  = normalize(light_direction)")
                .l("vec4 light_color = computeLight(light.color, light.intensity, position, to_light_dir, normal)");

        code.l().cmt("Attenuation")
                .l("float distance = length(light_direction)")
                .l("float attenuationInv = light.att_constant + light.att_linear * distance + light.att_quadratic * (distance * distance)");

        code.l().l("return light_color / attenuationInv");

        return code.endFunction();
    }

    public static ShaderCode computeSpotLight(ShaderCode code) {
        code.function("vec4", "computeSpotLight", new String[][]{{"SpotLight", "light"}
                , {"vec3", "position"}
                , {"vec3", "normal"}});

        code.l().l("vec3 light_direction = light.pl.position - position")
                .l("vec3 to_light_dir  = normalize(light_direction)")
                .l("vec3 from_light_dir  = -to_light_dir")
                .l("float spot_alfa = dot(from_light_dir, normalize(light.conedir))")
                .l("vec4 color = vec4(0, 0, 0, 0)");

        code.beginIf("spot_alfa > light.cutoff")
                .l("color = computePointLight(light.pl, position, normal)")
                .l("color *= (1.0 - (1.0 - spot_alfa)/(1.0 - light.cutoff))")
                .endIf();

        code.l("return color");

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
                .cmt("Fragment shader for A(mbient) D(iffuse) S(pecular) rendering")
                .l();

        code.cmt("Max lights constants (forward rendering)");
        if (maxPointLights == 0) {
            code.cmt("No point light");
        } else {
            code.l("const int MAX_POINT_LIGHTS = " + maxPointLights);
        }

        if (maxSpotLights == 0) {
            code.cmt("No spot light");
        } else {
            code.l("const int MAX_SPOT_LIGHTS = " + maxSpotLights);
        }


        code.l().cmt("Input values")
                .l("in vec3 vPos")
                .l("in vec2 vTexCoord")
                .l("in vec3 vNorm")

                 .cmt("Vertexes color")
                .l("in vec4 vColor")
                .cmt("Normal map tangents")
                .l("in vec3 vTangent");
        if (withShadows) {
            code.l("in vec4 vDirectionalShadowSpace");
        }

        code.l()
                .cmt("Output values")
                .l("out vec4 fragColor");

        code.l().cmt("Structures").l();

        code.beginStruct("DirectionalLight")
                .item("vec3", "color")
                .item("float", "intensity")
                .item("vec3", "direction")
                .endStruct().l();

        code.beginStruct("PointLight")
                .item("vec3", "color")
                .item("float", "intensity")
                .item("vec3", "position", "Position assumed in view coordinates")
                .cmt("Attenuations")
                .item("float", "att_constant")
                .item("float", "att_linear")
                .item("float", "att_quadratic")
                .endStruct().l();

        code.beginStruct("SpotLight")
                .item("PointLight", "pl")
                .item("vec3", "conedir")
                .item("float", "cutoff")
                .endStruct().l();

        code.beginStruct("Material");

        if (hasTexture) {
            code.item("sampler2D", "texture_sampler", "Texture (2D)");
        } else {
            code.item("vec3", "color", "Non-textured material");
        }

        code.item("sampler2D", "specularMap", "Specular");
        code.item("sampler2D", "normalMap", "Normal map");
        code.item("vec3", "ambient", "the ambient color (multiplied by ambient light)")
                .item("vec3", "emissive", "Emissive color (added to overally color)")
                .item("float", "emissiveAmount", "The percentage (0 .. 1.0) of emissive color")
                .item("vec3", "diffuse", "the diffuse color (if no diffuse map")
                .item("vec3", "specular", "the specular color (if no specular map)")
                .item("float", "shineness", "for reflectance computation")
                .item("float", "opacity","material opacity from 0.0 (fully transparent) to 1.0 (fully opaque)")
                .endStruct().l();

        code.cmt("Fragment shader uniforms")
                .l("uniform vec3 camera_pos")
                .l("uniform Material material")
                .l().cmt("Lights")
                .l("uniform vec3 ambientLight")
                .cmt("Determine whether the material has a specular or normal map")
                .l("uniform int useSpecularMap")
                .l("uniform int useNormalMap");

        if (hasDirectionalLight) {
            code.l("uniform DirectionalLight directionalLight");
        }

        if (maxPointLights > 0) {
            code.l("uniform PointLight pointLights[MAX_POINT_LIGHTS]");
            code.l("uniform int nbPointLights");
        }

        if (maxSpotLights > 0) {
            code.l("uniform SpotLight spotLights[MAX_SPOT_LIGHTS]");
            code.l("uniform int nbSpotLights");
        }

        if (withShadows) {
            code.l().cmt("Shadow map uniforms")
                    .l("uniform sampler2D shadowMapSampler")
                    .l("uniform float shadowBias");
        }

        code.l().cmt("Auxiliary functions").l();

        code = computeLight(code);

        if (withShadows) {
            code.l();
            code = computeShadow(code);
        }

        if (hasDirectionalLight) {
            code.l();
            code = computeDirectionalLight(code, withShadows);
        }

        if (maxPointLights > 0) {
            code.l();
            code = computePointLight(code);
        }

        if (maxSpotLights > 0) {
            code.l();
            code = computeSpotLight(code);
        }

        code.l().beginMain()
                .l("vec3 normal = vNorm").l();

        // normal map
        code.beginIf("useNormalMap == 1");
        code.l("vec3 tangent = normalize(vTangent)")
                .l("vec3 bitangent = normalize(cross(normal, tangent))")

                .l().l("mat3 tbn = mat3(tangent, bitangent, normal)")
                .l("normal = texture(material.normalMap, vTexCoord).rgb * 3. - 1.")
                .l("normal = normalize(tbn * normal)");
        code.endIf();

        if (hasTexture) {
            code.l("vec4 basecolor = texture(material.texture_sampler, vTexCoord)");
        } else {
            code.l("vec4 basecolor = vec4(material.color, 1)");
        }

        code.l().l("vec4 totalLight = vec4(ambientLight * material.ambient, 1.0)");

        if (hasDirectionalLight) {
            code.l().l("totalLight += computeDirectionalLight(directionalLight, vPos, normal)");
        }

        if (maxPointLights > 0) {
            code.beginFor("int i = 0", "i < nbPointLights", "i++")
                    .beginIf("pointLights[i].intensity > 0")
                    .l("totalLight += computePointLight(pointLights[i], vPos, normal)")
                    .endIf();
            code.endFor();
        }

        if (maxSpotLights > 0) {
            code.beginFor("int i = 0", "i < nbSpotLights", "i++")
                    .beginIf("spotLights[i].pl.intensity > 0")
                    .l("totalLight += computeSpotLight(spotLights[i], vPos, normal)")
                    .endIf();
            code.endFor();
        }

        //calculations
        code.l("vec3 lightDirection = normalize(directionalLight.direction);")
                .l("float fakeLight = dot(lightDirection, normal) * .5 + .5;")
                .l("vec3 surfaceToViewDirection = normalize(vPos);")
                .l("vec3 halfVector = normalize(lightDirection + surfaceToViewDirection);")
                .l("float specularLight = clamp(dot(normal, halfVector), 0.0, 1.0);");


        code.l().l("vec3 effectiveDiffuse = material.diffuse * basecolor.rgb * vColor.rgb;")
                .l("float effectiveOpacity = material.opacity * basecolor.a * vColor.a;");

        code.l("vec3 totalLightRGB = totalLight.rgb;")
                .l("vec3 emissiveRGB = material.emissiveAmount * material.emissive;");

        code.l().l("vec4 finalColor = vec4(basecolor * totalLight);");

        code.beginIf ("useSpecularMap == 1");
        code.l().l("vec4 specularIntensity = texture(material.specularMap, vTexCoord);")
                .l("vec3 effectiveSpecular = material.specular  * specularIntensity.rgb;");

            code.l().l(" finalColor += vec4(emissiveRGB + effectiveDiffuse * fakeLight + effectiveSpecular * pow(specularLight, material.shineness), effectiveOpacity);");
        code.endIf();

        code.beginElse();
            code.l().l("finalColor += vec4(emissiveRGB + effectiveDiffuse * fakeLight + material.specular * pow(specularLight, material.shineness), effectiveOpacity);");
        code.endElse();

        code.l().l("fragColor = finalColor");

        return code.endMain();

    }

    /**
     * Create uniform for each attribute of the material
     *
     * @param uniformName uniform name
     */
    public void createMaterialUniform(String uniformName, boolean textured) {
        if (textured) {
            createUniform(uniformName + ".texture_sampler");
        } else {
            createUniform(uniformName + ".color");
        }
        createUniform(uniformName + ".ambient");
        createUniform(uniformName + ".emissive");
        createUniform(uniformName + ".diffuse");
        createUniform(uniformName + ".specular");
        createUniform(uniformName + ".shineness");
    }

    /**
     * Modifies the value of a uniform material with the specified material
     *
     * @param uniformName the uniform name
     * @param material    the material
     */
    public void setUniform(String uniformName, Material material) {
        if (material.isTextured()) {
            setUniform(uniformName + ".texture_sampler", 0); // TODO : assign sampler slots more dynamically
        } else {
            setUniform(uniformName + ".color", material.getBaseColor());
        }
        if (material.hasSpecularMap()) {
            createUniform(uniformName + ".specularMap");
            setUniform(uniformName + ".specularMap", 1);
        }
        if (material.hasNormalMap()){
            createUniform(uniformName + ".normalMap");
            setUniform(uniformName + ".normalMap", 2);
        }
        setUniform(uniformName + ".ambient", material.getAmbientColor());
        Vector3f emissiveColor = new Vector3f();
        material.getEmissiveColor().mul(material.getEmissiveAmount(), emissiveColor);
        setUniform(uniformName + ".emissive", emissiveColor);
        setUniform(uniformName + ".diffuse", material.getDiffuseColor());
        setUniform(uniformName + ".specular", material.getSpecularColor());
        setUniform(uniformName + ".shineness", material.getShineness());
        setUniform(uniformName + ".diffuse", material.getDiffuseColor());
        setUniform(uniformName + ".specular", material.getSpecularColor());
        setUniform(uniformName + ".shineness", material.getShineness());
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
    public void prepareMaterial(Material material) {
        if (material.hasSpecularMap()) {
            glUniform1i(glGetUniformLocation(this.getId(), "useSpecularMap"), 1); //assigne la valeur 1 pour le code du frag shader
        } else {
            glUniform1i(glGetUniformLocation(this.getId(), "useSpecularMap"), 0);
        }

        if (material.hasNormalMap()) {
            glUniform1i(glGetUniformLocation(this.getId(), "useNormalMap"), 1);
        } else {
            glUniform1i(glGetUniformLocation(this.getId(), "useNormalMap"), 0);
        }
    }
}
