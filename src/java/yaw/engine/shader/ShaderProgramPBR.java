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

    public static ShaderCode computeDirectionalLight(ShaderCode code, boolean withShadows) {
        code.function("vec4", "computeDirectionalLightPBR",
                new String[][]{{"DirectionalLight", "light"},
                        {"vec3", "position"},
                        {"vec3", "normal"},
                        {"vec3", "viewDir"},
                        {"Material", "material"},
                        {"bool", "withShadows"}});

        code.l("vec3 lightDir = normalize(-light.direction);")
                .l("vec3 view = normalize(viewDir - position);")
                .l("vec4 lightEffect = calcPBRLight(light.color, light.intensity, position, lightDir, normal, view, 1, material);");

        if (withShadows) {
            code.l("float shadowFactor = computeShadow(light.shadowCoords, light.shadowMapSampler);")
                    .l("lightEffect *= shadowFactor;");
        }

        code.l("return lightEffect;")
                .endFunction();
        return code;
    }

    public static ShaderCode computePointLight(ShaderCode code) {
        code.function("vec4", "computePointLightPBR",
                new String[][]{{"PointLight", "light"},
                        {"vec3", "position"},
                        {"vec3", "normal"},
                        {"vec3", "viewDir"},
                        {"Material", "material"}});

        code.l("vec3 toLight = light.position - position;")
                .l("vec3 lightDir = normalize(toLight);")
                .l("vec3 view = normalize(viewDir - position);")
                .l("float distance = length(toLight);")
                .l("float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);")
                .l("vec4 lightEffect = attenuation * calcPBRLight(light.color, light.intensity, position, lightDir, normal, view, 0, material);");

        code.l("return lightEffect;")
                .endFunction();
        return code;
    }

    public static ShaderCode computeSpotLight(ShaderCode code) {
        code.function("vec4", "computeSpotLightPBR", new String[][]{{"SpotLight", "light"}
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

    public static ShaderCode computeShadow(ShaderCode code) {
        code.function("Computation of shadowmap (only for directional lights, for now ...)",
                "float", "computeShadow",
                new String[][]{{"vec4", "lightSpace"}, {"sampler2D", "shadowMapSampler"}, {"float", "shadowBias"}});

        // Transforming the light space coordinates to texture coordinates
        code.l("vec3 projCoords = lightSpace.xyz / lightSpace.w;")
                .l("projCoords = projCoords * 0.5 + 0.5;") // Transform to [0,1] range
                .l("float currentDepth = projCoords.z;");

        // Avoid using depth outside of the [0,1] range
        code.l("if(currentDepth > 1.0) currentDepth = 1.0;");

        // Calculate bias based on the maximum error introduced by the slope of the polygon
        code.l("float cosTheta = clamp(dot(normalize(lightSpace.xyz), normalize(projCoords.xyz)), 0.0, 1.0);")
                .l("float rbias = shadowBias * tan(acos(cosTheta));")
                .l("rbias = clamp(rbias, 0.0, 0.01);");

        // Initialize shadow intensity to zero
        code.l("float shadow = 0.0;")
                .l("vec2 texelSize = 1.0 / textureSize(shadowMapSampler, 0);"); // Calculate the size of one texel

        // Loop over a 3x3 kernel around the projected coordinate
        code.beginFor("int x = -1", "x <= 1", "++x")
                .beginFor("int y = -1", "y <= 1", "++y")
                .l("float pcfDepth = texture(shadowMapSampler, projCoords.xy + vec2(x, y) * texelSize).r;")
                // Compare the current fragment depth to the depth in the shadow map
                .l("shadow += (currentDepth - rbias > pcfDepth) ? 1.0 : 0.0;")
                .endFor()
                .endFor();

        // Average the total shadow contribution from the kernel to get percentage closer soft shadows
        code.l("shadow /= 9.0;") // Normalize the accumulated shadow determinations
                .l("return shadow;");

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

        code.l("in vec3 vPos")
                .l("in vec2 vTexCoord")
                .l("in vec3 vNorm")
                .l("in vec4 vColor")
                .l("in vec3 vTangent");

        if (withShadows) {
            code.l("in vec4 vDirectionalShadowSpace");
        }

        // Output color
        code.l("out vec4 fragColor");

        // Structs for lights
        code.beginStruct("Material")
                .item("sampler2D", "baseColorMap")
                .item("sampler2D", "metallicRoughnessMap")
                .item("sampler2D", "normalMap")
                .item("sampler2D", "emissiveMap")
                .item("float", "metallic")
                .item("float", "roughness")
                .item("int", "isMetal")
                .endStruct()
                .l();

        // Uniforms
        code.l("uniform Material material")
                .l("uniform vec3 ambientLight")
                .l("uniform vec3 cameraPos");

        if (hasDirectionalLight) {
            code.beginStruct("DirectionalLight")
                    .item("vec3", "color")
                    .item("float", "intensity")
                    .item("vec3", "direction")
                    .endStruct()
                    .l("uniform DirectionalLight directionalLight");
        }

        if (maxPointLights > 0) {
            code.beginStruct("PointLight")
                    .item("vec3", "color")
                    .item("float", "intensity")
                    .item("vec3", "position")
                    .item("float", "att_constant")
                    .item("float", "att_linear")
                    .item("float", "att_quadratic")
                    .endStruct()
                    .l("uniform PointLight pointLights[MAX_POINT_LIGHTS]")
                    .l("uniform int numPointLights");
        }

        if (maxSpotLights > 0) {
            code.beginStruct("SpotLight")
                    .item("PointLight", "pl")
                    .item("vec3", "conedir")
                    .item("float", "cutoff")
                    .endStruct()
                    .l("uniform SpotLight spotLights[MAX_SPOT_LIGHTS]")
                    .l("uniform int numSpotLights");
        }

        if (withShadows) {
            code.l("uniform sampler2D shadowMap")
                    .l("uniform float shadowBias");
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

        code.l()
                .function("vec3", "calcLightColor", new String[][]{{"vec3", "lightColor"}, {"float", "intensity"}})
                .l("return lightColor * intensity;")
                .endFunction()
                .l();


        code.beginMain()
                .l("vec3 accumLight = vec3(0.0);");  // Accumulateur de lumière

        if (hasDirectionalLight) {
            code.l("vec4 directionalShadowCoords = directionalShadowMatrix * vec4(vPos, 1.0);")
                    .l("vec3 lightDir = normalize(-directionalLight.direction);")
                    .l("vec4 directionalLightEffect = computeDirectionalLightPBR(directionalLight, vPos, vNorm, vViewDir, material, withShadows ? computeShadow(directionalShadowCoords, shadowMapSampler, shadowBias) : 1.0);")
                    .l("accumLight += directionalLightEffect.rgb;");
        }

        if (maxPointLights > 0) {
            code.beginFor("int i = 0", "i < maxPointLights", "i++")
                    .l("accumLight += computePointLightPBR(pointLights[i], vPos, vNorm, vViewDir, material).rgb;")
                    .endFor();
        }

        if (maxSpotLights > 0) {
            code.beginFor("int i = 0", "i < maxSpotLights", "i++")
                    .l("accumLight += computeSpotLightPBR(spotLights[i], vPos, vNorm).rgb;")
                    .endFor();
        }

        code.l().cmt("Calculate reflections based on PBR material properties")
                .l("vec3 viewDirection = normalize(-vPos);")
                .l("vec3 normal = normalize(vNorm);")
                .l("vec3 totalSpecular = vec3(0.0);")
                .l("vec3 totalDiffuse = vec3(0.0);");

        code.l("vec3 lightDir = normalize(lightDirection);")
                .l("float nDotL = max(dot(normal, lightDir), 0.0);")
                .l("if(nDotL > 0.0) {")
                .l("    vec3 halfVector = normalize(lightDir + viewDirection);")
                .l("    float nDotH = max(dot(normal, halfVector), 0.0);")
                .l("    float vDotH = max(dot(viewDirection, halfVector), 0.0);")
                .l("    vec3 F = schlickFresnel(vDotH);")
                .l("    float D = ggxDistribution(nDotH, material.roughness);")
                .l("    float G = geomSmith(nDotL, material.roughness) * geomSmith(max(dot(normal, viewDirection), 0.0), material.roughness);")
                .l("    vec3 specular = (D * F * G) / (4.0 * nDotL * max(dot(normal, viewDirection), 0.0) + 0.0001);")
                .l("    vec3 kD = (vec3(1.0) - F) * (1.0 - material.metallic);")
                .l("    vec3 diffuse = kD * material.baseColor / PI;")
                .l("    totalSpecular += specular * accumLight;")
                .l("    totalDiffuse += diffuse * accumLight * nDotL;")
                .l("}");

        code.l("vec3 emissive = material.emissiveColor * material.emissiveIntensity;")
                .l("vec4 finalColor = vec4(totalDiffuse + totalSpecular + emissive, material.opacity);");

        code.l("fragColor = finalColor;");

        code.endMain();

        return code;
    }


    /**
     * Create uniform for each attribute of the PBR material
     *
     * @param uniformName uniform name
     */
    public void createMaterialUniform(String uniformName, boolean textured) {
        if (textured) {
            createUniform(uniformName + ".texture_sampler");
        } else {
            createUniform(uniformName + ".color");
        }
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
        if (material.isTextured()) {
            setUniform(uniformName + ".texture_sampler", 0); // TODO : assign sampler slots more dynamically
        } else {
            setUniform(uniformName + ".color", material.getBaseColor());
        }
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
            setUniform(uniformName + ".emissiveTex", 2);
        }
        setUniform(uniformName + ".metallicFactor", material.getMetallic());
        setUniform(uniformName + ".roughnessFactor", material.getRoughness());
    }

    public static ShaderCode calcPBRLight(ShaderCode code) {
        code.function("Compute PBR-based direct lighting",
                "vec3", "calcPBRLight",
                new String[][]{{"vec3", "light_color"},
                        {"float", "light_intensity"},
                        {"vec3", "position"},
                        {"vec3", "light_dir"},
                        {"vec3", "normal"},
                        {"vec3", "view_dir"},
                        {"bool", "isDirLight"},
                        {"Material", "material"}});

        code.l().l("vec3 lightIntensity = light_color * light_intensity;")
                .l("vec3 l = vec3(0.0);")
                .l("if (isDirLight) {")
                .l("l = -light_dir;")
                .l("} else {")
                .l().l("l = light_dir - LocalPos0;")
                .l("float lightToPixelDist = length(l);")
                .l("l = normalize(l);")
                .l("lightIntensity /= (lightToPixelDist * lightToPixelDist);")
                .l("}");

        code.l().l("vec3 n = normal")
                .l("vec3 v = normalize(gCameraLocalPos - LocalPos0);")
                .l("vec3 h = normalize(v + l);");

        code.l().l("float nDotH = max(dot(n, h), 0.0);")
                .l("float vDotH = max(dot(v, h), 0.0);")
                .l("float nDotL = max(dot(n, l), 0.0);")
                .l("float nDotV = max(dot(n, v), 0.0);");

        code.l().l("vec3 F = schlickFresnel(vDotH);")
                .l("vec3 kS = F;")
                .l("vec3 kD = vec3(1.0) - kS;");

        code.l().l("vac 3 specBRDF_nom = ggxDistribution(nDotH, roughness) * F * geomSmith(nDotL, roughness) * geomSmith(nDotV, roughness);")
                .l("float specBRDF_denom = 4.0 * nDotV * nDotL + 0.0001;")
                .l("vec3 specBRDF = specBRDF_nom / specBRDF_denom;");

        code.l().l("vec3 flambert = vec3(0.0);")
                .l("if (material.isMetal == 1) {")
                .l("flambert = material.color;") //CHECKME: fix
                .l("}");

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
                .l("float d = nDotH * nDotH * (alpha2 - 1.0) + 1.0;")
                .l("float NdotH2 = NdotH * NdotH;");

        code.l().l("return alpha2 / (PI * d * d);");
        return code.endFunction();
    }

    public static ShaderCode schlickFresnel(ShaderCode code){
        code.function("Schlick's approximation for Fresnel effect",
                "vec3", "schlickFresnel",
                new String[][]{{"float", "vDotH"},
                        {"Material","material"}});

        code.l().l("vec3 f0 = vec3(0.04);") // actual F0 for the metals
                .l("if (material.isMetal) {")
                .l("f0 = material.color;") //FIXME: fix
                .l("}");

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
