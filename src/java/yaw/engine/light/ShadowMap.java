package yaw.engine.light;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import yaw.engine.SceneRenderer;
import yaw.engine.camera.Camera;
import yaw.engine.items.ItemObject;
import yaw.engine.mesh.Mesh;
import yaw.engine.shader.*;


import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;

public class ShadowMap {

    private class ShadowShaderProgram extends ShaderProgram {

        public ShadowShaderProgram() {
            super();
        }

        public void init() {}

    }

    private int width;
    private int height;

    private boolean initialized = false;

    private int framebuffer;

    private int depthMap;

    /**
     * Coverage settings for shadow map
     */
    private Vector3f center = new Vector3f();
    private float left = -10;
    private float right = 10;
    private float bottom = -10;
    private float top = 10;
    private float zNear = -10;
    private float zFar = 10;
    private float bias = 0.05f;

    private boolean autoPlace = true;
    private final float margin = 0.1f;

    private Matrix4f projection = new Matrix4f();
    private Matrix4f view = new Matrix4f();

    private ShadowShaderProgram mShaderProgram;

    public ShadowMap(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public ShadowMap() {
        this(2048, 2048);
    }

    public void init(ShaderManager shaderManager) throws Exception {

        mShaderProgram = new ShadowShaderProgram();
        mShaderProgram.createVertexShader(shadowVertShader.SHADER_STRING);
        mShaderProgram.createFragmentShader(shadowFragShader.SHADER_STRING);

        /* Binds the code and checks that everything has been done correctly. */
        mShaderProgram.link();

        mShaderProgram.createUniform("projectionMatrix");
        mShaderProgram.createUniform("viewMatrix");
        mShaderProgram.createUniform("modelMatrix");

        depthMap = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthMap);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (FloatBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        //float borderColor[] = { 0,0,0,0 };
        float[] borderColor = { 1,1,1,1 };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthMap, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

    }

    public void render(SceneRenderer pSceneRenderer, DirectionalLight light, Camera pCamera, ShaderManager shaderManager) {

        if(!initialized) {
            try {
                init(shaderManager);
                initialized = true;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        glViewport(0, 0, width, height);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glClearDepth(1);
        glClear(GL_DEPTH_BUFFER_BIT);

        mShaderProgram.bind();

        glDisable(GL_CULL_FACE); // if geometry isn't always enclosed
        glCullFace(GL_FRONT);

        if(autoPlace) autoPlace(pSceneRenderer, light);

        createView(light);
        createProjection();

        /* Set the camera to render. */
        mShaderProgram.setUniform("projectionMatrix", projection);
        mShaderProgram.setUniform("viewMatrix", view);

        Map<Mesh, List<ItemObject>> meshMap = pSceneRenderer.getMeshMap();

        for (Mesh lMesh : meshMap.keySet()) {
            List<ItemObject> lItems = meshMap.get(lMesh);
            List<ItemObject> castingItems = new ArrayList<>();

            for(ItemObject item : lItems) {
                if(!item.doesCastShadows()) continue;
                castingItems.add(item);
            }
            if(castingItems.isEmpty()) continue;

            try {
                // XXX : this will fail
                ShaderProgramADS shaderProgram = (ShaderProgramADS) shaderManager.fetch("ADS");
                lMesh.renderSetup(pCamera, shaderProgram);
                for(ItemObject item : castingItems) {
                    lMesh.renderItem(item, shaderProgram);
                }
                lMesh.renderCleanup(shaderProgram);
            } catch (Exception e) {
                System.out.println("Erreur ShadowMap");
            }
        }


        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glEnable(GL_CULL_FACE);

    }

    public void bind(ShaderProgram shaderProgram) {
        if(!initialized) return;

        shaderProgram.setUniform("directionalShadowMatrix", new Matrix4f(projection).mul(view));

        shaderProgram.setUniform("shadowMapSampler", 1);
        shaderProgram.setUniform("shadowBias", bias);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, depthMap);

    }

    public void autoPlace(SceneRenderer pSceneRenderer, DirectionalLight light) {

        left = Float.MAX_VALUE;
        right = -Float.MAX_VALUE;
        bottom = Float.MAX_VALUE;
        top = -Float.MAX_VALUE;
        zNear = Float.MAX_VALUE;
        zFar = -Float.MAX_VALUE;

        center = new Vector3f();

        Matrix4f mat = createView(light);

        for(ItemObject io : pSceneRenderer.getItemsList()) {
            float[] verts = io.getMesh().getGeometry().getVertices();
            for(int i = 0; i<verts.length; i+=3) {
                Vector4f v = new Vector4f(verts[i], verts[i+1], verts[i+2], 1);
                Vector4f world_space = io.getModelMatrix().transform(v);
                Vector4f light_space = mat.transform(world_space);

                if(io.doesCastShadows()) {
                    left = Math.min(left, light_space.x);
                    right = Math.max(right, light_space.x);
                    bottom = Math.min(bottom, light_space.y);
                    top = Math.max(top, light_space.y);
                }
                zNear = Math.min(zNear, -light_space.z);
                zFar = Math.max(zFar, -light_space.z);
            }
        }

        /*
        var translation = new Vector3f(left + right, bottom + top, zNear + zFar).div(2);
        center.add(translation);
        left -= translation.x;
        right -= translation.x;
        bottom -= translation.y;
        top -= translation.y;
        zNear -= translation.z;
        zFar -= translation.z;

         */

        left += -margin;
        right += margin;
        bottom += -margin;
        top += margin;
        zNear += -margin;
        zFar += margin;



    }

    Matrix4f createView(DirectionalLight light) {
        return view.setLookAt(center, new Vector3f(light.mDirection).add(center), new Vector3f(0,1,0));
        //view = new Matrix4f().identity().lookAlong(light.mDirection, new Vector3f(0,1,0));
    }

    Matrix4f createProjection() {
        return projection.setOrtho(left, right, bottom, top, zNear, zFar);
    }

    public void cleanUp() {

        glDeleteProgram(mShaderProgram.getId());

        glDeleteFramebuffers(framebuffer);

        glDeleteTextures(depthMap);

    }

    public Vector3f getCenter() {
        return center;
    }

    public void setCenter(Vector3f center) {
        this.center = center;
        autoPlace = false;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
        autoPlace = false;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
        autoPlace = false;
    }

    public float getBottom() {
        return bottom;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
        autoPlace = false;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
        autoPlace = false;
    }

    public float getzNear() {
        return zNear;
    }

    public void setzNear(float zNear) {
        this.zNear = zNear;
        autoPlace = false;
    }

    public float getzFar() {
        return zFar;
    }

    public void setzFar(float zFar) {
        this.zFar = zFar;
        autoPlace = false;
    }

    public float getBias() {
        return bias;
    }

    public void setBias(float bias) {
        this.bias = bias;
    }

    public boolean isAutoPlace() {
        return autoPlace;
    }

    public void setAutoPlace(boolean autoPlace) {
        this.autoPlace = autoPlace;
    }
}
