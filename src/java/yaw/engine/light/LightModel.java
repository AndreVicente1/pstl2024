package yaw.engine.light;

import yaw.engine.SceneRenderer;
import yaw.engine.camera.Camera;
import yaw.engine.shader.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class LightModel {
    private AmbientLight ambientLight;

    public final boolean hasDirectionalLight;
    private DirectionalLight directionalLight;

    public final int maxPointLights;
    private int nbPointLights;
    private PointLight[] pointLights;

    public final int maxSpotLights;
    private SpotLight[] spotLights;
    private int nbSpotLights;

    /**
     * Constructor without parameters, it used to create the maximum of point light and spot light.
     */
    public LightModel(boolean hasDirectionalLight, int maxPointLights, int maxSpotLights) {
        ambientLight = new AmbientLight();
        this.hasDirectionalLight = hasDirectionalLight;
        directionalLight = null;

        this.maxPointLights = maxPointLights;
        this.maxSpotLights = maxSpotLights;

        pointLights = new PointLight[maxPointLights];
        nbPointLights = 0;

        this.spotLights = new SpotLight[maxSpotLights];
        nbSpotLights = 0;
    }

    public LightModel() {
        this(true, 5, 5);
    }

    public void renderShadowMap(SceneRenderer pSceneRenderer, Camera pCamera, ShaderManager shaderManager) {
        directionalLight.renderShadowMap(pSceneRenderer, pCamera, shaderManager);
    }

    /**
     * Set to the render the different light
     *
     * @param viewMatrix viewMatrix
     */
    public void setupShader(Matrix4f viewMatrix, ShaderProgram shaderProgram) {
        shaderProgram.bind();
        if (shaderProgram instanceof ShaderProgramADS) {
            shaderProgram.setUniform("ambientLight", ambientLight);
        }
        // Process Point Lights
        for (int i = 0; i < nbPointLights; i++) {
            // Get a copy of the point light object and transform its position to view coordinates
            PointLight currPointLight = new PointLight(pointLights[i]);
            Vector3f lightPos = new Vector3f(currPointLight.getPosition());
            Vector4f aux = new Vector4f(lightPos, 1);
            aux.mul(viewMatrix);
            lightPos.x = aux.x;
            lightPos.y = aux.y;
            lightPos.z = aux.z;

            //Bug correction
            currPointLight.setPosition(lightPos);

            shaderProgram.setUniform("pointLights", currPointLight, i);
        }

        // Process Spot Ligths
        for (int i = 0; i < nbSpotLights; i++) {
            // Get a copy of the spot light object and transform its position and cone direction to view coordinates
            SpotLight currSpotLight = new SpotLight(spotLights[i]);
            Vector4f dir = new Vector4f(currSpotLight.mConedir, 0);
            dir.mul(viewMatrix);
            currSpotLight.mConedir = new Vector3f(dir.x, dir.y, dir.z);
            currSpotLight.mCutoffAngle = (float) Math.cos(Math.toRadians(currSpotLight.mCutoffAngle));
            Vector3f lightPos = new Vector3f(currSpotLight.getPosition());
            Vector4f aux = new Vector4f(lightPos, 1);
            aux.mul(viewMatrix);
            lightPos.x = aux.x;
            lightPos.y = aux.y;
            lightPos.z = aux.z;

            //Bug Correction
            currSpotLight.setPosition(lightPos);

            shaderProgram.setUniform("spotLights", currSpotLight, i);
        }

        // Get a copy of the directional light object and transform its position to view coordinates
        if (hasDirectionalLight && directionalLight != null) {
            DirectionalLight currDirLight = new DirectionalLight(directionalLight);
            Vector4f dir = new Vector4f(currDirLight.mDirection, 0);
            dir.mul(viewMatrix);
            currDirLight.mDirection = new Vector3f(dir.x, dir.y, dir.z);
            shaderProgram.setUniform("directionalLight", currDirLight);
            //directionalLight.bindShadowMap(shaderProgram);
        }

    }

    public AmbientLight getAmbientLight() {
        return ambientLight;
    }

    public void setAmbientLight(AmbientLight ambientLight) {
        this.ambientLight = ambientLight;
    }

    public DirectionalLight getDirectionalLight() {
        return directionalLight;
    }

    public void setDirectionalLight(DirectionalLight directionalLight) {
        if (!hasDirectionalLight)  {
            throw new Error("The light model as constructed does not accept a directional light");
        }
        this.directionalLight = directionalLight;
    }

    public int addPointLight(PointLight pointLight) {
        if (nbSpotLights >= maxSpotLights) {
            throw new Error("Cannot add point light: maximum number reached");
        }
        pointLights[nbPointLights] = pointLight;
        return nbPointLights - 1;
    }

    public int addSpotLight(SpotLight spotLight) {
        if (nbSpotLights >= maxSpotLights) {
            throw new Error("Cannot add spot light: maximum number reached");
        }
        spotLights[nbSpotLights] = spotLight;
        return nbSpotLights - 1;
    }
}
