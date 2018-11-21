package com.golhan.app.model3D.view;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.andresoviedo.util.android.GLUtil;
import com.andresoviedo.android_3d_model_engine.animation.Animator;
import com.andresoviedo.android_3d_model_engine.drawer.DrawerFactory;
import com.andresoviedo.android_3d_model_engine.model.Camera;
import com.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import com.andresoviedo.android_3d_model_engine.model.Object3D;
import com.andresoviedo.android_3d_model_engine.services.Object3DBuilder;
import com.andresoviedo.android_3d_model_engine.model.Object3DData;
import com.andresoviedo.android_3d_model_engine.drawer.Object3DImpl;
import com.golhan.app.model3D.controller.SceneLoader;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ModelRenderer implements GLSurfaceView.Renderer {

	private final static String TAG = ModelRenderer.class.getName();

	private ModelSurfaceView main;

	private int width;

	private int height;

	private static final float near = 1f;

	private static final float far = 100f;

	private DrawerFactory drawer;
	private Map<Object3DData, Object3DData> wireframes = new HashMap<Object3DData, Object3DData>();

	private Map<byte[], Integer> textures = new HashMap<byte[], Integer>();

	private Map<Object3DData, Object3DData> boundingBoxes = new HashMap<Object3DData, Object3DData>();

	private Map<Object3DData, Object3DData> normals = new HashMap<Object3DData, Object3DData>();
	private Map<Object3DData, Object3DData> skeleton = new HashMap<>();


	private final float[] modelProjectionMatrix = new float[16];
	private final float[] modelViewMatrix = new float[16];
	private final float[] mvpMatrix = new float[16];


	private final float[] lightPosInEyeSpace = new float[4];

	private boolean infoLogged = false;

	private Animator animator = new Animator();

	public ModelRenderer(ModelSurfaceView modelSurfaceView) {
		this.main = modelSurfaceView;
	}

	public float getNear() {
		return near;
	}

	public float getFar() {
		return far;
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		float[] backgroundColor = main.getModelActivity().getBackgroundColor();
		GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		drawer = new DrawerFactory();
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		this.width = width;
		this.height = height;

		GLES20.glViewport(0, 0, width, height);

		SceneLoader scene = main.getModelActivity().getScene();
		Camera camera = scene.getCamera();
		Matrix.setLookAtM(modelViewMatrix, 0, camera.xPos, camera.yPos, camera.zPos, camera.xView, camera.yView,
				camera.zView, camera.xUp, camera.yUp, camera.zUp);

		float ratio = (float) width / height;
		Log.d(TAG, "projection: [" + -ratio + "," + ratio + ",-1,1]-near/far[1,10]");
		Matrix.frustumM(modelProjectionMatrix, 0, -ratio, ratio, -1, 1, getNear(), getFar());

		Matrix.multiplyMM(mvpMatrix, 0, modelProjectionMatrix, 0, modelViewMatrix, 0);
	}

	@Override
	public void onDrawFrame(GL10 unused) {

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		SceneLoader scene = main.getModelActivity().getScene();
		if (scene == null) {
			return;
		}

        scene.onDrawFrame();

		Camera camera = scene.getCamera();
		if (camera.hasChanged()) {
			Matrix.setLookAtM(modelViewMatrix, 0, camera.xPos, camera.yPos, camera.zPos, camera.xView, camera.yView,
					camera.zView, camera.xUp, camera.yUp, camera.zUp);
			Matrix.multiplyMM(mvpMatrix, 0, modelProjectionMatrix, 0, modelViewMatrix, 0);
			camera.setChanged(false);
		}

		if (scene.isDrawLighting()) {

			Object3DImpl lightBulbDrawer = (Object3DImpl) drawer.getPointDrawer();

			float[] lightModelViewMatrix = lightBulbDrawer.getMvMatrix(lightBulbDrawer.getMMatrix(scene.getLightBulb()),modelViewMatrix);

			Matrix.multiplyMV(lightPosInEyeSpace, 0, lightModelViewMatrix, 0, scene.getLightPosition(), 0);

			lightBulbDrawer.draw(scene.getLightBulb(), modelProjectionMatrix, modelViewMatrix, -1, lightPosInEyeSpace);
		}

		List<Object3DData> objects = scene.getObjects();
		for (int i=0; i<objects.size(); i++) {
			Object3DData objData = null;
			try {
				objData = objects.get(i);
				boolean changed = objData.isChanged();

				Object3D drawerObject = drawer.getDrawer(objData, scene.isDrawTextures(), scene.isDrawLighting(),
                        scene.isDrawAnimation());

				if (!infoLogged) {
					Log.i("ModelRenderer","Çiziliyor "+drawerObject.getClass());
					infoLogged = true;
				}

				Integer textureId = textures.get(objData.getTextureData());
				if (textureId == null && objData.getTextureData() != null) {
					Log.i("ModelRenderer","GL dokusu yükleniyor...");
					ByteArrayInputStream textureIs = new ByteArrayInputStream(objData.getTextureData());
					textureId = GLUtil.loadTexture(textureIs);
					textureIs.close();
					textures.put(objData.getTextureData(), textureId);
				}

				if (objData.getDrawMode() == GLES20.GL_POINTS){
					Object3DImpl lightBulbDrawer = (Object3DImpl) drawer.getPointDrawer();
					lightBulbDrawer.draw(objData,modelProjectionMatrix, modelViewMatrix, GLES20.GL_POINTS,lightPosInEyeSpace);
				} else if (scene.isAnaglyph()){

				} else if (scene.isDrawWireframe() && objData.getDrawMode() != GLES20.GL_POINTS
						&& objData.getDrawMode() != GLES20.GL_LINES && objData.getDrawMode() != GLES20.GL_LINE_STRIP
						&& objData.getDrawMode() != GLES20.GL_LINE_LOOP) {
					try{
						Object3DData wireframe = wireframes.get(objData);
						if (wireframe == null || changed) {
							Log.i("ModelRenderer","wireframe modu etkinleştirildi...");
							wireframe = Object3DBuilder.buildWireframe(objData);
							wireframes.put(objData, wireframe);
						}
						drawerObject.draw(wireframe,modelProjectionMatrix,modelViewMatrix,wireframe.getDrawMode(),
								wireframe.getDrawSize(),textureId != null? textureId:-1, lightPosInEyeSpace);
					}catch(Error e){
						Log.e("ModelRenderer",e.getMessage(),e);
					}
				} else if (scene.isDrawPoints() || objData.getFaces() == null || !objData.getFaces().loaded()){
					drawerObject.draw(objData, modelProjectionMatrix, modelViewMatrix
							,GLES20.GL_POINTS, objData.getDrawSize(),
							textureId != null ? textureId : -1, lightPosInEyeSpace);
				} else if (scene.isDrawSkeleton() && objData instanceof AnimatedModel && ((AnimatedModel) objData)
						.getAnimation() != null){
					Object3DData skeleton = this.skeleton.get(objData);
					if (skeleton == null){
						skeleton = Object3DBuilder.buildSkeleton((AnimatedModel) objData);
						this.skeleton.put(objData, skeleton);
					}
					animator.update(skeleton);
					drawerObject = drawer.getDrawer(skeleton, false, scene.isDrawLighting(), scene
                            .isDrawAnimation());
					drawerObject.draw(skeleton, modelProjectionMatrix, modelViewMatrix,-1, lightPosInEyeSpace);
				} else {
					drawerObject.draw(objData, modelProjectionMatrix, modelViewMatrix,
							textureId != null ? textureId : -1, lightPosInEyeSpace);
				}

				if (scene.isDrawBoundingBox() || scene.getSelectedObject() == objData) {
					Object3DData boundingBoxData = boundingBoxes.get(objData);
					if (boundingBoxData == null || changed) {
						boundingBoxData = Object3DBuilder.buildBoundingBox(objData);
						boundingBoxes.put(objData, boundingBoxData);
					}
					Object3D boundingBoxDrawer = drawer.getBoundingBoxDrawer();
					boundingBoxDrawer.draw(boundingBoxData, modelProjectionMatrix, modelViewMatrix, -1, null);
				}

				if (scene.isDrawNormals()) {
					Object3DData normalData = normals.get(objData);
					if (normalData == null || changed) {
						normalData = Object3DBuilder.buildFaceNormals(objData);
						if (normalData != null) {
							normals.put(objData, normalData);
						}
					}
					if (normalData != null) {
						Object3D normalsDrawer = drawer.getFaceNormalsDrawer();
						normalsDrawer.draw(normalData, modelProjectionMatrix, modelViewMatrix, -1, null);
					}
				}
			} catch (Exception ex) {
				Log.e("ModelRenderer","Renderlanan objede problem var... '"+objData.getId()+"':"+ex.getMessage(),ex);
			}
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public float[] getModelProjectionMatrix() {
		return modelProjectionMatrix;
	}

	public float[] getModelViewMatrix() {
		return modelViewMatrix;
	}
}