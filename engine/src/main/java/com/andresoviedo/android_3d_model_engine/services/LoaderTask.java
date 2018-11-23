package com.andresoviedo.android_3d_model_engine.services;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;

import com.andresoviedo.android_3d_model_engine.model.Object3DData;

import java.util.List;


public abstract class LoaderTask extends AsyncTask<Void, Integer, List<Object3DData>> {


	protected final Uri uri;

	private final Callback callback;

	private final ProgressDialog dialog;


	public LoaderTask(Activity parent, Uri uri, Callback callback) {
		this.uri = uri;
		// this.dialog = ProgressDialog.show(this.parent, "Please wait ...", "Loading model data...", true);
		// this.dialog.setTitle(modelId);
		this.dialog = new ProgressDialog(parent);
		this.callback = callback; }


	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.dialog.setMessage("Loading...");
		this.dialog.setCancelable(false);
		this.dialog.show();
	}



	@Override
	protected List<Object3DData> doInBackground(Void... params) {
		try {
		    callback.onStart();
			List<Object3DData> data = build();
			build(data);
            callback.onLoadComplete(data);
			return  data;
		} catch (Exception ex) {
            callback.onLoadError(ex);
			return null;
		}
	}

	protected abstract List<Object3DData> build() throws Exception;

	protected abstract void build(List<Object3DData> data) throws Exception;

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		switch (values[0]) {
			case 0:
				this.dialog.setMessage("Model analiz ediliyor...");
				break;
			case 1:
				this.dialog.setMessage("Hafızadan yer ayrılıyor...");
				break;
			case 2:
				this.dialog.setMessage("Datalar yükleniyor...");
				break;
			case 3:
				this.dialog.setMessage("Cisim ölçekleniyor...");
				break;
			case 4:
				this.dialog.setMessage("3B model oluşturuluyor...");
				break;
			case 5:
				// Toast.makeText(parent, modelId + " Build!", Toast.LENGTH_LONG).show();
				break;
		}
	}

	@Override
	protected void onPostExecute(List<Object3DData> data) {
		super.onPostExecute(data);
		if (dialog.isShowing()) {
			dialog.dismiss();
		}
	}


    public interface Callback {

        void onStart();

        void onLoadError(Exception ex);

        void onLoadComplete(List<Object3DData> data);
    }
}