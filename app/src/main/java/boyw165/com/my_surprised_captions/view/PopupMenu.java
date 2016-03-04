package boyw165.com.my_surprised_captions.view;

import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

public class PopupMenu extends PopupWindow {

    public PopupMenu(View contentView) {
        this(contentView, 0, 0, true);
    }

    public PopupMenu(View contentView, int width, int height) {
        this(contentView, width, height, true);
    }

    public PopupMenu(View contentView, int width, int height, boolean focusable) {
        super(contentView, width, height, true);

        setOutsideTouchable(true);

        if (width == 0 || height == 0) {
            // Measure view width and height.
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            setWidth(contentView.getMeasuredWidth());
            setHeight(contentView.getMeasuredHeight());
        }
    }

    /**
     * Set {@link View.OnClickListener} to children views with valid id of the
     * content view.
     */
    public void setOnClickListener(final View.OnClickListener listener) {
        if (!(getContentView() instanceof ViewGroup)) return;

        ViewGroup viewGroup = (ViewGroup) getContentView();

        for (int i = 0; i < viewGroup.getChildCount(); ++i) {
            View view = viewGroup.getChildAt(i);
            if (View.NO_ID == view.getId()) continue;

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onClick(v);
                    }

                    dismiss();
                }
            });
        }
    }

    /**
     * Set {@link View.OnClickListener} to view according to given id.
     * @param id        The id of target view.
     * @param listener  The listener that will run.
     */
    public void setOnClickListener(int id, final View.OnClickListener listener) {
        getContentView()
            .findViewById(id)
            .setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onClick(v);
                    }

                    dismiss();
                }
            });
    }

    /**
     * Show the {@link PopupWindow} by the given view.
     * @param parent    The given view to be shown by.
     */
    public void showByView(View parent) {
        int width = getContentView().getMeasuredWidth();
        int height = getContentView().getMeasuredHeight();
        Rect globalRect = new Rect();

        parent.getGlobalVisibleRect(globalRect);

        // TODO: Check display boundary and make adjustment.
        showAtLocation(parent, Gravity.NO_GRAVITY,
                       globalRect.centerX() - width / 2,
                       globalRect.top - height - 100);
    }
}
