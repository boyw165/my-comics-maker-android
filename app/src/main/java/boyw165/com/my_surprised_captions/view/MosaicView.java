package boyw165.com.my_surprised_captions.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import boyw165.com.my_surprised_captions.model.SomeModel;
import boyw165.com.my_surprised_captions.tool.LogUtils;
import boyw165.com.my_surprised_captions.view_model.MosaicCache;
import rx.Observable;
import rx.Subscriber;

public class MosaicView extends View {

    private static final String TAG = "MosaicView";

    protected CollageMultiTouchListener mMultiTouchListener;

    protected Paint mOriginPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected Paint mPlaceholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected Rect mBound = new Rect(0, 0, 700, 700);

    protected SomeModel mModel;

    protected MosaicCache mDrawingCache;
    protected Paint mDrawingCachePaint = new Paint();
    protected Path mDrawingCachePath;

    protected Matrix mDrawingMtx = new Matrix();
    protected final Rect mDrawingSrcRect = new Rect();
    protected final Rect mDrawingDstRect = new Rect();

    public MosaicView(Context context) {
        this(context, null);
    }

    /**
     * To allow Android Studio to interact this view, we have to provide the
     * constructor that takes a {@link Context} and {@link AttributeSet} object
     * as parameters.
     */
    public MosaicView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mOriginPaint.setColor(Color.rgb(255, 0, 0));
        mOriginPaint.setStyle(Paint.Style.STROKE);
        mOriginPaint.setStrokeWidth(15);

        mPlaceholderPaint.setColor(Color.rgb(0, 0, 0xFF));
        mPlaceholderPaint.setStyle(Paint.Style.STROKE);
        mPlaceholderPaint.setStrokeWidth(15);

        mDrawingCachePaint.setAntiAlias(false);
        mDrawingCachePaint.setDither(false);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(mBound.width(), mBound.height());
//        params.leftMargin = 200;
//        params.topMargin = 200;
        setLayoutParams(params);

        // Initial generic multi-touch function.
        mMultiTouchListener = new CollageMultiTouchListener(getContext());
        setOnTouchListener(mMultiTouchListener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mDrawingCache != null) {
            canvas.save();

            // TODO: Get mosaic bitmap from the model.
//            mDrawingCachePath.reset();
//            mDrawingCachePath.addRect(mDrawingDstRect.left, mDrawingDstRect.top,
//                                      mDrawingDstRect.right, mDrawingDstRect.bottom,
//                                      Path.Direction.CW);
//            mDrawingCachePath.moveTo(mDrawingSrcRect.left, mDrawingSrcRect.top);
//            mDrawingCachePath.lineTo(mDrawingSrcRect.right, mDrawingSrcRect.top);
//            mDrawingCachePath.lineTo(mDrawingSrcRect.right, mDrawingSrcRect.bottom);
//            mDrawingCachePath.lineTo(mDrawingSrcRect.left, mDrawingSrcRect.bottom);
//            mDrawingCachePath.lineTo(mDrawingSrcRect.left, mDrawingSrcRect.top);
//            canvas.concat(mtx);
            mDrawingMtx.reset();
            mDrawingMtx.setScale(mDrawingCache.scaleFactor,
                                 mDrawingCache.scaleFactor);
            mDrawingMtx.postTranslate(-getX(), -getY());
            canvas.drawBitmap(mDrawingCache.cachedBmp,
                              mDrawingMtx,
                              mDrawingCachePaint);

            canvas.restore();
        }

        mDrawingDstRect.set(0, 0, getWidth(), getHeight());
        canvas.drawRect(mDrawingDstRect,
                        mPlaceholderPaint);

        drawOrigin(canvas);
    }

    public void subscribeToMosaic(Observable<MosaicCache> observable) {
//        observable.publish()
        observable.subscribe(new Subscriber<MosaicCache>() {
            @Override
            public void onCompleted() {
                LogUtils.log("onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                LogUtils.log("onError");
            }

            @Override
            public void onNext(MosaicCache mosaicCache) {
                LogUtils.log("onNext");

                if (mDrawingCache == null) {
                    mDrawingCache = mosaicCache;
                    mMultiTouchListener.setScaleLimit(
                        1.0f,
                        2.5f);

                    invalidate();
                }
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private ////////////////////////////////////////////////////////////////

    private void drawOrigin(Canvas canvas) {
        canvas.drawLine(-20, 0, 20, 0, mOriginPaint);
        canvas.drawLine(0, -20, 0, 20, mOriginPaint);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////
}
