package ttyy.family.support.wheel;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import ttyy.family.support.wheel.base.IWheelListener;
import ttyy.family.support.wheel.base.Wheel;
import ttyy.family.support.wheel.base.WheelState;

/**
 * Author: hujinqi
 * Date  : 2016-08-25
 * Description: 日期选择器
 */
public class DatePickerView extends LinearLayout {

    /**
     * 年份选择器
     */
    Wheel yearWheel;
    /**
     * 月份选择器
     */
    Wheel monthWheel;
    /**
     * 天数选择器
     */
    Wheel dayWheel;

    String mSelectedDate = "";

    List<Integer> bigMonths = Arrays.asList(1, 3, 5, 7, 8, 10, 12);
    List<Integer> smallMonths = Arrays.asList(4, 6, 9, 11);

    DateSelectedListener mListener;

    public DatePickerView(Context context) {
        super(context);
        initUIs(context);
    }

    public DatePickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initUIs(context);
    }

    void initUIs(Context context) {
        setOrientation(LinearLayout.HORIZONTAL);

        yearWheel = new Wheel(context);
        LinearLayout.LayoutParams year_params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        year_params.weight = 1;
        addView(yearWheel, year_params);

        monthWheel = new Wheel(context);
        LinearLayout.LayoutParams month_params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        month_params.weight = 1;
        addView(monthWheel, month_params);

        dayWheel = new Wheel(context);
        LinearLayout.LayoutParams day_params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        day_params.weight = 1;
        addView(dayWheel, day_params);

        yearWheel.setWheelListener(new IWheelListener() {
            @Override
            public void onWheelItemSelected(int position, String value) {
                if (mListener == null) {
                    return;
                }
                if (monthWheel.currentState() == WheelState.IDLE
                        && dayWheel.currentState() == WheelState.IDLE) {
                    String date = value + "-" + monthWheel.getSelectedItemValue() + "-" + dayWheel.getSelectedItemValue();
                    if (!mSelectedDate.equals(date)) {
                        mSelectedDate = date;
                        mListener.onDateSelected(mSelectedDate);
                    }
                }
            }
        });

        monthWheel.setWheelListener(new IWheelListener() {
            @Override
            public void onWheelItemSelected(int position, String value) {
                String year = yearWheel.getSelectedItemValue();
                String month = value;
                loadDayDatas(Integer.parseInt(year), Integer.parseInt(month));

                if (mListener == null) {
                    return;
                }
                if (yearWheel.currentState() == WheelState.IDLE
                        && dayWheel.currentState() == WheelState.IDLE) {
                    String date = yearWheel.getSelectedItemValue() + "-" + value + "-" + dayWheel.getSelectedItemValue();
                    if (!mSelectedDate.equals(date)) {
                        mSelectedDate = date;
                        mListener.onDateSelected(mSelectedDate);
                    }
                }
            }
        });

        dayWheel.setWheelListener(new IWheelListener() {
            @Override
            public void onWheelItemSelected(int position, String value) {
                if (mListener == null) {
                    return;
                }
                if (monthWheel.currentState() == WheelState.IDLE
                        && yearWheel.currentState() == WheelState.IDLE) {
                    String date = yearWheel.getSelectedItemValue() + "-" + monthWheel.getSelectedItemValue() + "-" + value;
                    if (!mSelectedDate.equals(date)) {
                        mSelectedDate = date;
                        mListener.onDateSelected(mSelectedDate);
                    }
                }
            }
        });

        loadDatas();
    }

    void loadDatas() {
        loadYearDatas();
        loadMonthDatas();

        Calendar c = Calendar.getInstance();
        int currentYear = c.get(Calendar.YEAR);
        int currentMonth = c.get(Calendar.MONTH) + 1;
        loadDayDatas(currentYear, currentMonth);
    }

    protected void loadYearDatas() {
        Calendar c = Calendar.getInstance();
        int currentYear = c.get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int i = currentYear - 100; i < currentYear + 50; i++) {
            years.add(String.valueOf(i));
        }
        yearWheel.setDatas(years);
        yearWheel.setSelectedPos(years.indexOf(currentYear + ""));
    }

    protected void loadMonthDatas() {
        Calendar c = Calendar.getInstance();
        int currentMonth = c.get(Calendar.MONTH) + 1;
        List<String> months = new ArrayList<>();
        for (int i = 1; i < 13; i++) {
            if (i < 10)
                months.add("0" + i);
            else
                months.add(String.valueOf(i));
        }
        monthWheel.setDatas(months);
        monthWheel.setSelectedPos(currentMonth - 1);
    }

    protected void loadDayDatas(int year, int month) {
        Calendar c = Calendar.getInstance();
        int currentDay = c.get(Calendar.DATE);
        String currentKey = dayWheel.getSelectedItemValue();
        int max = 31;

        if (bigMonths.contains(month)) {
            max = 31;
        } else if (smallMonths.contains(month)) {
            max = 30;
        } else if (year % 4 == 0 && year % 100 != 0 || year % 400 == 0) {
            // 是闰年
            max = 29;
        } else {
            // 不是闰年
            max = 28;
        }

        List<String> days = new ArrayList<>();
        for (int i = 1; i <= max; i++) {
            if (i < 10)
                days.add("0" + i);
            else
                days.add(String.valueOf(i));
        }
        dayWheel.setDatas(days);
        if (currentKey == null) {
            dayWheel.setSelectedPos(currentDay);
        }
    }

    /**
     * 设置日期选中监听器
     *
     * @param listener
     */
    public void setDateSelectedListener(DateSelectedListener listener) {
        this.mListener = listener;
    }

    /**
     * 获取当前选中的日期
     *
     * @return
     */
    public String currentSelectedDate() {
        return mSelectedDate;
    }

    /**
     * 日期选中监听器
     */
    public interface DateSelectedListener {
        void onDateSelected(String date);
    }
}
