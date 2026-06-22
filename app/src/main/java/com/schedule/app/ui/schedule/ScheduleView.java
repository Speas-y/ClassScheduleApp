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
        
        // 创建一个临时列表用于排序，避免修改原始数据
        List<Course> sortedCourses = new ArrayList<>(this.courses);
        
        // 排序：活跃的课程排在后面（这样会绘制在最上层）
        java.util.Collections.sort(sortedCourses, (c1, c2) -> {
            boolean active1 = c1.isActiveInWeek(currentWeek);
            boolean active2 = c2.isActiveInWeek(currentWeek);
            if (active1 == active2) return 0;
            return active1 ? 1 : -1;
        });
        
        // 用于检测同一时间段是否已经添加了活跃课程
        java.util.Map<String, Boolean> slotHasActive = new java.util.HashMap<>();
        
        for (Course course : sortedCourses) {
            if (course.getStartSection() <= totalSections) {
                // 检查是否需要添加这个课程
                // 如果当前课程不活跃，但同一时间段已经有活跃课程了，就不添加（避免遮挡）
                if (!course.isActiveInWeek(currentWeek)) {
                    String slotKey = course.getDayOfWeek() + "_" + course.getStartSection() + "_" + course.getEndSection();
                    if (slotHasActive.containsKey(slotKey)) {
                        continue; // 跳过不活跃的课程，如果该位置已经有活跃课程
                    }
                } else {
                    // 标记该时间段已有活跃课程
                    String slotKey = course.getDayOfWeek() + "_" + course.getStartSection() + "_" + course.getEndSection();
                    slotHasActive.put(slotKey, true);
                }
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
        
        // Track occupied positions to handle overlapping courses
        java.util.Map<String, java.util.List<RectF>> occupiedPositions = new java.util.HashMap<>();
        
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Course course = (Course) child.getTag();
            
            int dayIndex = course.getDayOfWeek() - 1;
            int startSection = course.getStartSection();
            int endSection = course.getEndSection();
            int sections = endSection - startSection + 1;
            
            // Calculate base position
            int baseLeft = headerColumnWidth + dayIndex * dayWidth + dp(3);
            int baseTop = (startSection - 1) * sectionHeight + dp(4);
            int cardWidth = dayWidth - dp(6);
            int cardHeight = sectionHeight * sections - dp(8);
            
            // Create key for this day+time slot
            String slotKey = dayIndex + "_" + startSection + "_" + endSection;
            
            // Get or create list of occupied positions for this slot
            java.util.List<RectF> occupied = occupiedPositions.get(slotKey);
            if (occupied == null) {
                occupied = new java.util.ArrayList<>();
                occupiedPositions.put(slotKey, occupied);
            }
            
            // Find a position that doesn't overlap with existing courses
            int left = baseLeft;
            int top = baseTop;
            boolean foundPosition = false;
            
            // Try to fit in the same column first
            RectF newRect = new RectF(left, top, left + cardWidth, top + cardHeight);
            boolean overlaps = false;
            for (RectF existing : occupied) {
                if (RectF.intersects(newRect, existing)) {
                    overlaps = true;
                    break;
                }
            }
            
            if (!overlaps) {
                // No overlap, use this position
                foundPosition = true;
            } else {
                // Try shifting horizontally within the day column
                int maxShifts = 3; // Try up to 3 shifts
                for (int shift = 1; shift <= maxShifts; shift++) {
                    int shiftedLeft = baseLeft + (shift * (cardWidth / (maxShifts + 1)));
                    if (shiftedLeft + cardWidth > headerColumnWidth + (dayIndex + 1) * dayWidth - dp(3)) {
                        break; // Don't go outside the column
                    }
                    
                    newRect.set(shiftedLeft, top, shiftedLeft + cardWidth, top + cardHeight);
                    overlaps = false;
                    for (RectF existing : occupied) {
                        if (RectF.intersects(newRect, existing)) {
                            overlaps = true;
                            break;
                        }
                    }
                    
                    if (!overlaps) {
                        left = shiftedLeft;
                        foundPosition = true;
                        break;
                    }
                }
                
                // If still overlapping, try stacking vertically
                if (!foundPosition) {
                    int stackOffset = dp(8);
                    for (int stack = 1; stack <= 5; stack++) {
                        int stackedTop = top + (stack * stackOffset);
                        newRect.set(left, stackedTop, left + cardWidth, stackedTop + cardHeight);
                        overlaps = false;
                        for (RectF existing : occupied) {
                            if (RectF.intersects(newRect, existing)) {
                                overlaps = true;
                                break;
                            }
                        }
                        
                        if (!overlaps) {
                            top = stackedTop;
                            foundPosition = true;
                            break;
                        }
                    }
                }
            }
            
            // If still no position found, use the base position (will overlap but at least show)
            if (!foundPosition) {
                left = baseLeft;
                top = baseTop;
            }
            
            // Add to occupied positions
            occupied.add(new RectF(left, top, left + cardWidth, top + cardHeight));
            
            child.layout(left, top, left + cardWidth, top + cardHeight);
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
