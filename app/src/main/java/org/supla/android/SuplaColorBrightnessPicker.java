package org.supla.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

/*
 Copyright (C) AC SOFTWARE SP. Z O.O.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

public class SuplaColorBrightnessPicker extends View {

    static private final int[] Colors = new int[]{
            0xFFFF0000, 0xFFFF00FF,
            0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000
    };
    final static private double m160d = Math.toRadians(-160);
    final static private double m90d = Math.toRadians(-90);
    final static private double m90_01d = Math.toRadians(-90.01);
    final static private double m20d = Math.toRadians(-20);
    private int[] BW = new int[]{
            Color.BLACK,
            Color.WHITE,
            Color.WHITE

    };
    private PointerTop outerTop;
    private PointerTop innerTop;
    private RectF rectF = new RectF();
    private float centerX;
    private float centerY;
    private float outerWheelWidth;
    private double outerArrowHeight_a;
    private double outerArrowHeight_b;
    private float outerWheelRadius;
    private double outerWheelPointerAngle;
    private double innerArrowHeight_a;
    private double innerArrowHeight_b;
    private float innerWheelWidth;
    private float innerWheelRadius;
    private double innerWheelPointerAngle;
    private int selectedColor;
    private double selectedBrightness;
    private Path outerArrowPath;
    private Paint outerArrowPaint;
    private Path innerArrowPath;
    private Paint innerArrowPaint;
    private Paint paint;
    private Paint cwPaint;   // color wheel paint
    private Shader cwShader; // color wheel shader
    private Paint bwPaint;   // brightness wheel paint
    private Shader bwShader; // brightness wheel shader
    private Matrix gradientRotationMatrix;
    private boolean colorWheelVisible;
    private boolean colorWheelMove;
    private boolean brightnessWheelMove;
    private boolean colorfulBrightnessWheel;
    private boolean circleInsteadArrow;
    private double lastTouchedAngle;
    private OnColorBrightnessChangeListener mOnChangeListener;
    private Rect bounds;
    private ArrayList<Double> ColorMarkers;
    private ArrayList<Double> BrightnessMarkers;

    public SuplaColorBrightnessPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public SuplaColorBrightnessPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SuplaColorBrightnessPicker(Context context) {
        super(context);
        init();
    }

    private void init() {

        colorWheelVisible = true;

        paint = new Paint();

        cwShader = new SweepGradient(0, 0, Colors, null);
        cwPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cwPaint.setStyle(Paint.Style.STROKE);

        bwShader = new SweepGradient(0, 0, BW, null);
        bwPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bwPaint.setStyle(Paint.Style.STROKE);

        outerArrowPath = new Path();
        outerArrowPaint = new Paint();

        outerTop = new PointerTop();
        innerTop = new PointerTop();

        outerWheelPointerAngle = Math.toRadians(-90);
        selectedColor = calculateColor((float) outerWheelPointerAngle, Colors);

        innerArrowPath = new Path();
        innerArrowPaint = new Paint();

        innerWheelPointerAngle = Math.toRadians(-90);
        selectedBrightness = 0;

        gradientRotationMatrix = new Matrix();
        gradientRotationMatrix.preRotate(-90);
        bwShader.setLocalMatrix(gradientRotationMatrix);

        colorWheelMove = false;
        brightnessWheelMove = false;
        colorfulBrightnessWheel = true;
        circleInsteadArrow = false;

        bounds = new Rect();

        setBWcolor();
    }

    private int ave(int s, int d, float p) {
        return s + Math.round(p * (d - s));
    }

    private int calculateColor(float angle, int[] Colors) {
        float unit = (float) (angle / (2 * Math.PI));
        if (unit < 0) {
            unit += 1;
        }

        if (unit <= 0) {
            return Colors[0];
        }
        if (unit >= 1) {
            return Colors[Colors.length - 1];
        }

        float p = unit * (Colors.length - 1);
        int i = (int) p;
        p -= i;

        int c0 = Colors[i];
        int c1 = Colors[i + 1];
        int a = ave(Color.alpha(c0), Color.alpha(c1), p);
        int r = ave(Color.red(c0), Color.red(c1), p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0), Color.blue(c1), p);

        return Color.argb(a, r, g, b);
    }

    private float colorToAngle(int color) {
        float[] colors = new float[3];
        Color.colorToHSV(color, colors);

        return (float) Math.toRadians(-colors[0]);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(widthSize, heightSize);
        setMeasuredDimension(size, size);
    }

    private void drawMarkers(Canvas canvas, float radius, float markerSize, ArrayList<Double> markers, boolean brightness) {

        double angle;
        paint.setAntiAlias(true);
        paint.setStrokeWidth(markerSize / 4);

        if (markers == null) {
            return;
        }

        for (int a = 0; a < markers.size(); a++) {

            double v = markers.get(a);
            if (brightness) {
                if (v < 0.5) {
                    v = 0.5;
                } else if (v > 99.5) {
                    v = 99.5;
                }

                angle = brightnessToAngle(v);
            } else {
                angle = colorToAngle((int) v);
            }

            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;

            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);

            canvas.drawCircle(x, y, markerSize, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);

            canvas.drawCircle(x, y, markerSize, paint);

        }

    }

    private void drawCirclePointer(Canvas canvas, double angle,
                                   float wheelRadius, float wheelWidth) {

        float x = (float)Math.cos(angle) * wheelRadius;
        float y = (float)Math.sin(angle) * wheelRadius;

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);

        canvas.drawCircle(x, y, wheelWidth * 0.47f, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.translate(centerX, centerY);

        if (colorWheelVisible) {

            cwPaint.setShader(cwShader);
            rectF.set(-outerWheelRadius, -outerWheelRadius, outerWheelRadius, outerWheelRadius);
            canvas.drawOval(rectF, cwPaint);

            if (circleInsteadArrow) {
                drawCirclePointer(canvas, outerWheelPointerAngle,
                        outerWheelRadius, outerWheelWidth);
            } else {
                drawArrow(canvas, outerTop,
                        outerWheelPointerAngle,
                        outerWheelRadius,
                        outerWheelWidth,
                        -(outerArrowHeight_a / 4),
                        outerArrowHeight_a,
                        outerArrowHeight_b,
                        selectedColor,
                        outerArrowPath,
                        outerArrowPaint);
            }


            drawMarkers(canvas, outerWheelRadius,
                    outerWheelWidth / 6, ColorMarkers, false);
        }

        bwPaint.setShader(bwShader);
        rectF.set(-innerWheelRadius, -innerWheelRadius, innerWheelRadius, innerWheelRadius);
        canvas.drawOval(rectF, bwPaint);

        int negative;
        double arrowOffset;

        if (colorWheelVisible) {
            negative = -1;
            arrowOffset = innerArrowHeight_a / 4 - innerWheelWidth;
        } else {
            negative = 1;
            arrowOffset = -(innerArrowHeight_a / 4);
        }

        if (circleInsteadArrow) {
            drawCirclePointer(canvas, innerWheelPointerAngle,
                    innerWheelRadius, innerWheelWidth);
        } else {
            drawArrow(canvas, innerTop,
                    innerWheelPointerAngle,
                    innerWheelRadius,
                    innerWheelWidth,
                    arrowOffset,
                    negative * innerArrowHeight_a,
                    negative * innerArrowHeight_b,
                    calculateColor((float) (innerWheelPointerAngle - m90d), BW),
                    innerArrowPath,
                    innerArrowPaint);
        }

        drawMarkers(canvas, innerWheelRadius, innerWheelWidth / 6,
                BrightnessMarkers, true);


    }

    private void drawArrow(Canvas canvas, PointerTop top, double topAngle,
                           float wheelRadius, float wheelWidth, double arrowOffset,
                           double arrowHeight_a, double arrowHeight_b,
                           int color, Path arrowPath, Paint arrowPaint) {

        top.X = Math.cos(topAngle) * (wheelRadius + wheelWidth / 2 + arrowOffset);
        top.Y = Math.sin(topAngle) * (wheelRadius + wheelWidth / 2 + arrowOffset);

        top.Height = Math.abs(arrowHeight_a + arrowHeight_b);

        double arrowRad = Math.toRadians(40);

        double leftX = top.X + Math.cos(topAngle + arrowRad) * arrowHeight_a;
        double leftY = top.Y + Math.sin(topAngle + arrowRad) * arrowHeight_a;

        double rightX = top.X + Math.cos(topAngle - arrowRad) * arrowHeight_a;
        double rightY = top.Y + Math.sin(topAngle - arrowRad) * arrowHeight_a;

        double backLeftX = leftX + Math.cos(topAngle) * arrowHeight_b;
        double backLeftY = leftY + Math.sin(topAngle) * arrowHeight_b;

        double backRightX = rightX + Math.cos(topAngle) * arrowHeight_b;
        double backRightY = rightY + Math.sin(topAngle) * arrowHeight_b;

        arrowPath.reset();
        arrowPath.moveTo((float) top.X, (float) top.Y);
        arrowPath.lineTo((float) leftX, (float) leftY);
        arrowPath.lineTo((float) backLeftX, (float) backLeftY);

        arrowPath.moveTo((float) top.X, (float) top.Y);
        arrowPath.lineTo((float) rightX, (float) rightY);
        arrowPath.lineTo((float) backRightX, (float) backRightY);
        arrowPath.lineTo((float) backLeftX, (float) backLeftY);

        arrowPaint.setColor(color);
        arrowPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowPath, arrowPaint);

        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeJoin(Paint.Join.ROUND);
        arrowPaint.setColor(Color.BLACK);
        canvas.drawPath(arrowPath, arrowPaint);


    }

    private void setBWcolor() {
        int color = colorWheelVisible && colorfulBrightnessWheel ? selectedColor : Color.WHITE;

        if (BW[1] != color) {
            BW[1] = color;
            BW[2] = color;

            bwShader = new SweepGradient(0, 0, BW, null);
            bwShader.setLocalMatrix(gradientRotationMatrix);
        }
    }

    private void _onSizeChanged() {

        float arrowHeight = 0;
        float wheelWidth = this.getWidth() > this.getHeight() ? this.getHeight() : this.getWidth();

        if (circleInsteadArrow) {
            wheelWidth /= 7.0f;
        } else {
            wheelWidth /= 10.0f;
            arrowHeight = wheelWidth * 0.9f;
        }

        outerWheelWidth = wheelWidth / 2;
        outerArrowHeight_a = arrowHeight;
        outerArrowHeight_b = outerArrowHeight_a * 0.6;
        outerArrowHeight_a -= outerArrowHeight_b;

        innerWheelWidth = outerWheelWidth;
        innerArrowHeight_a = arrowHeight;
        innerArrowHeight_b = innerArrowHeight_a * 0.6;
        innerArrowHeight_a -= innerArrowHeight_b;

        centerX = this.getWidth() / 2;
        centerY = this.getHeight() / 2;

        if (colorWheelVisible && !circleInsteadArrow) {
            outerWheelWidth = wheelWidth / 2;
        } else {
            outerWheelWidth = wheelWidth;
        }

        innerWheelWidth = outerWheelWidth;
        int margin = circleInsteadArrow ? 0 : (int) (arrowHeight);
        outerWheelRadius = Math.min(centerX, centerY) - outerWheelWidth / 2 - margin;

        if (colorWheelVisible) {
            innerWheelRadius = outerWheelRadius - innerWheelWidth;
        } else {
            innerWheelRadius = outerWheelRadius;
        }

        cwPaint.setStrokeWidth(outerWheelWidth);
        bwPaint.setStrokeWidth(innerWheelWidth);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        _onSizeChanged();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private double calculateAngle(double pointerAngle, double inRads) {

        double delta;

        if (Math.abs(lastTouchedAngle - inRads) > Math.PI) {

            delta = 2 * Math.PI - Math.abs(lastTouchedAngle) - Math.abs(inRads);

            if (lastTouchedAngle > 0 && inRads < 0) {
                delta *= -1;
            }

        } else {
            delta = lastTouchedAngle - inRads;
        }

        double result = (pointerAngle - delta);

        if (Math.abs(result) > Math.PI) {

            result = Math.PI - (Math.abs(result) % Math.PI);

            if (lastTouchedAngle < inRads) {
                result *= -1;
            }
        }


        return result;
    }

    private void calculateBrightness() {

        double d = Math.toDegrees(innerWheelPointerAngle) + 90;

        if (d < 0)
            d = d + 360;

        if (d >= 359.99)
            d = 360;

        selectedBrightness = (d / 360) * 100;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX() - centerX;
        float y = event.getY() - centerY;

        double inRads = (float) Math.atan2(y, x);

        int action = event.getAction();

        switch (action) {

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                colorWheelMove = false;
                brightnessWheelMove = false;

                if (mOnChangeListener != null)
                    mOnChangeListener.onChangeFinished();

                break;

            case MotionEvent.ACTION_DOWN:

                double sqrt = Math.sqrt(x * x + y * y);


                if (colorWheelVisible
                        && Math.abs(outerTop.X - x) <= outerTop.Height
                        && Math.abs(outerTop.Y - y) <= outerTop.Height
                        && sqrt >= outerWheelRadius - outerWheelWidth / 2
                        && sqrt <= outerWheelRadius + (outerArrowHeight_a + outerArrowHeight_b) * 2) {

                    colorWheelMove = true;
                    brightnessWheelMove = false;

                } else if (Math.abs(innerTop.X - x) <= innerTop.Height
                        && Math.abs(innerTop.Y - y) <= innerTop.Height
                        && ((colorWheelVisible
                        && sqrt <= innerWheelRadius - innerWheelWidth / 2
                        && sqrt >= innerWheelRadius - (innerArrowHeight_a + innerArrowHeight_b) * 2)
                        || (!colorWheelVisible
                        && sqrt <= innerWheelRadius + (innerArrowHeight_a + innerArrowHeight_b) * 2
                        && sqrt >= innerWheelRadius - innerWheelWidth / 2))) {

                    colorWheelMove = false;
                    brightnessWheelMove = true;
                }


                lastTouchedAngle = inRads;

                if (!isMoving())
                    return super.onTouchEvent(event);

                break;

            case MotionEvent.ACTION_MOVE:

                if (colorWheelMove) {

                    outerWheelPointerAngle = calculateAngle(outerWheelPointerAngle, inRads);

                    int newColor = calculateColor((float) outerWheelPointerAngle, Colors);

                    if (newColor != selectedColor) {

                        setBWcolor();
                        selectedColor = newColor;
                        invalidate();

                        if (mOnChangeListener != null)
                            mOnChangeListener.onColorChanged(this, selectedColor);

                    }

                } else if (brightnessWheelMove) {

                    double newAngle = calculateAngle(innerWheelPointerAngle, inRads);

                    if (newAngle >= m160d
                            && newAngle <= m20d) {

                        if (innerWheelPointerAngle > newAngle) {

                            if (innerWheelPointerAngle >= m90d
                                    && newAngle < m90d) {
                                newAngle = m90d;
                            }

                        } else if (innerWheelPointerAngle < newAngle) {

                            if (innerWheelPointerAngle <= m90_01d
                                    && newAngle > m90_01d) {
                                newAngle = m90_01d;
                            }

                        }

                    }

                    if (innerWheelPointerAngle != newAngle) {

                        innerWheelPointerAngle = newAngle;

                        calculateBrightness();
                        invalidate();

                        if (mOnChangeListener != null)
                            mOnChangeListener.onBrightnessChanged(this, selectedBrightness);

                    }

                }

                lastTouchedAngle = inRads;

                break;
        }

        return true;
    }

    public void setOnChangeListener(OnColorBrightnessChangeListener l) {
        mOnChangeListener = l;
    }

    public int getColor() {
        return selectedColor;
    }

    public void setColor(int color) {

        if ((color & 0xFFFFFF) == 0xFFFFFF)
            color = 0xFFFFFFFF;

        if (selectedColor != color) {
            selectedColor = color;
            outerWheelPointerAngle = colorToAngle(color);

            if (color == Color.WHITE)
                selectedColor = color;
            else
                selectedColor = calculateColor((float) outerWheelPointerAngle, Colors);

            invalidate();
        }
    }

    public boolean getColorWheelVisible() {
        return colorWheelVisible;
    }

    public void setColorWheelVisible(boolean visible) {
        if (visible != colorWheelVisible) {
            colorWheelVisible = visible;
            setBWcolor();
            _onSizeChanged();
            invalidate();
        }
    }

    public double getBrightnessValue() {
        return selectedBrightness;
    }

    public void setBrightnessValue(double value) {

        innerWheelPointerAngle = brightnessToAngle(value);
        selectedBrightness = value;
        invalidate();

    }

    private double brightnessToAngle(double value) {

        double result;

        if (value < 0)
            value = 0;
        else if (value > 100)
            value = 100;


        if (value == 100) {
            result = m90_01d;
        } else {

            double a = 360 * value / 100;

            if (a > 180)
                a -= 360;

            result = Math.toRadians(a) + m90d;
        }

        return result;
    }

    public boolean isMoving() {
        return colorWheelMove || brightnessWheelMove;
    }

    public ArrayList<Double> getColorMarkers() {
        return new ArrayList<>(ColorMarkers);
    }

    public void setColorMarkers(ArrayList<Double> colorMarkers) {
        ColorMarkers = colorMarkers == null ? null : new ArrayList<>(colorMarkers);
        invalidate();
    }

    public ArrayList<Double> getBrightnessMarkers() {
        return new ArrayList<>(BrightnessMarkers);
    }

    public void setBrightnessMarkers(ArrayList<Double> brightnessMarkers) {
        BrightnessMarkers = brightnessMarkers == null ? null : new ArrayList<>(brightnessMarkers);
        invalidate();
    }

    public boolean isColorfulBrightnessWheel() {
        return colorfulBrightnessWheel;
    }

    public void setColorfulBrightnessWheel(boolean colorfulBrightnessWheel) {
        this.colorfulBrightnessWheel = colorfulBrightnessWheel;
        setBWcolor();
        invalidate();
    }

    public boolean isCircleInsteadArrow() {
        return circleInsteadArrow;
    }

    public void setCircleInsteadArrow(boolean circleInsteadArrow) {
        this.circleInsteadArrow = circleInsteadArrow;
        _onSizeChanged();
        invalidate();
    }

    public interface OnColorBrightnessChangeListener {
        void onColorChanged(SuplaColorBrightnessPicker scbPicker, int color);

        void onBrightnessChanged(SuplaColorBrightnessPicker scbPicker, double brightness);

        void onChangeFinished();
    }

    private class PointerTop {
        double X;
        double Y;
        double Height;
    }

}
