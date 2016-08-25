package ttyy.family.support.wheel.base;

/**
 * Author: hujinqi
 * Date  : 2016-08-25
 * Description: 侦听器
 */
public interface IWheelListener {

    /**
     * Y轴上滚动的偏差值
     * @param scrollOffsetY
     */
//    void onWheelScrolled(float scrollOffsetY);

    /**
     * 滚动过程中选中的position
     * @param position
     */
//    void onWheelItemScrolledPos(int position);

    /**
     * 滚动停止后选中的position
     * @param position
     */
    void onWheelItemSelected(int position, String value);

}
