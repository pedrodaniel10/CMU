package pt.ulisboa.tecnico.cmu.tasks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.gson.Gson;
import dmax.dialog.SpotsDialog;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import pt.ulisboa.tecnico.cmu.R;
import pt.ulisboa.tecnico.cmu.activities.MainActivity;
import pt.ulisboa.tecnico.cmu.adapters.AlbumMenuAdapter;
import pt.ulisboa.tecnico.cmu.dataobjects.Album;
import pt.ulisboa.tecnico.cmu.exceptions.UnauthorizedException;
import pt.ulisboa.tecnico.cmu.utils.GoogleDriveUtils;
import pt.ulisboa.tecnico.cmu.utils.RequestsUtils;
import pt.ulisboa.tecnico.cmu.utils.SharedPropertiesUtils;

public class GetAlbumsTask extends AsyncTask<Void, List<Album>, Boolean> {

    private final Context context;
    private static final String TAG = "GetAlbumsTask";

    private final AlbumMenuAdapter adapter;

    // UI components
    private AlertDialog progress;
    private List<Album> albums;


    public GetAlbumsTask(Context context, AlbumMenuAdapter adapter) {
        this.context = context;
        this.adapter = adapter;
    }

    @Override
    protected void onPreExecute() {
        progress = new SpotsDialog.Builder().setContext(context)
            .setMessage("Retrieving albums.")
            .setCancelable(false)
            .setTheme(R.style.ProgressBar)
            .build();
        GoogleDriveUtils.connectDriveService(context);
        showProgress(true);
    }

    @Override
    protected synchronized Boolean doInBackground(Void... noParams) {
        String albumsCache;
        try {
            if (MainActivity.choseWifiDirect) {
                albumsCache = SharedPropertiesUtils.getAlbumsWifi(context, RequestsUtils.getUserId());
            } else {
                albumsCache = SharedPropertiesUtils.getAlbums(context, RequestsUtils.getUserId());
            }

            Album[] albumsFromCache = new Gson().fromJson(albumsCache, Album[].class);
            publishProgress(Arrays.asList(albumsFromCache));
            showProgress(false);

            String albumsJson = "[]";
            if (MainActivity.choseWifiDirect) {
                albumsJson = RequestsUtils.getAlbumsWifi(context);
            } else {
                albumsJson = RequestsUtils.getAlbums(context);
            }

            Album[] albumsFromJson = new Gson().fromJson(albumsJson, Album[].class);

            this.albums = Arrays.asList(albumsFromJson);
            return true;
        } catch (UnauthorizedException e) {
            return false;
        }
    }

    @Override
    protected void onProgressUpdate(List<Album>... values) {
        if (values.length == 1) {
            adapter.addAlbums(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            if (MainActivity.choseWifiDirect) {
                createMissingCatalogs(this.albums);
            }
            adapter.addAlbums(this.albums);
        } else {
            Intent launchNextActivity;
            launchNextActivity = new Intent(context, MainActivity.class);
            launchNextActivity.putExtra("startLogin", true);
            launchNextActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchNextActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            launchNextActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(launchNextActivity);
        }
        showProgress(false);
    }

    private void createMissingCatalogs(List<Album> albums) {

        File folder = WifiDirectConnectionManager.getCatalogFolder();

        for (Album album : albums) {
            WifiDirectConnectionManager.createCatalog(album, folder, context);
        }
    }

    private void showProgress(boolean show) {
        if (show) {
            progress.show();
        } else {
            progress.dismiss();
        }
    }
}