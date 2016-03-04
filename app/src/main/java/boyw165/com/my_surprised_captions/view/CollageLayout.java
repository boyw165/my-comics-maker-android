package boyw165.com.my_surprised_captions.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import boyw165.com.my_surprised_captions.tool.LogUtils;
import boyw165.com.my_surprised_captions.view.CollageMultiTouchListener.TransformInfo;
import boyw165.com.my_surprised_captions.view_model.MosaicCache;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;

public class CollageLayout extends FrameLayout {

    private static final int MOSAIC_FILTER = 0x00000001;
    private static final int SOMEAA_FILTER = 0x00000002;
    private static final int SOMEBB_FILTER = 0x00000004;

    private int mFiltersFlag;

    //    private Bitmap mDrawingCacheBmp;
    private boolean mIsDrawingCacheDirty = true;
    private MosaicCache mMosaicCacheLv1 = new MosaicCache();
    private MosaicCache mMosaicCacheLv2 = new MosaicCache();
    private MosaicCache mMosaicCacheLv3 = new MosaicCache();
    private int mMosaicIndex = 0;
    private List<MosaicCache> mMosaicCaches = new ArrayList<>();
    {
        mMosaicCaches.add(mMosaicCacheLv1);
        mMosaicCaches.add(mMosaicCacheLv2);
        mMosaicCaches.add(mMosaicCacheLv3);
    }

    // For normal children views that have to be rendered into drawing cache.
    private FrameLayout mLayer1;
    // For special children views that are invisible to be rendered.
    private FrameLayout mLayer2;

    // TODO: Make a HOT observable doing image processing on demand.
    private CompositeSubscription mSubscriptions = new CompositeSubscription();
    private Subject<MosaicCache, MosaicCache> mMosaicSubject;

    // FIXME: Remove following debug codes.
    private TransformInfo mTransformInfo = new TransformInfo();
    private CollageMultiTouchListener mMultiTouchListener;
    private Paint mDebugPaint;
    private Path mDebugPath;

    public CollageLayout(Context context) {
        this(context, null, 0, 0);
    }

    public CollageLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public CollageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CollageLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayersHierarchyChange listener = new LayersHierarchyChange();
        mLayer1 = new FrameLayout(context, attrs, defStyleAttr, defStyleRes);
        mLayer1.setOnHierarchyChangeListener(listener);
        mLayer2 = new FrameLayout(context, attrs, defStyleAttr, defStyleRes);
        mLayer2.setOnHierarchyChangeListener(listener);

        addView(mLayer1);
        addView(mLayer2);

        if (!isInEditMode()) {
            PublishSubject<MosaicCache> subject = PublishSubject.create();
            mMosaicSubject = new SerializedSubject<>(subject);
        }

//        mDebugPaint = new Paint();
//        mDebugPaint.setColor(Color.rgb(0xFF, 0, 0));
//        mDebugPaint.setStyle(Paint.Style.STROKE);
//        mDebugPaint.setStrokeWidth(10);
//        mDebugPath = new Path();
//        mMultiTouchListener = new CollageMultiTouchListener(getContext(),
//                                                            mTransformInfo);
//        setOnTouchListener(mMultiTouchListener);
    }

    // FIXME: Remove following debug codes.
//    @Override
//    protected void onDraw(Canvas canvas) {
//        TransformInfo info = mTransformInfo;
//
//        if (info.deltaPos.x != 0 || info.deltaPos.y != 0) {
//            float[] deltaVec = {info.deltaPos.x,
//                                info.deltaPos.y};
//            mDebugPath.reset();
//            mDebugPath.moveTo(info.prevPos.x, info.prevPos.y);
//            mDebugPath.lineTo(info.prevPos.x + info.deltaPos.x,
//                         info.prevPos.y + info.deltaPos.y);
//            canvas.drawPath(mDebugPath, mDebugPaint);
//        }
//    }

    public void addViewToLayer1(View child) {
        mLayer1.addView(child);

        // FIXME: Debug mosaic effect.
//        getMosaicCache()
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(new Subscriber<Bitmap>() {
//                @Override
//                public void onCompleted() {
//
//                }
//
//                @Override
//                public void onError(Throwable e) {
//
//                }
//
//                @Override
//                public void onNext(Bitmap bitmap) {
//                    ImageView imageView = (ImageView) mLayer1.getChildAt(0);
//                    imageView.setImageBitmap(mMosaicCacheLv1);
//                }
//            });
    }

    public void addViewToLayer2(View child) {
        mLayer2.addView(child);
    }

    public void invalidFilters() {
        if ((mFiltersFlag & MOSAIC_FILTER) == MOSAIC_FILTER) {
            // Subscribe mosaic subject to the effect observable.
            mSubscriptions.add(getMosaicCache()
                                   .observeOn(AndroidSchedulers.mainThread())
                                   .subscribe(mMosaicSubject));
        }
    }

    public void unsubscribeAll() {
        mSubscriptions.unsubscribe();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private ////////////////////////////////////////////////////////////////

    private Observable<Bitmap> getDrawingCacheBitmap() {
        return Observable
            .create(new Observable.OnSubscribe<Bitmap>() {
                @Override
                public void call(Subscriber<? super Bitmap> subscriber) {
                    LogUtils.log("getDrawingCacheBitmap");
                    Bitmap bitmap = null;

                    if (mIsDrawingCacheDirty) {
//                    // TODO: don't know if it is necessary to protect the drawing cache.
                        // Check if need to re-new drawing cache.
                        mLayer1.setDrawingCacheEnabled(true);
                        bitmap = Bitmap.createBitmap(mLayer1.getDrawingCache());
                        mLayer1.setDrawingCacheEnabled(false);

                        mIsDrawingCacheDirty = false;

                        // TODO: Any subscription leak?
                    }

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(bitmap);
                    }
                }
            })
            .subscribeOn(AndroidSchedulers.mainThread());
    }

    private Observable<MosaicCache> getMosaicCache() {
        return getDrawingCacheBitmap()
            .observeOn(Schedulers.computation())
            .map(new Func1<Bitmap, MosaicCache>() {
                @Override
                public MosaicCache call(Bitmap bitmap) {
                    LogUtils.log("getMosaicCache");
                    MosaicCache cache = mMosaicCaches.get(mMosaicIndex);

                    if (cache.cachedBmp == null && bitmap != null) {
                        int scaleFactor = 1 << (mMosaicIndex + 4);
                        Matrix scaleDown = new Matrix();
                        Matrix scaleUp = new Matrix();
                        scaleDown.setScale(1f / scaleFactor, 1f / scaleFactor);
                        scaleUp.setScale(scaleFactor, scaleFactor);

                        synchronized (CollageLayout.this) {
                            try {
                                cache.scaleFactor = scaleFactor;
                                cache.cachedBmp = Bitmap.createBitmap(
                                    bitmap, 0, 0,
                                    bitmap.getWidth(),
                                    bitmap.getHeight(),
                                    scaleDown, false);
                            } catch (Throwable throwable) {
                                LogUtils.log(throwable.toString());
                            }
                        }

                        // Recycle the drawing cache.
                        bitmap.recycle();
                        System.gc();

                        // Update index.
                        if (++mMosaicIndex >= mMosaicCaches.size()) {
                            mMosaicIndex = 0;
                        }
                    }

                    cache.canvasWidth = getMeasuredWidth();
                    cache.canvasHeight = getMeasuredHeight();

                    return cache;
                }
            });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Class //////////////////////////////////////////////////////////////////

    private class LayersHierarchyChange implements OnHierarchyChangeListener {

        @Override
        public void onChildViewAdded(View parent, View child) {
            mIsDrawingCacheDirty = true;

            if (child instanceof MosaicView) {
                // Update the filters flag so that this container will update the
                // cached effects.
                mFiltersFlag |= MOSAIC_FILTER;

                // Make children subscribe to the effect.
                ((MosaicView) child).subscribeToMosaic(mMosaicSubject);

                invalidFilters();
            }
        }

        @Override
        public void onChildViewRemoved(View parent, View child) {

        }
    }
}
