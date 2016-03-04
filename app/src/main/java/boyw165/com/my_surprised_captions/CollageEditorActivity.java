package boyw165.com.my_surprised_captions;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import boyw165.com.my_surprised_captions.tool.ImageUtil;
import boyw165.com.my_surprised_captions.view.CollageMultiTouchListener;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class CollageEditorActivity extends AppCompatActivity {

    private EmotionServiceClient mClient;

    private CompositeSubscription mSubscription;

    private AlertDialog mAlert;
    private ProgressDialog mProgress;
    private Bitmap mScrap;
    private FrameLayout mCanvas;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_collage_editor);

        // Init action bar.
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Init canvas.
        mCanvas = (FrameLayout) findViewById(R.id.my_canvas);

        // Init client.
        if (mClient == null) {
            mClient = new EmotionServiceRestClient(getString(R.string.subscription_key));
        }

        // Init dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Oops, the fairy doesn't recognize your photo.");
        mAlert = builder.create();
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Talking with the fairy...");

        // Emotion detection.
        mSubscription = new CompositeSubscription();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSubscription
            .add(detectEmotion(getIntent().getData())
                     .subscribeOn(AndroidSchedulers.mainThread())
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(new Subscriber<List<RecognizeResult>>() {
                         @Override
                         public void onCompleted() {
                             Log.d("...", "well... onCompleted");
                         }

                         @Override
                         public void onError(Throwable e) {
                         }

                         @Override
                         public void onNext(List<RecognizeResult> o) {
                             Log.d("...", "well... onNext");

                             mProgress.hide();

                             if (o == null || o.size() == 0) {
                                 mAlert.show();
                             } else {
                                 addScrap();
                                 addCaption(o);
                             }
                         }
                     }));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
    }

    protected Observable<List<RecognizeResult>> detectEmotion(final Uri imgUri) {
        Observable<List<RecognizeResult>> ob = Observable
            .create(new Observable.OnSubscribe<List<RecognizeResult>>() {
                @Override
                public void call(Subscriber<? super List<RecognizeResult>> subscriber) {
                    try {
                        Gson gson = new Gson();

                        // Put the image into an input stream for detection.
                        ByteArrayOutputStream output = new ByteArrayOutputStream();

                        mScrap = ImageUtil.loadSizeLimitedBitmapFromUri(
                            imgUri, getContentResolver());
                        mScrap.compress(Bitmap.CompressFormat.JPEG, 100, output);

                        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

                        long timeMark = System.currentTimeMillis();
                        Log.d("emotion", "Start face detection using Face API");
                        FaceRectangle[] faceRectangles = null;
                        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
                        FaceServiceRestClient faceClient = new FaceServiceRestClient(faceSubscriptionKey);
                        Face faces[] = faceClient.detect(inputStream, false, false, null);
                        Log.d("emotion", String
                            .format("Face detection is done. Elapsed time: %d ms",
                                    (System.currentTimeMillis() - timeMark)));

                        if (faces != null) {
                            faceRectangles = new FaceRectangle[faces.length];

                            for (int i = 0; i < faceRectangles.length; i++) {
                                // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                                com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                                faceRectangles[i] = new FaceRectangle(rect.left,
                                                                      rect.top,
                                                                      rect.width,
                                                                      rect.height);
                            }
                        }

                        List<RecognizeResult> result = null;
                        if (faceRectangles != null) {
                            inputStream.reset();

                            timeMark = System.currentTimeMillis();
                            Log.d("emotion", "Start emotion detection using Emotion API");
                            // -----------------------------------------------------------------------
                            // KEY SAMPLE CODE STARTS HERE
                            // -----------------------------------------------------------------------
                            result = mClient.recognizeImage(inputStream, faceRectangles);

                            String json = gson.toJson(result);
                            Log.d("result", json);
                            // -----------------------------------------------------------------------
                            // KEY SAMPLE CODE ENDS HERE
                            // -----------------------------------------------------------------------
                            Log.d("emotion", String
                                .format("Emotion detection is done. Elapsed time: %d ms",
                                        (System.currentTimeMillis() - timeMark)));

                            subscriber.onNext(result);
                            subscriber.onCompleted();
                        }
                    } catch (Throwable ex) {
                        subscriber.onError(ex);
                    }
                }
            })
            .observeOn(Schedulers.computation())
            .subscribeOn(Schedulers.computation());

        mProgress.show();

        return ob;
    }

    protected void addScrap() {
        LayoutInflater inflater = getLayoutInflater();

        ImageView view = (ImageView) inflater.inflate(R.layout.scrap_image, null, false);

        view.setImageBitmap(mScrap);
        view.setOnTouchListener(new CollageMultiTouchListener(this, 1.0f, 3.0f));

        mCanvas.addView(view);
    }

    protected void addCaption(List<RecognizeResult> recogResult) {
        for (int i = 0; i < recogResult.size(); ++i) {
            RecognizeResult res = recogResult.get(i);
            double anger = res.scores.anger;
            double happiness = res.scores.happiness;
            double sadness = res.scores.sadness;
            double surprise = res.scores.surprise;
            double[] emotions = {anger, happiness, sadness, surprise};
            double max = anger;
            int maxIndex = 0;
            int rand = 1 + (int) (emotions.length * Math.random());
            ImageView caption = (ImageView) getLayoutInflater().inflate(R.layout.scrap_caption, null, false);
            ViewGroup.LayoutParams params = caption.getLayoutParams();

            // Find out most likely emotion.
            for (int j = 0; j < emotions.length; ++j) {
                if (emotions[j] > max) {
                    max = emotions[j];
                    maxIndex = j;
                }
            }

            // Update caption.
            caption.setOnTouchListener(new CollageMultiTouchListener(this, 0.2f, 1.5f));
            switch (maxIndex) {
                case 0: {
                    int resId = getResources()
                        .getIdentifier(String.format("img_anger_%d", rand),
                                       "drawable",
                                       getPackageName());

                    caption.setImageDrawable(getResources().getDrawable(resId));
                    break;
                }
                case 1: {
                    int resId = getResources()
                        .getIdentifier(String.format("img_happiness_%d", rand),
                                       "drawable",
                                       getPackageName());

                    caption.setImageDrawable(getResources().getDrawable(resId));
                    break;
                }
                case 2: {
                    int resId = getResources()
                        .getIdentifier(String.format("img_sadness_%d", rand),
                                       "drawable",
                                       getPackageName());

                    caption.setImageDrawable(getResources().getDrawable(resId));
                    break;
                }
                case 3: {
                    int resId = getResources()
                        .getIdentifier(String.format("img_surprise_%d", rand),
                                       "drawable",
                                       getPackageName());

                    caption.setImageDrawable(getResources().getDrawable(resId));
                    break;
                }
            }

            // Calculate the scale and position.
            float scale = (float) res.faceRectangle.width / 480;
            caption.setScaleX(0.35f);
            caption.setScaleY(0.35f);

            mCanvas.addView(caption);
        }
    }
}
