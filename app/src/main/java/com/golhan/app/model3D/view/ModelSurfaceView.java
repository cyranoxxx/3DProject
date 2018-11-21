package com.golhan.app.model3D.view;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.golhan.app.model3D.activity.ModelActivity;
import com.golhan.app.model3D.controller.TouchController;


public class ModelSurfaceView extends GLSurfaceView {

	private ModelActivity parent;
	private ModelRenderer mRenderer;
	private TouchController touchHandler;

	public ModelSurfaceView(ModelActivity parent) {
		super(parent);

		this.parent = parent;

		setEGLContextClientVersion(2);

		mRenderer = new ModelRenderer(this);
		setRenderer(mRenderer);

		touchHandler = new TouchController(this, mRenderer);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return touchHandler.onTouchEvent(event);
	}

	public ModelActivity getModelActivity() {
		return parent;
	}

	public ModelRenderer getModelRenderer(){
		return mRenderer;
	}

}