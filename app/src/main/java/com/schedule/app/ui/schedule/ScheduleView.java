package com.schedule.app.ui.schedule;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.schedule.app.data.entity.Course;
import com.schedule.app.util.SectionTimeMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义 7 列 × N 节网格：自绘格线与左侧节次时间，课程以 {@link androidx.cardview.widget.CardView} 叠加布局；
 * 非当前周次生效的课程会以灰色半透明显示。
 */
public class ScheduleView extends ViewGroup {

    private static final int TOTAL_DAYS = 7;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int sectionHeight;
    private int headerColumnWidth;
    private int totalSections;

    private List<Course> courses = new ArrayList<>();
    private int currentWeek = 1;
    private OnCourseClickListener listener;

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    public ScheduleView(Context context) {
        this(context, null);
    }

    public ScheduleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScheduleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        init();
    }

    private void init() {
        totalSections = SectionTimeMapper.getTotalSections(getContext());
        sectionHeight = dp(68);
        headerColumnWidth = dp(42);

        gridPaint.setColor(Color.parseColor("#EEF1F6"));
        gridPaint.setStrokeWidth(dp(0.6f));

        textPaint.setColor(Color.parseColor("#5D6677"));
        textPaint.setTextSize(sp(11));
        textPaint.setTextAlign(Paint.Align.CENTER);

        timePaint.setColor(Color.parseColor("#A8B0BF"));
        timePaint.setTextSize(sp(8));
        timePaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setCourses(List<Course> courses, int currentWeek) {
        totalSections = SectionTimeMapper.getTotalSections(getContext());
        this.courses = courses != null ? courses : new ArrayList<>();
        this.currentWeek = currentWeek;
        removeAllViews();
        for (Course course : this.courses) {
            if (course.getStartSection() <= totalSections) {
                addCourseCard(course);
            }
        }
        requestLayout();
        invalidate();
    }

    public void setOnCourseClickListener(OnCourseClickListener listener) {
        this.listener = listener;
    }

    private void addCourseCard(Course course) {
        boolean active = course.isActiveInWeek(currentWeek);

        CardView card = new CardView(getContext());
        card.setCardElevation(0);
        card.setRadius(dp(9));
        card.setUseCompatPadding(false);
        // 课程原色用于保存个性化选择，显示时柔化成浅色块以匹配当前浅色 UI。
        card.setCardBackgroundColor(active ? softenColor(course.getColor()) : Color.parseColor("#E9EEF5"));
        card.setTag(course);
        card.setClickable(true);
        card.setFocusable(true);
        card.setForeground(getSelectableBackground());

        TextView tv = new TextView(getContext());
        tv.setText(course.getCourseName() + "\n" + course.getLocation() + "\n" + course.getTeacher());
        tv.setTextColor(active ? Color.parseColor("#273142") : Color.parseColor("#8B94A5"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tv.setPadding(dp(4), dp(5), dp(4), dp(5));
        tv.setGravity(Gravity.CENTER);
        tv.setLineSpacing(dp(1), 1.05f);
        card.addView(tv, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        card.setOnClickListener(v -> {
            if (listener != null) listener.onCourseClick(course);
        });

        addView(card);
    }

    private android.graphics.drawable.Drawable getSelectableBackground() {
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        return getContext().getDrawable(outValue.resourceId);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = sectionHeight * totalSections;
        setMeasuredDimension(width, height);

        int dayWidth = (width - headerColumnWidth) / TOTAL_DAYS;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Course course = (Course) child.getTag();
            int sections = course.getEndSection() - course.getStartSection() + 1;
            int cardWidth = dayWidth - dp(6);
            int cardHeight = sectionHeight * sections - dp(8);
            child.measure(
                    MeasureSpec.makeMeasureSpec(cardWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(cardHeight, MeasureSpec.EXACTLY)
            );
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int dayWidth = (getWidth() - headerColumnWidth) / TOTAL_DAYS;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Course course = (Course) child.getTag();
            int left = headerColumnWidth + (course.getDayOfWeek() - 1) * dayWidth + dp(3);
            int top = (course.getStartSection() - 1) * sectionHeight + dp(4);
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int dayWidth = (width - headerColumnWidth) / TOTAL_DAYS;

        // 横向节次分隔线
        for (int i = 0; i <= totalSections; i++) {
            float y = i * sectionHeight;
            canvas.drawLine(0, y, width, y, gridPaint);
        }

        // 纵向星期分隔线
        canvas.drawLine(headerColumnWidth, 0, headerColumnWidth, getHeight(), gridPaint);
        for (int i = 1; i <= TOTAL_DAYS; i++) {
            float x = headerColumnWidth + i * dayWidth;
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }

        // 左侧节次编号与开始时间
        for (int i = 0; i < totalSections; i++) {
            float centerX = headerColumnWidth / 2f;
            float centerY = i * sectionHeight + sectionHeight / 2f;
            canvas.drawText(String.valueOf(i + 1), centerX, centerY - sp(5), textPaint);

            String startTime = SectionTimeMapper.getStartTime(getContext(), i + 1);
            canvas.drawText(startTime, centerX, centerY + sp(8), timePaint);
        }
    }

    private int softenColor(int color) {
        // 将课程色向白色混合，保留辨识度同时降低饱和度。
        int red = (int) (Color.red(color) * 0.2f + 255 * 0.8f);
        int green = (int) (Color.green(color) * 0.2f + 255 * 0.8f);
        int blue = (int) (Color.blue(color) * 0.2f + 255 * 0.8f);
        return Color.rgb(red, green, blue);
    }

    private int dp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private int sp(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getResources().getDisplayMetrics());
    }
}
