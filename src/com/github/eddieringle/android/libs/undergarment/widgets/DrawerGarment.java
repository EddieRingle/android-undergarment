/*
 * Copyright (c) 2012 Eddie Ringle <eddie@eringle.net>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.eddieringle.android.libs.undergarment.widgets;

import com.github.eddieringle.android.libs.undergarment.R;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Scroller;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * DrawerGarment <p/> An implementation of the slide-out navigation pattern.
 */
public class DrawerGarment extends FrameLayout {

    public static final int SLIDE_TARGET_CONTENT = 0;

    public static final int SLIDE_TARGET_WINDOW = 1;

    private static final int SCROLL_DURATION = 400;

    private static final float TOUCH_TARGET_WIDTH_DIP = 48.0f;

    private boolean mAdded = false;

    private boolean mDrawerEnabled = true;

    private boolean mDrawerOpened = false;

    private boolean mDrawerMoving = false;

    private boolean mGestureStarted = false;

    private int mDecorContentBackgroundColor = Color.TRANSPARENT;

    private int mDecorOffsetX = 0;

    private int mDrawerMaxWidth = WRAP_CONTENT;

    private int mDrawerWidth;

    private int mGestureStartX;

    private int mGestureCurrentX;

    private int mGestureStartY;

    private int mGestureCurrentY;

    private int mSlideTarget;

    private int mTouchTargetWidth;

    private Drawable mShadowDrawable;

    private Handler mScrollerHandler;

    private Scroller mScroller;

    private ViewGroup mDecorView;

    private ViewGroup mContentTarget;

    private ViewGroup mContentTargetParent;

    private ViewGroup mWindowTarget;

    private ViewGroup mWindowTargetParent;

    private ViewGroup mDecorContent;

    private ViewGroup mDecorContentParent;

    private ViewGroup mDrawerContent;

    private Runnable mDrawOpenRunnable, mDrawCloseRunnable;

    private VelocityTracker mVelocityTracker;

    private IDrawerCallbacks mDrawerCallbacks;

    public static interface IDrawerCallbacks {

        public void onDrawerOpened();

        public void onDrawerClosed();
    }

    public static class SmoothInterpolator implements Interpolator {

        @Override
        public float getInterpolation(float v) {
            return (float) (Math.pow((double) v - 1.0, 5.0) + 1.0f);
        }
    }

    public void reconfigureViewHierarchy() {
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        final int widthPixels = dm.widthPixels;

        if (mDecorView == null) {
            return;
        }
        if (mDrawerContent != null) {
            removeView(mDrawerContent);
        }
        if (mDecorContent != null) {
            /*
             * Add the window/content (whatever it is at the time) back to its original parent.
             */
            removeView(mDecorContent);
            mDecorContentParent.addView(mDecorContent);

            /*
             * Reset the window/content's OnClickListener/background color to default values as well
             */
            mDecorContent.setOnClickListener(null);
            mDecorContent.setBackgroundColor(Color.TRANSPARENT);
        }
        if (mAdded) {
            mDecorContentParent.removeView(this);
        }
        if (mSlideTarget == SLIDE_TARGET_CONTENT) {
            mDecorContent = mContentTarget;
            mDecorContentParent = mContentTargetParent;
        } else if (mSlideTarget == SLIDE_TARGET_WINDOW) {
            mDecorContent = mWindowTarget;
            mDecorContentParent = mWindowTargetParent;
        } else {
            throw new IllegalArgumentException(
                    "Slide target must be one of SLIDE_TARGET_CONTENT or SLIDE_TARGET_WINDOW.");
        }
        ((ViewGroup) mDecorContent.getParent()).removeView(mDecorContent);
        addView(mDrawerContent, new ViewGroup.LayoutParams(mDrawerMaxWidth, MATCH_PARENT));
        addView(mDecorContent, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        mDecorContentParent.addView(this);
        mAdded = true;

        /* Set background color of the content view (it defaults to transparent) */
        mDecorContent.setBackgroundColor(mDecorContentBackgroundColor);

        /* Reset shadow bounds */
        mShadowDrawable.setBounds(-mTouchTargetWidth / 6, 0, 0, dm.heightPixels);

        /*
         * Set an empty onClickListener on the Decor content parent to prevent any touch events
         * from escaping and passing through to the drawer even while it's closed.
         */
        mDecorContent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
    }

    public DrawerGarment(Activity activity, int drawerLayout) {
        super(activity);

        final DisplayMetrics dm = activity.getResources().getDisplayMetrics();

        mTouchTargetWidth = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TOUCH_TARGET_WIDTH_DIP, dm));

        mShadowDrawable = getResources().getDrawable(R.drawable.decor_shadow);

        mScrollerHandler = new Handler();
        mScroller = new Scroller(activity, new SmoothInterpolator());
        
        /* Default to targeting the entire window (i.e., including the Action Bar) */
        mSlideTarget = SLIDE_TARGET_WINDOW;

        mDecorView = (ViewGroup) activity.getWindow().getDecorView();
        mWindowTarget = (ViewGroup) mDecorView.getChildAt(0);
        mWindowTargetParent = (ViewGroup) mWindowTarget.getParent();
        mContentTarget = (ViewGroup) mDecorView.findViewById(android.R.id.content);
        mContentTargetParent = (ViewGroup) mContentTarget.getParent();
        mDrawerContent = (ViewGroup) LayoutInflater.from(activity).inflate(drawerLayout, null);

        mDrawerContent.setVisibility(INVISIBLE);

        /*
         * Mutilate the view hierarchy and re-appropriate the slide target,
         * be it the entire window or just android.R.id.content, under
         * this DrawerGarment.
         */
        reconfigureViewHierarchy();

        /*
         * This currently causes lock-ups on 10" tablets (e.g., Xoom & Transformer),
         * should probably look into why this is happening.
         *
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_HARDWARE, null);
        }
         */
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Rect windowRect = new Rect();
        mDecorView.getWindowVisibleDisplayFrame(windowRect);

        if (mSlideTarget == SLIDE_TARGET_WINDOW) {
            mDrawerContent.layout(left, top + windowRect.top, right, bottom);
            mDecorContent.layout(mDecorContent.getLeft(), mDecorContent.getTop(),
                    mDecorContent.getLeft() + right, bottom);
        } else {
            mDrawerContent.layout(left, 0, right, bottom);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mDecorContent.layout(mDecorContent.getLeft(), 0,
                        mDecorContent.getLeft() + right, bottom);
            } else {
                mDecorContent.layout(mDecorContent.getLeft(), top,
                        mDecorContent.getLeft() + right, bottom);
            }
        }

        mDrawerWidth = mDrawerContent.getMeasuredWidth();
        if (mDrawerWidth > right - mTouchTargetWidth) {
            mDrawerContent.setPadding(0, 0, mTouchTargetWidth, 0);
            mDrawerWidth -= mTouchTargetWidth;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final ViewConfiguration vc = ViewConfiguration.get(getContext());
        final float touchThreshold = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30.0f,
                getResources().getDisplayMetrics());
        final int widthPixels = getResources().getDisplayMetrics().widthPixels;

        final double hypo;
        final boolean overcameSlop;

        /* Immediately bomb out if the drawer is disabled */
        if (!mDrawerEnabled) {
            return false;
        }

        /*
         * ...otherwise, handle the various types of input events.
         */
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                /*
                * Record the starting X and Y positions for the possible gesture.
                */
                mGestureStartX = mGestureCurrentX = (int) (ev.getX() + 0.5f);
                mGestureStartY = mGestureCurrentY = (int) (ev.getY() + 0.5f);

                /*
                * If the starting X position is within the touch threshold of 30dp inside the
                * screen's
                * left edge, set mGestureStared to true so that future ACTION_MOVE events will
                * continue being handled here.
                */

                if (mGestureStartX < touchThreshold && !mDrawerOpened) {
                    mGestureStarted = true;
                }

                if (mGestureStartX > mDrawerWidth && mDrawerOpened) {
                    mGestureStarted = true;
                }

                if (mDrawerMoving && mGestureStartX > mDecorOffsetX) {
                    return true;
                }

                /*
                * We still want to return false here since we aren't positive we've got a gesture
                * we want just yet.
                */
                return false;
            case MotionEvent.ACTION_MOVE:

                /* Make sure the gesture was started within 30dp of the screen's left edge. */
                if (!mGestureStarted) {
                    return false;
                }

                /* Make sure we're not going backwards, but only if the drawer isn't open yet */
                if (!mDrawerOpened && (ev.getX() < mGestureCurrentX || ev
                        .getX() < mGestureStartX)) {
                    return (mGestureStarted = false);
                }

                /*
                * Update the current X and Y positions for the gesture.
                */
                mGestureCurrentX = (int) (ev.getX() + 0.5f);
                mGestureCurrentY = (int) (ev.getY() + 0.5f);

                /*
                * Decide whether there is enough movement to do anything real.
                */
                hypo = Math.hypot(mGestureCurrentX - mGestureStartX,
                        mGestureCurrentY - mGestureStartY);
                overcameSlop = hypo > vc.getScaledTouchSlop();

                /*
                * If the last check is true, we'll start handling events in DrawerGarment's
                * onTouchEvent(MotionEvent) method from now on.
                */
                return overcameSlop;
            case MotionEvent.ACTION_UP:

                mGestureStarted = false;

                /*
                * If we just tapped the right edge with the drawer open, close the drawer.
                */
                if (mGestureStartX > mDrawerWidth && mDrawerOpened) {
                    closeDrawer();
                    mGestureStartX = mGestureCurrentX = -1;
                    mGestureStartY = mGestureCurrentY = -1;
                    return true;
                } else {
                    mGestureStartX = mGestureCurrentX = -1;
                    mGestureStartY = mGestureCurrentY = -1;
                    return false;
                }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final ViewConfiguration vc = ViewConfiguration.get(getContext());
        final int widthPixels = getResources().getDisplayMetrics().widthPixels;

        final int deltaX = (int) (event.getX() + 0.5f) - mGestureCurrentX;
        final int deltaY = (int) (event.getY() + 0.5f) - mGestureCurrentY;

        /*
         * Obtain a new VelocityTracker if we don't already have one. Also add this MotionEvent
         * to the new/existing VelocityTracker so we can determine flings later on.
         */
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        /*
         * Update the current X and Y positions for the ongoing gesture.
         */
        mGestureCurrentX = (int) (event.getX() + 0.5f);
        mGestureCurrentY = (int) (event.getY() + 0.5f);

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                mDrawerContent.setVisibility(VISIBLE);
                mDrawerMoving = true;

                if (mDecorOffsetX + deltaX > mDrawerWidth) {
                    if (mDecorOffsetX != mDrawerWidth) {
                        mDrawerOpened = true;
                        mDecorContent.offsetLeftAndRight(
                                mDrawerWidth - mDecorOffsetX);
                        mDecorOffsetX = mDrawerWidth;
                        invalidate();
                    }
                } else if (mDecorOffsetX + deltaX < 0) {
                    if (mDecorOffsetX != 0) {
                        mDrawerOpened = false;
                        mDecorContent.offsetLeftAndRight(0 - mDecorContent.getLeft());
                        mDecorOffsetX = 0;
                        invalidate();
                    }
                } else {
                    mDecorContent.offsetLeftAndRight(deltaX);
                    mDecorOffsetX += deltaX;
                    invalidate();
                }

                return true;
            case MotionEvent.ACTION_UP:
                mGestureStarted = false;
                mDrawerMoving = false;

                /*
                * Determine if the user performed a fling based on the final velocity of the
                * gesture.
                */
                mVelocityTracker.computeCurrentVelocity(1000);
                if (Math.abs(mVelocityTracker.getXVelocity()) > vc
                        .getScaledMinimumFlingVelocity()) {
                    /*
                    * Okay, the user did a fling, so determine the direction in which
                    * the fling was flung so we know which way to toggle the drawer state.
                    */
                    if (mVelocityTracker.getXVelocity() > 0) {
                        mDrawerOpened = false;
                        openDrawer();
                    } else {
                        mDrawerOpened = true;
                        closeDrawer();
                    }
                } else {
                    /*
                    * No sizable fling has been flung, so fling the drawer towards whichever side
                    * we're closest to being flung at.
                    */
                    if (mDecorOffsetX > (widthPixels / 2.0)) {
                        mDrawerOpened = false;
                        openDrawer();
                    } else {
                        mDrawerOpened = true;
                        closeDrawer();
                    }
                }
                return true;
        }
        return false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mDrawerOpened || mDrawerMoving) {
            canvas.save();
            canvas.translate(mDecorOffsetX, 0);
            mShadowDrawable.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * Sets the background color of the content view.
     * Color.TRANSPARENT looks ugly and Color.WHITE is default.
     *
     * @param color
     */
    public void setDecorContentBackgroundColor(final int color) {
        mDecorContentBackgroundColor = color;
    }

    public int getDecorContentBackgroundColor() {
        return mDecorContentBackgroundColor;
    }

    /**
     * Sets the minimum width in pixels the content area will be when the drawer is open.
     *
     * @param width
     */
    public void setTouchTargetWidth(final int width) {
        mTouchTargetWidth = width;
    }

    public int getTouchTargetWidth() {
        return mTouchTargetWidth;
    }

    /**
     * Sets the maximum width in pixels the drawer will open to.
     * Default is WRAP_CONTENT. Can also be MATCH_PARENT or another value in pixels.
     *
     * @param maxWidth
     */
    public void setDrawerMaxWidth(final int maxWidth) {
        mDrawerMaxWidth = maxWidth;
    }

    public int getDrawerMaxWidth() {
        return mDrawerMaxWidth;
    }

    public void setDrawerEnabled(final boolean enabled) {
        mDrawerEnabled = enabled;
    }

    public boolean isDrawerEnabled() {
        return mDrawerEnabled;
    }

    public void toggleDrawer(final boolean animate) {
        if (!mDrawerOpened) {
            openDrawer(animate);
        } else {
            closeDrawer(animate);
        }
    }

    public void toggleDrawer() {
        toggleDrawer(true);
    }

    public void openDrawer(final boolean animate) {
        if(mDrawerMoving){
            mScrollerHandler.removeCallbacks(mDrawCloseRunnable);
            mScrollerHandler.removeCallbacks(mDrawOpenRunnable);
        }

        if (mDrawerOpened) {
            return;
        }

        mDrawerContent.setVisibility(VISIBLE);
        mDrawerMoving = true;

        final int widthPixels = getResources().getDisplayMetrics().widthPixels;
        if (mDrawerWidth > widthPixels - mTouchTargetWidth) {
            mScroller.startScroll(mDecorOffsetX, 0,
                    (widthPixels - mTouchTargetWidth) - mDecorOffsetX,
                    0, animate ? SCROLL_DURATION : 0);
        } else {
            mScroller.startScroll(mDecorOffsetX, 0,
                    mDrawerWidth - mDecorOffsetX,
                    0, animate ? SCROLL_DURATION : 0);
        }

        mDrawOpenRunnable = new Runnable() {
            @Override
            public void run() {
                final boolean scrolling = mScroller.computeScrollOffset();
                mDecorContent.offsetLeftAndRight(mScroller.getCurrX() - mDecorOffsetX);
                mDecorOffsetX = mScroller.getCurrX();
                postInvalidate();

                if (!scrolling) {
                    mDrawerMoving = false;
                    mDrawerOpened = true;
                    if (mDrawerCallbacks != null) {
                        mScrollerHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mDrawerCallbacks.onDrawerOpened();
                            }
                        });
                    }
                } else {
                    mScrollerHandler.post(this);
                }
            }
        };
        mScrollerHandler.post(mDrawOpenRunnable);
    }

    public void openDrawer() {
        openDrawer(true);
    }

    public void closeDrawer(final boolean animate) {
        if(mDrawerMoving){
            mScrollerHandler.removeCallbacks(mDrawCloseRunnable);
            mScrollerHandler.removeCallbacks(mDrawOpenRunnable);
        } else if (!mDrawerOpened) {
            return;
        }

        mDrawerMoving = true;

        final int widthPixels = getResources().getDisplayMetrics().widthPixels;
        mScroller.startScroll(mDecorOffsetX, 0, -mDecorOffsetX, 0,
                animate ? SCROLL_DURATION : 0);

        mDrawCloseRunnable = new Runnable() {
            @Override
            public void run() {
                final boolean scrolling = mScroller.computeScrollOffset();
                mDecorContent.offsetLeftAndRight(mScroller.getCurrX() - mDecorOffsetX);
                mDecorOffsetX = mScroller.getCurrX();
                postInvalidate();

                if (!scrolling) {
                    mDrawerMoving = false;
                    mDrawerOpened = false;
                    mDrawerContent.setVisibility(INVISIBLE);
                    if (mDrawerCallbacks != null) {
                        mScrollerHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mDrawerCallbacks.onDrawerClosed();
                            }
                        });
                    }
                } else {
                    mScrollerHandler.post(this);
                }
            }
        };
        mScrollerHandler.post(mDrawCloseRunnable);
    }

    public void closeDrawer() {
        closeDrawer(true);
    }

    public boolean isDrawerOpened() {
        return mDrawerOpened;
    }

    public boolean isDrawerMoving() {
        return mDrawerMoving;
    }

    public void setDrawerCallbacks(final IDrawerCallbacks callbacks) {
        mDrawerCallbacks = callbacks;
    }

    public IDrawerCallbacks getDrawerCallbacks() {
        return mDrawerCallbacks;
    }

    public int getSlideTarget() {
        return mSlideTarget;
    }

    public void setSlideTarget(final int slideTarget) {
        if (mSlideTarget != slideTarget) {
            mSlideTarget = slideTarget;
            reconfigureViewHierarchy();
        }

    }
}