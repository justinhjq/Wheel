package ttyy.family.support.wheel.base;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Author: hujinqi
 * Date  : 2016-08-24
 * Description: 滚轮
 */
public class Wheel extends View implements Runnable {

    /**
     * 滚动管理器
     */
    Scroller mScroller;
    boolean isForceFinished;

    /**
     * 速度计算器
     */
    VelocityTracker mTracker;

    /**
     * 画笔
     */
    Paint mPaint;

    Rect mRectDrawn;
    Camera mCamera;
    /**
     * 旋转度数, 在深度z轴上的移动matrix
     */
    Matrix mMatrixRotate, mMatrixDepth;

    /**
     * 默认可视5个
     * 但是实际绘画多出两个
     */
    int mVisibleItemCount = 5, mDrawItemCount = mVisibleItemCount + 2, mHalfDrawItemCount = mDrawItemCount / 2;

    /**
     * 单个数据项的高度
     * 单个数据项的高度的一半
     */
    int mItemHeight, mHalfItemHeight, mHalfWheelHeight;

    /**
     * 正中央显示的相对文字索引
     */
    int mSelectedItemPos = mVisibleItemCount / 2;

    /**
     * 数据项的文字大小
     * 数据项文字大小与单个数据项高度的比例关系
     */
    float mItemTextSize, mItemTextSizeScale = 1f;

    /**
     * 中间text的绘画坐标
     */
    float mDrawCenterY;

    /**
     * Y轴上滚动的距离s
     */
    float mScrollOffset, mLastPointY;

    /**
     * 滑动到的临界值，最小Y，最大Y
     */
    float mMinFlingY, mMaxFlingY;

    /**
     * 是否支持循环滚动
     * 默认支持循环滚动
     */
    boolean isCyclic = true;

    /**
     * 滚轮滑动时的最小/最大速度
     */
    int mMinimumVelocity = 50, mMaximumVelocity = 8000;

    /**
     * 滚动监听器
     */
    IWheelListener listener;

    WheelState mState = WheelState.IDLE;

    /**
     * 数据源
     */
    List<String> datas = new ArrayList<>();

    public Wheel(Context context) {
        super(context);
        init();
    }

    public Wheel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    protected void init() {

        mScroller = new Scroller(getContext());

        mCamera = new Camera();
        mMatrixRotate = new Matrix();
        mMatrixDepth = new Matrix();

        mPaint = new Paint();
        mPaint.setColor(Color.parseColor("#333333"));
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextAlign(Paint.Align.CENTER);// 横向居中

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
            ViewConfiguration conf = ViewConfiguration.get(getContext());
            mMinimumVelocity = conf.getScaledMinimumFlingVelocity();
            mMaximumVelocity = conf.getScaledMaximumFlingVelocity();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mRectDrawn = new Rect(0, 0,
                getMeasuredWidth(),
                getMeasuredHeight());

        // Item Height
        mItemHeight = mRectDrawn.height() / mVisibleItemCount;
        mHalfItemHeight = mItemHeight / 2;
        mHalfWheelHeight = mRectDrawn.centerY();

        // TextSize
        mItemTextSize = mItemHeight * mItemTextSizeScale;
        mPaint.setTextSize(mItemTextSize);

        // 计算中心坐标 矫正误差
        mDrawCenterY = (int) (mHalfWheelHeight - ((mPaint.ascent() + mPaint.descent()) / 2));

        computeFlingLimitY();

        setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (null == mTracker)
                    mTracker = VelocityTracker.obtain();
                else
                    mTracker.clear();
                mTracker.addMovement(event);
                // 从按下开始就是拖动
                mState = WheelState.DRAG;
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    isForceFinished = true;
                }

                mLastPointY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                mTracker.addMovement(event);

                mScrollOffset += event.getY() - mLastPointY;
                mLastPointY = event.getY();

                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                mTracker.addMovement(event);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT)
                    mTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                else
                    mTracker.computeCurrentVelocity(1000);
                int velocity = (int) mTracker.getYVelocity();
                if (Math.abs(velocity) > mMinimumVelocity) {
                    // 速度很大 继续滑动
                    mScroller.fling(0, (int) mScrollOffset,
                            0, velocity,
                            0, 0,
                            (int) mMinFlingY, (int) mMaxFlingY);
                    mScroller.setFinalY(mScroller.getFinalY() + computeScrollEndPoint(mScroller.getFinalY() % mItemHeight));
                } else {
                    // 速度忽略不计 矫正坐标
                    mScroller.startScroll(0, (int) mScrollOffset,
                            0, computeScrollEndPoint((int) mScrollOffset % mItemHeight));
                }

                // 校正坐标
                if (!isCyclic)
                    if (mScroller.getFinalY() > mMaxFlingY)
                        mScroller.setFinalY((int) mMaxFlingY);
                    else if (mScroller.getFinalY() < mMinFlingY)
                        mScroller.setFinalY((int) mMinFlingY);

                isForceFinished = false;
                post(this);

                if (null != mTracker) {
                    mTracker.recycle();
                    mTracker = null;
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                isForceFinished = false;
                if (null != getParent())
                    getParent().requestDisallowInterceptTouchEvent(false);
                if (null != mTracker) {
                    mTracker.recycle();
                    mTracker = null;
                }
                break;
        }

        return true;
    }

    @Override
    public void run() {

        if (mScroller.isFinished() && !isForceFinished) {
            // 只有手势抬起时并且scroller滚动结束，才会计算当前选中的item位置
            // 状态 停止
            mState = WheelState.IDLE;
            if (listener != null) {
                int position = getSelectedItemPosition();
                String value = null;
                if (datas != null
                        && datas.size() > position) {
                    value = datas.get(position);
                }

                listener.onWheelItemSelected(position, value);
            }
        }

        if (mScroller.computeScrollOffset()) {
            // 状态 滑动
            mState = WheelState.FLING;
            mScrollOffset = mScroller.getCurrY();

            postInvalidate();
            postDelayed(this, 16);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (datas == null
                || datas.size() < 1) {
            return;
        }
        // 计算首个绘画的数据源的数据index
        int drawStartPos = -mHalfDrawItemCount - (int) (mScrollOffset / mItemHeight);
        for (int i = drawStartPos,
             drawnOffsetPos = -mHalfDrawItemCount;

             i < drawStartPos + mDrawItemCount;

             i++, drawnOffsetPos++) {
            float centerY = computeDrawCenterY(drawnOffsetPos);
            float centerX = computeDrawCenterX();

            // 如果支持滚轮特效那么centerY就得冲定义
            centerY = setWheelMatrix(centerX, centerY);

            String text = "";
            if (isCyclic) {
                // 支持循环滚动
                int dataIndex = i + mSelectedItemPos;
                dataIndex = dataIndex % datas.size();
                dataIndex = dataIndex < 0 ? dataIndex + datas.size() : dataIndex;
                text = datas.get(dataIndex);
            } else {
                int dataIndex = i + mSelectedItemPos;
                if (dataIndex >= 0 && dataIndex <= datas.size() - 1) {
                    text = datas.get(dataIndex);
                }
            }

            canvas.save();
            canvas.concat(mMatrixRotate);

            // 设置透明度 区别选中状态
            int alpha = (int) ((mDrawCenterY - Math.abs(mDrawCenterY - centerY)) *
                    1.0F / mDrawCenterY * 255);
            alpha = alpha < 0 ? 0 : alpha;
            mPaint.setAlpha(alpha);

            canvas.drawText(text, centerX, centerY, mPaint);
            canvas.restore();
        }
    }

    /**
     * 获取当前选中的Item
     *
     * @return
     */
    public int getSelectedItemPosition() {
        int dataIndex = mSelectedItemPos - (int) (mScrollOffset / mItemHeight);
        if (isCyclic) {
            // 支持循环滚动
            dataIndex = dataIndex % datas.size();
            dataIndex = dataIndex < 0 ? dataIndex + datas.size() : dataIndex;
        } else {
            // 不支持循环滚动
            if (dataIndex < 0) {
                dataIndex = 0;
            } else if (dataIndex > datas.size() - 1) {
                dataIndex = datas.size() - 1;
            }
        }
        return dataIndex;
    }

    /**
     * 获取当前选中的Item Value
     *
     * @return
     */
    public String getSelectedItemValue() {
        if (datas == null
                || datas.size() < 1) {
            return null;
        }
        return datas.get(getSelectedItemPosition());
    }

    /**
     * 设置是否支持循环
     *
     * @param isCylic
     */
    public void setCyclic(boolean isCylic) {
        this.isCyclic = isCylic;

        computeFlingLimitY();
        postInvalidate();
    }

    /**
     * 是否支持循环滚动
     *
     * @return
     */
    public boolean isCyclic() {
        return isCyclic;
    }

    /**
     * 当前滚轮状态
     *
     * @return
     */
    public WheelState currentState() {
        return mState;
    }

    /**
     * 设置数据源
     *
     * @param datas
     */
    public void setDatas(List<String> datas) {
        this.datas = datas;

        computeFlingLimitY();
        postInvalidate();
    }

    /**
     * 设置相对选中项
     *
     * @param pos
     */
    public void setSelectedPos(int pos) {
        mScrollOffset = 0;

        mSelectedItemPos = pos;
        mSelectedItemPos = mSelectedItemPos > datas.size() ? datas.size() - 1 : mSelectedItemPos;

        computeFlingLimitY();
        postInvalidate();
    }

    /**
     * 滚轮选择状态监听器
     *
     * @param listener
     */
    public void setWheelListener(IWheelListener listener) {
        this.listener = listener;
    }

    /**
     * 计算滚动器的可滚动范围
     */
    void computeFlingLimitY() {
        int currentItemOffset = mSelectedItemPos * mItemHeight;
        mMinFlingY = isCyclic ? Integer.MIN_VALUE : -mItemHeight * (datas.size() - 1) + currentItemOffset;
        mMaxFlingY = isCyclic ? Integer.MAX_VALUE : currentItemOffset;
    }

    /**
     * 计算当前的text的绘画中心Y坐标
     *
     * @param index
     * @return
     */
    float computeDrawCenterY(int index) {
        float centerY = mDrawCenterY + index * mItemHeight + mScrollOffset % mItemHeight;
        return centerY;
    }

    /**
     * 计算当前的text的绘画中心X坐标
     *
     * @return
     */
    float computeDrawCenterX() {
        return (float) mRectDrawn.centerX();
    }

    /**
     * 滚轮扭曲变换矩阵设置
     *
     * @param itemCenterX
     * @param itemCenterY
     */
    float setWheelMatrix(float itemCenterX, float itemCenterY) {
        // 计算数据项绘制中心距离滚轮中心的距离比率
        float ratio = (mDrawCenterY - Math.abs(mDrawCenterY - itemCenterY) -
                mRectDrawn.top) * 1.0F / (mDrawCenterY - mRectDrawn.top);

        // 计算单位
        int unit = 0;
        if (itemCenterY > mDrawCenterY)
            unit = 1;
        else if (itemCenterY < mDrawCenterY)
            unit = -1;

        float degree = (-(1 - ratio) * 90 * unit);
        if (degree < -90) degree = -90;
        if (degree > 90) degree = 90;
        float distanceToCenter = computeSpace(degree);
        float transY = mRectDrawn.centerY() - distanceToCenter;
        float transX = itemCenterX;

        mCamera.save();
        mCamera.rotateX(degree);
        mCamera.getMatrix(mMatrixRotate);
        mCamera.restore();
        // 以transX transY坐标点为中心进行旋转
        // 若没有以下操作 那么久默认为0，0点旋转
        mMatrixRotate.preTranslate(-transX, -transY);
        mMatrixRotate.postTranslate(transX, transY);

        mCamera.save();
        mCamera.translate(0, 0, computeDepth((int) degree));
        mCamera.getMatrix(mMatrixDepth);
        mCamera.restore();
        mMatrixDepth.preTranslate(-transX, -transY);
        mMatrixDepth.postTranslate(transX, transY);

        mMatrixRotate.postConcat(mMatrixDepth);

        itemCenterY = mDrawCenterY - distanceToCenter;
        return itemCenterY;
    }

    /**
     * 正余弦函数更贴切滚轮弧线
     *
     * @param degree
     * @return
     */
    float computeSpace(float degree) {
        return (float) (Math.sin(Math.toRadians(degree)) * mHalfWheelHeight);
    }

    /**
     * 正余弦函数更贴切滚轮弧线
     *
     * @param degree
     * @return
     */
    float computeDepth(int degree) {
        return (float) (mHalfWheelHeight - Math.cos(Math.toRadians(degree)) * mHalfWheelHeight);
    }

    /**
     * 计算scroller最终滑向的中点坐标的矫正值
     *
     * @param remainder
     * @return
     */
    int computeScrollEndPoint(int remainder) {
        if (Math.abs(remainder) > mHalfItemHeight)
            // 大于一半 就要进1 同时还要把多出来的余数给去掉
            if (mScrollOffset < 0) {
                // 向上滑动 那么实际上数据源的index应该-1 这样数据才能上去 默认数据索引从小打到绘画顺序是从上到下
                return -mItemHeight - remainder;
            } else {
                // 向下滑动 那么实际上数据源的index应该+1 这样数据才能下来
                return mItemHeight - remainder;
            }
        else
            return -remainder;
    }
}
