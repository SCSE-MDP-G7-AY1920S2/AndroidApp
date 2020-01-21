package com.example.mdpandroid;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.constraintlayout.solver.widgets.Rectangle;

import com.example.mdpandroid.entity.Map;
import com.example.mdpandroid.entity.Robot;

import java.text.DecimalFormat;

public class MapDrawer extends View {

    // Some constants for compatibility reasons
    public static final int GRID_DIMEN_TABLET = 36; // large (N7/Acer Tablet)
    public static final int GRID_DIMEN_PHABLET = 50; // regular (Phones with high dpi, P4XL)

    /**
     * helper variables
     */
    public static int gridDimensions = GRID_DIMEN_TABLET;
    private Paint gridPaint;
    private Paint gridPaintBorder;
    private Paint robotPaint;
    private Paint directionPaint;
    private Paint exploredPaint;
    private Paint exploredPaintBorder;

    private Paint wayPointPaint;
    private Paint wayPointPaintBorder;
    private Paint startPointPaint;
    private Paint startPointPaintBorder;
    private Paint endPointPaint;
    private Paint endPointPaintBorder;

    private Paint selectionPaint;
    private Paint selectionPaintBorder;
    private Paint selectionTextPaint;

    private Paint obstaclePaint;
    private Paint obstaclePaintBorder;
    private Paint obstacleTextPaint;

    /**
     * state variables
     */
    private static int Robot_X = Robot.START_POS_X;
    private static int Robot_Y = Robot.START_POS_Y;
    private static int Start_Point_X = Map.START_POINT_X;
    private static int Start_Point_Y = Map.START_POINT_Y;
    private static int Way_Point_X = Map.WAY_POINT_X;
    private static int Way_Point_Y = Map.WAY_POINT_Y;
    private static int End_Point_X = Map.END_POINT_X;
    private static int End_Point_Y = Map.END_POINT_Y;
    private static String direction = Robot.START_DIRECTION;
    private static boolean selectStartPoint = false;
    private static boolean selectWayPoint = false;
    private static String[][] exploredPath = new String[Map.COLUMN][Map.ROW];

    public MapDrawer(Context context){
        this(context, null);
    }

    public MapDrawer(Context context, AttributeSet attrs){
        super(context, attrs);
        Robot_X = Robot.START_POS_X;
        Robot_Y = Robot.START_POS_Y;
        direction = Robot.START_DIRECTION;
        exploredPath = new String[Map.COLUMN][Map.ROW];
        init();
        initMap();
    }

    private static void initMap(){
        for (int i = 0; i < Map.COLUMN; i++){
            for (int j = 0; j < Map.ROW; j++){
                exploredPath[i][j] = "0";
            }
        }
        updateExplored();
    }

    private void init(){
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        robotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        directionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        exploredPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        exploredPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        wayPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wayPointPaintBorder  = new Paint(Paint.ANTI_ALIAS_FLAG);
        startPointPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        startPointPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        endPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        endPointPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        obstaclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        obstaclePaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        obstacleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        gridPaint.setStyle(Paint.Style.FILL);
        gridPaint.setColor(Color.parseColor("#3A96C2"));
        gridPaintBorder.setStyle(Paint.Style.STROKE);
        gridPaintBorder.setColor(Color.parseColor("#eeeeee"));

        robotPaint.setStyle(Paint.Style.FILL);
        robotPaint.setColor(Color.parseColor("#ffeb3b"));

        directionPaint.setStyle(Paint.Style.STROKE);
        directionPaint.setColor(Color.parseColor("#424242"));

        exploredPaint.setStyle(Paint.Style.FILL);
        exploredPaint.setColor(Color.parseColor("#f5f5f5"));
        exploredPaintBorder.setStyle(Paint.Style.STROKE);
        exploredPaintBorder.setColor(Color.parseColor("#3A96C2"));

        wayPointPaint.setStyle(Paint.Style.FILL);
        wayPointPaint.setColor(Color.parseColor("#e53935"));
        wayPointPaintBorder.setStyle(Paint.Style.STROKE);
        wayPointPaintBorder.setColor(Color.parseColor("#f5f5f5"));

        startPointPaint.setStyle(Paint.Style.FILL);
        startPointPaint.setColor(Color.parseColor("#607d8b"));
        startPointPaintBorder.setStyle(Paint.Style.STROKE);
        startPointPaintBorder.setColor(Color.parseColor("#f5f5f5"));

        endPointPaint.setStyle(Paint.Style.FILL);
        endPointPaint.setColor(Color.parseColor("#009688"));
        endPointPaintBorder.setStyle(Paint.Style.STROKE);
        endPointPaintBorder.setColor(Color.parseColor("#f5f5f5"));

        selectionPaint.setStyle(Paint.Style.FILL);
        selectionPaint.setColor(Color.parseColor("#9ccc65"));
        selectionPaintBorder.setStyle(Paint.Style.STROKE);
        selectionPaintBorder.setColor(Color.parseColor("#f5f5f5"));

        selectionTextPaint.setStyle(Paint.Style.STROKE);
        selectionTextPaint.setColor(Color.parseColor("#f5f5f5"));
        selectionTextPaint.setTextSize(100);

        obstaclePaint.setStyle(Paint.Style.FILL);
        obstaclePaint.setColor(Color.parseColor("#212121"));
        obstaclePaintBorder.setStyle(Paint.Style.STROKE);
        obstaclePaintBorder.setColor(Color.parseColor("#f5f5f5"));

        obstacleTextPaint.setStyle(Paint.Style.STROKE);
        obstacleTextPaint.setColor(Color.parseColor("#f5f5f5"));
        obstacleTextPaint.setTextSize(22);

        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        gridDimensions = (screenSize < Configuration.SCREENLAYOUT_SIZE_LARGE) ? GRID_DIMEN_PHABLET : GRID_DIMEN_TABLET;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        System.out.println("DRAWING GRID MAP");

        if (!selectWayPoint && !selectStartPoint) {
            drawMap(canvas);
            drawExploredMap(canvas);
            drawStartPoint(canvas);
            drawEndPoint(canvas);
            drawWayPoint(canvas);
            drawRobot(canvas);
        } else {
            drawMap(canvas);
            drawStartPoint(canvas);
            drawEndPoint(canvas);
            drawSelectionMap(canvas);

            if (selectWayPoint){
                drawWayPoint(canvas);
            } else if (selectStartPoint){
                drawRobot(canvas);
            }
        }
    }

    private void drawMap(Canvas canvas){
        for (int i = 0; i < Map.COLUMN; i++){
            for (int j = 0; j < Map.ROW; j++){
                int left = (int) (i * gridDimensions);
                int top = (int) (j * gridDimensions);

                Rect rectangle = new Rect(left, top, left + gridDimensions, top + gridDimensions);
                canvas.drawRect(rectangle, gridPaint);
                canvas.drawRect(rectangle, gridPaintBorder);
            }
        }
    }

    private void drawExploredMap(Canvas canvas){
        for (int i = 0; i < Map.COLUMN; i++){
            for (int j = 0; j < Map.ROW; j++){
                int left = (int) (i * gridDimensions);
                int top = (int) (j * gridDimensions);

                if (exploredPath[i][j].equals("1")){
                    Rect rectangle = new Rect(left, top, left + gridDimensions, top + gridDimensions);
                    canvas.drawRect(rectangle, exploredPaint);
                    canvas.drawRect(rectangle, exploredPaintBorder);
                } else if (!exploredPath[i][j].equals("0")){
                    System.out.println(exploredPath[i][j]);
                    drawObstacles(canvas, left, top, exploredPath[i][j]);
                }
            }
        }
    }

    private void drawRobot(Canvas canvas){
        float grid_x = ((Robot_X * gridDimensions) + ((Robot_X + 1) * gridDimensions)) / 2;
        float grid_y = ((Robot_Y * gridDimensions) + ((Robot_Y + 1) * gridDimensions)) / 2;
        canvas.drawCircle(grid_x, grid_y, (gridDimensions * 3 / 2), robotPaint);
        if (direction.equals("Right")){
            canvas.drawLine(grid_x ,grid_y ,((Robot_X + 2) * gridDimensions), grid_y, directionPaint);
        } else if (direction.equals("Left")){
            canvas.drawLine(grid_x ,grid_y ,((Robot_X - 1) * gridDimensions), grid_y, directionPaint);
        } else if (direction.equals("Up")){
            canvas.drawLine(grid_x ,grid_y ,grid_x, ((Robot_Y - 1) * gridDimensions), directionPaint);
        } else if (direction.equals("Down")){
            canvas.drawLine(grid_x ,grid_y ,grid_x, ((Robot_Y + 2) * gridDimensions), directionPaint);
        }
    }

    private void drawStartPoint(Canvas canvas){
        int left = Start_Point_X * gridDimensions;
        int top = Start_Point_Y * gridDimensions;
        Rect rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, startPointPaint);
        canvas.drawRect(rect, startPointPaintBorder);

        left = (Start_Point_X + 1) * gridDimensions;
        top = Start_Point_Y * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, startPointPaint);
        canvas.drawRect(rect, startPointPaintBorder);

        left = (Start_Point_X - 1) * gridDimensions;
        top = Start_Point_Y * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, startPointPaint);
        canvas.drawRect(rect, startPointPaintBorder);

        left = Start_Point_X * gridDimensions;
        top = (Start_Point_Y + 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, startPointPaint);
        canvas.drawRect(rect, startPointPaintBorder);

        left = Start_Point_X * gridDimensions;
        top = (Start_Point_Y - 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, startPointPaint);
        canvas.drawRect(rect, startPointPaintBorder);

        left = (Start_Point_X - 1) * gridDimensions;
        top = (Start_Point_Y + 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, startPointPaint);
        canvas.drawRect(rect, startPointPaintBorder);

        left = (Start_Point_X - 1) * gridDimensions;
        top = (Start_Point_Y - 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, startPointPaint);
        canvas.drawRect(rect, startPointPaintBorder);

        left = (Start_Point_X + 1) * gridDimensions;
        top = (Start_Point_Y + 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, startPointPaint);
        canvas.drawRect(rect, startPointPaintBorder);

        left = (Start_Point_X + 1) * gridDimensions;
        top = (Start_Point_Y - 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, startPointPaint);
        canvas.drawRect(rect, startPointPaintBorder);
    }

    private void drawEndPoint(Canvas canvas){
        int left = End_Point_X * gridDimensions;
        int top = End_Point_Y * gridDimensions;
        Rect rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, endPointPaint);
        canvas.drawRect(rect, endPointPaintBorder);

        left = (End_Point_X + 1) * gridDimensions;
        top = End_Point_Y * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, endPointPaint);
        canvas.drawRect(rect, endPointPaintBorder);

        left = (End_Point_X - 1) * gridDimensions;
        top = End_Point_Y * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, endPointPaint);
        canvas.drawRect(rect, endPointPaintBorder);

        left = End_Point_X * gridDimensions;
        top = (End_Point_Y + 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, endPointPaint);
        canvas.drawRect(rect, endPointPaintBorder);

        left = End_Point_X * gridDimensions;
        top = (End_Point_Y - 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, endPointPaint);
        canvas.drawRect(rect, endPointPaintBorder);

        left = (End_Point_X - 1) * gridDimensions;
        top = (End_Point_Y + 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, endPointPaint);
        canvas.drawRect(rect, endPointPaintBorder);

        left = (End_Point_X - 1) * gridDimensions;
        top = (End_Point_Y - 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, endPointPaint);
        canvas.drawRect(rect, endPointPaintBorder);

        left = (End_Point_X + 1) * gridDimensions;
        top = (End_Point_Y + 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, endPointPaint);
        canvas.drawRect(rect, endPointPaintBorder);

        left = (End_Point_X + 1) * gridDimensions;
        top = (End_Point_Y - 1) * gridDimensions;
        rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, endPointPaint);
        canvas.drawRect(rect, endPointPaintBorder);
    }

    private void drawWayPoint(Canvas canvas){
        int left = Way_Point_X * gridDimensions;
        int top = Way_Point_Y * gridDimensions;
        Rect rect = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        canvas.drawRect(rect, wayPointPaint);
        canvas.drawRect(rect, wayPointPaintBorder);
    }

    private void drawSelectionMap(Canvas canvas){
        for (int i = 1; i < Map.VIRTUAL_COLUMN; i++){
            for (int j = 1; j < Map.VIRTUAL_ROW; j++){
                int left = (int) (i * gridDimensions);
                int top = (int) (j * gridDimensions);

                if (isSurroundingObstacle(i, j)) {
                    Rect rectangle = new Rect(left, top, left + gridDimensions, top + gridDimensions);
                    canvas.drawRect(rectangle, selectionPaint);
                    canvas.drawRect(rectangle, selectionPaintBorder);
                }
            }
        }

        int left = 3 * gridDimensions;
        int top = 9 * gridDimensions;
        if (getSelectWayPoint()){
            canvas.drawText("WAY", left, top, selectionTextPaint);
        } else if (getSelectStartPoint()){
            canvas.drawText("START", left, top, selectionTextPaint);
        }

        top = 12 * gridDimensions;
        canvas.drawText("POINT", left, top, selectionTextPaint);
    }

    private void drawObstacles(Canvas canvas, int left, int top, String obstacle){
        Rect rectangle = new Rect(left, top, left + gridDimensions, top + gridDimensions);
        top = top + 25;
        left = left + 6;

        switch (obstacle) {
            case "A":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("01", left, top, obstacleTextPaint);
                break;
            case "B":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("02", left, top, obstacleTextPaint);
                break;
            case "C":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("03", left, top, obstacleTextPaint);
                break;
            case "D":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("04", left, top, obstacleTextPaint);
                break;
            case "E":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("05", left, top, obstacleTextPaint);
                break;
            case "F":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("06", left, top, obstacleTextPaint);
                break;
            case "G":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("07", left, top, obstacleTextPaint);
                break;
            case "H":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("08", left, top, obstacleTextPaint);
                break;
            case "I":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("09", left, top, obstacleTextPaint);
                break;
            case "J":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("10", left, top, obstacleTextPaint);
                break;
            case "K":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("11", left, top, obstacleTextPaint);
                break;
            case "L":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("12", left, top, obstacleTextPaint);
                break;
            case "M":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("13", left, top, obstacleTextPaint);
                break;
            case "N":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("14", left, top, obstacleTextPaint);
                break;
            case "P":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                canvas.drawText("15", left, top, obstacleTextPaint);
                break;
            case "O":
                canvas.drawRect(rectangle, obstaclePaint);
                canvas.drawRect(rectangle, obstaclePaintBorder);
                break;
        }
    }

    public static void moveRight(){
        if (direction.equals("Right")){
            direction = "Down";
        } else if (direction.equals("Left")){
            direction = "Up";
        } else if (direction.equals("Up")){
            direction = "Right";
        } else if (direction.equals("Down")){
            direction = "Left";
        }
    }

    public static void moveLeft(){
        if (direction.equals("Right")){
            direction = "Up";
        } else if (direction.equals("Left")){
            direction = "Down";
        } else if (direction.equals("Up")){
            direction = "Left";
        } else if (direction.equals("Down")){
            direction = "Right";
        }
    }

    public static void moveUp(){
        if (direction.equals("Right")){
            if (Robot_X + 1 != Map.VIRTUAL_COLUMN && isSurroundingObstacle(Robot_X + 1, Robot_Y)){
                Robot_X++;
            }
        } else if (direction.equals("Left") && isSurroundingObstacle(Robot_X - 1, Robot_Y)){
            if (Robot_X - 1 != 0){
                Robot_X--;
            }
        } else if (direction.equals("Up") && isSurroundingObstacle(Robot_X, Robot_Y - 1)){
            if (Robot_Y - 1 != 0){
                Robot_Y--;
            }
        } else if (direction.equals("Down") && isSurroundingObstacle(Robot_X, Robot_Y + 1)){
            if (Robot_Y + 1 != Map.VIRTUAL_ROW){
                Robot_Y++;
            }
        }
        updateExplored();
    }

    public static void updateCoordinates(int x_axis, int y_axis, String dir){
        if (!validMidpoint(x_axis, y_axis)) return;
        System.out.print("X Axis : " + x_axis);
        System.out.print("Y Axis : " + y_axis);
        int new_y_axis = invertYAxis(y_axis);

        Robot_X = x_axis;
        Robot_Y = new_y_axis;
        System.out.println(Robot_X + ", " + Robot_Y);

        switch(dir){
            case "UP":
                direction = "Up";
                break;
            case "DOWN":
                direction = "Down";
                break;
            case "LEFT":
                direction = "Left";
                break;
            case "RIGHT":
                direction = "Right";
                break;
        }

        updateExplored();
    }

    public static void updateImage(char imgID, int x_axis, int y_axis){
        System.out.println("Image ID : " + imgID);
        System.out.println("X Axis : " + x_axis);
        System.out.println("Y Axis : " + y_axis);
        int new_y_axis = invertYAxis(y_axis);

        boolean flag = !exploredPath[x_axis][new_y_axis].equals("0") && !exploredPath[x_axis][new_y_axis].equals("1");
        try {
            switch (imgID) {
                case '0':
                    exploredPath[x_axis][new_y_axis] = "O";
                    break;
                case '1':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "A";
                    break;
                case '2':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "B";
                    break;
                case '3':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "C";
                    break;
                case '4':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "D";
                    break;
                case '5':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "E";
                    break;
                case '6':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "F";
                    break;
                case '7':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "G";
                    break;
                case '8':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "H";
                    break;
                case '9':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "I";
                    break;
                case 'A':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "J";
                    break;
                case 'B':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "K";
                    break;
                case 'C':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "L";
                    break;
                case 'D':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "M";
                    break;
                case 'E':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "N";
                    break;
                case 'F':
                    if (flag) return;
                    exploredPath[x_axis][new_y_axis] = "P";
                    break;
            }
        } catch (IndexOutOfBoundsException indexEx){
            System.out.println("Invalid index for array");
        }
    }

    public static void updateSelection(int x_axis, int y_axis){
        if (isSurroundingObstacle(x_axis, y_axis)) {
            if (selectStartPoint) {
                Robot_X = x_axis;
                Robot_Y = y_axis;
            } else if (selectWayPoint) {
                Way_Point_X = x_axis;
                Way_Point_Y = y_axis;
            }
        }
    }

    private static void updateExplored(){
        exploredPath[Robot_X][Robot_Y] = "1";
        exploredPath[Robot_X - 1][Robot_Y] = "1";
        exploredPath[Robot_X + 1][Robot_Y] = "1";

        exploredPath[Robot_X][Robot_Y + 1] = "1";
        exploredPath[Robot_X][Robot_Y - 1] = "1";

        exploredPath[Robot_X - 1][Robot_Y - 1] = "1";
        exploredPath[Robot_X - 1][Robot_Y + 1] = "1";

        exploredPath[Robot_X + 1][Robot_Y - 1] = "1";
        exploredPath[Robot_X + 1][Robot_Y + 1] = "1";
    }

    public static boolean validMidpoint(int x_axis, int y_axis){
        return (x_axis >= 1 && x_axis < Map.VIRTUAL_COLUMN) && (y_axis >= 1 && y_axis < Map.VIRTUAL_ROW);
    }

    public static void setGrid(String[][] exploredMap){
        for (int i = 0; i < Map.ROW; i++){
            for (int j = 0; j < Map.COLUMN; j++){
                exploredPath[j][i] = exploredMap[j][invertYAxis(i)];
            }
        }
    }

    public static int invertYAxis(int y_axis){
        return (Map.VIRTUAL_ROW - y_axis);
    }

    public static boolean getSelectWayPoint(){
        return selectWayPoint;
    }

    public static boolean getSelectStartPoint(){
        return selectStartPoint;
    }

    public static void setSelectWayPoint(){
        selectWayPoint = !selectWayPoint;
    }

    public static void setSelectStartPoint(){
        selectStartPoint = !selectStartPoint;
    }

    public static void updateStartPoint(){ updateExplored(); }

    public static String getRobotPosition(){
        return Robot_X + "," + invertYAxis(Robot_Y);
    }

    public static String getWayPoint(){
        return Way_Point_X + "," + invertYAxis(Way_Point_Y);
    }

    public static int getWay_Point_X(){
        return Way_Point_X;
    }

    public static int getWay_Point_Y(){
        return invertYAxis(Way_Point_Y);
    }

    public static String getStartPoint(){ return Start_Point_X + "," + invertYAxis(Start_Point_Y); }

    public static int getStart_Point_X(){
        return Start_Point_X;
    }

    public static int getStart_Point_Y(){
        return invertYAxis(Start_Point_Y);
    }

    public static int getRobotX(){
        return Robot_X;
    }

    public static int getRobotY(){
        return Robot_Y;
    }

    public static void resetMap(){
        initMap();
    }

    public static boolean isSurroundingObstacle(int x_axis, int y_axis){
        // just make sure there is no surround obstacles, inclusive of itself
        if (!validMidpoint(x_axis, y_axis))
            return false;

        if (!exploredPath[x_axis][y_axis].equals("1") && !exploredPath[x_axis][y_axis].equals("0"))
            return false;
        else if (!exploredPath[x_axis][y_axis + 1].equals("1") && !exploredPath[x_axis][y_axis + 1].equals("0"))
            return false;
        else if (!exploredPath[x_axis][y_axis - 1].equals("1") && !exploredPath[x_axis][y_axis - 1].equals("0"))
            return false;
        else if (!exploredPath[x_axis + 1][y_axis].equals("1") && !exploredPath[x_axis + 1][y_axis].equals("0"))
            return false;
        else if (!exploredPath[x_axis + 1][y_axis + 1].equals("1") && !exploredPath[x_axis + 1][y_axis + 1].equals("0"))
            return false;
        else if (!exploredPath[x_axis + 1][y_axis - 1].equals("1") && !exploredPath[x_axis + 1][y_axis - 1].equals("0"))
            return false;
        else if (!exploredPath[x_axis - 1][y_axis].equals("1") && !exploredPath[x_axis - 1][y_axis].equals("0"))
            return false;
        else if (!exploredPath[x_axis - 1][y_axis + 1].equals("1") && !exploredPath[x_axis - 1][y_axis + 1].equals("0"))
            return false;
        else if (!exploredPath[x_axis - 1][y_axis - 1].equals("1") && !exploredPath[x_axis - 1][y_axis - 1].equals("0"))
            return false;
        else
            return true;
    }
}
