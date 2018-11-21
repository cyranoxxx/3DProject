package com.golhan.app.model3D.controller;

import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.andresoviedo.android_3d_model_engine.collision.CollisionDetection;
import com.andresoviedo.android_3d_model_engine.model.Camera;
import com.andresoviedo.android_3d_model_engine.model.Object3DData;
import com.andresoviedo.android_3d_model_engine.services.LoaderTask;
import com.andresoviedo.android_3d_model_engine.services.Object3DBuilder;
import com.andresoviedo.android_3d_model_engine.services.collada.ColladaLoaderTask;
import com.andresoviedo.android_3d_model_engine.services.stl.STLLoaderTask;
import com.andresoviedo.android_3d_model_engine.services.wavefront.WavefrontLoaderTask;
import com.golhan.app.model3D.activity.ModelActivity;
import com.golhan.app.model3D.view.ModelRenderer;
import com.andresoviedo.util.android.ContentUtils;
import com.andresoviedo.util.io.IOUtils;
import com.andresoviedo.android_3d_model_engine.animation.Animator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SceneLoader implements LoaderTask.Callback {

    private static float[] DEFAULT_COLOR = {1.0f, 1.0f, 0, 1.0f};

    protected final ModelActivity parent;

    private List<Object3DData> objects = new ArrayList<Object3DData>();

    private Camera camera;

    private boolean drawWireframe = false;

    private boolean drawingPoints = false;

    private boolean drawBoundingBox = false;

    private boolean drawNormals = false;

    private boolean drawTextures = true;

    private boolean rotatingLight = true;

    private boolean drawLighting = true;

    private boolean animateModel = true;

    private boolean drawSkeleton = false;

    private boolean isCollision = false;

    private boolean isAnaglyph = false;

    private Object3DData selectedObject = null;

    private final float[] lightPosition = new float[]{0, 0, 6, 1};

    private final Object3DData lightPoint = Object3DBuilder.buildPoint(lightPosition).setId("light");

    private Animator animator = new Animator();

    private boolean userHasInteracted;

    private long startTime;

    public SceneLoader(ModelActivity main) {
        this.parent = main;
    }

    public void init() {


        camera = new Camera();

        if (parent.getParamUri() == null){
            return;
        }

        startTime = SystemClock.uptimeMillis();
        Uri uri = parent.getParamUri();
        Log.i("Object3DBuilder", "Model Yükleniyor " + uri + "...");
        if (uri.toString().toLowerCase().endsWith(".obj") || parent.getParamType() == 0) {
            new WavefrontLoaderTask(parent, uri, this).execute();
        } else if (uri.toString().toLowerCase().endsWith(".stl") || parent.getParamType() == 1) {
            Log.i("Object3DBuilder", "STL cismi yükleniyor: "+uri);
            new STLLoaderTask(parent, uri, this).execute();
        } else if (uri.toString().toLowerCase().endsWith(".dae") || parent.getParamType() == 2) {
            Log.i("Object3DBuilder", "Collada cismi yükleniyor: "+uri);
            new ColladaLoaderTask(parent, uri, this).execute();
        }
    }

    public Camera getCamera() {
        return camera;
    }

    private void makeToastText(final String text, final int toastDuration) {
        parent.runOnUiThread(() -> Toast.makeText(parent.getApplicationContext(), text, toastDuration).show());
    }

    public Object3DData getLightBulb() {
        return lightPoint;
    }

    public float[] getLightPosition() {
        return lightPosition;
    }


    public void onDrawFrame() {

        animateLight();


        camera.animate();

        if (!userHasInteracted) {
            animateCamera();
        }

        if (objects.isEmpty()) return;

        if (animateModel) {
            for (int i=0; i<objects.size(); i++) {
                Object3DData obj = objects.get(i);
                animator.update(obj);
            }
        }
    }

    private void animateLight() {
        if (!rotatingLight) return;

        long time = SystemClock.uptimeMillis() % 5000L;
        float angleInDegrees = (360.0f / 5000.0f) * ((int) time);
        lightPoint.setRotationY(angleInDegrees);
    }

    private void animateCamera(){
        camera.translateCamera(0.0025f, 0f);
    }

    synchronized void addObject(Object3DData obj) {
        List<Object3DData> newList = new ArrayList<Object3DData>(objects);
        newList.add(obj);
        this.objects = newList;
        requestRender();
    }

    private void requestRender() {
        if (parent.getGLView() != null) {
            parent.getGLView().requestRender();
        }
    }

    public synchronized List<Object3DData> getObjects() {
        return objects;
    }

    public void toggleWireframe() {
        if (this.drawWireframe && !this.drawingPoints) {
            this.drawWireframe = false;
            this.drawingPoints = true;
            makeToastText("Noktasal", Toast.LENGTH_SHORT);
        } else if (this.drawingPoints) {
            this.drawingPoints = false;
            makeToastText("Normal", Toast.LENGTH_SHORT);
        } else {
            makeToastText("Çizgisel", Toast.LENGTH_SHORT);
            this.drawWireframe = true;
        }
        requestRender();
    }

    public boolean isDrawWireframe() {
        return this.drawWireframe;
    }

    public boolean isDrawPoints() {
        return this.drawingPoints;
    }

    public void toggleBoundingBox() {
        this.drawBoundingBox = !drawBoundingBox;
        requestRender();
    }

    public boolean isDrawBoundingBox() {
        return drawBoundingBox;
    }

    public boolean isDrawNormals() {
        return drawNormals;
    }

    public void toggleTextures() {
        this.drawTextures = !drawTextures;
        makeToastText("Dokular "+this.drawTextures, Toast.LENGTH_SHORT);
    }

    public void toggleLighting() {
        if (this.drawLighting && this.rotatingLight) {
            this.rotatingLight = false;
            makeToastText("Işık durduruldu.", Toast.LENGTH_SHORT);
        } else if (this.drawLighting && !this.rotatingLight) {
            this.drawLighting = false;
            makeToastText("Işık Kapalı", Toast.LENGTH_SHORT);
        } else {
            this.drawLighting = true;
            this.rotatingLight = true;
            makeToastText("Işık Açık", Toast.LENGTH_SHORT);
        }
        requestRender();
    }

    public void toggleAnimation() {
        if (animateModel && !drawSkeleton){
            this.drawSkeleton = true;
            makeToastText("İskelet", Toast.LENGTH_SHORT);
        } else if (animateModel){
            this.drawSkeleton = false;
            this.animateModel = false;
            makeToastText("Animasyon Kapatıldı.", Toast.LENGTH_SHORT);
        } else {
            animateModel = true;
            makeToastText("Animasyon Açıldı.", Toast.LENGTH_SHORT);
        }
    }

    public boolean isDrawAnimation() {
        return animateModel;
    }

    public void toggleCollision() {
        this.isCollision = !isCollision;
        makeToastText("Collisions: "+isCollision, Toast.LENGTH_SHORT);
    }

    public boolean isDrawTextures() {
        return drawTextures;
    }

    public boolean isDrawLighting() {
        return drawLighting;
    }

    public boolean isDrawSkeleton() {
        return drawSkeleton;
    }

    public boolean isCollision() {
        return isCollision;
    }

    public boolean isAnaglyph() {
        return isAnaglyph;
    }

    @Override
    public void onStart(){
        ContentUtils.setThreadActivity(parent);
    }

    @Override
    public void onLoadComplete(List<Object3DData> datas) {

        for (Object3DData data : datas) {
            if (data.getTextureData() == null && data.getTextureFile() != null) {
                Log.i("Yükleyici","Doku yükleniyor... "+data.getTextureFile());
                try (InputStream stream = ContentUtils.getInputStream(data.getTextureFile())){
                    if (stream != null) {
                        data.setTextureData(IOUtils.read(stream));
                    }
                } catch (IOException ex) {
                    data.addError("Doku yüklenirken problem oluştu... " + data.getTextureFile());
                }
            }
        }
        List<String> allErrors = new ArrayList<>();
        for (Object3DData data : datas) {
            addObject(data);
            allErrors.addAll(data.getErrors());
        }
        if (!allErrors.isEmpty()){
            makeToastText(allErrors.toString(), Toast.LENGTH_LONG);
        }
        final String elapsed = (SystemClock.uptimeMillis() - startTime) / 1000 + " secs";
        makeToastText("Tamamlandı (" + elapsed + ")", Toast.LENGTH_LONG);
        ContentUtils.setThreadActivity(null);
    }

    @Override
    public void onLoadError(Exception ex) {
        Log.e("SceneLoader", ex.getMessage(), ex);
        makeToastText("Model yüklenirken hata oluştu: " + ex.getMessage(), Toast.LENGTH_LONG);
        ContentUtils.setThreadActivity(null);
    }

    public Object3DData getSelectedObject() {
        return selectedObject;
    }

    private void setSelectedObject(Object3DData selectedObject) {
        this.selectedObject = selectedObject;
    }

    public void loadTexture(Object3DData obj, Uri uri) throws IOException {
        if (obj == null && objects.size() != 1) {
            makeToastText("Tanımlanamıyor...", Toast.LENGTH_SHORT);
            return;
        }
        obj = obj != null ? obj : objects.get(0);
        obj.setTextureData(IOUtils.read(ContentUtils.getInputStream(uri)));
        this.drawTextures = true;
    }

    public void processTouch(float x, float y) {
        ModelRenderer mr = parent.getGLView().getModelRenderer();
        Object3DData objectToSelect = CollisionDetection.getBoxIntersection(getObjects(), mr.getWidth(), mr.getHeight
                (), mr.getModelViewMatrix(), mr.getModelProjectionMatrix(), x, y);
        if (objectToSelect != null) {
            if (getSelectedObject() == objectToSelect) {
                Log.i("EkranYukleyici", "Cisim seçilmedi..." + objectToSelect.getId());
                setSelectedObject(null);
            } else {
                Log.i("EkranYukleyici", "Cisim seçildi... " + objectToSelect.getId());
                setSelectedObject(objectToSelect);
            }
            if (isCollision()) {
                Log.d("EkranYukleyici", "Taranıyor...");

                float[] point = CollisionDetection.getTriangleIntersection(getObjects(), mr.getWidth(), mr.getHeight
                        (), mr.getModelViewMatrix(), mr.getModelProjectionMatrix(), x, y);
                if (point != null) {
                    Log.i("EkranYukleyici", "Kesişme noktaları çiziliyor: " + Arrays.toString(point));
                    addObject(Object3DBuilder.buildPoint(point).setColor(new float[]{1.0f, 0f, 0f, 1f}));
                }
            }
        }
    }

    public void processMove(float dx1, float dy1) {
        userHasInteracted = true;
    }


}
