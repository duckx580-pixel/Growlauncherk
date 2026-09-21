package com.rtsoft.growtopia;

import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.ListIterator;

public class SharedMultiTouchInput {
    public static SharedActivity app;
    static LinkedList<TouchInfo> listTouches;

    public static class TouchInfo {
        int fingerID;
        public int pointerID;
    }

    public static int GetFingerByPointerID(int pointerId) {
        ListIterator<TouchInfo> it = listTouches.listIterator();
        while (it.hasNext()) {
            TouchInfo ti = it.next();
            if (pointerId == ti.pointerID) return ti.fingerID;
        }
        TouchInfo ti = new TouchInfo();
        ti.pointerID = pointerId;
        ti.fingerID = GetNextAvailableFingerID();
        listTouches.add(ti);
        return ti.fingerID;
    }

    public static int GetNextAvailableFingerID() {
        int id = 0;
        while (id < 12) {
            boolean available = true;
            ListIterator<TouchInfo> it = listTouches.listIterator();
            while (it.hasNext()) {
                if (id == it.next().fingerID) {
                    available = false;
                    break;
                }
            }
            if (available) break;
            id++;
        }
        return id;
    }

    public static boolean OnInput(MotionEvent event) {
        int index = event.getActionIndex();
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                processMouse(0, event.getX(index), event.getY(index), event.getPointerId(index));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                processMouse(1, event.getX(index), event.getY(index), event.getPointerId(index));
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    processMouse(2, event.getX(i), event.getY(i), event.getPointerId(i));
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                listTouches.clear();
                break;
        }
        return true;
    }

    public static void RemoveFinger(int pointerId) {
        ListIterator<TouchInfo> it = listTouches.listIterator();
        while (it.hasNext()) {
            if (pointerId == it.next().pointerID) {
                it.remove();
                return;
            }
        }
    }

    public static void init(SharedActivity activity) {
        app = activity;
        listTouches = new LinkedList<>();
    }

    public static void processMouse(int action, float x, float y, int pointerId) {
        int finger = GetFingerByPointerID(pointerId);
        if (action == 1) {
            RemoveFinger(pointerId);
        }
        if (!Main.ZennKuyRenderer.nativeOnTouch((int) x, (int) y, action)) {
            AppGLSurfaceView.nativeOnTouch(action, x, y, finger);
        }
    }
}
