package ttyy.family.support.wheel;

import android.content.Context;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Author: hujinqi
 * Date  : 2016-08-25
 * Description: 生日选择器
 */
public class BirthdayPickerView extends DatePickerView {

    public BirthdayPickerView(Context context) {
        super(context);
    }

    public BirthdayPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void loadYearDatas() {
        Calendar c = Calendar.getInstance();
        int currentYear = c.get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        // 5岁-80岁
        for (int i = currentYear - 80; i < currentYear - 5; i++) {
            years.add(String.valueOf(i));
        }
        yearWheel.setDatas(years);
        yearWheel.setSelectedPos(years.indexOf(currentYear + ""));
    }
}
