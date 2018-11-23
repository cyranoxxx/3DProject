package com.andresoviedo.android_3d_model_engine.services.collada.entities;

import android.opengl.Matrix;
import android.util.Log;
import com.andresoviedo.android_3d_model_engine.animation.Animator;

import java.util.ArrayList;
import java.util.List;



public class Joint {

    private final int index;// ID
    private final String name;
    private final float[] bindLocalTransform;
    private final List<Joint> children = new ArrayList<>();

    private float[] inverseBindTransform;

    private final float[] animatedTransform = new float[16];


    public Joint(int index, String name, float[] bindLocalTransform, float[] inverseBindTransform) {
        this.index = index;
        this.name = name;
        this.bindLocalTransform = bindLocalTransform;
        this.inverseBindTransform = inverseBindTransform;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public List<Joint> getChildren() {
        return children;
    }

    public float[] getBindLocalTransform() {
        return bindLocalTransform;
    }


    public void addChild(Joint child) {
        this.children.add(child);
    }


    public float[] getAnimatedTransform() {
        return animatedTransform;
    }


    public float[] getInverseBindTransform() {
        return inverseBindTransform;
    }

    public void calcInverseBindTransform(float[] parentBindTransform, boolean override) {

        float[] bindTransform = new float[16];
        Matrix.multiplyMM(bindTransform, 0, parentBindTransform, 0, bindLocalTransform, 0);
        if (index >= 0 && (override || this.inverseBindTransform == null)) {
            // when model has inverse bind transforms available, don't overwrite it
            // this way we calculate only the joints with no animations which has no inverse bind transform available
            this.inverseBindTransform = new float[16];
            if (!Matrix.invertM(inverseBindTransform, 0, bindTransform, 0)) {
                Log.w("Joint", "Couldn't calculate inverse matrix for " + name);
            }
        }
        for (Joint child : children) {
            child.calcInverseBindTransform(bindTransform, override);
        }
    }

    @Override
    public Joint clone() {
        final Joint ret = new Joint(this.index, this.name, this.bindLocalTransform.clone(), this.inverseBindTransform !=
                null? this.inverseBindTransform.clone() : null);
        for (final Joint child : this.children){
            ret.addChild(child.clone());
        }
        return ret;
    }
}
